package com.github.liyibo1110.hc.client5.http.routing;

import com.github.liyibo1110.hc.client5.http.RouteInfo;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

/**
 * 提供有关建立route的指导。
 * 该接口的实现会将计划route与实际轨迹进行对比，并指示下一步需要采取的行动。
 * @author liyibo
 * @date 2026-04-14 21:52
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface HttpRouteDirector {

    /** 表示完全无法建立该路由 */
    int UNREACHABLE = -1;

    /** 表示route已完成 */
    int COMPLETE = 0;

    /** 步骤：建立与目标的连接 */
    int CONNECT_TARGET = 1;

    /** 步骤：建立与代理的连接 */
    int CONNECT_PROXY = 2;

    /** 步骤：通过代理连接到目标 */
    int TUNNEL_TARGET = 3;

    /** 步骤：通过代理连接到另一个代理 */
    int TUNNEL_PROXY = 4;

    /** 步骤：分层协议（通过隧道）。 */
    int LAYER_PROTOCOL = 5;

    /**
     * 提供下一个step。
     * @param plan 规划后的route
     * @param fact 当前已建立的route，若未建立则返回null。
     * @return 该接口中定义的常量之一，用于指示下一步操作、成功或失败，0表示成功，负数表示失败。
     */
    int nextStep(RouteInfo plan, RouteInfo fact);
}
