package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthChallenge;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthScope;
import com.github.liyibo1110.hc.client5.http.auth.AuthenticationException;
import com.github.liyibo1110.hc.client5.http.auth.Credentials;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.client5.http.auth.MalformedChallengeException;
import com.github.liyibo1110.hc.client5.http.auth.StandardAuthScheme;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.client5.http.utils.Base64;
import com.github.liyibo1110.hc.client5.http.utils.ByteArrayBuilder;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.NameValuePair;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AuthScheme的BASIC版本实现（即通过用户名和密码验证），基于RFC 2617。
 *
 * 不负责发请求和重试，也不负责管理整个认证状态机，它只做：
 * 在已经确定要用Basic认证时，读取challenge参数，拿到用户名密码，然后生成Authorization: Basic xxx这一行认证头。
 * @author liyibo
 * @date 2026-04-16 11:01
 */
public class BasicScheme implements AuthScheme, Serializable {
    private static final long serialVersionUID = -1931571557597830536L;

    private static final Logger LOG = LoggerFactory.getLogger(BasicScheme.class);

    private final Map<String, String> paramMap;
    private transient Charset defaultCharset;
    private transient ByteArrayBuilder buffer;
    private transient Base64 base64codec;
    private boolean complete;

    private String username;
    private char[] password;

    public BasicScheme(final Charset charset) {
        this.paramMap = new HashMap<>();
        this.defaultCharset = charset != null ? charset : StandardCharsets.US_ASCII;
        this.complete = false;
    }

    public BasicScheme() {
        this(StandardCharsets.US_ASCII);
    }

    private void applyCredentials(final Credentials credentials) {
        this.username = credentials.getUserPrincipal().getName();
        this.password = credentials.getPassword();
    }

    private void clearCredentials() {
        this.username = null;
        this.password = null;
    }

    public void initPreemptive(final Credentials credentials) {
        if (credentials != null)
            applyCredentials(credentials);
        else
            clearCredentials();
    }

    @Override
    public String getName() {
        return StandardAuthScheme.BASIC;
    }

    @Override
    public boolean isConnectionBased() {
        return false;
    }

    @Override
    public String getRealm() {
        return this.paramMap.get("realm");
    }

    /**
     * server第一次返回例如401 Unauthorized这样的头，就会进入这个方法。
     * 主要就是解析里面的realm、charset之类的参数保存到paramMap里面。
     */
    @Override
    public void processChallenge(final AuthChallenge authChallenge, final HttpContext context) throws MalformedChallengeException {
        this.paramMap.clear();
        final List<NameValuePair> params = authChallenge.getParams();
        if (params != null) {
            for (final NameValuePair param: params)
                this.paramMap.put(param.getName().toLowerCase(Locale.ROOT), param.getValue());
        }
        this.complete = true;
    }

    @Override
    public boolean isChallengeComplete() {
        return this.complete;
    }

    /**
     * processChallenge方法结束后，会调用这个方法。
     * 负责根据host、realm、schema去找Credentials，找到了就会更新自己的字段
     */
    @Override
    public boolean isResponseReady(final HttpHost host, final CredentialsProvider credentialsProvider, final HttpContext context)
            throws AuthenticationException {
        Args.notNull(host, "Auth host");
        Args.notNull(credentialsProvider, "CredentialsProvider");

        final AuthScope authScope = new AuthScope(host, getRealm(), getName());
        final Credentials credentials = credentialsProvider.getCredentials(authScope, context);
        if (credentials != null) {
            applyCredentials(credentials);
            return true;
        }

        // 没找到对应的Credentials
        if (LOG.isDebugEnabled()) {
            final HttpClientContext clientContext = HttpClientContext.adapt(context);
            final String exchangeId = clientContext.getExchangeId();
            LOG.debug("{} No credentials found for auth scope [{}]", exchangeId, authScope);
        }
        clearCredentials();
        return false;
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    private void validateUsername() throws AuthenticationException {
        if (username == null)
            throw new AuthenticationException("User credentials not set");
        for (int i = 0; i < username.length(); i++) {
            final char ch = username.charAt(i);
            if (Character.isISOControl(ch))
                throw new AuthenticationException("Username must not contain any control characters");
            if (ch == ':')
                throw new AuthenticationException("Username contains a colon character and is invalid");
        }
    }

    /**
     * 生成Authorization: Basic xxx这种认证头
     */
    @Override
    public String generateAuthResponse(final HttpHost host, final HttpRequest request, final HttpContext context)
            throws AuthenticationException {
        validateUsername();
        if (this.buffer == null) {
            this.buffer = new ByteArrayBuilder(64);
        } else {
            this.buffer.reset();
        }

        final Charset charset = AuthSchemeSupport.parseCharset(paramMap.get("charset"), defaultCharset);
        this.buffer.charset(charset);
        this.buffer.append(this.username).append(":").append(this.password);
        if (this.base64codec == null)
            this.base64codec = new Base64();
        // 生成要输出的byte[]
        final byte[] encodedCreds = this.base64codec.encode(this.buffer.toByteArray());
        this.buffer.reset();
        // 拼写最终的String
        return StandardAuthScheme.BASIC + " " + new String(encodedCreds, 0, encodedCreds.length, StandardCharsets.US_ASCII);
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(this.defaultCharset.name());
    }

    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            this.defaultCharset = Charset.forName(in.readUTF());
        } catch (final UnsupportedCharsetException ex) {
            this.defaultCharset = StandardCharsets.US_ASCII;
        }
    }

    private void readObjectNoData() {}

    @Override
    public String toString() {
        return getName() + this.paramMap;
    }
}
