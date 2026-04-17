package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;

import java.nio.ByteBuffer;

/**
 * @author liyibo
 * @date 2026-04-16 17:48
 */
public class Wire {
    private static final int MAX_STRING_BUILDER_SIZE = 2048;

    private static final ThreadLocal<StringBuilder> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 返回一个StringBuilder，该Layout实现可用于将格式化的日志事件写入其中
     */
    private static StringBuilder getStringBuilder() {
        StringBuilder result = THREAD_LOCAL.get();
        if (result == null) {
            result = new StringBuilder(MAX_STRING_BUILDER_SIZE);
            THREAD_LOCAL.set(result);
        }
        trimToMaxSize(result, MAX_STRING_BUILDER_SIZE);
        result.setLength(0);
        return result;
    }

    /**
     * 确保指定StringBuilder的char[]数组长度不超过指定的字符数。
     * 此方法有助于确保过长的char[]数组不会永久保存在内存中。
     */
    private static void trimToMaxSize(final StringBuilder sb, final int maxSize) {
        if (sb != null && sb.capacity() > maxSize) {
            sb.setLength(maxSize);
            sb.trimToSize();
        }
    }

    private final Logger log;
    private final String id;

    public Wire(final Logger log, final String id) {
        super();
        this.log = log;
        this.id = id;
    }

    private void wire(final String header, final byte[] b, final int pos, final int off) {
        final StringBuilder buffer = getStringBuilder();
        for (int i = 0; i < off; i++) {
            final int ch = b[pos + i];
            if (ch == 13) {
                buffer.append("[\\r]");
            } else if (ch == 10) {
                buffer.append("[\\n]\"");
                buffer.insert(0, "\"");
                buffer.insert(0, header);
                log.debug("{} {}", this.id, buffer);
                buffer.setLength(0);
            } else if ((ch < 32) || (ch >= 127)) {
                buffer.append("[0x");
                buffer.append(Integer.toHexString(ch));
                buffer.append("]");
            } else {
                buffer.append((char) ch);
            }
        }
        if (buffer.length() > 0) {
            buffer.append('\"');
            buffer.insert(0, '\"');
            buffer.insert(0, header);
            log.debug("{} {}", this.id, buffer);
        }
    }

    public boolean isEnabled() {
        return log.isDebugEnabled();
    }

    public void output(final byte[] b, final int pos, final int off) {
        Args.notNull(b, "Output");
        wire(">> ", b, pos, off);
    }

    public void input(final byte[] b, final int pos, final int off) {
        Args.notNull(b, "Input");
        wire("<< ", b, pos, off);
    }

    public void output(final byte[] b) {
        Args.notNull(b, "Output");
        output(b, 0, b.length);
    }

    public void input(final byte[] b) {
        Args.notNull(b, "Input");
        input(b, 0, b.length);
    }

    public void output(final int b) {
        output(new byte[] {(byte) b});
    }

    public void input(final int b) {
        input(new byte[] {(byte) b});
    }

    public void output(final String s) {
        Args.notNull(s, "Output");
        output(s.getBytes());
    }

    public void input(final String s) {
        Args.notNull(s, "Input");
        input(s.getBytes());
    }

    public void output(final ByteBuffer b) {
        Args.notNull(b, "Output");
        if (b.hasArray()) {
            output(b.array(), b.arrayOffset() + b.position(), b.remaining());
        } else {
            final byte[] tmp = new byte[b.remaining()];
            b.get(tmp);
            output(tmp);
        }
    }

    public void input(final ByteBuffer b) {
        Args.notNull(b, "Input");
        if (b.hasArray()) {
            input(b.array(), b.arrayOffset() + b.position(), b.remaining());
        } else {
            final byte[] tmp = new byte[b.remaining()];
            b.get(tmp);
            input(tmp);
        }
    }
}
