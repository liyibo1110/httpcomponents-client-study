package com.github.liyibo1110.hc.client5.http.ssl;

import com.github.liyibo1110.hc.core5.http.NameValuePair;
import com.github.liyibo1110.hc.core5.http.message.BasicNameValuePair;
import com.github.liyibo1110.hc.core5.util.CharArrayBuffer;
import com.github.liyibo1110.hc.core5.util.Tokenizer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * @author liyibo
 * @date 2026-04-16 16:01
 */
final class DistinguishedNameParser {
    public final static DistinguishedNameParser INSTANCE = new DistinguishedNameParser();

    private static final BitSet EQUAL_OR_COMMA_OR_PLUS      = Tokenizer.INIT_BITSET('=', ',', '+');
    private static final BitSet COMMA_OR_PLUS               = Tokenizer.INIT_BITSET(',', '+');

    private final Tokenizer tokenParser;

    DistinguishedNameParser() {
        this.tokenParser = new InternalTokenParser();
    }

    private String parseToken(final CharArrayBuffer buf, final Tokenizer.Cursor cursor, final BitSet delimiters) {
        return tokenParser.parseToken(buf, cursor, delimiters);
    }

    private String parseValue(final CharArrayBuffer buf, final Tokenizer.Cursor cursor, final BitSet delimiters) {
        return tokenParser.parseValue(buf, cursor, delimiters);
    }

    private NameValuePair parseParameter(final CharArrayBuffer buf, final Tokenizer.Cursor cursor) {
        final String name = parseToken(buf, cursor, EQUAL_OR_COMMA_OR_PLUS);
        if (cursor.atEnd()) {
            return new BasicNameValuePair(name, null);
        }
        final int delim = buf.charAt(cursor.getPos());
        cursor.updatePos(cursor.getPos() + 1);
        if (delim == ',') {
            return new BasicNameValuePair(name, null);
        }
        final String value = parseValue(buf, cursor, COMMA_OR_PLUS);
        if (!cursor.atEnd()) {
            cursor.updatePos(cursor.getPos() + 1);
        }
        return new BasicNameValuePair(name, value);
    }

    List<NameValuePair> parse(final CharArrayBuffer buf, final Tokenizer.Cursor cursor) {
        final List<NameValuePair> params = new ArrayList<>();
        tokenParser.skipWhiteSpace(buf, cursor);
        while (!cursor.atEnd()) {
            final NameValuePair param = parseParameter(buf, cursor);
            params.add(param);
        }
        return params;
    }

    List<NameValuePair> parse(final String s) {
        if (s == null) {
            return null;
        }
        final CharArrayBuffer buffer = new CharArrayBuffer(s.length());
        buffer.append(s);
        final Tokenizer.Cursor cursor = new Tokenizer.Cursor(0, s.length());
        return parse(buffer, cursor);
    }

    static class InternalTokenParser extends Tokenizer {

        @Override
        public void copyUnquotedContent(
                final CharSequence buf,
                final Tokenizer.Cursor cursor,
                final BitSet delimiters,
                final StringBuilder dst) {
            int pos = cursor.getPos();
            final int indexFrom = cursor.getPos();
            final int indexTo = cursor.getUpperBound();
            boolean escaped = false;
            for (int i = indexFrom; i < indexTo; i++, pos++) {
                final char current = buf.charAt(i);
                if (escaped) {
                    dst.append(current);
                    escaped = false;
                } else {
                    if ((delimiters != null && delimiters.get(current))
                            || Tokenizer.isWhitespace(current) || current == '\"') {
                        break;
                    } else if (current == '\\') {
                        escaped = true;
                    } else {
                        dst.append(current);
                    }
                }
            }
            cursor.updatePos(pos);
        }
    }
}
