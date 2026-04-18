package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthScope;
import com.github.liyibo1110.hc.client5.http.auth.Credentials;

import java.util.Map;

/**
 * 查找与给定身份验证范围匹配的凭据。
 * @author liyibo
 * @date 2026-04-17 17:43
 */
final class CredentialsMatcher {

    private CredentialsMatcher() {}

    static Credentials matchCredentials(final Map<AuthScope, Credentials> map, final AuthScope authScope) {
        // see if we get a direct hit
        Credentials creds = map.get(authScope);
        if (creds == null) {
            // Nope.
            // Do a full scan
            int bestMatchFactor  = -1;
            AuthScope bestMatch  = null;
            for (final AuthScope current: map.keySet()) {
                final int factor = authScope.match(current);
                if (factor > bestMatchFactor) {
                    bestMatchFactor = factor;
                    bestMatch = current;
                }
            }
            if (bestMatch != null)
                creds = map.get(bestMatch);
        }
        return creds;
    }
}
