package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.HttpResponseException;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpStatus;
import com.github.liyibo1110.hc.core5.http.io.HttpClientResponseHandler;
import com.github.liyibo1110.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;

/**
 * 一个通用的HttpClientResponseHandler，用于处理成功2xx响应的响应实体。
 * 如果响应码大于等于300，则会读取响应正文并抛出HttpResponseException。
 * 若将其与HttpClient.execute(ClassicHttpRequest, HttpClientResponseHandler) 配合使用，HttpClient可能会在内部处理重定向3xx响应。
 *
 * @author liyibo
 * @date 2026-04-17 12:41
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public abstract class AbstractHttpClientResponseHandler<T> implements HttpClientResponseHandler<T> {

    /**
     * 从响应正文中读取实体，并在响应成功（状态码为2xx）时将其传递给实体处理方法。
     * 如果不存在响应正文，则返回null。
     * 如果响应失败（状态码 >= 300），则抛出HttpResponseException。
     */
    @Override
    public T handleResponse(final ClassicHttpResponse response) throws IOException {
        final HttpEntity entity = response.getEntity();
        if (response.getCode() >= HttpStatus.SC_REDIRECTION) {
            EntityUtils.consume(entity);
            throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
        }
        return entity == null ? null : handleEntity(entity);
    }

    /**
     * 处理响应实体，并将其转换为实际的响应对象。
     */
    public abstract T handleEntity(HttpEntity entity) throws IOException;
}
