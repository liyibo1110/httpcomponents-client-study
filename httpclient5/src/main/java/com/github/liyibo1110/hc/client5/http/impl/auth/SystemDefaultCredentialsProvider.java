package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthScope;
import com.github.liyibo1110.hc.client5.http.auth.Credentials;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsStore;
import com.github.liyibo1110.hc.client5.http.auth.NTCredentials;
import com.github.liyibo1110.hc.client5.http.auth.StandardAuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.UsernamePasswordCredentials;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.URIScheme;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * 基于standard JRE Authenticator的CredentialsStore实现
 * @author liyibo
 * @date 2026-04-17 23:24
 */
@Contract(threading = ThreadingBehavior.SAFE)
public class SystemDefaultCredentialsProvider implements CredentialsStore {

    private final BasicCredentialsProvider internal;

    public SystemDefaultCredentialsProvider() {
        super();
        this.internal = new BasicCredentialsProvider();
    }

    @Override
    public void setCredentials(final AuthScope authScope, final Credentials credentials) {
        internal.setCredentials(authScope, credentials);
    }

    private static PasswordAuthentication getSystemCreds(
            final String protocol,
            final AuthScope authScope,
            final Authenticator.RequestorType requestorType,
            final HttpClientContext context) {
        final HttpRequest request = context != null ? context.getRequest() : null;
        URL targetHostURL;
        try {
            final URI uri = request != null ? request.getUri() : null;
            targetHostURL = uri != null ? uri.toURL() : null;
        } catch (final URISyntaxException | MalformedURLException ignore) {
            targetHostURL = null;
        }
        // use null addr, because the authentication fails if it does not exactly match the expected realm's host
        return Authenticator.requestPasswordAuthentication(
                authScope.getHost(),
                null,
                authScope.getPort(),
                protocol,
                authScope.getRealm(),
                authScope.getSchemeName(),
                targetHostURL,
                requestorType);
    }

    @Override
    public Credentials getCredentials(final AuthScope authScope, final HttpContext context) {
        Args.notNull(authScope, "Auth scope");
        final Credentials localcreds = internal.getCredentials(authScope, context);
        if (localcreds != null)
            return localcreds;

        final String host = authScope.getHost();
        if (host != null) {
            final HttpClientContext clientContext = context != null ? HttpClientContext.adapt(context) : null;
            final String protocol = authScope.getProtocol() != null ? authScope.getProtocol() : (authScope.getPort() == 443 ? URIScheme.HTTPS.id : URIScheme.HTTP.id);
            PasswordAuthentication systemcreds = getSystemCreds(protocol, authScope, Authenticator.RequestorType.SERVER, clientContext);
            if (systemcreds == null)
                systemcreds = getSystemCreds(protocol, authScope, Authenticator.RequestorType.PROXY, clientContext);

            if (systemcreds == null) {
                // Look for values given using http.proxyUser/http.proxyPassword or
                // https.proxyUser/https.proxyPassword. We cannot simply use the protocol from
                // the origin since a proxy retrieved from https.proxyHost/https.proxyPort will
                // still use http as protocol
                systemcreds = getProxyCredentials(URIScheme.HTTP.getId(), authScope);
                if (systemcreds == null)
                    systemcreds = getProxyCredentials(URIScheme.HTTPS.getId(), authScope);
            }
            if (systemcreds != null) {
                final String domain = System.getProperty("http.auth.ntlm.domain");
                if (domain != null)
                    return new NTCredentials(systemcreds.getUserName(), systemcreds.getPassword(), null, domain);

                if (StandardAuthScheme.NTLM.equalsIgnoreCase(authScope.getSchemeName())) {
                    // Domain may be specified in a fully qualified user name
                    return new NTCredentials(systemcreds.getUserName(), systemcreds.getPassword(), null, null);
                }
                return new UsernamePasswordCredentials(systemcreds.getUserName(), systemcreds.getPassword());
            }
        }
        return null;
    }

    private static PasswordAuthentication getProxyCredentials(final String protocol, final AuthScope authScope) {
        final String proxyHost = System.getProperty(protocol + ".proxyHost");
        if (proxyHost == null)
            return null;

        final String proxyPort = System.getProperty(protocol + ".proxyPort");
        if (proxyPort == null)
            return null;

        try {
            final AuthScope systemScope = new AuthScope(proxyHost, Integer.parseInt(proxyPort));
            if (authScope.match(systemScope) >= 0) {
                final String proxyUser = System.getProperty(protocol + ".proxyUser");
                if (proxyUser == null)
                    return null;
                final String proxyPassword = System.getProperty(protocol + ".proxyPassword");
                return new PasswordAuthentication(proxyUser, proxyPassword != null ? proxyPassword.toCharArray() : new char[] {});
            }
        } catch (final NumberFormatException ex) {

        }
        return null;
    }

    @Override
    public void clear() {
        internal.clear();
    }
}
