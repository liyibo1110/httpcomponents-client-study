package com.github.liyibo1110.hc.client5.http.utils;

/**
 * DNS相关工具方法。
 * @author liyibo
 * @date 2026-04-14 10:47
 */
public final class DnsUtils {

    private DnsUtils() {}

    private static boolean isUpper(final char c) {
        return c >= 'A' && c <= 'Z';
    }

    public static String normalize(final String s) {
        if (s == null)
            return null;
        int pos = 0;
        int remaining = s.length();
        while (remaining > 0) {
            if (isUpper(s.charAt(pos)))
                break;
            pos++;
            remaining--;
        }
        if (remaining > 0) {
            final StringBuilder buf = new StringBuilder(s.length());
            buf.append(s, 0, pos);
            while (remaining > 0) {
                final char c = s.charAt(pos);
                if (isUpper(c))
                    buf.append((char) (c + ('a' - 'A')));
                else
                    buf.append(c);
                pos++;
                remaining--;
            }
            return buf.toString();
        } else {
            return s;
        }
    }
}
