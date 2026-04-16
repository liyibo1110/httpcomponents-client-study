package com.github.liyibo1110.hc.client5.http.entity;

import com.github.liyibo1110.hc.core5.http.HttpEntity;

/**
 * 负责处理采用deflate内容编码的响应的HttpEntityWrapper。
 *
 * 根据RFC2616的定义，deflate指的是RFC1950中定义的zlib流。
 * 某些服务器实现错误地将RFC2616解释为应使用RFC1951中定义的deflate流（或者说，他们之所以这样做，是因为IE就是这么处理的？）。
 * 令人困惑的是，在HTTP 1.1中，deflate指的是zlib流，而非deflate流。由于互联网上实际存在这两种类型，因此我们在此同时处理它们。
 *
 * 结论：请优先使用gzip！
 * @author liyibo
 * @date 2026-04-15 18:04
 */
public class DeflateDecompressingEntity extends DecompressingEntity {

    public DeflateDecompressingEntity(final HttpEntity entity) {
        super(entity, DeflateInputStreamFactory.getInstance());
    }
}
