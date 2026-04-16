package com.github.liyibo1110.hc.client5.http.cookie;

/**
 * HttpClient支持的按名称分类的Cookie规范（意思就是Cookie也有很多不同版本的规范，入NetScape老规则、RFC 2109、RFC 2965、RFC 6265等不同的演进）。
 * @author liyibo
 * @date 2026-04-15 15:17
 */
public final class StandardCookieSpec {
    private StandardCookieSpec() {}

    /** 符合RFC 6265标准的策略（互操作性配置文件） */
    public static final String RELAXED = "relaxed";

    /** 符合RFC 6265标准的策略（严格配置文件）。 */
    public static final String STRICT = "strict";

    /** 忽略cookies的策略 */
    public static final String IGNORE = "ignore";
}
