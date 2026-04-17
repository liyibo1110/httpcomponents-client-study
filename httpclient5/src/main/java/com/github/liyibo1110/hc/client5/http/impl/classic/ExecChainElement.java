package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.classic.ExecChain;
import com.github.liyibo1110.hc.client5.http.classic.ExecChainHandler;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpException;

import java.io.IOException;

/**
 * @author liyibo
 * @date 2026-04-16 22:09
 */
class ExecChainElement {

    private final ExecChainHandler handler;
    private final ExecChainElement next;

    ExecChainElement(final ExecChainHandler handler, final ExecChainElement next) {
        this.handler = handler;
        this.next = next;
    }

    public ClassicHttpResponse execute(final ClassicHttpRequest request, final ExecChain.Scope scope) throws IOException, HttpException {
        return handler.execute(request, scope, next != null ? next::execute : null);
    }

    @Override
    public String toString() {
        return "{" +
                "handler=" + handler.getClass() +
                ", next=" + (next != null ? next.handler.getClass() : "null") +
                '}';
    }
}
