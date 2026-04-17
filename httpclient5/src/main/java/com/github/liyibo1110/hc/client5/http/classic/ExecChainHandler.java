package com.github.liyibo1110.hc.client5.http.classic;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpException;

import java.io.IOException;

/**
 * classic client request执行链中的抽象请求执行处理器。
 * 处理器可以是围绕另一个元素的装饰器（该元素实现横切关注点），也可以是能够为给定请求生成响应的自包含执行器。
 *
 * 重要提示：请注意，对于实现执行后切面或任何形式响应后处理的装饰器，若发生 I/O、协议或运行时异常，或者响应未传递给调用方，
 * 必须通过调用ClassicHttpResponse.close()方法释放与响应相关的资源。
 * @author liyibo
 * @date 2026-04-16 18:19
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface ExecChainHandler {

    /**
     * 执行实际的HTTP请求。处理程序可以选择返回响应消息，或将请求执行委托给执行链中的下一个元素。
     */
    ClassicHttpResponse execute(ClassicHttpRequest request, ExecChain.Scope scope, ExecChain chain) throws IOException, HttpException;
}
