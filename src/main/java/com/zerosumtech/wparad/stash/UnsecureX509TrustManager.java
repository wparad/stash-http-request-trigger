package com.zerosumtech.wparad.stash;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class UnsecureX509TrustManager implements X509TrustManager
{
    public X509Certificate[] getAcceptedIssuers()
	{
        return null;
    }

    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) throws CertificateException {}
    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) throws CertificateException {}
}