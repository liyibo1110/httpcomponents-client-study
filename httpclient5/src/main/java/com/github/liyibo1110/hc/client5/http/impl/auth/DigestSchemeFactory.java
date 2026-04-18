package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthSchemeFactory;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.nio.charset.Charset;

/**
 * 生成DigestScheme对象的工厂。
 * @author liyibo
 * @date 2026-04-17 17:05
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DigestSchemeFactory implements AuthSchemeFactory {

    public static final DigestSchemeFactory INSTANCE = new DigestSchemeFactory();

    private final Charset charset;

    public DigestSchemeFactory(final Charset charset) {
        this.charset = charset;
    }

    public DigestSchemeFactory() {
        this(null);
    }

    @Override
    public AuthScheme create(final HttpContext context) {
        return new DigestScheme(charset);
    }
}
