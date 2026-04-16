package com.github.liyibo1110.hc.client5.http.entity;

import com.github.liyibo1110.hc.core5.http.HttpEntity;

/**
 * 用于处理gzip压缩的响应的HttpEntityWrapper。
 * @author liyibo
 * @date 2026-04-15 17:58
 */
public class GzipDecompressingEntity extends DecompressingEntity {

    /**
     * 创建一个新的GzipDecompressingEntity，该实体将封装指定的HttpEntity。
     */
    public GzipDecompressingEntity(final HttpEntity entity) {
        super(entity, GZIPInputStreamFactory.getInstance());
    }
}
