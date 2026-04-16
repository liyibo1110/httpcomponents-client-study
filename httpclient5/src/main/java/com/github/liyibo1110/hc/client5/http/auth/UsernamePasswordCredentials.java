package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;

/**
 * 基于用户名/密码对的简易Credentials表示法。
 * @author liyibo
 * @date 2026-04-15 13:43
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class UsernamePasswordCredentials implements Credentials, Serializable {
    private static final long serialVersionUID = 243343858802739403L;

    private final BasicUserPrincipal principal;
    private final char[] password;

    public UsernamePasswordCredentials(final String userName, final char[] password) {
        super();
        Args.notNull(userName, "Username");
        this.principal = new BasicUserPrincipal(userName);
        this.password = password;
    }

    @Override
    public Principal getUserPrincipal() {
        return this.principal;
    }

    public String getUserName() {
        return this.principal.getName();
    }

    @Override
    public char[] getPassword() {
        return password;
    }

    @Override
    public int hashCode() {
        return this.principal.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o instanceof UsernamePasswordCredentials) {
            final UsernamePasswordCredentials that = (UsernamePasswordCredentials) o;
            return Objects.equals(this.principal, that.principal);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.principal.toString();
    }
}
