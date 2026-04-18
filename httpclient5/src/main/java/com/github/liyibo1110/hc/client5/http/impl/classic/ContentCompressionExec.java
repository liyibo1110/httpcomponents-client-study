package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.classic.ExecChain;
import com.github.liyibo1110.hc.client5.http.classic.ExecChainHandler;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.entity.BrotliDecompressingEntity;
import com.github.liyibo1110.hc.client5.http.entity.BrotliInputStreamFactory;
import com.github.liyibo1110.hc.client5.http.entity.DecompressingEntity;
import com.github.liyibo1110.hc.client5.http.entity.DeflateInputStreamFactory;
import com.github.liyibo1110.hc.client5.http.entity.GZIPInputStreamFactory;
import com.github.liyibo1110.hc.client5.http.entity.InputStreamFactory;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HeaderElement;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHeaders;
import com.github.liyibo1110.hc.core5.http.config.Lookup;
import com.github.liyibo1110.hc.core5.http.config.RegistryBuilder;
import com.github.liyibo1110.hc.core5.http.message.BasicHeaderValueParser;
import com.github.liyibo1110.hc.core5.http.message.MessageSupport;
import com.github.liyibo1110.hc.core5.http.message.ParserCursor;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * @author liyibo
 * @date 2026-04-17 11:50
 */
@Contract(threading = ThreadingBehavior.STATELESS)
@Internal
public class ContentCompressionExec implements ExecChainHandler {
    private final Header acceptEncoding;
    private final Lookup<InputStreamFactory> decoderRegistry;
    private final boolean ignoreUnknown;

    private static final String[] EMPTY_STRING_ARRAY = {};

    public ContentCompressionExec(final List<String> acceptEncoding,
                                  final Lookup<InputStreamFactory> decoderRegistry,
                                  final boolean ignoreUnknown) {
        final boolean brotliSupported = BrotliDecompressingEntity.isAvailable();
        final String[] encoding;
        if (brotliSupported)
            encoding = new String[] {"gzip", "x-gzip", "deflate", "br"};
        else
            encoding = new String[] {"gzip", "x-gzip", "deflate"};

        this.acceptEncoding = MessageSupport.format(HttpHeaders.ACCEPT_ENCODING,
                acceptEncoding != null ? acceptEncoding.toArray(EMPTY_STRING_ARRAY) : encoding);

        if (decoderRegistry != null) {
            this.decoderRegistry = decoderRegistry;
        } else {
            final RegistryBuilder<InputStreamFactory> builder = RegistryBuilder.<InputStreamFactory>create()
                    .register("gzip", GZIPInputStreamFactory.getInstance())
                    .register("x-gzip", GZIPInputStreamFactory.getInstance())
                    .register("deflate", DeflateInputStreamFactory.getInstance());
            if (brotliSupported)
                builder.register("br", BrotliInputStreamFactory.getInstance());

            this.decoderRegistry = builder.build();
        }

        this.ignoreUnknown = ignoreUnknown;
    }

    public ContentCompressionExec(final boolean ignoreUnknown) {
        this(null, null, ignoreUnknown);
    }

    public ContentCompressionExec() {
        this(null, null, true);
    }

    @Override
    public ClassicHttpResponse execute(final ClassicHttpRequest request, final ExecChain.Scope scope, final ExecChain chain)
            throws IOException, HttpException {
        Args.notNull(request, "HTTP request");
        Args.notNull(scope, "Scope");

        final HttpClientContext clientContext = scope.clientContext;
        final RequestConfig requestConfig = clientContext.getRequestConfig();

        /* Signal support for Accept-Encoding transfer encodings. */
        if (!request.containsHeader(HttpHeaders.ACCEPT_ENCODING) && requestConfig.isContentCompressionEnabled())
            request.addHeader(acceptEncoding);

        final ClassicHttpResponse response = chain.proceed(request, scope);

        final HttpEntity entity = response.getEntity();
        // entity can be null in case of 304 Not Modified, 204 No Content or similar
        // check for zero length entity.
        if (requestConfig.isContentCompressionEnabled() && entity != null && entity.getContentLength() != 0) {
            final String contentEncoding = entity.getContentEncoding();
            if (contentEncoding != null) {
                final ParserCursor cursor = new ParserCursor(0, contentEncoding.length());
                final HeaderElement[] codecs = BasicHeaderValueParser.INSTANCE.parseElements(contentEncoding, cursor);
                for (final HeaderElement codec : codecs) {
                    final String codecname = codec.getName().toLowerCase(Locale.ROOT);
                    final InputStreamFactory decoderFactory = decoderRegistry.lookup(codecname);
                    if (decoderFactory != null) {
                        response.setEntity(new DecompressingEntity(response.getEntity(), decoderFactory));
                        response.removeHeaders(HttpHeaders.CONTENT_LENGTH);
                        response.removeHeaders(HttpHeaders.CONTENT_ENCODING);
                        response.removeHeaders(HttpHeaders.CONTENT_MD5);
                    } else {
                        if (!"identity".equals(codecname) && !ignoreUnknown)
                            throw new HttpException("Unsupported Content-Encoding: " + codec.getName());
                    }
                }
            }
        }
        return response;
    }
}
