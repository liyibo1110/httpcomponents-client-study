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
 * 一种针对Microsoft Windows的凭据表示形式，其中包含Windows特有的属性，例如用户所属域的名称。
 * @author liyibo
 * @date 2026-04-15 14:00
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class NTCredentials implements Credentials, Serializable {
    private static final long serialVersionUID = -7385699315228907265L;

    private final NTUserPrincipal principal;
    private final char[] password;
    private final String workstation;
    private final String netbiosDomain;

    public NTCredentials(final String userName, final char[] password, final String workstation, final String domain) {
        this(userName, password, convertHost(workstation), domain, convertDomain(domain));
    }

    public NTCredentials(final String userName, final char[] password, final String workstation, final String domain, final String netbiosDomain) {
        super();
        Args.notNull(userName, "User name");
        this.principal = new NTUserPrincipal(domain, userName);
        this.password = password;
        if (workstation != null)
            this.workstation = workstation.toUpperCase(Locale.ROOT);
        else
            this.workstation = null;
        this.netbiosDomain = netbiosDomain;
    }

    @Override
    public Principal getUserPrincipal() {
        return this.principal;
    }

    public String getUserName() {
        return this.principal.getUsername();
    }

    @Override
    public char[] getPassword() {
        return this.password;
    }

    public String getDomain() {
        return this.principal.getDomain();
    }

    public String getNetbiosDomain() {
        return this.netbiosDomain;
    }

    public String getWorkstation() {
        return this.workstation;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o instanceof NTCredentials) {
            final NTCredentials that = (NTCredentials) o;
            return Objects.equals(this.principal, that.principal)
                    && Objects.equals(this.workstation, that.workstation)
                    && Objects.equals(this.netbiosDomain, that.netbiosDomain);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.principal);
        hash = LangUtils.hashCode(hash, this.workstation);
        hash = LangUtils.hashCode(hash, this.netbiosDomain);
        return hash;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("[principal: ");
        buffer.append(this.principal);
        buffer.append("][workstation: ");
        buffer.append(this.workstation);
        buffer.append("][netbiosDomain: ");
        buffer.append(this.netbiosDomain);
        buffer.append("]");
        return buffer.toString();
    }

    /** Strip dot suffix from a name */
    private static String stripDotSuffix(final String value) {
        if (value == null)
            return null;
        final int index = value.indexOf('.');
        if (index != -1)
            return value.substring(0, index);
        return value;
    }

    /** Convert host to standard form */
    private static String convertHost(final String host) {
        return stripDotSuffix(host);
    }

    /** Convert domain to standard form */
    private static String convertDomain(final String domain) {
        final String returnString = stripDotSuffix(domain);
        return returnString == null ? returnString : returnString.toUpperCase(Locale.ROOT);
    }
}
