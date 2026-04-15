package com.github.liyibo1110.hc.client5.http.config;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * 封装连接初始化和管理设置的不可变类。
 * @author liyibo
 * @date 2026-04-14 17:14
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class ConnectionConfig implements Cloneable {

    private static final Timeout DEFAULT_CONNECT_TIMEOUT = Timeout.ofMinutes(3);

    public static final ConnectionConfig DEFAULT = new Builder().build();

    private final Timeout connectTimeout;
    private final Timeout socketTimeout;
    private final TimeValue validateAfterInactivity;
    private final TimeValue timeToLive;

    protected ConnectionConfig() {
        this(DEFAULT_CONNECT_TIMEOUT, null, null, null);
    }

    ConnectionConfig(final Timeout connectTimeout,
                     final Timeout socketTimeout,
                     final TimeValue validateAfterInactivity,
                     final TimeValue timeToLive) {
        super();
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.validateAfterInactivity = validateAfterInactivity;
        this.timeToLive = timeToLive;
    }

    public Timeout getSocketTimeout() {
        return socketTimeout;
    }

    public Timeout getConnectTimeout() {
        return connectTimeout;
    }

    public TimeValue getValidateAfterInactivity() {
        return validateAfterInactivity;
    }

    public TimeValue getTimeToLive() {
        return timeToLive;
    }

    @Override
    protected ConnectionConfig clone() throws CloneNotSupportedException {
        return (ConnectionConfig) super.clone();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append("connectTimeout=").append(connectTimeout);
        builder.append(", socketTimeout=").append(socketTimeout);
        builder.append(", validateAfterInactivity=").append(validateAfterInactivity);
        builder.append(", timeToLive=").append(timeToLive);
        builder.append("]");
        return builder.toString();
    }

    public static ConnectionConfig.Builder custom() {
        return new Builder();
    }

    public static ConnectionConfig.Builder copy(final ConnectionConfig config) {
        return new Builder()
                .setConnectTimeout(config.getConnectTimeout())
                .setSocketTimeout(config.getSocketTimeout())
                .setValidateAfterInactivity(config.getValidateAfterInactivity())
                .setTimeToLive(config.getTimeToLive());
    }

    public static class Builder {
        private Timeout socketTimeout;
        private Timeout connectTimeout;
        private TimeValue validateAfterInactivity;
        private TimeValue timeToLive;

        Builder() {
            super();
            this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }

        public Builder setSocketTimeout(final int soTimeout, final TimeUnit timeUnit) {
            this.socketTimeout = Timeout.of(soTimeout, timeUnit);
            return this;
        }

        public Builder setSocketTimeout(final Timeout soTimeout) {
            this.socketTimeout = soTimeout;
            return this;
        }

        public Builder setConnectTimeout(final Timeout connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder setConnectTimeout(final long connectTimeout, final TimeUnit timeUnit) {
            this.connectTimeout = Timeout.of(connectTimeout, timeUnit);
            return this;
        }

        public Builder setValidateAfterInactivity(final TimeValue validateAfterInactivity) {
            this.validateAfterInactivity = validateAfterInactivity;
            return this;
        }

        public Builder setValidateAfterInactivity(final long validateAfterInactivity, final TimeUnit timeUnit) {
            this.validateAfterInactivity = TimeValue.of(validateAfterInactivity, timeUnit);
            return this;
        }

        public Builder setTimeToLive(final TimeValue timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public Builder setTimeToLive(final long timeToLive, final TimeUnit timeUnit) {
            this.timeToLive = TimeValue.of(timeToLive, timeUnit);
            return this;
        }

        public ConnectionConfig build() {
            return new ConnectionConfig(
                    connectTimeout != null ? connectTimeout : DEFAULT_CONNECT_TIMEOUT,
                    socketTimeout,
                    validateAfterInactivity,
                    timeToLive);
        }
    }
}
