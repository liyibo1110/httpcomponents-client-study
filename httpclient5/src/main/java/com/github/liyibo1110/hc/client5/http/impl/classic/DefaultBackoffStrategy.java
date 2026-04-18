package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.classic.ConnectionBackoffStrategy;
import com.github.liyibo1110.hc.core5.annotation.Experimental;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpStatus;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * ConnectionBackoffStrategy的默认实现，会在以下任一情况下进行重试：
 * 1、原始socket或连接超时。
 * 2、server明确返回429（请求过多）或503（服务不可用）响应。
 * @author liyibo
 * @date 2026-04-17 12:06
 */
@Experimental
public class DefaultBackoffStrategy implements ConnectionBackoffStrategy {

    @Override
    public boolean shouldBackoff(final Throwable t) {
        return t instanceof SocketTimeoutException || t instanceof ConnectException;
    }

    @Override
    public boolean shouldBackoff(final HttpResponse response) {
        return response.getCode() == HttpStatus.SC_TOO_MANY_REQUESTS ||
                response.getCode() == HttpStatus.SC_SERVICE_UNAVAILABLE;
    }
}
