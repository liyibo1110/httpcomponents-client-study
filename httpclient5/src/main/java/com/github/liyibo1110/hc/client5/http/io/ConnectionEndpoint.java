package com.github.liyibo1110.hc.client5.http.io;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.impl.io.HttpRequestExecutor;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.io.ModalCloseable;
import com.github.liyibo1110.hc.core5.util.Timeout;

import java.io.IOException;

/**
 * 从连接管理器租用的client endpoint，endpoint可用于执行HTTP请求。
 * 一旦不再需要该endpoint，必须使用close(CloseMode)方法将其释放。
 * @author liyibo
 * @date 2026-04-16 13:37
 */
@Contract(threading = ThreadingBehavior.SAFE)
public abstract class ConnectionEndpoint implements ModalCloseable {

    /**
     * 使用提供的请求执行器执行HTTP请求。
     * 一旦不再需要该endpoint，必须使用close(CloseMode)将其释放。
     */
    public abstract ClassicHttpResponse execute(String id,
                                                ClassicHttpRequest request,
                                                HttpRequestExecutor executor,
                                                HttpContext context) throws IOException, HttpException;

    public abstract boolean isConnected();

    public abstract void setSocketTimeout(Timeout timeout);
}
