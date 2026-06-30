package com.komod.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

actual fun httpClientEngine(): HttpClientEngine = OkHttp.create {
    config {
        // Trust the local ASP.NET Core dev certificate (self-signed).
        // The backend redirects HTTP → HTTPS, so we connect over HTTPS directly.
        // TODO: Replace with certificate pinning or remove before production release.
        val trustDevCert = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustDevCert), SecureRandom())
        }
        sslSocketFactory(sslContext.socketFactory, trustDevCert)
        hostnameVerifier { _, _ -> true }
    }
}
