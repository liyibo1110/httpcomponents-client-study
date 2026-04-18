package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthSchemeFactory;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.nio.charset.Charset;

/**
 * 生成BasicScheme对象的工厂。
 * @author liyibo
 * @date 2026-04-17 16:57
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class BasicSchemeFactory implements AuthSchemeFactory {
    public static final BasicSchemeFactory INSTANCE = new BasicSchemeFactory();

    private final Charset charset;

    public BasicSchemeFactory(final Charset charset) {
        super();
        this.charset = charset;
    }

    public BasicSchemeFactory() {
        this(null);
    }

    @Override
    public AuthScheme create(final HttpContext context) {
        return new BasicScheme(this.charset);
    }
}
