package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.DnsResolver;
import com.github.liyibo1110.hc.client5.http.SystemDefaultDnsResolver;
import com.github.liyibo1110.hc.client5.http.auth.AuthChallenge;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthScope;
import com.github.liyibo1110.hc.client5.http.auth.AuthenticationException;
import com.github.liyibo1110.hc.client5.http.auth.Credentials;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.client5.http.auth.InvalidCredentialsException;
import com.github.liyibo1110.hc.client5.http.auth.KerberosConfig;
import com.github.liyibo1110.hc.client5.http.auth.KerberosCredentials;
import com.github.liyibo1110.hc.client5.http.auth.MalformedChallengeException;
import com.github.liyibo1110.hc.client5.http.auth.StandardAuthScheme;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.client5.http.utils.Base64;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.security.Principal;

/**
 * 基于GSS的身份验证方案的常见行为。
 * @author liyibo
 * @date 2026-04-17 17:11
 */
public abstract class GGSSchemeBase implements AuthScheme {

    enum State {
        UNINITIATED,
        CHALLENGE_RECEIVED,
        TOKEN_GENERATED,
        FAILED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(GGSSchemeBase.class);
    private static final String NO_TOKEN = "";
    private static final String KERBEROS_SCHEME = "HTTP";
    private final KerberosConfig config;
    private final DnsResolver dnsResolver;

    /** Authentication process state */
    private State state;
    private GSSCredential gssCredential;
    private String challenge;
    private byte[] token;

    GGSSchemeBase(final KerberosConfig config, final DnsResolver dnsResolver) {
        super();
        this.config = config != null ? config : KerberosConfig.DEFAULT;
        this.dnsResolver = dnsResolver != null ? dnsResolver : SystemDefaultDnsResolver.INSTANCE;
        this.state = State.UNINITIATED;
    }

    GGSSchemeBase(final KerberosConfig config) {
        this(config, SystemDefaultDnsResolver.INSTANCE);
    }

    GGSSchemeBase() {
        this(KerberosConfig.DEFAULT, SystemDefaultDnsResolver.INSTANCE);
    }

    @Override
    public String getRealm() {
        return null;
    }

    @Override
    public void processChallenge(
            final AuthChallenge authChallenge,
            final HttpContext context) throws MalformedChallengeException {
        Args.notNull(authChallenge, "AuthChallenge");

        this.challenge = authChallenge.getValue() != null ? authChallenge.getValue() : NO_TOKEN;

        if (state == State.UNINITIATED) {
            token = Base64.decodeBase64(challenge.getBytes());
            state = State.CHALLENGE_RECEIVED;
        } else {
            if (LOG.isDebugEnabled()) {
                final HttpClientContext clientContext = HttpClientContext.adapt(context);
                final String exchangeId = clientContext.getExchangeId();
                LOG.debug("{} Authentication already attempted", exchangeId);
            }
            state = State.FAILED;
        }
    }

    protected GSSManager getManager() {
        return GSSManager.getInstance();
    }

    /**
     * @since 4.4
     */
    protected byte[] generateGSSToken(
            final byte[] input, final Oid oid, final String serviceName, final String authServer) throws GSSException {
        final GSSManager manager = getManager();
        final GSSName serverName = manager.createName(serviceName + "@" + authServer, GSSName.NT_HOSTBASED_SERVICE);

        final GSSContext gssContext = createGSSContext(manager, oid, serverName, gssCredential);
        if (input != null) {
            return gssContext.initSecContext(input, 0, input.length);
        } else {
            return gssContext.initSecContext(new byte[] {}, 0, 0);
        }
    }

    /**
     * @since 5.0
     */
    protected GSSContext createGSSContext(
            final GSSManager manager,
            final Oid oid,
            final GSSName serverName,
            final GSSCredential gssCredential) throws GSSException {
        final GSSContext gssContext = manager.createContext(serverName.canonicalize(oid), oid, gssCredential,
                GSSContext.DEFAULT_LIFETIME);
        gssContext.requestMutualAuth(true);
        if (config.getRequestDelegCreds() != KerberosConfig.Option.DEFAULT) {
            gssContext.requestCredDeleg(config.getRequestDelegCreds() == KerberosConfig.Option.ENABLE);
        }
        return gssContext;
    }
    /**
     * @since 4.4
     */
    protected abstract byte[] generateToken(byte[] input, String serviceName, String authServer) throws GSSException;

