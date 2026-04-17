package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpStatus;
import com.github.liyibo1110.hc.core5.http.Method;
import com.github.liyibo1110.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * 这是对核心类DefaultConnectionReuseStrategy的扩展，它将代理隧道中涉及的CONNECT方法交换视为一种特殊情况。
 * @author liyibo
 * @date 2026-04-16 17:01
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultClientConnectionReuseStrategy extends DefaultConnectionReuseStrategy {

    public static final DefaultClientConnectionReuseStrategy INSTANCE = new DefaultClientConnectionReuseStrategy();

    @Override
    public boolean keepAlive(final HttpRequest request, final HttpResponse response, final HttpContext context) {
        if (Method.CONNECT.isSame(request.getMethod()) && response.getCode() == HttpStatus.SC_OK)
            return true;
        return super.keepAlive(request, response, context);
    }
}
