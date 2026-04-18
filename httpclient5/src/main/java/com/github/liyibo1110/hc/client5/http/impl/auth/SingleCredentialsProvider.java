package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthScope;
import com.github.liyibo1110.hc.client5.http.auth.Credentials;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.client5.http.auth.UsernamePasswordCredentials;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;

/**
 * @author liyibo
 * @date 2026-04-17 23:23
 */
final class SingleCredentialsProvider implements CredentialsProvider {
    private final AuthScope authScope;
    private final Credentials credentials;

    public SingleCredentialsProvider(final AuthScope authScope, final Credentials credentials) {
        super();
        this.authScope = Args.notNull(authScope, "Auth scope");
        this.credentials = credentials;
    }

    public SingleCredentialsProvider(final AuthScope authScope, final String username, final char[] password) {
        this(authScope, new UsernamePasswordCredentials(username, password));
    }

    @Override
    public Credentials getCredentials(final AuthScope authScope, final HttpContext context) {
        return this.authScope.match(authScope) >= 0 ? credentials : null;
    }

    @Override
    public String toString() {
        return authScope.toString();
    }
}
