package com.github.liyibo1110.hc.client5.http.impl.io;

import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpResponseFactory;
import com.github.liyibo1110.hc.core5.http.config.Http1Config;
import com.github.liyibo1110.hc.core5.http.impl.io.DefaultHttpResponseParser;
import com.github.liyibo1110.hc.core5.http.message.LineParser;
import com.github.liyibo1110.hc.core5.util.CharArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 一种宽容的HTTP响应解析器实现，可在遇到有效的HTTP响应消息头之前跳过格式错误的数据。
 * @author liyibo
 * @date 2026-04-21 11:28
 */
public class LenientHttpResponseParser extends DefaultHttpResponseParser {
    private static final Logger LOG = LoggerFactory.getLogger(LenientHttpResponseParser.class);

    public LenientHttpResponseParser(final LineParser lineParser,
                                     final HttpResponseFactory<ClassicHttpResponse> responseFactory,
                                     final Http1Config h1Config) {
        super(lineParser, responseFactory, h1Config);
    }

    public LenientHttpResponseParser(final Http1Config h1Config) {
        this(null, null, h1Config);
    }

    @Override
    protected ClassicHttpResponse createMessage(final CharArrayBuffer buffer) throws IOException {
        try {
            return super.createMessage(buffer);
        } catch (final HttpException ex) {
            // 解析出现异常，则直接返回null结束
            if (LOG.isDebugEnabled())
                LOG.debug("Garbage in response: {}", buffer);
            return null;
        }
    }
}
