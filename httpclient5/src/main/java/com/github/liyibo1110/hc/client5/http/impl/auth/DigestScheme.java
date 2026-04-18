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
import com.github.liyibo1110.hc.client5.http.utils.ByteArrayBuilder;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.NameValuePair;
import com.github.liyibo1110.hc.core5.http.message.BasicHeaderValueFormatter;
import com.github.liyibo1110.hc.core5.http.message.BasicNameValuePair;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.CharArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * RFC 2617中定义的摘要认证方案，持MD5（默认）和MD5-sess。目前仅支持qop=auth或不指定 qop。
 * 不支持qop=auth-int。如果同时提供了auth和auth-int，则使用auth。
 *
 * 由于摘要用户名会以明文形式包含在生成的Authentication头中，因此用户名的字符集必须与连接所使用的HTTP元素字符集兼容。
 * @author liyibo
 * @date 2026-04-17 16:58
 */
public class DigestScheme implements AuthScheme, Serializable {
    private static final long serialVersionUID = 3883908186234566916L;

    private static final Logger LOG = LoggerFactory.getLogger(DigestScheme.class);

    /**
     * Hexa values used when creating 32 character long digest in HTTP DigestScheme
     * in case of authentication.
     *
     * @see #formatHex(byte[])
     */
    private static final char[] HEXADECIMAL = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Represent the possible values of quality of protection.
     */
    private enum QualityOfProtection {
        UNKNOWN, MISSING, AUTH_INT, AUTH
    }

    private transient Charset defaultCharset;
    private final Map<String, String> paramMap;
    private boolean complete;
    private transient ByteArrayBuilder buffer;

    private String lastNonce;
    private long nounceCount;
    private String cnonce;
    private byte[] a1;
    private byte[] a2;

    private String username;
    private char[] password;

    public DigestScheme() {
        this(StandardCharsets.ISO_8859_1);
    }

    public DigestScheme(final Charset charset) {
        this.defaultCharset = charset != null ? charset : StandardCharsets.ISO_8859_1;
        this.paramMap = new HashMap<>();
        this.complete = false;
    }

    public void initPreemptive(final Credentials credentials, final String cnonce, final String realm) {
        Args.notNull(credentials, "Credentials");
        this.username = credentials.getUserPrincipal().getName();
        this.password = credentials.getPassword();
        this.paramMap.put("cnonce", cnonce);
        this.paramMap.put("realm", realm);
    }

    @Override
    public String getName() {
        return StandardAuthScheme.DIGEST;
    }

    @Override
    public boolean isConnectionBased() {
        return false;
    }

    @Override
    public String getRealm() {
        return this.paramMap.get("realm");
    }

    @Override
    public void processChallenge(final AuthChallenge authChallenge, final HttpContext context) throws MalformedChallengeException {
        Args.notNull(authChallenge, "AuthChallenge");
        this.paramMap.clear();
        final List<NameValuePair> params = authChallenge.getParams();
        if (params != null) {
            for (final NameValuePair param: params)
                this.paramMap.put(param.getName().toLowerCase(Locale.ROOT), param.getValue());
        }
        if (this.paramMap.isEmpty())
            throw new MalformedChallengeException("Missing digest auth parameters");

        this.complete = true;
    }

    @Override
    public boolean isChallengeComplete() {
        final String s = this.paramMap.get("stale");
        return !"true".equalsIgnoreCase(s) && this.complete;
    }

