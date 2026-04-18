package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthScope;
import com.github.liyibo1110.hc.client5.http.auth.Credentials;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.client5.http.auth.UsernamePasswordCredentials;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.util.Args;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liyibo
 * @date 2026-04-17 23:07
 */
public final class CredentialsProviderBuilder {

    private final Map<AuthScope, Credentials> credMap;

    public static CredentialsProviderBuilder create() {
        return new CredentialsProviderBuilder();
    }

    public CredentialsProviderBuilder() {
        super();
        this.credMap = new HashMap<>();
    }

    public CredentialsProviderBuilder add(final AuthScope authScope, final Credentials credentials) {
        Args.notNull(authScope, "Host");
        credMap.put(authScope, credentials);
        return this;
    }

    public CredentialsProviderBuilder add(final AuthScope authScope, final String username, final char[] password) {
        Args.notNull(authScope, "Host");
        credMap.put(authScope, new UsernamePasswordCredentials(username, password));
        return this;
    }

    public CredentialsProviderBuilder add(final HttpHost httpHost, final Credentials credentials) {
        Args.notNull(httpHost, "Host");
        credMap.put(new AuthScope(httpHost), credentials);
        return this;
    }

    public CredentialsProviderBuilder add(final HttpHost httpHost, final String username, final char[] password) {
        Args.notNull(httpHost, "Host");
        credMap.put(new AuthScope(httpHost), new UsernamePasswordCredentials(username, password));
        return this;
    }

    public CredentialsProvider build() {
        if (credMap.size() == 0) {
            return new BasicCredentialsProvider();
        } else if (credMap.size() == 1) {
            final Map.Entry<AuthScope, Credentials> entry = credMap.entrySet().iterator().next();
            return new SingleCredentialsProvider(entry.getKey(), entry.getValue());
        } else {
            return new FixedCredentialsProvider(credMap);
        }
    }

    static class Entry {
        final AuthScope authScope;
        final Credentials credentials;

        Entry(final AuthScope authScope, final Credentials credentials) {
            this.authScope = authScope;
            this.credentials = credentials;
        }
    }
}
