package com.github.liyibo1110.hc.client5.http;

import java.io.IOException;

/**
 * @author liyibo
 * @date 2026-04-14 13:45
 */
public class UnsupportedSchemeException extends IOException {
    private static final long serialVersionUID = 3597127619218687636L;

    public UnsupportedSchemeException(final String message) {
        super(message);
    }
}
