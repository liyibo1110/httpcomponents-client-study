package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthSchemeFactory;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * 生成NTLMScheme对象的工厂。
 * @author liyibo
 * @date 2026-04-17 17:11
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class NTLMSchemeFactory implements AuthSchemeFactory {

    public static final NTLMSchemeFactory INSTANCE = new NTLMSchemeFactory();

    @Override
    public AuthScheme create(final HttpContext context) {
        return new NTLMScheme();
    }
}
