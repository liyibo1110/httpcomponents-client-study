package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

/**
 * 封装 Kerberos 配置选项的不可变类。
 * @author liyibo
 * @date 2026-04-15 13:56
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class KerberosConfig implements Cloneable {
    public enum Option { DEFAULT, ENABLE, DISABLE }

    public static final KerberosConfig DEFAULT = new Builder().build();

    private final Option stripPort;
    private final Option useCanonicalHostname;
    private final Option requestDelegCreds;

    protected KerberosConfig() {
        this(Option.DEFAULT, Option.DEFAULT, Option.DEFAULT);
    }

    KerberosConfig(final Option stripPort, final Option useCanonicalHostname, final Option requestDelegCreds) {
        super();
        this.stripPort = stripPort;
        this.useCanonicalHostname = useCanonicalHostname;
        this.requestDelegCreds = requestDelegCreds;
    }

    public Option getStripPort() {
        return stripPort;
    }

    public Option getUseCanonicalHostname() {
        return useCanonicalHostname;
    }

    public Option getRequestDelegCreds() {
        return requestDelegCreds;
    }

    @Override
    protected KerberosConfig clone() throws CloneNotSupportedException {
        return (KerberosConfig) super.clone();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append("stripPort=").append(stripPort);
        builder.append(", useCanonicalHostname=").append(useCanonicalHostname);
        builder.append(", requestDelegCreds=").append(requestDelegCreds);
        builder.append("]");
        return builder.toString();
    }

    public static KerberosConfig.Builder custom() {
        return new Builder();
    }

    public static KerberosConfig.Builder copy(final KerberosConfig config) {
        return new Builder()
                .setStripPort(config.getStripPort())
                .setUseCanonicalHostname(config.getUseCanonicalHostname())
                .setRequestDelegCreds(config.getRequestDelegCreds());
    }

    public static class Builder {
        private Option stripPort;
        private Option useCanonicalHostname;
        private Option requestDelegCreds;

        Builder() {
            super();
            this.stripPort = Option.DEFAULT;
            this.useCanonicalHostname = Option.DEFAULT;
            this.requestDelegCreds = Option.DEFAULT;
        }

        public Builder setStripPort(final Option stripPort) {
            this.stripPort = stripPort;
            return this;
        }

        public Builder setStripPort(final boolean stripPort) {
            this.stripPort = stripPort ? Option.ENABLE : Option.DISABLE;
            return this;
        }

        public Builder setUseCanonicalHostname(final Option useCanonicalHostname) {
            this.useCanonicalHostname = useCanonicalHostname;
            return this;
        }

        public Builder setUseCanonicalHostname(final boolean useCanonicalHostname) {
            this.useCanonicalHostname = useCanonicalHostname ? Option.ENABLE : Option.DISABLE;
            return this;
        }

        public Builder setRequestDelegCreds(final Option requestDelegCreds) {
            this.requestDelegCreds = requestDelegCreds;
            return this;
        }

        public KerberosConfig build() {
            return new KerberosConfig(stripPort, useCanonicalHostname, requestDelegCreds);
        }
    }
}
