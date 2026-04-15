package com.github.liyibo1110.hc.client5.http.utils;

import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.URIScheme;
import com.github.liyibo1110.hc.core5.net.URIAuthority;
import com.github.liyibo1110.hc.core5.net.URIBuilder;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TextUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * 一组用于URI的实用工具，用于解决类中的缺陷或提供便捷功能。
 * @author liyibo
 * @date 2026-04-14 11:06
 */
public final class URIUtils {

    private URIUtils() {}

    /**
     * 一种便捷方法，用于创建一个新的 URI：
     * 其schema、host和port取自目标主机，而path、query和fragment则取自现有URI。
     * 只有当dropFragment为false时，才会使用fragment。如果未显式指定，path将设置为 “/”。
     */
    @Deprecated
    public static URI rewriteURI(final URI uri, final HttpHost target, final boolean dropFragment) throws URISyntaxException {
        Args.notNull(uri, "URI");
        if (uri.isOpaque())
            return uri;
        final URIBuilder uribuilder = new URIBuilder(uri);
        if (target != null) {
            uribuilder.setScheme(target.getSchemeName());
            uribuilder.setHost(target.getHostName());
            uribuilder.setPort(target.getPort());
        } else {
            uribuilder.setScheme(null);
            uribuilder.setHost((String) null);
            uribuilder.setPort(-1);
        }
        if (dropFragment)
            uribuilder.setFragment(null);
        final List<String> originalPathSegments = uribuilder.getPathSegments();
        final List<String> pathSegments = new ArrayList<>(originalPathSegments);
        for (final Iterator<String> it = pathSegments.iterator(); it.hasNext(); ) {
            final String pathSegment = it.next();
            if (pathSegment.isEmpty() && it.hasNext())
                it.remove();
        }
        if (pathSegments.size() != originalPathSegments.size())
            uribuilder.setPathSegments(pathSegments);
        if (pathSegments.isEmpty())
            uribuilder.setPathSegments("");
        return uribuilder.build();
    }

    @Deprecated
    public static URI rewriteURI(final URI uri, final HttpHost target) throws URISyntaxException {
        return rewriteURI(uri, target, false);
    }

    @Deprecated
    public static URI rewriteURI(final URI uri) throws URISyntaxException {
        Args.notNull(uri, "URI");
        if (uri.isOpaque())
            return uri;
        final URIBuilder uribuilder = new URIBuilder(uri);
        if (uribuilder.getUserInfo() != null)
            uribuilder.setUserInfo(null);
        if (uribuilder.isPathEmpty())
            uribuilder.setPathSegments("");
        if (uribuilder.getHost() != null)
            uribuilder.setHost(uribuilder.getHost().toLowerCase(Locale.ROOT));
        uribuilder.setFragment(null);
        return uribuilder.build();
    }

    /**
     * 根据基URI解析URI引用。用于解决java.net.URI中的错误（例如：http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4708535）
     */
    public static URI resolve(final URI baseURI, final String reference) {
        return resolve(baseURI, URI.create(reference));
    }

    public static URI resolve(final URI baseURI, final URI reference) {
        Args.notNull(baseURI, "Base URI");
        Args.notNull(reference, "Reference URI");
        final String s = reference.toASCIIString();
        if (s.startsWith("?")) {
            String baseUri = baseURI.toASCIIString();
            final int i = baseUri.indexOf('?');
            baseUri = i > -1 ? baseUri.substring(0, i) : baseUri;
            return URI.create(baseUri + s);
        }
        final boolean emptyReference = s.isEmpty();
        URI resolved;
        if (emptyReference) {
            resolved = baseURI.resolve(URI.create("#"));
            final String resolvedString = resolved.toASCIIString();
            resolved = URI.create(resolvedString.substring(0, resolvedString.indexOf('#')));
        } else {
            resolved = baseURI.resolve(reference);
        }
        try {
            return normalizeSyntax(resolved);
        } catch (final URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * 移除点分隔符并执行基于语法的规范化。
     */
    static URI normalizeSyntax(final URI uri) throws URISyntaxException {
        if (uri.isOpaque() || uri.getAuthority() == null) {
            // opaque and file: URIs
            return uri;
        }
        final URIBuilder builder = new URIBuilder(uri);
        final String scheme = builder.getScheme();
        if (scheme == null)
            builder.setScheme(URIScheme.HTTP.id);
        else
            builder.setScheme(TextUtils.toLowerCase(scheme));
        final String host = builder.getHost();
        if (host != null)
            builder.setHost(TextUtils.toLowerCase(host));
        if (builder.isPathEmpty())
            builder.setPathSegments("");
        return builder.build();
    }

    /**
     * 从给定的URI对象中，提取host信息（即schema、host和port）。
     */
    public static HttpHost extractHost(final URI uri) {
        if (uri == null)
            return null;
        final URIBuilder uriBuilder = new URIBuilder(uri);
        final String scheme = uriBuilder.getScheme();
        final String host = uriBuilder.getHost();
        final int port = uriBuilder.getPort();
        if (!TextUtils.isBlank(host)) {
            try {
                return new HttpHost(scheme, host, port);
            } catch (final IllegalArgumentException ignore) {

            }
        }
        return null;
    }

    public static URI resolve(final URI originalURI, final HttpHost target, final List<URI> redirects) throws URISyntaxException {
        Args.notNull(originalURI, "Request URI");
        final URIBuilder uribuilder;
        if (redirects == null || redirects.isEmpty()) {
            uribuilder = new URIBuilder(originalURI);
        } else {
            uribuilder = new URIBuilder(redirects.get(redirects.size() - 1));
            String frag = uribuilder.getFragment();
            // read interpreted fragment identifier from redirect locations
            for (int i = redirects.size() - 1; frag == null && i >= 0; i--)
                frag = redirects.get(i).getFragment();
            uribuilder.setFragment(frag);
        }
        // read interpreted fragment identifier from original request
        if (uribuilder.getFragment() == null)
            uribuilder.setFragment(originalURI.getFragment());
        // last target origin
        if (target != null && !uribuilder.isAbsolute()) {
            uribuilder.setScheme(target.getSchemeName());
            uribuilder.setHost(target.getHostName());
            uribuilder.setPort(target.getPort());
        }
        return uribuilder.build();
    }

    @Deprecated
    public static URI create(final HttpHost host, final String path) throws URISyntaxException {
        final URIBuilder builder = new URIBuilder(path);
        if (host != null)
            builder.setHost(host.getHostName()).setPort(host.getPort()).setScheme(host.getSchemeName());
        return builder.build();
    }

    @Deprecated
    public static URI create(final String scheme, final URIAuthority host, final String path) throws URISyntaxException {
        final URIBuilder builder = new URIBuilder(path);
        if (scheme != null)
            builder.setScheme(scheme);
        if (host != null)
            builder.setHost(host.getHostName()).setPort(host.getPort());
        return builder.build();
    }
}
