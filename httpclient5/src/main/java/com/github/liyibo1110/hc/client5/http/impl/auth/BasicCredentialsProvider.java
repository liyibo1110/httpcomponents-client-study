package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthScope;
import com.github.liyibo1110.hc.client5.http.auth.Credentials;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsStore;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;

import java.util.concurrent.ConcurrentHashMap;

/**
 * CredentialsStore接口的默认实现。
 * @author liyibo
 * @date 2026-04-17 17:42
 */
@Contract(threading = ThreadingBehavior.SAFE)
public class BasicCredentialsProvider implements CredentialsStore {

    private final ConcurrentHashMap<AuthScope, Credentials> credMap;

    public BasicCredentialsProvider() {
        super();
        this.credMap = new ConcurrentHashMap<>();
    }

    @Override
    public void setCredentials(final AuthScope authScope, final Credentials credentials) {
        Args.notNull(authScope, "Authentication scope");
        credMap.put(authScope, credentials);
    }

    @Override
    public Credentials getCredentials(final AuthScope authScope, final HttpContext context) {
        return CredentialsMatcher.matchCredentials(this.credMap, authScope);
    }

    @Override
    public void clear() {
        this.credMap.clear();
    }

    @Override
    public String toString() {
        return credMap.keySet().toString();
    }
}
