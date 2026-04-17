package com.github.liyibo1110.hc.client5.http.impl;

/**
 * 请求执行管道支持的组件。
 * @author liyibo
 * @date 2026-04-16 16:15
 */
public enum ChainElement {
    REDIRECT,
    COMPRESS,
    BACK_OFF,
    RETRY,
    CACHING,
    PROTOCOL,
    CONNECT,
    MAIN_TRANSPORT
}
