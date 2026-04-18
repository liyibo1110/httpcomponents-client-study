package com.github.liyibo1110.hc.client5.http.impl.auth;

/**
 * NTLM身份验证引擎的摘要。
 * 该引擎可用于生成Type1消息，并能针对Type2挑战生成Type3消息。
 * @author liyibo
 * @date 2026-04-17 17:06
 */
public interface NTLMEngine {

    /**
     * Generates a Type1 message given the domain and workstation.
     *
     * @param domain Optional Windows domain name. Can be {@code null}.
     * @param workstation Optional Windows workstation name. Can be
     *  {@code null}.
     * @return Type1 message
     * @throws NTLMEngineException
     */
    String generateType1Msg(
            String domain,
            String workstation) throws NTLMEngineException;

    /**
     * Generates a Type3 message given the user credentials and the
     * authentication challenge.
     *
     * @param username Windows user name
     * @param password Password
     * @param domain Windows domain name
     * @param workstation Windows workstation name
     * @param challenge Type2 challenge.
     * @return Type3 response.
     * @throws NTLMEngineException
     */
    String generateType3Msg(
            String username,
            char[] password,
            String domain,
            String workstation,
            String challenge) throws NTLMEngineException;
}
