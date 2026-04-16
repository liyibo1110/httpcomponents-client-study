package com.github.liyibo1110.hc.client5.http.auth;

/**
 * Challenge的来源类型。
 * 所谓Challenge就是client第一次不带任何凭证去访问server，server一般会返回401（如果server是proxy则返回407），
 * 同时响应头会附带WWW-Authenticate（proxy可能是Proxy-Authenticate），内容是server可以支持的认证方案，这个响应头内容就是Challenge。
 * @author liyibo
 * @date 2026-04-15 13:10
 */
public enum ChallengeType {

    TARGET, PROXY
}
