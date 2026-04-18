package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthChallenge;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthScope;
import com.github.liyibo1110.hc.client5.http.auth.AuthenticationException;
import com.github.liyibo1110.hc.client5.http.auth.Credentials;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.client5.http.auth.MalformedChallengeException;
import com.github.liyibo1110.hc.client5.http.auth.NTCredentials;
import com.github.liyibo1110.hc.client5.http.auth.StandardAuthScheme;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

/**
 * NTLM是微软开发的一种专有身份验证方案，专为Windows平台进行了优化。
 * @author liyibo
 * @date 2026-04-17 17:08
 */
public final class NTLMScheme implements AuthScheme {
    private static final Logger LOG = LoggerFactory.getLogger(NTLMScheme.class);

    enum State {
        UNINITIATED,
        CHALLENGE_RECEIVED,
        MSG_TYPE1_GENERATED,
        MSG_TYPE2_RECEIVED,
        MSG_TYPE3_GENERATED,
        FAILED,
    }

    private final NTLMEngine engine;

    private State state;
    private String challenge;
    private NTCredentials credentials;

    public NTLMScheme(final NTLMEngine engine) {
        super();
        Args.notNull(engine, "NTLM engine");
        this.engine = engine;
        this.state = State.UNINITIATED;
    }

    /**
     * @since 4.3
     */
    public NTLMScheme() {
        this(new NTLMEngineImpl());
    }

    @Override
    public String getName() {
        return StandardAuthScheme.NTLM;
    }

    @Override
    public boolean isConnectionBased() {
        return true;
    }

    @Override
    public String getRealm() {
        return null;
    }

    @Override
    public void processChallenge(final AuthChallenge authChallenge, final HttpContext context) throws MalformedChallengeException {
        Args.notNull(authChallenge, "AuthChallenge");

        this.challenge = authChallenge.getValue();
        if (this.challenge == null || this.challenge.isEmpty()) {
            if (this.state == State.UNINITIATED)
                this.state = State.CHALLENGE_RECEIVED;
            else
                this.state = State.FAILED;
        } else {
            if (this.state.compareTo(State.MSG_TYPE1_GENERATED) < 0) {
                this.state = State.FAILED;
                throw new MalformedChallengeException("Out of sequence NTLM response message");
            } else if (this.state == State.MSG_TYPE1_GENERATED) {
                this.state = State.MSG_TYPE2_RECEIVED;
            }
        }
    }

    @Override
    public boolean isResponseReady(final HttpHost host, final CredentialsProvider credentialsProvider, final HttpContext context)
            throws AuthenticationException {
        Args.notNull(host, "Auth host");
        Args.notNull(credentialsProvider, "CredentialsProvider");

        final AuthScope authScope = new AuthScope(host, null, getName());
        final Credentials credentials = credentialsProvider.getCredentials(authScope, context);
        if (credentials instanceof NTCredentials) {
            this.credentials = (NTCredentials) credentials;
            return true;
        }

        if (LOG.isDebugEnabled()) {
            final HttpClientContext clientContext = HttpClientContext.adapt(context);
            final String exchangeId = clientContext.getExchangeId();
            LOG.debug("{} No credentials found for auth scope [{}]", exchangeId, authScope);
        }
        return false;
    }

    @Override
    public Principal getPrincipal() {
        return this.credentials != null ? this.credentials.getUserPrincipal() : null;
    }

    @Override
    public String generateAuthResponse(final HttpHost host, final HttpRequest request, final HttpContext context)
            throws AuthenticationException {
        if (this.credentials == null)
            throw new AuthenticationException("NT credentials not available");

        final String response;
        if (this.state == State.FAILED) {
            throw new AuthenticationException("NTLM authentication failed");
        } else if (this.state == State.CHALLENGE_RECEIVED) {
            response = this.engine.generateType1Msg(
                    this.credentials.getNetbiosDomain(),
                    this.credentials.getWorkstation());
            this.state = State.MSG_TYPE1_GENERATED;
        } else if (this.state == State.MSG_TYPE2_RECEIVED) {
            response = this.engine.generateType3Msg(
                    this.credentials.getUserName(),
                    this.credentials.getPassword(),
                    this.credentials.getNetbiosDomain(),
                    this.credentials.getWorkstation(),
                    this.challenge);
            this.state = State.MSG_TYPE3_GENERATED;
        } else {
            throw new AuthenticationException("Unexpected state: " + this.state);
        }
        return StandardAuthScheme.NTLM + " " + response;
    }

    @Override
    public boolean isChallengeComplete() {
        return this.state == State.MSG_TYPE3_GENERATED || this.state == State.FAILED;
    }

    @Override
    public String toString() {
        return getName() + "{" + this.state + " " + challenge + '}';
    }
}
