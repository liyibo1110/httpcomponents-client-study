package com.github.liyibo1110.hc.client5.http.entity;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream的装饰器工厂接口。
 * @author liyibo
 * @date 2026-04-15 17:22
 */
public interface InputStreamFactory {
    InputStream create(InputStream inputStream) throws IOException;
}
