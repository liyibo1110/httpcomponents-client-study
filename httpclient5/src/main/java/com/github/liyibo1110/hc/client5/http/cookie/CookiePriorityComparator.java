package com.github.liyibo1110.hc.client5.http.cookie;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

import java.time.Instant;
import java.util.Comparator;

/**
 * 此Cookie比较器确保路径较长的Cookie优先于路径较短的Cookie。
 * 对于路径长度相同的Cookie，创建时间较早的Cookie优先于创建时间较晚的Cookie。
 * @author liyibo
 * @date 2026-04-15 15:27
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class CookiePriorityComparator implements Comparator<Cookie> {

    public static final CookiePriorityComparator INSTANCE = new CookiePriorityComparator();

    private int getPathLength(final Cookie cookie) {
        final String path = cookie.getPath();
        return path != null ? path.length() : 1;
    }

    @Override
    public int compare(final Cookie c1, final Cookie c2) {
        final int l1 = getPathLength(c1);
        final int l2 = getPathLength(c2);
        final int result = l2 - l1;
        if (result == 0) {
            final Instant d1 = c1.getCreationInstant();
            final Instant d2 = c2.getCreationInstant();
            if (d1 != null && d2 != null) {
                return d1.compareTo(d2);
            }
        }
        return result;
    }
}
