package com.github.liyibo1110.hc.client5.http.entity.mime;

/**
 * MIME的兼容模式。
 * @author liyibo
 * @date 2026-04-15 22:17
 */
public enum HttpMultipartMode {

    /**
     * 向后兼容模式。
     * 在此模式下，仅生成最基本的字段，例如Content-Type和Content-Disposition。
     */
    LEGACY,

    /**
     * 严格符合MIME规范。
     * 目前符合RFC 822、RFC 2045和RFC 2046标准。
     */
    STRICT,

    /**
     * 符合扩展MIME规范。
     * 在此模式下，标头字段的值可能包含国际化的UTF-8编码字符。
     * 目前符合RFC 6532和RFC 7578规范。
     */
    EXTENDED
}
