package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.net.NamedEndpoint;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * ConnectionException相关支持方法。
 * @author liyibo
 * @date 2026-04-14 14:19
 */
@Internal
public final class ConnectExceptionSupport {

    private ConnectExceptionSupport() {}

    public static ConnectTimeoutException createConnectTimeoutException(final IOException cause,
                                                                        final NamedEndpoint namedEndpoint,
                                                                        final InetAddress... remoteAddresses) {
        final String message = "Connect to " +
                (namedEndpoint != null ? namedEndpoint : "remote endpoint") +
                (remoteAddresses != null && remoteAddresses.length > 0 ? " " + Arrays.asList(remoteAddresses) : "") +
                ((cause != null && cause.getMessage() != null) ? " failed: " + cause.getMessage() : " timed out");
        return new ConnectTimeoutException(message, namedEndpoint);
    }

    public static HttpHostConnectException createHttpHostConnectException(final IOException cause,
                                                                          final NamedEndpoint namedEndpoint,
                                                                          final InetAddress... remoteAddresses) {
        final String message = "Connect to " +
                (namedEndpoint != null ? namedEndpoint : "remote endpoint") +
                (remoteAddresses != null && remoteAddresses.length > 0 ? " " + Arrays.asList(remoteAddresses) : "") +
                ((cause != null && cause.getMessage() != null) ? " failed: " + cause.getMessage() : " refused");
        return new HttpHostConnectException(message, namedEndpoint);
    }

    public static IOException enhance(final IOException cause,
                                      final NamedEndpoint namedEndpoint,
                                      final InetAddress... remoteAddresses) {
        if (cause instanceof SocketTimeoutException) {
            final IOException ex = createConnectTimeoutException(cause, namedEndpoint, remoteAddresses);
            ex.setStackTrace(cause.getStackTrace());
            return ex;
        } else if (cause instanceof ConnectException) {
            if ("Connection timed out".equals(cause.getMessage())) {
                final IOException ex = createConnectTimeoutException(cause, namedEndpoint, remoteAddresses);
                ex.initCause(cause);
                return ex;
            }
            final IOException ex = createHttpHostConnectException(cause, namedEndpoint, remoteAddresses);
            ex.setStackTrace(cause.getStackTrace());
            return ex;
        } else {
            return cause;
        }
    }
}
