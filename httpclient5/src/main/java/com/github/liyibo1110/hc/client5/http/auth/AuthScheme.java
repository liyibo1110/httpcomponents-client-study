package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.security.Principal;

/**
 * 该接口表示一种基于挑战-响应的抽象身份验证方案。
 *
 * 身份验证方案可以是基于请求的，也可以是基于连接的。
 * 前者预计会在每个请求消息中提供授权响应，而后者仅执行一次，并在整个生命周期内适用于底层连接。
 * 在复用通过基于连接的身份验证方案授权的连接时需格外谨慎，因为这些连接可能携带特定的安全上下文，并针对特定用户身份进行了授权。
 * 此类方案必须始终通过getPrincipal()方法提供其所代表的用户身份。
 *
 * 身份验证方案应经历一系列标准阶段或状态。
 * 身份验证方案在其生命周期开始时既无上下文也无特定状态。
 *
 * 调用processChallenge(AuthChallenge, HttpContext)方法来处理从目标服务器或代理接收的身份验证挑战。
 * 身份验证方案将过渡到CHALLENGED状态，并应验证作为参数传递给它的令牌，并根据挑战详细信息初始化其内部状态。
 * 标准身份验证方案应在挑战中提供realm属性。可调用getRealm()来获取需要授权的域的标识符。
 *
 * 挑战处理完成后，需调用isResponseReady(HttpHost, CredentialsProvider, HttpContext)方法，以确定该方案能否基于当前状态生成授权响应，
 * 且是否持有执行此操作所需的用户凭证。若该方法返回false，则认证被视为处于FAILED状态，且不生成授权响应。
 * 否则该方案被视为处于RESPONSE_READY状态。
 *
 * 当方案准备好响应挑战时，调用generateAuthResponse(HttpHost, HttpRequest, HttpContext)方法生成响应令牌，该令牌将在后续请求消息中发送至对端。
 * 某些非标准方案可能需要多次挑战/响应交互，以完全建立共享上下文并完成身份验证过程。身份验证方案必须在握手被视为完成时，使isChallengeComplete()返回true。
 * 如果对端接受了包含授权响应的请求消息，并返回一条表明未发生身份验证失败的消息，则认为身份验证方案已成功完成，并处于SUCCESS状态。
 * 如果对端针对包含最终授权响应的请求消息返回状态码401或407，则认为该方案未成功，并处于FAILED状态。
 * @author liyibo
 * @date 2026-04-15 14:05
 */
public interface AuthScheme {

    /**
     * 返回指定身份验证方案的文本名称。
     */
    String getName();

    /**
     * 确定身份验证方案是否应按连接提供授权响应，而非按请求提供（这是标准做法）。
     */
    boolean isConnectionBased();

    /**
     * 处理给定的身份验证Challenge。
     * 某些身份验证方案可能涉及多次挑战-响应交互。此类方案在处理连续Challenge时，必须能够维护内部状态。
     */
    void processChallenge(AuthChallenge authChallenge, HttpContext context) throws MalformedChallengeException;

    /**
     * 身份验证过程可能涉及一系列挑战-响应交互。
     * 该方法用于验证授权过程是否已完全完成（无论成功与否），即所有必需的授权挑战是否已全部处理完毕。
     */
    boolean isChallengeComplete();

    /**
     * 返回身份验证域。如果身份验证域的概念不适用于给定的身份验证方案，则返回null。
     */
    String getRealm();

    /**
     * 根据实际身份验证状态，确定是否可以生成授权响应。
     * 通常，此方法的结果取决于生成授权响应所需的用户凭据是否可用。
     */
    boolean isResponseReady(HttpHost host, CredentialsProvider credentialsProvider, HttpContext context) throws AuthenticationException;

    /**
     * 返回其凭据用于生成身份验证响应的主体。
     * 如果授权在整个连接生命周期内均适用，则基于连接的方案必须返回用户主体。
     */
    Principal getPrincipal();

    /**
     * 根据当前状态生成授权响应。
     * 某些身份验证方案可能需要在调用此方法之前，从CredentialsProvider加载生成授权响应所需的用户凭据。
     */
    String generateAuthResponse(HttpHost host, HttpRequest request, HttpContext context) throws AuthenticationException;
}
