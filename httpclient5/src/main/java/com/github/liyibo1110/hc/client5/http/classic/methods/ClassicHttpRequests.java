package com.github.liyibo1110.hc.client5.http.classic.methods;

import com.github.liyibo1110.hc.core5.http.Method;
import com.github.liyibo1110.hc.core5.util.Args;

import java.net.URI;

/**
 * 使用HttpUriRequest作为HTTP请求消息表示形式的常用HTTP方法。
 * 每个静态方法都会创建一个HttpUriRequest的精确子类请求对象，且该对象的URI不为空。
 *
 * 建议使用core项目的ClassicRequestBuilder工具类来替代。
 * @author liyibo
 * @date 2026-04-14 19:34
 */
@Deprecated
public class ClassicHttpRequests {

    public static HttpUriRequest create(final Method method, final String uri) {
        return create(method, URI.create(uri));
    }

    public static HttpUriRequest create(final Method method, final URI uri) {
        switch (Args.notNull(method, "method")) {
            case DELETE:
                return delete(uri);
            case GET:
                return get(uri);
            case HEAD:
                return head(uri);
            case OPTIONS:
                return options(uri);
            case PATCH:
                return patch(uri);
            case POST:
                return post(uri);
            case PUT:
                return put(uri);
            case TRACE:
                return trace(uri);
            default:
                throw new IllegalArgumentException(method.toString());
        }
    }

    public static HttpUriRequest create(final String method, final String uri) {
        return create(Method.normalizedValueOf(method), uri);
    }

    public static HttpUriRequest create(final String method, final URI uri) {
        return create(Method.normalizedValueOf(method), uri);
    }

    public static HttpUriRequest delete(final String uri) {
        return delete(URI.create(uri));
    }

    public static HttpUriRequest delete(final URI uri) {
        return new HttpDelete(uri);
    }

    public static HttpUriRequest get(final String uri) {
        return get(URI.create(uri));
    }

    public static HttpUriRequest get(final URI uri) {
        return new HttpGet(uri);
    }

    public static HttpUriRequest head(final String uri) {
        return head(URI.create(uri));
    }

    public static HttpUriRequest head(final URI uri) {
        return new HttpHead(uri);
    }

    public static HttpUriRequest options(final String uri) {
        return options(URI.create(uri));
    }

    public static HttpUriRequest options(final URI uri) {
        return new HttpOptions(uri);
    }

    public static HttpUriRequest patch(final String uri) {
        return patch(URI.create(uri));
    }

    public static HttpUriRequest patch(final URI uri) {
        return new HttpPatch(uri);
    }

    public static HttpUriRequest post(final String uri) {
        return post(URI.create(uri));
    }

    public static HttpUriRequest post(final URI uri) {
        return new HttpPost(uri);
    }

    public static HttpUriRequest put(final String uri) {
        return put(URI.create(uri));
    }

    public static HttpUriRequest put(final URI uri) {
        return new HttpPut(uri);
    }

    public static HttpUriRequest trace(final String uri) {
        return trace(URI.create(uri));
    }

    public static HttpUriRequest trace(final URI uri) {
        return new HttpTrace(uri);
    }
}
