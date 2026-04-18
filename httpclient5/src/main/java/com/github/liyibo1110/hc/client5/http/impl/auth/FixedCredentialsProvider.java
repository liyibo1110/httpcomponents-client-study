package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthScope;
import com.github.liyibo1110.hc.client5.http.auth.Credentials;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liyibo
 * @date 2026-04-17 23:27
 */
public class FixedCredentialsProvider implements CredentialsProvider {

    private final Map<AuthScope, Credentials> credMap;

    public FixedCredentialsProvider(final Map<AuthScope, Credentials> credMap) {
        super();
        this.credMap = Collections.unmodifiableMap(new HashMap<>(credMap));
    }

    @Override
    public Credentials getCredentials(final AuthScope authScope, final HttpContext context) {
        return CredentialsMatcher.matchCredentials(this.credMap, authScope);
    }

    @Override
    public String toString() {
        return credMap.keySet().toString();
    }
}
