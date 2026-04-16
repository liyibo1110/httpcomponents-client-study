package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.LangUtils;

import java.util.Locale;
import java.util.Objects;

/**
 * AuthScope代表一个身份验证范围，由应用程序schema、hostname、port、域名称（realm name）和身份验证方案名称组成。
 * @author liyibo
 * @date 2026-04-15 12:57
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class AuthScope {
    private final String protocol;
    private final String host;
    private final int port;
    private final String realm;
    private final String schemeName;

    public AuthScope(final String protocol, final String host, final int port, final String realm, final String schemeName) {
        this.protocol = protocol != null ? protocol.toLowerCase(Locale.ROOT) : null;
        this.host = host != null ? host.toLowerCase(Locale.ROOT) : null;
        this.port = port >= 0 ? port: -1;
        this.realm = realm;
        this.schemeName = schemeName;
    }

    public AuthScope(final HttpHost origin, final String realm, final String schemeName) {
        Args.notNull(origin, "Host");
        this.protocol = origin.getSchemeName().toLowerCase(Locale.ROOT);
        this.host = origin.getHostName().toLowerCase(Locale.ROOT);
        this.port = origin.getPort() >= 0 ? origin.getPort() : -1;
        this.realm = realm;
        this.schemeName = schemeName;
    }

    public AuthScope(final HttpHost origin) {
        this(origin, null, null);
    }

    public AuthScope(final String host, final int port) {
        this(null, host, port, null, null);
    }

    public AuthScope(final AuthScope authScope) {
        super();
        Args.notNull(authScope, "Scope");
        this.protocol = authScope.getProtocol();
        this.host = authScope.getHost();
        this.port = authScope.getPort();
        this.realm = authScope.getRealm();
        this.schemeName = authScope.getSchemeName();
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getRealm() {
        return this.realm;
    }

    public String getSchemeName() {
        return this.schemeName;
    }

    /**
     * 检查2个身份验证范围是否匹配
     * @return 匹配因子。负值表示不匹配。非负值表示匹配。返回值越大，匹配度越高
     */
    public int match(final AuthScope that) {
        int factor = 0;
        // schemeName相等，加1分，不相等直接返回不匹配
        if (Objects.equals(toNullSafeLowerCase(this.schemeName), toNullSafeLowerCase(that.schemeName))) {
            factor += 1;
        } else {
            if (this.schemeName != null && that.schemeName != null)
                return -1;
        }
        // schemeName相等，加2分，不相等直接返回不匹配
        if (Objects.equals(this.realm, that.realm)) {
            factor += 2;
        } else {
            if (this.realm != null && that.realm != null)
                return -1;
        }
        // port相等，加4分，不相等直接返回不匹配
        if (this.port == that.port) {
            factor += 4;
        } else {
            if (this.port != -1 && that.port != -1)
                return -1;
        }
        // protocol相等，加8分，不相等直接返回不匹配
        if (Objects.equals(this.protocol, that.protocol)) {
            factor += 8;
        } else {
            if (this.protocol != null && that.protocol != null)
                return -1;
        }
        // host相等，加8分，不相等直接返回不匹配
        if (Objects.equals(this.host, that.host)) {
            factor += 16;
        } else {
            if (this.host != null && that.host != null)
                return -1;
        }
        return factor;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof AuthScope) {
            final AuthScope that = (AuthScope) obj;
            return Objects.equals(this.protocol, that.protocol)
                    && Objects.equals(this.host, that.host)
                    && this.port == that.port
                    && Objects.equals(this.realm, that.realm)
                    && Objects.equals(toNullSafeLowerCase(this.schemeName),
                    toNullSafeLowerCase(that.schemeName));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.protocol);
        hash = LangUtils.hashCode(hash, this.host);
        hash = LangUtils.hashCode(hash, this.port);
        hash = LangUtils.hashCode(hash, this.realm);
        hash = LangUtils.hashCode(hash, toNullSafeLowerCase(this.schemeName));
        return hash;
    }

    private String toNullSafeLowerCase(final String str) {
        return str != null ? str.toLowerCase(Locale.ROOT) : null;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        if (this.schemeName != null)
            buffer.append(this.schemeName);
        else
            buffer.append("<any auth scheme>");
        buffer.append(' ');
        if (this.realm != null) {
            buffer.append('\'');
            buffer.append(this.realm);
            buffer.append('\'');
        } else {
            buffer.append("<any realm>");
        }
        buffer.append(' ');
        if (this.protocol != null)
            buffer.append(this.protocol);
        else
            buffer.append("<any protocol>");
        buffer.append("://");
        if (this.host != null)
            buffer.append(this.host);
        else
            buffer.append("<any host>");
        buffer.append(':');
        if (this.port >= 0)
            buffer.append(this.port);
        else
            buffer.append("<any port>");
        return buffer.toString();
    }
}
