package com.github.liyibo1110.hc.client5.http.auth;

/**
 * HttpClient支持的按名称分类的身份验证方案。
 * @author liyibo
 * @date 2026-04-15 14:26
 */
public final class StandardAuthScheme {

    private StandardAuthScheme() {}

    /** RFC 2617中定义的基本身份验证方案（在没有传输层加密的情况下被认为本质上是不安全的，但支持范围最广） */
    public static final String BASIC = "Basic";

    /** RFC 2617中定义的摘要认证方案 */
    public static final String DIGEST = "Digest";

    /** NTLM身份验证方案是[MS-NLMP]中定义的Microsoft Windows专有身份验证协议 */
    public static final String NTLM = "NTLM";

    /** RFC 4559和RFC 4178中定义的SPNEGO身份验证方案（如果选择了Kerberos，则被认为是当前支持的身份验证方案中最安全的） */
    public static final String SPNEGO = "Negotiate";

    /** RFC 4120中定义的Kerberos身份验证方案（被认为是当前支持的身份验证方案中最安全的） */
    public static final String KERBEROS = "Kerberos";
}
