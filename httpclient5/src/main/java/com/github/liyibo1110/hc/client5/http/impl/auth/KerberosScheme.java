package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.DnsResolver;
import com.github.liyibo1110.hc.client5.http.auth.KerberosConfig;
import com.github.liyibo1110.hc.client5.http.auth.StandardAuthScheme;
import com.github.liyibo1110.hc.core5.annotation.Experimental;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

/**
 * Kerberos身份验证方案。
 * 请注意，该类目前仍处于实验阶段，未来可能会被停用或移除。
 * @author liyibo
 * @date 2026-04-17 23:28
 */
@Experimental
public class KerberosScheme extends GGSSchemeBase {
    private static final String KERBEROS_OID = "1.2.840.113554.1.2.2";

    /**
     * @since 5.0
     */
    public KerberosScheme(final KerberosConfig config, final DnsResolver dnsResolver) {
        super(config, dnsResolver);
    }

    public KerberosScheme() {
        super();
    }

    @Override
    public String getName() {
        return StandardAuthScheme.KERBEROS;
    }

    @Override
    protected byte[] generateToken(final byte[] input, final String serviceName, final String authServer) throws GSSException {
        return generateGSSToken(input, new Oid(KERBEROS_OID), serviceName, authServer);
    }

    @Override
    public boolean isConnectionBased() {
        return true;
    }
}
