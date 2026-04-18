package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.AuthenticationStrategy;
import com.github.liyibo1110.hc.client5.http.auth.AuthChallenge;
import com.github.liyibo1110.hc.client5.http.auth.AuthExchange;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthenticationException;
import com.github.liyibo1110.hc.client5.http.auth.ChallengeType;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.client5.http.auth.MalformedChallengeException;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.FormattedHeader;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpHeaders;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpStatus;
import com.github.liyibo1110.hc.core5.http.ParseException;
import com.github.liyibo1110.hc.core5.http.message.BasicHeader;
import com.github.liyibo1110.hc.core5.http.message.ParserCursor;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Asserts;
import com.github.liyibo1110.hc.core5.util.CharArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * 一个实现客户端HTTP身份验证通用功能的辅助类。
 * 请注意自5.2版本起，该类不再更新与执行上下文绑定的身份验证缓存。
 * @author liyibo
 * @date 2026-04-17 23:30
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class HttpAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(HttpAuthenticator.class);

    private final AuthChallengeParser parser;

    public HttpAuthenticator() {
        this.parser = new AuthChallengeParser();
    }

    /**
     * 确定给定的响应是否代表身份验证挑战。
     */
    public boolean isChallenged(final HttpHost host,
                                final ChallengeType challengeType,
                                final HttpResponse response,
                                final AuthExchange authExchange,
                                final HttpContext context) {
        final int challengeCode;
        switch (challengeType) {
            case TARGET:
                challengeCode = HttpStatus.SC_UNAUTHORIZED;
                break;
            case PROXY:
                challengeCode = HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED;
                break;
            default:
                throw new IllegalStateException("Unexpected challenge type: " + challengeType);
        }

        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final String exchangeId = clientContext.getExchangeId();

        if (response.getCode() == challengeCode) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Authentication required", exchangeId);
            return true;
        }
        switch (authExchange.getState()) {
            case CHALLENGED:
            case HANDSHAKE:
                if (LOG.isDebugEnabled())
                    LOG.debug("{} Authentication succeeded", exchangeId);
                authExchange.setState(AuthExchange.State.SUCCESS);
                break;
            case SUCCESS:
                break;
            default:
                authExchange.setState(AuthExchange.State.UNCHALLENGED);
        }
        return false;
    }

    /**
     * 根据响应消息中提供的挑战，使用给定的AuthenticationStrategy更新 AuthExchange 的状态。
     */
    public boolean updateAuthState(final HttpHost host,
                                   final ChallengeType challengeType,
                                   final HttpResponse response,
                                   final AuthenticationStrategy authStrategy,
                                   final AuthExchange authExchange,
                                   final HttpContext context) {

        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final String exchangeId = clientContext.getExchangeId();

        if (LOG.isDebugEnabled())
            LOG.debug("{} {} requested authentication", exchangeId, host.toHostString());

        final Header[] headers = response.getHeaders(challengeType == ChallengeType.PROXY
                ? HttpHeaders.PROXY_AUTHENTICATE
                : HttpHeaders.WWW_AUTHENTICATE);
        final Map<String, AuthChallenge> challengeMap = new HashMap<>();
        for (final Header header: headers) {
            final CharArrayBuffer buffer;
            final int pos;
            if (header instanceof FormattedHeader) {
                buffer = ((FormattedHeader) header).getBuffer();
                pos = ((FormattedHeader) header).getValuePos();
            } else {
                final String s = header.getValue();
                if (s == null)
                    continue;
                buffer = new CharArrayBuffer(s.length());
                buffer.append(s);
                pos = 0;
            }
            final ParserCursor cursor = new ParserCursor(pos, buffer.length());
            final List<AuthChallenge> authChallenges;
            try {
                authChallenges = parser.parse(challengeType, buffer, cursor);
            } catch (final ParseException ex) {
                if (LOG.isWarnEnabled())
                    LOG.warn("{} Malformed challenge: {}", exchangeId, header.getValue());
                continue;
            }
            for (final AuthChallenge authChallenge: authChallenges) {
                final String schemeName = authChallenge.getSchemeName().toLowerCase(Locale.ROOT);
                if (!challengeMap.containsKey(schemeName))
                    challengeMap.put(schemeName, authChallenge);
            }
        }
        if (challengeMap.isEmpty()) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Response contains no valid authentication challenges", exchangeId);
            authExchange.reset();
            return false;
        }

        switch (authExchange.getState()) {
            case FAILURE:
                return false;
            case SUCCESS:
                authExchange.reset();
                break;
            case CHALLENGED:
            case HANDSHAKE:
                Asserts.notNull(authExchange.getAuthScheme(), "AuthScheme");
            case UNCHALLENGED:
                final AuthScheme authScheme = authExchange.getAuthScheme();
                if (authScheme != null) {
                    final String schemeName = authScheme.getName();
                    final AuthChallenge challenge = challengeMap.get(schemeName.toLowerCase(Locale.ROOT));
                    if (challenge != null) {
                        if (LOG.isDebugEnabled())
                            LOG.debug("{} Authorization challenge processed", exchangeId);
                        try {
                            authScheme.processChallenge(challenge, context);
                        } catch (final MalformedChallengeException ex) {
                            if (LOG.isWarnEnabled())
                                LOG.warn("{} {}", exchangeId, ex.getMessage());
                            authExchange.reset();
                            authExchange.setState(AuthExchange.State.FAILURE);
                            return false;
                        }
                        if (authScheme.isChallengeComplete()) {
                            if (LOG.isDebugEnabled())
                                LOG.debug("{} Authentication failed", exchangeId);
                            authExchange.reset();
                            authExchange.setState(AuthExchange.State.FAILURE);
                            return false;
                        }
                        authExchange.setState(AuthExchange.State.HANDSHAKE);
                        return true;
                    }
                    authExchange.reset();
                    // Retry authentication with a different scheme
                }
        }

        final List<AuthScheme> preferredSchemes = authStrategy.select(challengeType, challengeMap, context);
        final CredentialsProvider credsProvider = clientContext.getCredentialsProvider();
        if (credsProvider == null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Credentials provider not set in the context", exchangeId);
            return false;
        }

        final Queue<AuthScheme> authOptions = new LinkedList<>();
        if (LOG.isDebugEnabled())
            LOG.debug("{} Selecting authentication options", exchangeId);

        for (final AuthScheme authScheme: preferredSchemes) {
            try {
                final String schemeName = authScheme.getName();
                final AuthChallenge challenge = challengeMap.get(schemeName.toLowerCase(Locale.ROOT));
                authScheme.processChallenge(challenge, context);
                if (authScheme.isResponseReady(host, credsProvider, context)) {
                    authOptions.add(authScheme);
                }
            } catch (final AuthenticationException | MalformedChallengeException ex) {
                if (LOG.isWarnEnabled())
                    LOG.warn(ex.getMessage());
            }
        }
        if (!authOptions.isEmpty()) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Selected authentication options: {}", exchangeId, authOptions);
            authExchange.reset();
            authExchange.setState(AuthExchange.State.CHALLENGED);
            authExchange.setOptions(authOptions);
            return true;
        }
        return false;
    }

    /**
     * 根据当前的AuthExchange状态生成对身份验证挑战的响应，并将其添加到指定的HttpRequest消息中。
     */
    public void addAuthResponse(final HttpHost host,
                                final ChallengeType challengeType,
                                final HttpRequest request,
                                final AuthExchange authExchange,
                                final HttpContext context) {
        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final String exchangeId = clientContext.getExchangeId();
        AuthScheme authScheme = authExchange.getAuthScheme();
        switch (authExchange.getState()) {
            case FAILURE:
                return;
            case SUCCESS:
                Asserts.notNull(authScheme, "AuthScheme");
                if (authScheme.isConnectionBased())
                    return;
                break;
            case HANDSHAKE:
                Asserts.notNull(authScheme, "AuthScheme");
                break;
            case CHALLENGED:
                final Queue<AuthScheme> authOptions = authExchange.getAuthOptions();
                if (authOptions != null) {
                    while (!authOptions.isEmpty()) {
                        authScheme = authOptions.remove();
                        authExchange.select(authScheme);
                        if (LOG.isDebugEnabled())
                            LOG.debug("{} Generating response to an authentication challenge using {} scheme", exchangeId, authScheme.getName());
                        try {
                            final String authResponse = authScheme.generateAuthResponse(host, request, context);
                            final Header header = new BasicHeader(challengeType == ChallengeType.TARGET
                                    ? HttpHeaders.AUTHORIZATION
                                    : HttpHeaders.PROXY_AUTHORIZATION,
                                    authResponse);
                            request.addHeader(header);
                            break;
                        } catch (final AuthenticationException ex) {
                            if (LOG.isWarnEnabled())
                                LOG.warn("{} {} authentication error: {}", exchangeId, authScheme, ex.getMessage());
                        }
                    }
                    return;
                }
                Asserts.notNull(authScheme, "AuthScheme");
            default:
        }
        if (authScheme != null) {
            try {
                final String authResponse = authScheme.generateAuthResponse(host, request, context);
                final Header header = new BasicHeader(challengeType == ChallengeType.TARGET
                        ? HttpHeaders.AUTHORIZATION
                        : HttpHeaders.PROXY_AUTHORIZATION,
                        authResponse);
                request.addHeader(header);
            } catch (final AuthenticationException ex) {
                if (LOG.isErrorEnabled())
                    LOG.error("{} {} authentication error: {}", exchangeId, authScheme, ex.getMessage());
            }
        }
    }
}
