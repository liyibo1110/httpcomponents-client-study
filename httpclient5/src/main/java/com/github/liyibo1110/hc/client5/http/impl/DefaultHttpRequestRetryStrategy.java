package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.client5.http.HttpRequestRetryStrategy;
import com.github.liyibo1110.hc.client5.http.utils.DateUtils;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.concurrent.CancellableDependency;
import com.github.liyibo1110.hc.core5.http.ConnectionClosedException;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpHeaders;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpStatus;
import com.github.liyibo1110.hc.core5.http.Method;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TimeValue;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * HttpRequestRetryStrategy接口的默认实现。
 * @author liyibo
 * @date 2026-04-16 17:09
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultHttpRequestRetryStrategy implements HttpRequestRetryStrategy {

    public static final DefaultHttpRequestRetryStrategy INSTANCE = new DefaultHttpRequestRetryStrategy();

    private final int maxRetries;

    private final TimeValue defaultRetryInterval;

    /** 不能retry的IOException */
    private final Set<Class<? extends IOException>> nonRetriableIOExceptionClasses;

    /** 可以retry的响应码 */
    private final Set<Integer> retriableCodes;

    protected DefaultHttpRequestRetryStrategy(final int maxRetries,
                                              final TimeValue defaultRetryInterval,
                                              final Collection<Class<? extends IOException>> clazzes,
                                              final Collection<Integer> codes) {
        Args.notNegative(maxRetries, "maxRetries");
        Args.notNegative(defaultRetryInterval.getDuration(), "defaultRetryInterval");
        this.maxRetries = maxRetries;
        this.defaultRetryInterval = defaultRetryInterval;
        this.nonRetriableIOExceptionClasses = new HashSet<>(clazzes);
        this.retriableCodes = new HashSet<>(codes);
    }

    public DefaultHttpRequestRetryStrategy(final int maxRetries, final TimeValue defaultRetryInterval) {
        this(maxRetries, defaultRetryInterval,
                Arrays.asList(
                        InterruptedIOException.class,
                        UnknownHostException.class,
                        ConnectException.class,
                        ConnectionClosedException.class,
                        NoRouteToHostException.class,
                        SSLException.class),
                Arrays.asList(
                        HttpStatus.SC_TOO_MANY_REQUESTS,
                        HttpStatus.SC_SERVICE_UNAVAILABLE));
    }

    public DefaultHttpRequestRetryStrategy() {
        this(1, TimeValue.ofSeconds(1L));
    }

    @Override
    public boolean retryRequest(final HttpRequest request, final IOException exception, final int execCount, final HttpContext context) {
        Args.notNull(request, "request");
        Args.notNull(exception, "exception");

        if (execCount > this.maxRetries)
            return false;

        if (this.nonRetriableIOExceptionClasses.contains(exception.getClass())) {
            return false;
        } else {
            for (final Class<? extends IOException> rejectException : this.nonRetriableIOExceptionClasses) {
                if (rejectException.isInstance(exception))
                    return false;
            }
        }
        if (request instanceof CancellableDependency && ((CancellableDependency) request).isCancelled())
            return false;

        return handleAsIdempotent(request);
    }

    @Override
    public boolean retryRequest(final HttpResponse response, final int execCount, final HttpContext context) {
        Args.notNull(response, "response");
        return execCount <= this.maxRetries && retriableCodes.contains(response.getCode());
    }

    @Override
    public TimeValue getRetryInterval(final HttpResponse response, final int execCount, final HttpContext context) {
        Args.notNull(response, "response");

        final Header header = response.getFirstHeader(HttpHeaders.RETRY_AFTER);
        TimeValue retryAfter = null;
        if (header != null) {
            final String value = header.getValue();
            try {
                retryAfter = TimeValue.ofSeconds(Long.parseLong(value));
            } catch (final NumberFormatException ignore) {
                final Instant retryAfterDate = DateUtils.parseStandardDate(value);
                if (retryAfterDate != null)
                    retryAfter = TimeValue.ofMilliseconds(retryAfterDate.toEpochMilli() - System.currentTimeMillis());
            }
            if (TimeValue.isPositive(retryAfter))
                return retryAfter;
        }
        return this.defaultRetryInterval;
    }

    /**
     * 检测request中的method是否为幂等。
     */
    protected boolean handleAsIdempotent(final HttpRequest request) {
        return Method.isIdempotent(request.getMethod());
    }
}
