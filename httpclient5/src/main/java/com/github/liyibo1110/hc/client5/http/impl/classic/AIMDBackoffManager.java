package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.classic.BackoffManager;
import com.github.liyibo1110.hc.core5.annotation.Experimental;
import com.github.liyibo1110.hc.core5.pool.ConnPoolControl;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TimeValue;

import java.util.HashMap;
import java.util.Map;

/**
 * AIMDBackoffManager采用加性增加、乘性减少（AIMD）算法，用于管理允许连接到特定主机的连接数动态限制。
 * 您可以尝试调整冷却时间和退避系数的设置，以获得所需的自适应行为。
 *
 * 一般而言，较短的冷却时间会导致稳态波动更大，但响应速度更快；而较长的冷却时间则会导致平衡行为更稳定，但响应速度较慢。
 *
 * 同样地，较高的退避因子能促进可用容量的更高利用率，但会牺牲客户端之间的公平性。
 * 较低的退避因子能更快地实现客户端之间的容量均等分配（公平性），但代价是短期内会有更多服务器容量处于闲置状态。
 * @author liyibo
 * @date 2026-04-17 12:40
 */
@Experimental
public class AIMDBackoffManager implements BackoffManager {
    private final ConnPoolControl<HttpRoute> connPerRoute;
    private final Clock clock;
    private final Map<HttpRoute, Long> lastRouteProbes;
    private final Map<HttpRoute, Long> lastRouteBackoffs;
    private TimeValue coolDown = TimeValue.ofSeconds(5L);
    private double backoffFactor = 0.5;
    private int cap = 2; // Per RFC 2616 sec 8.1.4

    /**
     * Creates an {@code AIMDBackoffManager} to manage
     * per-host connection pool sizes represented by the
     * given {@link ConnPoolControl}.
     * @param connPerRoute per-host routing maximums to
     *   be managed
     */
    public AIMDBackoffManager(final ConnPoolControl<HttpRoute> connPerRoute) {
        this(connPerRoute, new SystemClock());
    }

    AIMDBackoffManager(final ConnPoolControl<HttpRoute> connPerRoute, final Clock clock) {
        this.clock = clock;
        this.connPerRoute = connPerRoute;
        this.lastRouteProbes = new HashMap<>();
        this.lastRouteBackoffs = new HashMap<>();
    }

    @Override
    public void backOff(final HttpRoute route) {
        synchronized(connPerRoute) {
            final int curr = connPerRoute.getMaxPerRoute(route);
            final Long lastUpdate = getLastUpdate(lastRouteBackoffs, route);
            final long now = clock.getCurrentTime();
            if (now - lastUpdate < coolDown.toMilliseconds())
                return;
            connPerRoute.setMaxPerRoute(route, getBackedOffPoolSize(curr));
            lastRouteBackoffs.put(route, now);
        }
    }

    private int getBackedOffPoolSize(final int curr) {
        if (curr <= 1)
            return 1;
        return (int)(Math.floor(backoffFactor * curr));
    }

    @Override
    public void probe(final HttpRoute route) {
        synchronized(connPerRoute) {
            final int curr = connPerRoute.getMaxPerRoute(route);
            final int max = (curr >= cap) ? cap : curr + 1;
            final Long lastProbe = getLastUpdate(lastRouteProbes, route);
            final Long lastBackoff = getLastUpdate(lastRouteBackoffs, route);
            final long now = clock.getCurrentTime();
            if (now - lastProbe < coolDown.toMilliseconds() || now - lastBackoff < coolDown.toMilliseconds())
                return;
            connPerRoute.setMaxPerRoute(route, max);
            lastRouteProbes.put(route, now);
        }
    }

    private Long getLastUpdate(final Map<HttpRoute, Long> updates, final HttpRoute route) {
        Long lastUpdate = updates.get(route);
        if (lastUpdate == null)
            lastUpdate = 0L;
        return lastUpdate;
    }

    /**
     * Sets the factor to use when backing off; the new
     * per-host limit will be roughly the current max times
     * this factor. {@code Math.floor} is applied in the
     * case of non-integer outcomes to ensure we actually
     * decrease the pool size. Pool sizes are never decreased
     * below 1, however. Defaults to 0.5.
     * @param d must be between 0.0 and 1.0, exclusive.
     */
    public void setBackoffFactor(final double d) {
        Args.check(d > 0.0 && d < 1.0, "Backoff factor must be 0.0 < f < 1.0");
        backoffFactor = d;
    }

    /**
     * Sets the amount of time to wait between adjustments in
     * pool sizes for a given host, to allow enough time for
     * the adjustments to take effect. Defaults to 5 seconds.
     * @param coolDown must be positive
     */
    public void setCoolDown(final TimeValue coolDown) {
        Args.positive(coolDown.getDuration(), "coolDown");
        this.coolDown = coolDown;
    }

    /**
     * Sets the absolute maximum per-host connection pool size to
     * probe up to; defaults to 2 (the default per-host max).
     * @param cap must be &gt;= 1
     */
    public void setPerHostConnectionCap(final int cap) {
        Args.positive(cap, "Per host connection cap");
        this.cap = cap;
    }
}
