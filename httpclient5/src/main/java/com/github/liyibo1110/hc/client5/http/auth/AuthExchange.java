package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.util.Args;

import java.util.Queue;

/**
 * 该类表示身份验证握手过程的实际状态，包括当前用于请求授权的AuthScheme，以及（如有）一组备用身份验证选项。
 * @author liyibo
 * @date 2026-04-15 14:23
 */
public class AuthExchange {
    public enum State { UNCHALLENGED, CHALLENGED, HANDSHAKE, FAILURE, SUCCESS }

    private State state;
    private AuthScheme authScheme;
    private Queue<AuthScheme> authOptions;
    private String pathPrefix;

    public AuthExchange() {
        super();
        this.state = State.UNCHALLENGED;
    }

    public void reset() {
        this.state = State.UNCHALLENGED;
        this.authOptions = null;
        this.authScheme = null;
        this.pathPrefix = null;
    }

    public State getState() {
        return this.state;
    }

    public void setState(final State state) {
        this.state = state != null ? state : State.UNCHALLENGED;
    }

    public AuthScheme getAuthScheme() {
        return this.authScheme;
    }

    public boolean isConnectionBased() {
        return this.authScheme != null && this.authScheme.isConnectionBased();
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(final String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    /**
     * 使用给定的AuthScheme重置身份验证状态，并清除身份验证选项。
     */
    public void select(final AuthScheme authScheme) {
        Args.notNull(authScheme, "Auth scheme");
        this.authScheme = authScheme;
        this.authOptions = null;
    }

    public Queue<AuthScheme> getAuthOptions() {
        return this.authOptions;
    }

    public void setOptions(final Queue<AuthScheme> authOptions) {
        Args.notEmpty(authOptions, "Queue of auth options");
        this.authOptions = authOptions;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("[").append(this.state);
        if (this.authScheme != null)
            buffer.append(" ").append(this.authScheme);
        buffer.append("]");
        return buffer.toString();
    }
}
