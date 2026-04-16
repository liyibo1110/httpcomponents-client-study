package com.github.liyibo1110.hc.client5.http.entity;

import com.github.liyibo1110.hc.core5.http.HttpEntity;

/**
 * 用于处理Content Coded响应的HttpEntityWrapper。
 * @author liyibo
 * @date 2026-04-15 18:03
 */
public class BrotliDecompressingEntity extends DecompressingEntity {

    public BrotliDecompressingEntity(final HttpEntity entity) {
        super(entity, BrotliInputStreamFactory.getInstance());
    }

    public static boolean isAvailable() {
        try {
            Class.forName("org.brotli.dec.BrotliInputStream");
            return true;
        } catch (final ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }
}