    @Override
    public boolean isResponseReady(final HttpHost host, final CredentialsProvider credentialsProvider, final HttpContext context)
            throws AuthenticationException {
        Args.notNull(host, "Auth host");
        Args.notNull(credentialsProvider, "CredentialsProvider");

        final AuthScope authScope = new AuthScope(host, getRealm(), getName());
        final Credentials credentials = credentialsProvider.getCredentials(authScope, context);
        if (credentials != null) {
            this.username = credentials.getUserPrincipal().getName();
            this.password = credentials.getPassword();
            return true;
        }

        if (LOG.isDebugEnabled()) {
            final HttpClientContext clientContext = HttpClientContext.adapt(context);
            final String exchangeId = clientContext.getExchangeId();
            LOG.debug("{} No credentials found for auth scope [{}]", exchangeId, authScope);
        }
        this.username = null;
        this.password = null;
        return false;
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public String generateAuthResponse(final HttpHost host, final HttpRequest request, final HttpContext context)
            throws AuthenticationException {
        Args.notNull(request, "HTTP request");
        if (this.paramMap.get("realm") == null)
            throw new AuthenticationException("missing realm");
        if (this.paramMap.get("nonce") == null)
            throw new AuthenticationException("missing nonce");

        return createDigestResponse(request);
    }

    private static MessageDigest createMessageDigest(final String digAlg) throws UnsupportedDigestAlgorithmException {
        try {
            return MessageDigest.getInstance(digAlg);
        } catch (final Exception e) {
            throw new UnsupportedDigestAlgorithmException("Unsupported algorithm in HTTP Digest authentication: " + digAlg);
        }
    }

    private String createDigestResponse(final HttpRequest request) throws AuthenticationException {
        final String uri = request.getRequestUri();
        final String method = request.getMethod();
        final String realm = this.paramMap.get("realm");
        final String nonce = this.paramMap.get("nonce");
        final String opaque = this.paramMap.get("opaque");
        final String algorithm = this.paramMap.get("algorithm");

        final Set<String> qopset = new HashSet<>(8);
        QualityOfProtection qop = QualityOfProtection.UNKNOWN;
        final String qoplist = this.paramMap.get("qop");
        if (qoplist != null) {
            final StringTokenizer tok = new StringTokenizer(qoplist, ",");
            while (tok.hasMoreTokens()) {
                final String variant = tok.nextToken().trim();
                qopset.add(variant.toLowerCase(Locale.ROOT));
            }
            final HttpEntity entity = request instanceof ClassicHttpRequest ? ((ClassicHttpRequest) request).getEntity() : null;
            if (entity != null && qopset.contains("auth-int"))
                qop = QualityOfProtection.AUTH_INT;
            else if (qopset.contains("auth"))
                qop = QualityOfProtection.AUTH;
            else if (qopset.contains("auth-int"))
                qop = QualityOfProtection.AUTH_INT;
        } else {
            qop = QualityOfProtection.MISSING;
        }

        if (qop == QualityOfProtection.UNKNOWN)
            throw new AuthenticationException("None of the qop methods is supported: " + qoplist);

        final Charset charset = AuthSchemeSupport.parseCharset(paramMap.get("charset"), defaultCharset);
        String digAlg = algorithm;
        // If an algorithm is not specified, default to MD5.
        if (digAlg == null || digAlg.equalsIgnoreCase("MD5-sess"))
            digAlg = "MD5";

        final MessageDigest digester;
        try {
            digester = createMessageDigest(digAlg);
        } catch (final UnsupportedDigestAlgorithmException ex) {
            throw new AuthenticationException("Unsupported digest algorithm: " + digAlg);
        }

        if (nonce.equals(this.lastNonce)) {
            nounceCount++;
        } else {
            nounceCount = 1;
            cnonce = null;
            lastNonce = nonce;
        }

        final StringBuilder sb = new StringBuilder(8);
        try (final Formatter formatter = new Formatter(sb, Locale.ROOT)) {
            formatter.format("%08x", nounceCount);
        }
        final String nc = sb.toString();

        if (cnonce == null)
            cnonce = formatHex(createCnonce());

        if (buffer == null)
            buffer = new ByteArrayBuilder(128);
        else
            buffer.reset();

        buffer.charset(charset);

        a1 = null;
        a2 = null;
        // 3.2.2.2: Calculating digest
        if ("MD5-sess".equalsIgnoreCase(algorithm)) {
            // H( unq(username-value) ":" unq(realm-value) ":" passwd )
            //      ":" unq(nonce-value)
            //      ":" unq(cnonce-value)

            // calculated one per session
            buffer.append(username).append(":").append(realm).append(":").append(password);
            final String checksum = formatHex(digester.digest(this.buffer.toByteArray()));
            buffer.reset();
            buffer.append(checksum).append(":").append(nonce).append(":").append(cnonce);
        } else {
            // unq(username-value) ":" unq(realm-value) ":" passwd
            buffer.append(username).append(":").append(realm).append(":").append(password);
        }
        a1 = buffer.toByteArray();

        final String hasha1 = formatHex(digester.digest(a1));
        buffer.reset();

        if (qop == QualityOfProtection.AUTH) {
            // Method ":" digest-uri-value
            a2 = buffer.append(method).append(":").append(uri).toByteArray();
        } else if (qop == QualityOfProtection.AUTH_INT) {
            // Method ":" digest-uri-value ":" H(entity-body)
            final HttpEntity entity = request instanceof ClassicHttpRequest ? ((ClassicHttpRequest) request).getEntity() : null;
            if (entity != null && !entity.isRepeatable()) {
                // If the entity is not repeatable, try falling back onto QOP_AUTH
                if (qopset.contains("auth")) {
                    qop = QualityOfProtection.AUTH;
                    a2 = buffer.append(method).append(":").append(uri).toByteArray();
                } else {
                    throw new AuthenticationException("Qop auth-int cannot be used with a non-repeatable entity");
                }
            } else {
                final HttpEntityDigester entityDigester = new HttpEntityDigester(digester);
                try {
                    if (entity != null)
                        entity.writeTo(entityDigester);

                    entityDigester.close();
                } catch (final IOException ex) {
                    throw new AuthenticationException("I/O error reading entity content", ex);
                }
                a2 = buffer.append(method).append(":").append(uri)
                        .append(":").append(formatHex(entityDigester.getDigest())).toByteArray();
            }
        } else {
            a2 = buffer.append(method).append(":").append(uri).toByteArray();
        }

        final String hasha2 = formatHex(digester.digest(a2));
        buffer.reset();

        // 3.2.2.1

        final byte[] digestInput;
        if (qop == QualityOfProtection.MISSING) {
            buffer.append(hasha1).append(":").append(nonce).append(":").append(hasha2);
        } else {
            buffer.append(hasha1).append(":").append(nonce).append(":").append(nc).append(":")
                    .append(cnonce).append(":").append(qop == QualityOfProtection.AUTH_INT ? "auth-int" : "auth")
                    .append(":").append(hasha2);
        }
        digestInput = buffer.toByteArray();
        buffer.reset();

        final String digest = formatHex(digester.digest(digestInput));

        final CharArrayBuffer buffer = new CharArrayBuffer(128);
        buffer.append(StandardAuthScheme.DIGEST + " ");

        final List<BasicNameValuePair> params = new ArrayList<>(20);
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("realm", realm));
        params.add(new BasicNameValuePair("nonce", nonce));
        params.add(new BasicNameValuePair("uri", uri));
        params.add(new BasicNameValuePair("response", digest));

        if (qop != QualityOfProtection.MISSING) {
            params.add(new BasicNameValuePair("qop", qop == QualityOfProtection.AUTH_INT ? "auth-int" : "auth"));
            params.add(new BasicNameValuePair("nc", nc));
            params.add(new BasicNameValuePair("cnonce", cnonce));
        }
        if (algorithm != null)
            params.add(new BasicNameValuePair("algorithm", algorithm));

        if (opaque != null)
            params.add(new BasicNameValuePair("opaque", opaque));

        for (int i = 0; i < params.size(); i++) {
            final BasicNameValuePair param = params.get(i);
            if (i > 0)
                buffer.append(", ");

            final String name = param.getName();
            final boolean noQuotes = ("nc".equals(name) || "qop".equals(name) || "algorithm".equals(name));
            BasicHeaderValueFormatter.INSTANCE.formatNameValuePair(buffer, param, !noQuotes);
        }
        return buffer.toString();
    }

