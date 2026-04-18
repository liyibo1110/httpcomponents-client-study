package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.DnsResolver;
import com.github.liyibo1110.hc.client5.http.auth.KerberosConfig;
import com.github.liyibo1110.hc.client5.http.auth.StandardAuthScheme;
import com.github.liyibo1110.hc.core5.annotation.Experimental;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

/**
 * SPNEGO（简单且受保护的GSSAPI协商机制）身份验证方案。
 * 请注意，该类目前仍处于实验阶段，未来可能会被停用或移除。
 * @author liyibo
 * @date 2026-04-17 17:22
 */
@Experimental
public class SPNegoScheme extends GGSSchemeBase {

    private static final String SPNEGO_OID = "1.3.6.1.5.5.2";

    /**
     * @since 5.0
     */
    public SPNegoScheme(final KerberosConfig config, final DnsResolver dnsResolver) {
        super(config, dnsResolver);
    }

    public SPNegoScheme() {
        super();
    }

    @Override
    public String getName() {
        return StandardAuthScheme.SPNEGO;
    }

    @Override
    protected byte[] generateToken(final byte[] input, final String serviceName, final String authServer) throws GSSException {
        return generateGSSToken(input, new Oid(SPNEGO_OID), serviceName, authServer);
    }

    @Override
    public boolean isConnectionBased() {
        return true;
    }
}
