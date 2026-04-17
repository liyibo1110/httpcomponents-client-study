package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.net.PercentCodec;
import com.github.liyibo1110.hc.core5.net.URIBuilder;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * protocol相关的工具方法
 * @author liyibo
 * @date 2026-04-16 16:16
 */
@Internal
public class RequestSupport {

    public static String extractPathPrefix(final HttpRequest request) {
        final String path = request.getPath();
        try {
            final URIBuilder uriBuilder = new URIBuilder(path);
            uriBuilder.setFragment(null);
            uriBuilder.clearParameters();
            uriBuilder.normalizeSyntax();
            final List<String> pathSegments = uriBuilder.getPathSegments();

            if (!pathSegments.isEmpty())
                pathSegments.remove(pathSegments.size() - 1);

            if (pathSegments.isEmpty()) {
                return "/";
            } else {
                final StringBuilder buf = new StringBuilder();
                buf.append('/');
                for (final String pathSegment : pathSegments) {
                    PercentCodec.encode(buf, pathSegment, StandardCharsets.US_ASCII);
                    buf.append('/');
                }
                return buf.toString();
            }
        } catch (final URISyntaxException ex) {
            return path;
        }
    }
}
