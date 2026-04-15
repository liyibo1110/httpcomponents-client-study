package com.github.liyibo1110.hc.client5.http.classic;

import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.io.HttpClientResponseHandler;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.io.IOException;

/**
 * 该接口仅定义了执行HTTP请求的最基本契约。
 * 它对请求执行过程不作任何限制，也不规定具体细节，而将状态管理、身份验证和重定向处理的具体实现留给各个具体实现。
 * @author liyibo
 * @date 2026-04-14 16:05
 */
public interface HttpClient {

    /**
     * 使用默认上下文执行HTTP请求。
     *
     * 强烈建议使用带有HttpClientResponseHandler的execute方法（例如 execute(ClassicHttpRequest, HttpClientResponseHandler)），以确保客户端能自动释放资源。
     * 对于特殊情况，仍可使用executeOpen(HttpHost, ClassicHttpRequest, HttpContext)来保持响应对象在请求执行后保持打开状态。
     */
    @Deprecated
    HttpResponse execute(ClassicHttpRequest request) throws IOException;

    @Deprecated
    HttpResponse execute(ClassicHttpRequest request, HttpContext context) throws IOException;

    @Deprecated
    ClassicHttpResponse execute(HttpHost target, ClassicHttpRequest request) throws IOException;

    @Deprecated
    HttpResponse execute(HttpHost target, ClassicHttpRequest request, HttpContext context) throws IOException;

    @SuppressWarnings("deprecation")
    default ClassicHttpResponse executeOpen(HttpHost target, ClassicHttpRequest request, HttpContext context) throws IOException {
        return (ClassicHttpResponse) execute(target, request, context);
    }

    /**
     * 使用默认上下文执行HTTP请求，并使用给定的响应处理程序处理响应。
     *
     * 实现该接口的类必须确保在所有情况下，与响应关联的内容实体均被完全处理完毕，且底层连接会自动释放回连接管理器，
     * 从而免除各个HttpClientResponseHandler在内部管理资源释放的负担。
     */
    <T> T execute(ClassicHttpRequest request, HttpClientResponseHandler<? extends T> responseHandler) throws IOException;

    <T> T execute(ClassicHttpRequest request, HttpContext context, HttpClientResponseHandler<? extends T> responseHandler) throws IOException;

    <T> T execute(HttpHost target, ClassicHttpRequest request, HttpClientResponseHandler<? extends T> responseHandler) throws IOException;

    <T> T execute(HttpHost target, ClassicHttpRequest request, HttpContext context, HttpClientResponseHandler<? extends T> responseHandler) throws IOException;
}
