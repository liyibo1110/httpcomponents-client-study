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
 * 生成KerberosScheme对象的工厂。
 * @author liyibo
 * @date 2026-04-17 23:29
 */
@Contract(threading = ThreadingBehavior.STATELESS)
@Experimental
public class KerberosSchemeFactory implements AuthSchemeFactory {

    public static final KerberosSchemeFactory DEFAULT = new KerberosSchemeFactory(KerberosConfig.DEFAULT, SystemDefaultDnsResolver.INSTANCE);

    private final KerberosConfig config;
    private final DnsResolver dnsResolver;

    public KerberosSchemeFactory(final KerberosConfig config, final DnsResolver dnsResolver) {
        super();
        this.config = config;
        this.dnsResolver = dnsResolver;
    }

    @Override
    public AuthScheme create(final HttpContext context) {
        return new KerberosScheme(this.config, this.dnsResolver);
    }
}