    @Override
    public boolean isChallengeComplete() {
        return this.state == State.TOKEN_GENERATED || this.state == State.FAILED;
    }

    @Override
    public boolean isResponseReady(
            final HttpHost host,
            final CredentialsProvider credentialsProvider,
            final HttpContext context) throws AuthenticationException {

        Args.notNull(host, "Auth host");
        Args.notNull(credentialsProvider, "CredentialsProvider");

        final Credentials credentials = credentialsProvider.getCredentials(
                new AuthScope(host, null, getName()), context);
        if (credentials instanceof KerberosCredentials) {
            this.gssCredential = ((KerberosCredentials) credentials).getGSSCredential();
        } else {
            this.gssCredential = null;
        }
        return true;
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public String generateAuthResponse(
            final HttpHost host,
            final HttpRequest request,
            final HttpContext context) throws AuthenticationException {
        Args.notNull(host, "HTTP host");
        Args.notNull(request, "HTTP request");
        switch (state) {
            case UNINITIATED:
                throw new AuthenticationException(getName() + " authentication has not been initiated");
            case FAILED:
                throw new AuthenticationException(getName() + " authentication has failed");
            case CHALLENGE_RECEIVED:
                try {
                    final String authServer;
                    String hostname = host.getHostName();
                    if (config.getUseCanonicalHostname() != KerberosConfig.Option.DISABLE){
                        try {
                            hostname = dnsResolver.resolveCanonicalHostname(host.getHostName());
                        } catch (final UnknownHostException ignore){
                        }
                    }
                    if (config.getStripPort() != KerberosConfig.Option.DISABLE) {
                        authServer = hostname;
                    } else {
                        authServer = hostname + ":" + host.getPort();
                    }

                    if (LOG.isDebugEnabled()) {
                        final HttpClientContext clientContext = HttpClientContext.adapt(context);
                        final String exchangeId = clientContext.getExchangeId();
                        LOG.debug("{} init {}", exchangeId, authServer);
                    }
                    token = generateToken(token, KERBEROS_SCHEME, authServer);
                    state = State.TOKEN_GENERATED;
                } catch (final GSSException gsse) {
                    state = State.FAILED;
                    if (gsse.getMajor() == GSSException.DEFECTIVE_CREDENTIAL
                            || gsse.getMajor() == GSSException.CREDENTIALS_EXPIRED) {
                        throw new InvalidCredentialsException(gsse.getMessage(), gsse);
                    }
                    if (gsse.getMajor() == GSSException.NO_CRED ) {
                        throw new InvalidCredentialsException(gsse.getMessage(), gsse);
                    }
                    if (gsse.getMajor() == GSSException.DEFECTIVE_TOKEN
                            || gsse.getMajor() == GSSException.DUPLICATE_TOKEN
                            || gsse.getMajor() == GSSException.OLD_TOKEN) {
                        throw new AuthenticationException(gsse.getMessage(), gsse);
                    }
                    // other error
                    throw new AuthenticationException(gsse.getMessage());
                }
            case TOKEN_GENERATED:
                final Base64 codec = new Base64(0);
                final String tokenstr = new String(codec.encode(token));
                if (LOG.isDebugEnabled()) {
                    final HttpClientContext clientContext = HttpClientContext.adapt(context);
                    final String exchangeId = clientContext.getExchangeId();
                    LOG.debug("{} Sending response '{}' back to the auth server", exchangeId, tokenstr);
                }
                return StandardAuthScheme.SPNEGO + " " + tokenstr;
            default:
                throw new IllegalStateException("Illegal state: " + state);
        }
    }

    @Override
    public String toString() {
        return getName() + "{" + this.state + " " + challenge + '}';
    }
}
