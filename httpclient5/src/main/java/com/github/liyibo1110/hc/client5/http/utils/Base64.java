package com.github.liyibo1110.hc.client5.http.utils;

import com.github.liyibo1110.hc.core5.annotation.Internal;

import static java.util.Base64.getEncoder;
import static java.util.Base64.getMimeDecoder;

/**
 * 提供来自Commons Codec的Base64转换方法的实现，并委托给Java的Base64实现。
 * 此处仅实现了HttpClient当前使用的功能，而非Commons Codec的全部功能。
 * 1、Commons Codec接受空输入，因此此处也接受这种输入。Java 8的实现中并未这样做。
 * 2、解码无效输入会返回空值。Java 8的实现会抛出异常，此处捕获了该异常。
 * 3、Commons Codec解码器同时接受标准格式和URL安全格式的输入。由于这并非HttpClient的要求，因此此处未实现此功能。
 * @author liyibo
 * @date 2026-04-14 10:02
 */
@Internal
public class Base64 {
    private static final Base64 CODEC = new Base64();
    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * 返回一个使用标准Base64字符集（而非URL安全字符集）的Base64编解码器实例。
     * 请注意，与Commons Codec版本不同，该类不会解码URL安全字符集中的字符。
     */
    public Base64() {}

    /**
     * 创建一个用于在URL不安全模式下进行解码和编码的Base64编解码器。
     * 由于HttpClient从未使用过非零长度的数据，因此此功能未实现。
     */
    public Base64(final int lineLength) {
        if (lineLength != 0)
            throw new UnsupportedOperationException("Line breaks not supported");
    }

    /**
     * base64 -> octets
     */
    public static byte[] decodeBase64(final byte[] base64) {
        return CODEC.decode(base64);
    }

    public static byte[] decodeBase64(final String base64) {
        return CODEC.decode(base64);
    }

    /**
     * octets -> base64
     */
    public static byte[] encodeBase64(final byte[] base64) {
        return CODEC.encode(base64);
    }

    public static String encodeBase64String(final byte[] bytes) {
        if (null == bytes)
            return null;
        return getEncoder().encodeToString(bytes);
    }

    public byte[] decode(final byte[] base64) {
        if (null == base64)
            return null;

        try {
            return getMimeDecoder().decode(base64);
        } catch (final IllegalArgumentException e) {
            return EMPTY_BYTES;
        }
    }

    public byte[] decode(final String base64) {
        if (null == base64)
            return null;

        try {
            // getMimeDecoder is used instead of getDecoder as it better matches the
            // functionality of the default Commons Codec implementation (primarily more forgiving of strictly
            // invalid inputs to decode)
            // Code using java.util.Base64 directly should make a choice based on whether this forgiving nature is
            // appropriate.
            return getMimeDecoder().decode(base64);
        } catch (final IllegalArgumentException e) {
            return EMPTY_BYTES;
        }
    }

    public byte[] encode(final byte[] value) {
        if (null == value)
            return null;
        return getEncoder().encode(value);
    }
}
