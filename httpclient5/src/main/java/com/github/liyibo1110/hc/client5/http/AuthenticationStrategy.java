package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.client5.http.auth.AuthChallenge;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.ChallengeType;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.util.List;
import java.util.Map;

/**
 * 一种策略，用于根据对端（目标服务器或代理）提出的身份验证挑战，按优先级顺序选择身份验证方案。
 * 该接口的实现必须是线程安全的。由于该接口的方法可能由多个线程执行，因此必须对共享数据的访问进行同步。
 * @author liyibo
 * @date 2026-04-16 10:57
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface AuthenticationStrategy {

    /**
     * 返回AuthSchemes列表，用于按优先级顺序处理给定的AuthChallenges。
     */
    List<AuthScheme> select(ChallengeType challengeType, Map<String, AuthChallenge> challenges, HttpContext context);
}
