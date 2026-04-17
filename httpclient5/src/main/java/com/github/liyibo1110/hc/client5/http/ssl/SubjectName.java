package com.github.liyibo1110.hc.client5.http.ssl;

import com.github.liyibo1110.hc.core5.util.Args;

/**
 * @author liyibo
 * @date 2026-04-16 14:38
 */
final class SubjectName {
    static final int DNS = 2;
    static final int IP = 7;

    private final String value;
    private final int type;

    static SubjectName IP(final String value) {
        return new SubjectName(value, IP);
    }

    static SubjectName DNS(final String value) {
        return new SubjectName(value, DNS);
    }

    SubjectName(final String value, final int type) {
        this.value = Args.notNull(value, "Value");
        this.type = Args.positive(type, "Type");
    }

    public int getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
