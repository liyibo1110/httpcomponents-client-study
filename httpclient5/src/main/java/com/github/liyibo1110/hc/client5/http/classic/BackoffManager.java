package com.github.liyibo1110.hc.client5.http.classic;

import com.github.liyibo1110.hc.client5.http.HttpRoute;

/**
 * 表示一个控制器，它会根据连接使用情况的反馈，动态调整可用连接池的大小。
 * @author liyibo
 * @date 2026-04-14 16:59
 */
public interface BackoffManager {

    /**
     * 当决定将使用连接的结果解释为退避信号时，会调用此函数。
     */
    void backOff(HttpRoute route);

    /**
     * 当确定使用连接的结果成功，且可以尝试建立更多连接时调用此方法。
     */
    void probe(HttpRoute route);
}
