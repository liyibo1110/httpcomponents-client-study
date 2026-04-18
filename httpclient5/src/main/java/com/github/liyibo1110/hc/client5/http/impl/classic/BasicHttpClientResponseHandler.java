package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.ClientProtocolException;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.ParseException;
import com.github.liyibo1110.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;

/**
 * AbstractHttpClientResponseHandler的基础实现类，最终输出String
 * @author liyibo
 * @date 2026-04-17 12:44
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class BasicHttpClientResponseHandler extends AbstractHttpClientResponseHandler<String> {

    @Override
    public String handleResponse(final ClassicHttpResponse response) throws IOException {
        return super.handleResponse(response);
    }

    @Override
    public String handleEntity(final HttpEntity entity) throws IOException {
        try {
            return EntityUtils.toString(entity);
        } catch (final ParseException e) {
            throw new ClientProtocolException(e);
        }
    }
}
