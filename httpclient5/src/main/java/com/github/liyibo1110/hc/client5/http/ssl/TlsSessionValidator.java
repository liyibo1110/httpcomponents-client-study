package com.github.liyibo1110.hc.client5.http.ssl;

import org.slf4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author liyibo
 * @date 2026-04-21 11:43
 */
public class TlsSessionValidator {

    private final Logger log;

    TlsSessionValidator(final Logger log) {
        this.log = log;
    }

    void verifySession(final String hostname, final SSLSession sslsession, final HostnameVerifier hostnameVerifier)
            throws SSLException {

        if (log.isDebugEnabled()) {
            log.debug("Secure session established");
            log.debug(" negotiated protocol: {}", sslsession.getProtocol());
            log.debug(" negotiated cipher suite: {}", sslsession.getCipherSuite());

            try {

                final Certificate[] certs = sslsession.getPeerCertificates();
                final Certificate cert = certs[0];
                if (cert instanceof X509Certificate) {
                    final X509Certificate x509 = (X509Certificate) cert;
                    final X500Principal peer = x509.getSubjectX500Principal();

                    log.debug(" peer principal: {}", peer);
                    final Collection<List<?>> altNames1 = x509.getSubjectAlternativeNames();
                    if (altNames1 != null) {
                        final List<String> altNames = new ArrayList<>();
                        for (final List<?> aC : altNames1) {
                            if (!aC.isEmpty())
                                altNames.add(Objects.toString(aC.get(1), null));
                        }
                        log.debug(" peer alternative names: {}", altNames);
                    }

                    final X500Principal issuer = x509.getIssuerX500Principal();
                    log.debug(" issuer principal: {}", issuer);
                    final Collection<List<?>> altNames2 = x509.getIssuerAlternativeNames();
                    if (altNames2 != null) {
                        final List<String> altNames = new ArrayList<>();
                        for (final List<?> aC : altNames2) {
                            if (!aC.isEmpty())
                                altNames.add(Objects.toString(aC.get(1), null));
                        }
                        log.debug(" issuer alternative names: {}", altNames);
                    }
                }
            } catch (final Exception ignore) {
            }
        }

        if (hostnameVerifier != null) {
            final Certificate[] certs = sslsession.getPeerCertificates();
            if (certs.length < 1)
                throw new SSLPeerUnverifiedException("Peer certificate chain is empty");

            final Certificate peerCertificate = certs[0];
            final X509Certificate x509Certificate;
            if (peerCertificate instanceof X509Certificate) {
                x509Certificate = (X509Certificate) peerCertificate;
            } else {
                throw new SSLPeerUnverifiedException("Unexpected certificate type: " + peerCertificate.getType());
            }
            if (hostnameVerifier instanceof HttpClientHostnameVerifier) {
                ((HttpClientHostnameVerifier) hostnameVerifier).verify(hostname, x509Certificate);
            } else if (!hostnameVerifier.verify(hostname, sslsession)) {
                final List<SubjectName> subjectAlts = DefaultHostnameVerifier.getSubjectAltNames(x509Certificate);
                throw new SSLPeerUnverifiedException("Certificate for <" + hostname + "> doesn't match any " +
                        "of the subject alternative names: " + subjectAlts);
            }
        }
    }
}
