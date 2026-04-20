package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CommonCookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.CookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpec;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Asserts;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一种抽象的Cookie规范，可将解析、验证或匹配Cookie属性的任务委托给任意数量的CookieAttributeHandler。
 * @author liyibo
 * @date 2026-04-19 11:09
 */
@Contract(threading = ThreadingBehavior.SAFE)
public abstract class AbstractCookieSpec implements CookieSpec {

    /** attribute name -> attribute handler */
    private final Map<String, CookieAttributeHandler> attribHandlerMap;

    public AbstractCookieSpec() {
        super();
        this.attribHandlerMap = new ConcurrentHashMap<>(10);
    }

    protected AbstractCookieSpec(final HashMap<String, CookieAttributeHandler> map) {
        super();
        Asserts.notNull(map, "Attribute handler map");
        this.attribHandlerMap = new ConcurrentHashMap<>(map);
    }

    protected AbstractCookieSpec(final CommonCookieAttributeHandler... handlers) {
        super();
        this.attribHandlerMap = new ConcurrentHashMap<>(handlers.length);
        for (final CommonCookieAttributeHandler handler: handlers)
            this.attribHandlerMap.put(handler.getAttributeName(), handler);
    }

    protected CookieAttributeHandler findAttribHandler(final String name) {
        return this.attribHandlerMap.get(name);
    }

    protected CookieAttributeHandler getAttribHandler(final String name) {
        final CookieAttributeHandler handler = findAttribHandler(name);
        Asserts.check(handler != null, "Handler not registered for " + name + " attribute");
        return handler;
    }

    protected Collection<CookieAttributeHandler> getAttribHandlers() {
        return this.attribHandlerMap.values();
    }
}
