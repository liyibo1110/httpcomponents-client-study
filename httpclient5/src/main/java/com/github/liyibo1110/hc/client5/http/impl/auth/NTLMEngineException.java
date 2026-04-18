package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthenticationException;

/**
 * NTLM protocol失败的异常。
 * @author liyibo
 * @date 2026-04-17 17:07
 */
public class NTLMEngineException extends AuthenticationException {

    private static final long serialVersionUID = 6027981323731768824L;

    public NTLMEngineException() {
        super();
    }

    public NTLMEngineException(final String message) {
        super(message);
    }

    public NTLMEngineException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
