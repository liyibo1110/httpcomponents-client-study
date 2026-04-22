package com.github.liyibo1110.hc.client5.http.impl.io;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpResponseFactory;
import com.github.liyibo1110.hc.core5.http.config.Http1Config;
import com.github.liyibo1110.hc.core5.http.impl.io.DefaultClassicHttpResponseFactory;
import com.github.liyibo1110.hc.core5.http.io.HttpMessageParser;
import com.github.liyibo1110.hc.core5.http.io.HttpMessageParserFactory;
import com.github.liyibo1110.hc.core5.http.message.BasicLineParser;
import com.github.liyibo1110.hc.core5.http.message.LineParser;

/**
 * 生成LenientHttpResponseParser对象的工厂。
 * @author liyibo
 * @date 2026-04-21 11:33
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultHttpResponseParserFactory implements HttpMessageParserFactory<ClassicHttpResponse> {

    public static final DefaultHttpResponseParserFactory INSTANCE = new DefaultHttpResponseParserFactory();

    private final LineParser lineParser;
    private final HttpResponseFactory<ClassicHttpResponse> responseFactory;

    public DefaultHttpResponseParserFactory(final LineParser lineParser,
                                            final HttpResponseFactory<ClassicHttpResponse> responseFactory) {
        super();
        this.lineParser = lineParser != null ? lineParser : BasicLineParser.INSTANCE;
        this.responseFactory = responseFactory != null ? responseFactory : DefaultClassicHttpResponseFactory.INSTANCE;
    }

    public DefaultHttpResponseParserFactory(final HttpResponseFactory<ClassicHttpResponse> responseFactory) {
        this(null, responseFactory);
    }

    public DefaultHttpResponseParserFactory() {
        this(null, null);
    }

    @Override
    public HttpMessageParser<ClassicHttpResponse> create(final Http1Config h1Config) {
        return new LenientHttpResponseParser(this.lineParser, this.responseFactory, h1Config);
    }
}
