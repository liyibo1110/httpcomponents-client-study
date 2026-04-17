package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.client5.http.AuthenticationStrategy;
import com.github.liyibo1110.hc.client5.http.auth.AuthChallenge;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthSchemeFactory;
import com.github.liyibo1110.hc.client5.http.auth.ChallengeType;
import com.github.liyibo1110.hc.client5.http.auth.StandardAuthScheme;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.config.Lookup;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AuthenticationStrategy的默认实现类。
 * @author liyibo
 * @date 2026-04-16 16:57
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultAuthenticationStrategy implements AuthenticationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAuthenticationStrategy.class);

    public static final DefaultAuthenticationStrategy INSTANCE = new DefaultAuthenticationStrategy();

    /** 优先级高的会先尝试匹配 */
    private static final List<String> DEFAULT_SCHEME_PRIORITY =
            Collections.unmodifiableList(Arrays.asList(
                    StandardAuthScheme.SPNEGO,
                    StandardAuthScheme.KERBEROS,
                    StandardAuthScheme.NTLM,
                    StandardAuthScheme.DIGEST,
                    StandardAuthScheme.BASIC));

    @Override
    public List<AuthScheme> select(final ChallengeType challengeType, final Map<String, AuthChallenge> challenges, final HttpContext context) {
        Args.notNull(challengeType, "ChallengeType");
        Args.notNull(challenges, "Map of auth challenges");
        Args.notNull(context, "HTTP context");

        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final String exchangeId = clientContext.getExchangeId();

        final List<AuthScheme> options = new ArrayList<>();
        final Lookup<AuthSchemeFactory> registry = clientContext.getAuthSchemeRegistry();
        if (registry == null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Auth scheme registry not set in the context", exchangeId);
            return options;
        }
        final RequestConfig config = clientContext.getRequestConfig();
        Collection<String> authPrefs = challengeType == ChallengeType.TARGET
                ? config.getTargetPreferredAuthSchemes()
                : config.getProxyPreferredAuthSchemes();
        if (authPrefs == null)
            authPrefs = DEFAULT_SCHEME_PRIORITY;
        if (LOG.isDebugEnabled())
            LOG.debug("{} Authentication schemes in the order of preference: {}", exchangeId, authPrefs);

        // 按照优先级逐一匹配
        for (final String schemeName: authPrefs) {
            final AuthChallenge challenge = challenges.get(schemeName.toLowerCase(Locale.ROOT));
            if (challenge != null) {
                final AuthSchemeFactory authSchemeFactory = registry.lookup(schemeName);
                if (authSchemeFactory == null) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("{} Authentication scheme {} not supported", exchangeId, schemeName);
                        // Try again
                    }
                    continue;
                }
                final AuthScheme authScheme = authSchemeFactory.create(context);
                options.add(authScheme);
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("{} Challenge for {} authentication scheme not available", exchangeId, schemeName);
            }
        }
        return options;
    }
}
