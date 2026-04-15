package com.github.liyibo1110.hc.client5.http.utils;

import com.github.liyibo1110.hc.core5.annotation.Internal;

/**
 * @author liyibo
 * @date 2026-04-14 10:48
 */
@Internal
public final class Hex {

    private Hex() {}

    private static final char[] DIGITS_LOWER = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * byte[] -> 16进制字符串
     */
    public static String encodeHexString(final byte[] bytes) {
        final char[] out = new char[bytes.length * 2];
        encodeHex(bytes, 0, bytes.length, DIGITS_LOWER, out, 0);
        return new String(out);
    }

    private static void encodeHex(final byte[] data, final int dataOffset, final int dataLen, final char[] toDigits,
                                  final char[] out, final int outOffset) {
        // two characters form the hex value.
        for (int i = dataOffset, j = outOffset; i < dataOffset + dataLen; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
    }
}
