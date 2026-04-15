package com.github.liyibo1110.hc.client5.http.config;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ssl.TLS;
import com.github.liyibo1110.hc.core5.util.Timeout;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 封装TLS协议设置的不可变类。
 * @author liyibo
 * @date 2026-04-14 17:09
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class TlsConfig implements Cloneable {

    public static final TlsConfig DEFAULT = new Builder().build();

    private final Timeout handshakeTimeout;
    private final String[] supportedProtocols;
    private final String[] supportedCipherSuites;
    private final HttpVersionPolicy httpVersionPolicy;

    protected TlsConfig() {
        this(null, null, null, null);
    }

    TlsConfig(final Timeout handshakeTimeout,
              final String[] supportedProtocols,
              final String[] supportedCipherSuites,
              final HttpVersionPolicy httpVersionPolicy) {
        super();
        this.handshakeTimeout = handshakeTimeout;
        this.supportedProtocols = supportedProtocols;
        this.supportedCipherSuites = supportedCipherSuites;
        this.httpVersionPolicy = httpVersionPolicy;
    }

    public Timeout getHandshakeTimeout() {
        return handshakeTimeout;
    }

    public String[] getSupportedProtocols() {
        return supportedProtocols != null ? supportedProtocols.clone() : null;
    }

    public String[] getSupportedCipherSuites() {
        return supportedCipherSuites != null ? supportedCipherSuites.clone() : null;
    }

    public HttpVersionPolicy getHttpVersionPolicy() {
        return httpVersionPolicy;
    }

    @Override
    protected TlsConfig clone() throws CloneNotSupportedException {
        return (TlsConfig) super.clone();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append("handshakeTimeout=").append(handshakeTimeout);
        builder.append(", supportedProtocols=").append(Arrays.toString(supportedProtocols));
        builder.append(", supportedCipherSuites=").append(Arrays.toString(supportedCipherSuites));
        builder.append(", httpVersionPolicy=").append(httpVersionPolicy);
        builder.append("]");
        return builder.toString();
    }

    public static TlsConfig.Builder custom() {
        return new Builder();
    }

    public static TlsConfig.Builder copy(final TlsConfig config) {
        return new Builder()
                .setHandshakeTimeout(config.getHandshakeTimeout())
                .setSupportedProtocols(config.getSupportedProtocols())
                .setSupportedCipherSuites(config.getSupportedCipherSuites())
                .setVersionPolicy(config.getHttpVersionPolicy());
    }

    public static class Builder {
        private Timeout handshakeTimeout;
        private String[] supportedProtocols;
        private String[] supportedCipherSuites;
        private HttpVersionPolicy versionPolicy;

        public Builder setHandshakeTimeout(final Timeout handshakeTimeout) {
            this.handshakeTimeout = handshakeTimeout;
            return this;
        }

        public Builder setHandshakeTimeout(final long handshakeTimeout, final TimeUnit timeUnit) {
            this.handshakeTimeout = Timeout.of(handshakeTimeout, timeUnit);
            return this;
        }

        public Builder setSupportedProtocols(final String... supportedProtocols) {
            this.supportedProtocols = supportedProtocols;
            return this;
        }

        public Builder setSupportedProtocols(final TLS... supportedProtocols) {
            this.supportedProtocols = new String[supportedProtocols.length];
            for (int i = 0; i < supportedProtocols.length; i++) {
                final TLS protocol = supportedProtocols[i];
                if (protocol != null)
                    this.supportedProtocols[i] = protocol.id;
            }
            return this;
        }

        public Builder setSupportedCipherSuites(final String... supportedCipherSuites) {
            this.supportedCipherSuites = supportedCipherSuites;
            return this;
        }

        public Builder setVersionPolicy(final HttpVersionPolicy versionPolicy) {
            this.versionPolicy = versionPolicy;
            return this;
        }

        public TlsConfig build() {
            return new TlsConfig(
                    handshakeTimeout,
                    supportedProtocols,
                    supportedCipherSuites,
                    versionPolicy != null ? versionPolicy : HttpVersionPolicy.NEGOTIATE);
        }
    }
}
