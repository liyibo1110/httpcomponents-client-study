package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.DnsResolver;
import com.github.liyibo1110.hc.client5.http.SystemDefaultDnsResolver;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthSchemeFactory;
import com.github.liyibo1110.hc.client5.http.auth.KerberosConfig;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Experimental;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * 生成SPNegoScheme对象的工厂。
 * @author liyibo
 * @date 2026-04-17 17:23
 */
@Contract(threading = ThreadingBehavior.STATELESS)
@Experimental
public class SPNegoSchemeFactory implements AuthSchemeFactory {
    public static final SPNegoSchemeFactory DEFAULT = new SPNegoSchemeFactory(KerberosConfig.DEFAULT, SystemDefaultDnsResolver.INSTANCE);

    private final KerberosConfig config;
    private final DnsResolver dnsResolver;

    public SPNegoSchemeFactory(final KerberosConfig config, final DnsResolver dnsResolver) {
        super();
        this.config = config;
        this.dnsResolver = dnsResolver;
    }

    @Override
    public AuthScheme create(final HttpContext context) {
        return new SPNegoScheme(this.config, this.dnsResolver);
    }
}
