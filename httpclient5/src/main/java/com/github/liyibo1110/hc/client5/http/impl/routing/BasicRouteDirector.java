package com.github.liyibo1110.hc.client5.http.impl.routing;

import com.github.liyibo1110.hc.client5.http.RouteInfo;
import com.github.liyibo1110.hc.client5.http.routing.HttpRouteDirector;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;

/**
 * HttpRouteDirector接口的基础实现类。
 *
 * 职责是：为了把当前fact推进到目标plan，下一步该做什么。
 * @author liyibo
 * @date 2026-04-20 14:23
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class BasicRouteDirector implements HttpRouteDirector {

    public static final BasicRouteDirector INSTANCE = new BasicRouteDirector();

    /**
     * 根据plan和fact的差距，返回一个动作指令。
     * 可以看作是一个状态推进器。
     * 重点先要了解2个RouteInfo是怎么来的：
     * 1、plan：是上层路由规划阶段产出的目标路线，例如ConnectExec里的scope.route所对应的对象。也就是理想终态（即我要到哪儿）。
     * 2、fact：是运行过程中，根据当前已经完成的步骤，由RouteTracker组件动态记录的（即我现在到哪儿了）。
     * 因此这个方法要做的事就是：从fact推导下一步该往哪儿走。
     */
    @Override
    public int nextStep(final RouteInfo plan, final RouteInfo fact) {
        Args.notNull(plan, "Planned route");

        int step = UNREACHABLE;

        if (fact == null || fact.getHopCount() < 1) // 还没建立route，或者route没有至少1跳，则直接根据plan来定结果
            step = firstStep(plan);
        else if (plan.getHopCount() > 1)    // 如果目标route是经过代理的，说明目标route至少有2跳：proxy -> target
            step = proxiedStep(plan, fact);
        else    // 否则说明plan的跳数只有1跳，就是直接连到target，最终结果要么有问题了，要么已经连上了
            step = directStep(plan, fact);

        return step;
    }

    /**
     * 决定连接第一步
     */
    protected int firstStep(final RouteInfo plan) {
        return plan.getHopCount() > 1 ? CONNECT_PROXY : CONNECT_TARGET;
    }

    /**
     * 确定通过代理建立连接的下一步操作。
     * 1、先验证fact至少已经是代理路线。
     * 2、再验证target是否一致。
     * 3、再比较hopCount。
     * 4、再比较已建立的代理链是否一致。
     * 5、再决定是否要继续tunnel proxy。
     * 6、再检查tunnel / layering状态。
     * 7、再检查secure状态。
     * 8、最后决定COMPLETE或下一步动作。
     */
    protected int proxiedStep(final RouteInfo plan, final RouteInfo fact) {
        // fact必须是代理路线
        if (fact.getHopCount() <= 1)
            return UNREACHABLE;

        // 最终目标主机必须一样
        if (!plan.getTargetHost().equals(fact.getTargetHost()))
            return UNREACHABLE;

        // 比较跳数，phc < fhc判断是否fact跳数比plan还要多，说明跳过头了，例如plan是1个proxy，而fact已经有2个proxy了
        final int phc = plan.getHopCount();
        final int fhc = fact.getHopCount();
        if (phc < fhc)
            return UNREACHABLE;

        /**
         * 比较已经建立的代理链前缀是否一致，例如：
         * plan route： source -> p1 -> p2 -> target
         * fact route： source -> p1 -> target
         * phc < fhc不成立，说明fact至少是plan的合法前缀，还可以继续推进。
         *
         * plan route：source -> X -> target
         * fact route：source -> p1 -> p2 -> target
         * phc < fhc成立，说明第一跳proxy就不一致，返回不可达。
         */
        for (int i = 0; i < fhc - 1; i++) {
            if (!plan.getHopTarget(i).equals(fact.getHopTarget(i)))
                return UNREACHABLE;
        }

        // 如果plan比fact还长，说明要继续扩展代理链（TUNNEL_PROXY意思就是：继续往下一个proxy建tunnel）
        if (phc > fhc)
            return TUNNEL_PROXY;

        /**
         * 当hop链长度已经一致之后，再检查tunnel和layer，如果fact比plan更高级，则不能接受。
         * 1、如果fact已经tunnel了，但plan不要求tunnel。
         * 2、如果fact已经layered了，但plan不要求layered。
         * 因为采用的严格route匹配，不接受“比计划更强也算可以”的宽松策略。
         */
        if ((fact.isTunnelled() && !plan.isTunnelled()) || (fact.isLayered() && !plan.isLayered()))
            return UNREACHABLE;

        // plan要tunnel，但fact还没，说明代理链长度已经对上了，但到target的tunnel还没建立，下一步就是TUNNEL_TARGET（对应ConnectExec的createTunnelToTarget方法）
        if (plan.isTunnelled() && !fact.isTunnelled())
            return TUNNEL_TARGET;

        // plan要layered，但fact还没，说明tunnel已经有了，但还没把TLS这种上层协议叠上去，下一步就是LAYER_PROTOCOL（对应execRuntime的upgradeTls方法）
        if (plan.isLayered() && !fact.isLayered())
            return LAYER_PROTOCOL;

        // 最后再次检查secure，因为上面动作都完成后，才有资格最终比较secure状态，看是否达标
        if (plan.isSecure() != fact.isSecure())
            return UNREACHABLE;

        return COMPLETE;
    }

    /**
     * 目标plan本身是直连route，但是当前fact之前已经有了一定建立的进度。
     * 如果当前fact和plan能都能对上，就返回成功，对不上就返回错误。
     */
    protected int directStep(final RouteInfo plan, final RouteInfo fact) {
        // fact已经有多跳，说明事实已经走了代理路线了，但plan是直连路线，直接返回不可达
        if (fact.getHopCount() > 1)
            return UNREACHABLE;

        // 如果fact的目标主机，都不是plan的目标主机，直接返回不可达
        if (!plan.getTargetHost().equals(fact.getTargetHost()))
            return UNREACHABLE;

        // 安全属性不一致，直接返回不可达
        if (plan.isSecure() != fact.isSecure())
            return UNREACHABLE;

        // 本地地址不匹配，注意只有plan制定了localAddress才能比对检查，如果有则fact也要匹配
        if ((plan.getLocalAddress() != null) && !plan.getLocalAddress().equals(fact.getLocalAddress()))
            return UNREACHABLE;

        return COMPLETE;
    }
}
