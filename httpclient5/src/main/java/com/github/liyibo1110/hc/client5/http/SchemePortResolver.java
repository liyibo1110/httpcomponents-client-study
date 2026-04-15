package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.net.NamedEndpoint;

/**
 * 协议schema的默认端口解析策略。
 * @author liyibo
 * @date 2026-04-14 13:48
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface SchemePortResolver {

    /**
     * 根据HttpHost里面的schema返回主机的实际端口。
     */
    int resolve(HttpHost host);

    default int resolve(String scheme, NamedEndpoint endpoint) {
        return resolve(new HttpHost(scheme, endpoint));
    }
}
