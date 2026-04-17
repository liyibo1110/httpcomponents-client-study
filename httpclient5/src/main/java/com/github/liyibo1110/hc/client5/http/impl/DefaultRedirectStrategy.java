package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.client5.http.protocol.RedirectStrategy;
import com.github.liyibo1110.hc.client5.http.utils.URIUtils;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHeaders;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpStatus;
import com.github.liyibo1110.hc.core5.http.ProtocolException;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.net.URIBuilder;
import com.github.liyibo1110.hc.core5.util.Args;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * RedirectStrategy的默认实现类，直接根据header响应头的location和状态码来判断是否要重定向。
 * @author liyibo
 * @date 2026-04-16 16:44
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultRedirectStrategy implements RedirectStrategy {


    public static final DefaultRedirectStrategy INSTANCE = new DefaultRedirectStrategy();

    @Override
    public boolean isRedirected(final HttpRequest request, final HttpResponse response, final HttpContext context) throws ProtocolException {
        Args.notNull(request, "HTTP request");
        Args.notNull(response, "HTTP response");

        if (!response.containsHeader(HttpHeaders.LOCATION))
            return false;
        final int statusCode = response.getCode();
        switch (statusCode) {
            case HttpStatus.SC_MOVED_PERMANENTLY:
            case HttpStatus.SC_MOVED_TEMPORARILY:
            case HttpStatus.SC_SEE_OTHER:
            case HttpStatus.SC_TEMPORARY_REDIRECT:
            case HttpStatus.SC_PERMANENT_REDIRECT:
                return true;
            default:
                return false;
        }
    }

    @Override
    public URI getLocationURI(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException {
        Args.notNull(request, "HTTP request");
        Args.notNull(response, "HTTP response");
        Args.notNull(context, "HTTP context");

        //get the location header to find out where to redirect to
        final Header locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
        if (locationHeader == null)
            throw new HttpException("Redirect location is missing");

        final String location = locationHeader.getValue();
        URI uri = createLocationURI(location);
        try {
            if (!uri.isAbsolute()) {
                // Resolve location URI
                uri = URIUtils.resolve(request.getUri(), uri);
            }
        } catch (final URISyntaxException ex) {
            throw new ProtocolException(ex.getMessage(), ex);
        }

        return uri;
    }

    protected URI createLocationURI(final String location) throws ProtocolException {
        try {
            final URIBuilder b = new URIBuilder(new URI(location).normalize());
            final String host = b.getHost();
            if (host != null)
                b.setHost(host.toLowerCase(Locale.ROOT));
            if (b.isPathEmpty())
                b.setPathSegments("");
            return b.build();
        } catch (final URISyntaxException ex) {
            throw new ProtocolException("Invalid redirect URI: " + location, ex);
        }
    }
}