    @Internal
    public String getNonce() {
        return lastNonce;
    }

    @Internal
    public long getNounceCount() {
        return nounceCount;
    }

    @Internal
    public String getCnonce() {
        return cnonce;
    }

    String getA1() {
        return a1 != null ? new String(a1, StandardCharsets.US_ASCII) : null;
    }

    String getA2() {
        return a2 != null ? new String(a2, StandardCharsets.US_ASCII) : null;
    }

    /**
     * Encodes the 128 bit (16 bytes) MD5 digest into a 32 characters long
     * {@code String} according to RFC 2617.
     *
     * @param binaryData array containing the digest
     * @return encoded MD5, or {@code null} if encoding failed
     */
    static String formatHex(final byte[] binaryData) {
        final int n = binaryData.length;
        final char[] buffer = new char[n * 2];
        for (int i = 0; i < n; i++) {
            final int low = (binaryData[i] & 0x0f);
            final int high = ((binaryData[i] & 0xf0) >> 4);
            buffer[i * 2] = HEXADECIMAL[high];
            buffer[(i * 2) + 1] = HEXADECIMAL[low];
        }

        return new String(buffer);
    }

    /**
     * Creates a random cnonce value based on the current time.
     *
     * @return The cnonce value as String.
     */
    static byte[] createCnonce() {
        final SecureRandom rnd = new SecureRandom();
        final byte[] tmp = new byte[8];
        rnd.nextBytes(tmp);
        return tmp;
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(defaultCharset.name());
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.defaultCharset = Charset.forName(in.readUTF());
    }

    @Override
    public String toString() {
        return getName() + this.paramMap;
    }
}
