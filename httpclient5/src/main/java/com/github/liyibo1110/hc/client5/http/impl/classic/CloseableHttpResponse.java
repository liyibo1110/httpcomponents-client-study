package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.classic.ExecRuntime;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.ProtocolException;
import com.github.liyibo1110.hc.core5.http.ProtocolVersion;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * 为确保与4.x的向后兼容性而提供。
 * @author liyibo
 * @date 2026-04-16 18:04
 */
public final class CloseableHttpResponse implements ClassicHttpResponse {
    private final ClassicHttpResponse response;
    private final ExecRuntime execRuntime;

    static CloseableHttpResponse adapt(final ClassicHttpResponse response) {
        if (response == null)
            return null;
        return response instanceof CloseableHttpResponse
                ? (CloseableHttpResponse) response
                : new CloseableHttpResponse(response, null);
    }

    CloseableHttpResponse(final ClassicHttpResponse response, final ExecRuntime execRuntime) {
        this.response = Args.notNull(response, "Response");
        this.execRuntime = execRuntime;
    }

    @Override
    public int getCode() {
        return response.getCode();
    }

    @Override
    public HttpEntity getEntity() {
        return response.getEntity();
    }

    @Override
    public boolean containsHeader(final String name) {
        return response.containsHeader(name);
    }

    @Override
    public void setVersion(final ProtocolVersion version) {
        response.setVersion(version);
    }

    @Override
    public void setCode(final int code) {
        response.setCode(code);
    }

    @Override
    public String getReasonPhrase() {
        return response.getReasonPhrase();
    }

    @Override
    public int countHeaders(final String name) {
        return response.countHeaders(name);
    }

    @Override
    public void setEntity(final HttpEntity entity) {
        response.setEntity(entity);
    }

    @Override
    public ProtocolVersion getVersion() {
        return response.getVersion();
    }

    @Override
    public void setReasonPhrase(final String reason) {
        response.setReasonPhrase(reason);
    }

    @Override
    public Header[] getHeaders(final String name) {
        return response.getHeaders(name);
    }

    @Override
    public void addHeader(final Header header) {
        response.addHeader(header);
    }

    @Override
    public Locale getLocale() {
        return response.getLocale();
    }

    @Override
    public void addHeader(final String name, final Object value) {
        response.addHeader(name, value);
    }

    @Override
    public void setLocale(final Locale loc) {
        response.setLocale(loc);
    }

    @Override
    public Header getHeader(final String name) throws ProtocolException {
        return response.getHeader(name);
    }

    @Override
    public void setHeader(final Header header) {
        response.setHeader(header);
    }

    @Override
    public Header getFirstHeader(final String name) {
        return response.getFirstHeader(name);
    }

    @Override
    public void setHeader(final String name, final Object value) {
        response.setHeader(name, value);
    }

    @Override
    public void setHeaders(final Header... headers) {
        response.setHeaders(headers);
    }

    @Override
    public boolean removeHeader(final Header header) {
        return response.removeHeader(header);
    }

    @Override
    public boolean removeHeaders(final String name) {
        return response.removeHeaders(name);
    }

    @Override
    public Header getLastHeader(final String name) {
        return response.getLastHeader(name);
    }

    @Override
    public Header[] getHeaders() {
        return response.getHeaders();
    }

    @Override
    public Iterator<Header> headerIterator() {
        return response.headerIterator();
    }

    @Override
    public Iterator<Header> headerIterator(final String name) {
        return response.headerIterator(name);
    }

    @Override
    public void close() throws IOException {
        if (execRuntime != null) {
            try {
                response.close();
                execRuntime.disconnectEndpoint();
            } finally {
                execRuntime.discardEndpoint();
            }
        } else {
            response.close();
        }
    }

    @Override
    public String toString() {
        return response.toString();
    }
}
