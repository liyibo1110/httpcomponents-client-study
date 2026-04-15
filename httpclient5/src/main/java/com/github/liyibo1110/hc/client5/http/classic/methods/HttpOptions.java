package com.github.liyibo1110.hc.client5.http.classic.methods;

import com.github.liyibo1110.hc.core5.http.HeaderElement;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.message.MessageSupport;
import com.github.liyibo1110.hc.core5.util.Args;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * HTTP OPTIONS请求。
 * @author liyibo
 * @date 2026-04-14 19:19
 */
public class HttpOptions extends HttpUriRequestBase {
    private static final long serialVersionUID = 1L;

    public final static String METHOD_NAME = "OPTIONS";

    public HttpOptions(final URI uri) {
        super(METHOD_NAME, uri);
    }

    public HttpOptions(final String uri) {
        this(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }

    /**
     * 查看给定response的Allow头，返回可以支持的method name。
     */
    public Set<String> getAllowedMethods(final HttpResponse response) {
        Args.notNull(response, "HTTP response");
        final Iterator<HeaderElement> it = MessageSupport.iterate(response, "Allow");
        final Set<String> methods = new HashSet<>();
        while (it.hasNext()) {
            final HeaderElement element = it.next();
            methods.add(element.getName());
        }
        return methods;
    }
}
