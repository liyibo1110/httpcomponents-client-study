package com.github.liyibo1110.hc.client5.http.ssl;

import com.github.liyibo1110.hc.core5.util.TextUtils;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * HTTPS相关工具方法。
 * @author liyibo
 * @date 2026-04-16 13:55
 */
public final class HttpsSupport {

    private HttpsSupport() {}

    private static String[] split(final String s) {
        if (TextUtils.isBlank(s))
            return null;
        return s.split(" *, *");
    }

    private static String getProperty(final String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    public static String[] getSystemProtocols() {
        return split(getProperty("https.protocols"));
    }

    public static String[] getSystemCipherSuits() {
        return split(getProperty("https.cipherSuites"));
    }
}
