package com.github.liyibo1110.hc.client5.http.auth;

import java.security.Principal;

/**
 * 该接口表示一组凭据，由一个安全主体和一个密钥（密码）组成，可用于验证用户身份。
 * @author liyibo
 * @date 2026-04-15 12:55
 */
public interface Credentials {

    Principal getUserPrincipal();

    char[] getPassword();
}
