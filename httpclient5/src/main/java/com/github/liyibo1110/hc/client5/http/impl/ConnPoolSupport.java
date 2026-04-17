package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.pool.ConnPoolControl;
import com.github.liyibo1110.hc.core5.pool.PoolStats;
import com.github.liyibo1110.hc.core5.util.Identifiable;

/**
 * 连接池相关工具方法。
 * @author liyibo
 * @date 2026-04-16 16:53
 */
@Internal
public final class ConnPoolSupport {

    public static String getId(final Object object) {
        if (object == null)
            return null;
        return object instanceof Identifiable
                ? ((Identifiable) object).getId()
                : object.getClass().getSimpleName() + "-" + Integer.toHexString(System.identityHashCode(object));
    }

    public static String formatStats(final HttpRoute route, final Object state, final ConnPoolControl<HttpRoute> connPool) {
        final StringBuilder buf = new StringBuilder();
        buf.append("[route: ").append(route).append("]");
        if (state != null)
            buf.append("[state: ").append(state).append("]");

        final PoolStats totals = connPool.getTotalStats();
        final PoolStats stats = connPool.getStats(route);
        buf.append("[total available: ").append(totals.getAvailable()).append("; ");
        buf.append("route allocated: ").append(stats.getLeased() + stats.getAvailable());
        buf.append(" of ").append(stats.getMax()).append("; ");
        buf.append("total allocated: ").append(totals.getLeased() + totals.getAvailable());
        buf.append(" of ").append(totals.getMax()).append("]");
        return buf.toString();
    }
}
