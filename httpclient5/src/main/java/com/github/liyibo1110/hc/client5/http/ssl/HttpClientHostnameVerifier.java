package com.github.liyibo1110.hc.client5.http.ssl;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import java.security.cert.X509Certificate;

/**
 * HostnameVerifier的扩展接口。
 * @author liyibo
 * @date 2026-04-16 14:33
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface HttpClientHostnameVerifier extends HostnameVerifier {

    /**
     * 验证提供的服务器X.509证书，并确保其与原始主机名一致
     */
    void verify(String host, X509Certificate cert) throws SSLException;
}
