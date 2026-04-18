package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.AuthenticationStrategy;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.auth.AuthExchange;
import com.github.liyibo1110.hc.client5.http.auth.AuthSchemeFactory;
import com.github.liyibo1110.hc.client5.http.auth.AuthScope;
import com.github.liyibo1110.hc.client5.http.auth.ChallengeType;
import com.github.liyibo1110.hc.client5.http.auth.Credentials;
import com.github.liyibo1110.hc.client5.http.auth.StandardAuthScheme;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.impl.DefaultAuthenticationStrategy;
import com.github.liyibo1110.hc.client5.http.impl.DefaultClientConnectionReuseStrategy;
import com.github.liyibo1110.hc.client5.http.impl.TunnelRefusedException;
import com.github.liyibo1110.hc.client5.http.io.ManagedHttpClientConnection;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.client5.http.protocol.RequestClientConnControl;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.ConnectionReuseStrategy;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHeaders;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.Method;
import com.github.liyibo1110.hc.core5.http.config.CharCodingConfig;
import com.github.liyibo1110.hc.core5.http.config.Http1Config;
import com.github.liyibo1110.hc.core5.http.config.Lookup;
import com.github.liyibo1110.hc.core5.http.config.RegistryBuilder;
import com.github.liyibo1110.hc.core5.http.impl.io.HttpRequestExecutor;
import com.github.liyibo1110.hc.core5.http.io.HttpConnectionFactory;
import com.github.liyibo1110.hc.core5.http.io.entity.EntityUtils;
import com.github.liyibo1110.hc.core5.http.message.BasicClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.message.StatusLine;
import com.github.liyibo1110.hc.core5.http.protocol.BasicHttpContext;
import com.github.liyibo1110.hc.core5.http.protocol.DefaultHttpProcessor;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.http.protocol.HttpCoreContext;
import com.github.liyibo1110.hc.core5.http.protocol.HttpProcessor;
import com.github.liyibo1110.hc.core5.http.protocol.RequestTargetHost;
import com.github.liyibo1110.hc.core5.http.protocol.RequestUserAgent;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;
import java.net.Socket;

/**
 * ProxyClient可用于通过HTTP/1.1代理建立隧道。
 * @author liyibo
 * @date 2026-04-17 14:52
 */
public class ProxyClient {
    private final HttpConnectionFactory<ManagedHttpClientConnection> connFactory;
    private final RequestConfig requestConfig;
    private final HttpProcessor httpProcessor;
    private final HttpRequestExecutor requestExec;
    private final AuthenticationStrategy proxyAuthStrategy;
    private final HttpAuthenticator authenticator;
    private final AuthExchange proxyAuthExchange;
    private final Lookup<AuthSchemeFactory> authSchemeRegistry;
    private final ConnectionReuseStrategy reuseStrategy;

    public ProxyClient(final HttpConnectionFactory<ManagedHttpClientConnection> connFactory,
                       final Http1Config h1Config,
                       final CharCodingConfig charCodingConfig,
                       final RequestConfig requestConfig) {
        super();
        this.connFactory = connFactory != null
                ? connFactory
                : ManagedHttpClientConnectionFactory.builder()
                .http1Config(h1Config)
                .charCodingConfig(charCodingConfig)
                .build();
        this.requestConfig = requestConfig != null ? requestConfig : RequestConfig.DEFAULT;
        this.httpProcessor = new DefaultHttpProcessor(
                new RequestTargetHost(), new RequestClientConnControl(), new RequestUserAgent());
        this.requestExec = new HttpRequestExecutor();
        this.proxyAuthStrategy = new DefaultAuthenticationStrategy();
        this.authenticator = new HttpAuthenticator();
        this.proxyAuthExchange = new AuthExchange();
        this.authSchemeRegistry = RegistryBuilder.<AuthSchemeFactory>create()
                .register(StandardAuthScheme.BASIC, BasicSchemeFactory.INSTANCE)
                .register(StandardAuthScheme.DIGEST, DigestSchemeFactory.INSTANCE)
                .register(StandardAuthScheme.NTLM, NTLMSchemeFactory.INSTANCE)
                .register(StandardAuthScheme.SPNEGO, SPNegoSchemeFactory.DEFAULT)
                .register(StandardAuthScheme.KERBEROS, KerberosSchemeFactory.DEFAULT)
                .build();
        this.reuseStrategy = DefaultClientConnectionReuseStrategy.INSTANCE;
    }

    public ProxyClient(final RequestConfig requestConfig) {
        this(null, null, null, requestConfig);
    }

    public ProxyClient() {
        this(null, null, null, null);
    }

    public Socket tunnel(final HttpHost proxy, final HttpHost target, final Credentials credentials)
            throws IOException, HttpException {
        Args.notNull(proxy, "Proxy host");
        Args.notNull(target, "Target host");
        Args.notNull(credentials, "Credentials");
        HttpHost host = target;
        if (host.getPort() <= 0)
            host = new HttpHost(host.getSchemeName(), host.getHostName(), 80);

        final HttpRoute route = new HttpRoute(host, null, proxy, false, TunnelType.TUNNELLED, LayerType.PLAIN);

        final ManagedHttpClientConnection conn = this.connFactory.createConnection(null);
        final HttpContext context = new BasicHttpContext();
        ClassicHttpResponse response;

        final ClassicHttpRequest connect = new BasicClassicHttpRequest(Method.CONNECT, proxy, host.toHostString());

        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(proxy), credentials);

        // Populate the execution context
        context.setAttribute(HttpCoreContext.HTTP_REQUEST, connect);
        context.setAttribute(HttpClientContext.HTTP_ROUTE, route);
        context.setAttribute(HttpClientContext.CREDS_PROVIDER, credsProvider);
        context.setAttribute(HttpClientContext.AUTHSCHEME_REGISTRY, this.authSchemeRegistry);
        context.setAttribute(HttpClientContext.REQUEST_CONFIG, this.requestConfig);

        this.requestExec.preProcess(connect, this.httpProcessor, context);

        while (true) {
            if (!conn.isOpen()) {
                final Socket socket = new Socket(proxy.getHostName(), proxy.getPort());
                conn.bind(socket);
            }

            this.authenticator.addAuthResponse(proxy, ChallengeType.PROXY, connect, this.proxyAuthExchange, context);

            response = this.requestExec.execute(connect, conn, context);

            final int status = response.getCode();
            if (status < 200)
                throw new HttpException("Unexpected response to CONNECT request: " + response);

            if (this.authenticator.isChallenged(proxy, ChallengeType.PROXY, response, this.proxyAuthExchange, context)) {
                if (this.authenticator.updateAuthState(proxy, ChallengeType.PROXY, response,
                        this.proxyAuthStrategy, this.proxyAuthExchange, context)) {
                    // Retry request
                    if (this.reuseStrategy.keepAlive(connect, response, context)) {
                        // Consume response content
                        final HttpEntity entity = response.getEntity();
                        EntityUtils.consume(entity);
                    } else {
                        conn.close();
                    }
                    // discard previous auth header
                    connect.removeHeaders(HttpHeaders.PROXY_AUTHORIZATION);
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        final int status = response.getCode();

        if (status > 299) {
            // Buffer response content
            final HttpEntity entity = response.getEntity();
            final String responseMessage = entity != null ? EntityUtils.toString(entity) : null;
            conn.close();
            throw new TunnelRefusedException("CONNECT refused by proxy: " + new StatusLine(response), responseMessage);
        }
        return conn.getSocket();
    }
}
