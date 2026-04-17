package com.github.liyibo1110.hc.client5.http.ssl;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.ssl.TrustStrategy;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * 一种将自签名证书视为受信任证书的信任策略。
 * 所有其他证书的验证工作由SSL上下文中配置的信任管理器负责。
 * @author liyibo
 * @date 2026-04-16 15:21
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class TrustSelfSignedStrategy implements TrustStrategy {

    public static final TrustSelfSignedStrategy INSTANCE = new TrustSelfSignedStrategy();

    @Override
    public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        return chain.length == 1;
    }
}
