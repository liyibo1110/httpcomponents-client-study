package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.LangUtils;

import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;

/**
 * 基于用户名的Principal实现类。
 * @author liyibo
 * @date 2026-04-15 13:36
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class BasicUserPrincipal implements Principal, Serializable {
    private static final long serialVersionUID = -2266305184969850467L;

    private final String username;

    public BasicUserPrincipal(final String username) {
        super();
        Args.notNull(username, "User name");
        this.username = username;
    }

    @Override
    public String getName() {
        return this.username;
    }

    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.username);
        return hash;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o instanceof BasicUserPrincipal) {
            final BasicUserPrincipal that = (BasicUserPrincipal) o;
            return Objects.equals(this.username, that.username);
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("[principal: ");
        buffer.append(this.username);
        buffer.append("]");
        return buffer.toString();
    }
}
