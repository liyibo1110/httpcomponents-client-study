package com.github.liyibo1110.hc.client5.http.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * 用于字节序列的builder类。
 * @author liyibo
 * @date 2026-04-14 10:09
 */
public final class ByteArrayBuilder {
    private CharsetEncoder charsetEncoder;
    private ByteBuffer buffer;

    public ByteArrayBuilder() {}

    public ByteArrayBuilder(final int initialCapacity) {
        this.buffer = ByteBuffer.allocate(initialCapacity);
    }

    public int capacity() {
        return this.buffer != null ? this.buffer.capacity() : 0;
    }

    /**
     * 如果给定的capacity不够用，则尝试扩容给定的ByteBuffer（扩容capacity个新位置）
     */
    static ByteBuffer ensureFreeCapacity(final ByteBuffer buffer, final int capacity) {
        if (buffer == null)
            return ByteBuffer.allocate(capacity);
        if (buffer.remaining() < capacity) {
            // 空间不够就扩容，返回新的buffer
            final ByteBuffer newBuffer = ByteBuffer.allocate(buffer.position() + capacity);
            buffer.flip();
            newBuffer.put(buffer);
            return newBuffer;
        }
        return buffer;
    }

    static ByteBuffer encode(final ByteBuffer buffer, final CharBuffer in, final CharsetEncoder encoder)
            throws CharacterCodingException {
        // 计算编码后需要的总字节长度，并扩容到该长度
        final int capacity = (int) (in.remaining() * encoder.averageBytesPerChar());
        ByteBuffer out = ensureFreeCapacity(buffer, capacity);
        while (in.hasRemaining()) {
            CoderResult result = encoder.encode(in, out, true);
            if (result.isError())
                result.throwException();
            if (result.isUnderflow())
                result = encoder.flush(out);
            if (result.isUnderflow())
                break;
            if (result.isOverflow())
                out = ensureFreeCapacity(out, capacity);
        }
        return out;
    }

    public void ensureFreeCapacity(final int freeCapacity) {
        this.buffer = ensureFreeCapacity(this.buffer, freeCapacity);
    }

    /**
     * 将CharBuffer的字符进行编码，然后追加到buffer中
     */
    private void doAppend(final CharBuffer charBuffer) {
        if (this.charsetEncoder == null) {
            this.charsetEncoder = StandardCharsets.US_ASCII.newEncoder()
                    .onMalformedInput(CodingErrorAction.IGNORE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
        }
        this.charsetEncoder.reset();
        try {
            this.buffer = encode(this.buffer, charBuffer, this.charsetEncoder);
        } catch (final CharacterCodingException ex) {
            // Should never happen
            throw new IllegalStateException("Unexpected character coding error", ex);
        }
    }

    /**
     * 设置buffer需要的字符集。
     */
    public ByteArrayBuilder charset(final Charset charset) {
        if (charset == null) {
            this.charsetEncoder = null;
        } else {
            this.charsetEncoder = charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.IGNORE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
        }
        return this;
    }

    public ByteArrayBuilder append(final byte[] b, final int off, final int len) {
        if (b == null)
            return this;
        if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) < 0) || ((off + len) > b.length))
            throw new IndexOutOfBoundsException("off: " + off + " len: " + len + " b.length: " + b.length);
        ensureFreeCapacity(len);
        this.buffer.put(b, off, len);
        return this;
    }

    public ByteArrayBuilder append(final byte[] b) {
        if (b == null)
            return this;
        return append(b, 0, b.length);
    }

    public ByteArrayBuilder append(final CharBuffer charBuffer) {
        if (charBuffer == null)
            return this;
        doAppend(charBuffer);
        return this;
    }

    public ByteArrayBuilder append(final char[] b, final int off, final int len) {
        if (b == null)
            return this;
        if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) < 0) || ((off + len) > b.length))
            throw new IndexOutOfBoundsException("off: " + off + " len: " + len + " b.length: " + b.length);
        return append(CharBuffer.wrap(b, off, len));
    }

    public ByteArrayBuilder append(final char[] b) {
        if (b == null)
            return this;
        return append(b, 0, b.length);
    }

    public ByteArrayBuilder append(final String s) {
        if (s == null)
            return this;
        return append(CharBuffer.wrap(s));
    }

    /**
     * 复制buffer并返回。
     */
    public ByteBuffer toByteBuffer() {
        return this.buffer != null ? this.buffer.duplicate() : ByteBuffer.allocate(0);
    }

    /**
     * ByteBuffer -> byte[]
     */
    public byte[] toByteArray() {
        if (this.buffer == null)
            return new byte[] {};
        this.buffer.flip();
        final byte[] b = new byte[this.buffer.remaining()];
        this.buffer.get(b);
        this.buffer.clear();
        return b;
    }

    public void reset() {
        if (this.charsetEncoder != null)
            this.charsetEncoder.reset();
        if (this.buffer != null)
            this.buffer.clear();
    }

    @Override
    public String toString() {
        return this.buffer != null ? this.buffer.toString() : "null";
    }
}
