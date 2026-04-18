package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.auth.AuthCache;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.impl.DefaultSchemePortResolver;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.net.NamedEndpoint;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.LangUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuthCache的默认实现。该实现要求AuthScheme必须是Serializable的，才能被缓存。
 * 从4.4版本开始，该类的实例是线程安全的。
 * @author liyibo
 * @date 2026-04-17 17:27
 */
@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
public class BasicAuthCache implements AuthCache {
    private static final Logger LOG = LoggerFactory.getLogger(BasicAuthCache.class);

    static class Key {
        final String scheme;
        final String host;
        final int port;
        final String pathPrefix;

        Key(final String scheme, final String host, final int port, final String pathPrefix) {
            Args.notBlank(scheme, "Scheme");
            Args.notBlank(host, "Scheme");
            this.scheme = scheme.toLowerCase(Locale.ROOT);
            this.host = host.toLowerCase(Locale.ROOT);
            this.port = port;
            this.pathPrefix = pathPrefix;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj instanceof Key) {
                final Key that = (Key) obj;
                return this.scheme.equals(that.scheme)
                        && this.host.equals(that.host)
                        && this.port == that.port
                        && Objects.equals(this.pathPrefix, that.pathPrefix);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = LangUtils.HASH_SEED;
            hash = LangUtils.hashCode(hash, this.scheme);
            hash = LangUtils.hashCode(hash, this.host);
            hash = LangUtils.hashCode(hash, this.port);
            hash = LangUtils.hashCode(hash, this.pathPrefix);
            return hash;
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            buf.append(scheme).append("://").append(host);
            if (port >= 0)
                buf.append(":").append(port);
            if (pathPrefix != null) {
                if (!pathPrefix.startsWith("/"))
                    buf.append("/");
                buf.append(pathPrefix);
            }
            return buf.toString();
        }
    }

    private final Map<Key, byte[]> map;
    private final SchemePortResolver schemePortResolver;

    public BasicAuthCache(final SchemePortResolver schemePortResolver) {
        super();
        this.map = new ConcurrentHashMap<>();
        this.schemePortResolver = schemePortResolver != null ? schemePortResolver : DefaultSchemePortResolver.INSTANCE;
    }

    public BasicAuthCache() {
        this(null);
    }

    private Key key(final String scheme, final NamedEndpoint authority, final String pathPrefix) {
        return new Key(scheme, authority.getHostName(), schemePortResolver.resolve(scheme, authority), pathPrefix);
    }

    @Override
    public void put(final HttpHost host, final AuthScheme authScheme) {
        put(host, null, authScheme);
    }

    @Override
    public AuthScheme get(final HttpHost host) {
        return get(host, null);
    }

    @Override
    public void remove(final HttpHost host) {
        remove(host, null);
    }

    @Override
    public void put(final HttpHost host, final String pathPrefix, final AuthScheme authScheme) {
        Args.notNull(host, "HTTP host");
        if (authScheme == null)
            return;

        if (authScheme instanceof Serializable) {
            try {
                final ByteArrayOutputStream buf = new ByteArrayOutputStream();
                try (final ObjectOutputStream out = new ObjectOutputStream(buf)) {
                    out.writeObject(authScheme);
                }
                this.map.put(key(host.getSchemeName(), host, pathPrefix), buf.toByteArray());
            } catch (final IOException ex) {
                if (LOG.isWarnEnabled())
                    LOG.warn("Unexpected I/O error while serializing auth scheme", ex);
            }
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("Auth scheme {} is not serializable", authScheme.getClass());
        }
    }

    @Override
    public AuthScheme get(final HttpHost host, final String pathPrefix) {
        Args.notNull(host, "HTTP host");
        final byte[] bytes = this.map.get(key(host.getSchemeName(), host, pathPrefix));
        if (bytes != null) {
            try {
                final ByteArrayInputStream buf = new ByteArrayInputStream(bytes);
                try (final ObjectInputStream in = new ObjectInputStream(buf)) {
                    return (AuthScheme) in.readObject();
                }
            } catch (final IOException ex) {
                if (LOG.isWarnEnabled())
                    LOG.warn("Unexpected I/O error while de-serializing auth scheme", ex);
            } catch (final ClassNotFoundException ex) {
                if (LOG.isWarnEnabled())
                    LOG.warn("Unexpected error while de-serializing auth scheme", ex);
            }
        }
        return null;
    }

    @Override
    public void remove(final HttpHost host, final String pathPrefix) {
        Args.notNull(host, "HTTP host");
        this.map.remove(key(host.getSchemeName(), host, pathPrefix));
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public String toString() {
        return this.map.toString();
    }
}
