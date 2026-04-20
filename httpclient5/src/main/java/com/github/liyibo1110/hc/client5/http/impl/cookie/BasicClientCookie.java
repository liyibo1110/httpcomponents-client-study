package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.SetCookie;
import com.github.liyibo1110.hc.client5.http.utils.DateUtils;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SetCookie接口的默认实现。
 * @author liyibo
 * @date 2026-04-19 11:27
 */
public class BasicClientCookie implements SetCookie, Cloneable, Serializable {
    private static final long serialVersionUID = -3869795591041535538L;

    /** Cookie name */
    private final String name;

    /** Cookie attributes as specified by the origin server */
    private Map<String, String> attribs;

    /** Cookie value */
    private String value;

    /** Domain attribute. */
    private String  cookieDomain;

    /** Expiration {@link Instant}. */
    private Instant cookieExpiryDate;

    /** Path attribute. */
    private String cookiePath;

    /** My secure flag. */
    private boolean isSecure;

    private Instant creationDate;

    /** The {@code httpOnly} flag. */
    private boolean httpOnly;

    public BasicClientCookie(final String name, final String value) {
        super();
        Args.notNull(name, "Name");
        this.name = name;
        this.attribs = new HashMap<>();
        this.value = value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public void setValue(final String value) {
        this.value = value;
    }

    @Deprecated
    @Override
    public Date getExpiryDate() {
        return DateUtils.toDate(cookieExpiryDate);
    }

    @Override
    public Instant getExpiryInstant() {
        return cookieExpiryDate;
    }

    @Deprecated
    @Override
    public void setExpiryDate(final Date expiryDate) {
        cookieExpiryDate = DateUtils.toInstant(expiryDate);
    }

    @Override
    public void setExpiryDate(final Instant expiryInstant) {
        cookieExpiryDate = expiryInstant;
    }

    @Override
    public boolean isPersistent() {
        return cookieExpiryDate != null;
    }

    @Override
    public String getDomain() {
        return cookieDomain;
    }

    @Override
    public void setDomain(final String domain) {
        if (domain != null)
            cookieDomain = domain.toLowerCase(Locale.ROOT);
        else
            cookieDomain = null;
    }

    @Override
    public String getPath() {
        return cookiePath;
    }

    @Override
    public void setPath(final String path) {
        cookiePath = path;
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public void setSecure(final boolean secure) {
        isSecure = secure;
    }

    @Override
    public void setHttpOnly(final boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    @Deprecated
    @Override
    public boolean isExpired(final Date date) {
        Args.notNull(date, "Date");
        return (cookieExpiryDate != null && cookieExpiryDate.compareTo(DateUtils.toInstant(date)) <= 0);
    }

    @Override
    public boolean isExpired(final Instant instant) {
        Args.notNull(instant, "Instant");
        return (cookieExpiryDate != null && cookieExpiryDate.compareTo(instant) <= 0);
    }

    @Deprecated
    @Override
    public Date getCreationDate() {
        return DateUtils.toDate(creationDate);
    }

    @Override
    public Instant getCreationInstant() {
        return creationDate;
    }

    @Override
    public boolean isHttpOnly() {
        return httpOnly;
    }

    @Deprecated
    public void setCreationDate(final Date creationDate) {
        this.creationDate = DateUtils.toInstant(creationDate);
    }

    public void setCreationDate(final Instant creationDate) {
        this.creationDate = creationDate;
    }

    public void setAttribute(final String name, final String value) {
        this.attribs.put(name, value);
    }

    @Override
    public String getAttribute(final String name) {
        return this.attribs.get(name);
    }

    @Override
    public boolean containsAttribute(final String name) {
        return this.attribs.containsKey(name);
    }

    public boolean removeAttribute(final String name) {
        return this.attribs.remove(name) != null;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        final BasicClientCookie clone = (BasicClientCookie) super.clone();
        clone.attribs = new HashMap<>(this.attribs);
        return clone;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("[name: ");
        buffer.append(this.name);
        buffer.append("; ");
        buffer.append("value: ");
        buffer.append(this.value);
        buffer.append("; ");
        buffer.append("domain: ");
        buffer.append(this.cookieDomain);
        buffer.append("; ");
        buffer.append("path: ");
        buffer.append(this.cookiePath);
        buffer.append("; ");
        buffer.append("expiry: ");
        buffer.append(this.cookieExpiryDate);
        buffer.append("]");
        return buffer.toString();
    }
}
