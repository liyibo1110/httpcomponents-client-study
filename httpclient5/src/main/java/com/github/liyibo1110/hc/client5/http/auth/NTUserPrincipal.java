package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.LangUtils;

import java.io.Serializable;
import java.security.Principal;
import java.util.Locale;
import java.util.Objects;

/**
 * Microsoft Windows特定的用户主体实现。
 * @author liyibo
 * @date 2026-04-15 13:58
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class NTUserPrincipal implements Principal, Serializable {
    private static final long serialVersionUID = -6870169797924406894L;

    private final String username;
    private final String domain;
    private final String ntname;

    public NTUserPrincipal(final String domain, final String username) {
        super();
        Args.notNull(username, "User name");
        this.username = username;
        if (domain != null)
            this.domain = domain.toUpperCase(Locale.ROOT);
        else
            this.domain = null;

        if (this.domain != null && !this.domain.isEmpty()) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append(this.domain);
            buffer.append('\\');
            buffer.append(this.username);
            this.ntname = buffer.toString();
        } else {
            this.ntname = this.username;
        }
    }

    public String getUsername() {
        return this.username;
    }

    public String getDomain() {
        return this.domain;
    }

    @Override
    public String getName() {
        return this.ntname;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o instanceof NTUserPrincipal) {
            final NTUserPrincipal that = (NTUserPrincipal) o;
            return Objects.equals(this.username, that.username) && Objects.equals(this.domain, that.domain);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.username);
        hash = LangUtils.hashCode(hash, this.domain);
        return hash;
    }

    @Override
    public String toString() {
        return this.ntname;
    }
}
