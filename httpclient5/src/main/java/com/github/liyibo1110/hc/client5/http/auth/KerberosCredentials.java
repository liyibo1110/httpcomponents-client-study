package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import org.ietf.jgss.GSSCredential;

import java.io.Serializable;
import java.security.Principal;

/**
 * 基于GSSCredential的Kerberos专用Credentials表示形式。
 * @author liyibo
 * @date 2026-04-15 13:54
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class KerberosCredentials implements Credentials, Serializable {
    private static final long serialVersionUID = 487421613855550713L;

    /** GSSCredential  */
    private final GSSCredential gssCredential;

    public KerberosCredentials(final GSSCredential gssCredential) {
        this.gssCredential = gssCredential;
    }

    public GSSCredential getGSSCredential() {
        return gssCredential;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public char[] getPassword() {
        return null;
    }
}
