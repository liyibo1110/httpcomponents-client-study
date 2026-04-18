package com.github.liyibo1110.hc.client5.http.classic;

import com.github.liyibo1110.hc.core5.http.HttpResponse;

/**
 * 在管理特定路由的动态连接数时，该策略会根据生成的Throwable异常或响应结果（例如其状态码）来判断，某个请求的执行结果是否应触发退避信号。
 * @author liyibo
 * @date 2026-04-14 16:57
 */
public interface ConnectionBackoffStrategy {

    /**
     * 确定在请求执行过程中遇到给定的Throwable时，是否应触发回退信号。
     */
    boolean shouldBackoff(Throwable t);

    /**
     * 确定在请求执行后收到给定的HttpResponse是否应触发退避信号。
     * 实现必须仅检查响应头，且不得读取响应正文（如有）。
     */
    boolean shouldBackoff(HttpResponse response);
}
