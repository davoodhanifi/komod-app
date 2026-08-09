package com.komod.api.data.api

import com.komod.api.data.auth.SupabaseAuthDataSource
import com.komod.api.httpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun provideKtorClient(authDataSource: SupabaseAuthDataSource): HttpClient = HttpClient(httpClientEngine()) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        )
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 30_000
        requestTimeoutMillis = 60_000
        socketTimeoutMillis = 60_000
    }
    defaultRequest {
        url(KomodApiConfig.BASE_URL)
        // Not a blanket Content-Type default: every JSON-bodied call already sets its
        // own via contentType(ContentType.Application.Json), and Ktor's DefaultRequest
        // plugin applies this block *after* a per-request builder runs, then keeps
        // whatever it sets here unless the per-request block's headers already had that
        // key present — so a default Content-Type here silently overrides (rather than
        // yields to) a later per-request attempt to clear/replace it, such as the
        // multipart boundary Content-Type that setBody(MultiPartFormDataContent(...))
        // needs to set for itself.
        headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
        authDataSource.currentAccessToken()?.let { token ->
            headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}

// No base URL, no default JSON content type, no auth header — used to fetch raw bytes
// from arbitrary URLs (e.g. pre-signed storage URLs, which already carry their own
// token) without leaking this app's backend bearer token to a third-party host.
fun providePlainHttpClient(): HttpClient = HttpClient(httpClientEngine()) {
    install(HttpTimeout) {
        connectTimeoutMillis = 30_000
        requestTimeoutMillis = 60_000
        socketTimeoutMillis = 60_000
    }
}

fun provideImageHttpClient(authDataSource: SupabaseAuthDataSource): HttpClient = HttpClient(httpClientEngine()) {
    install(HttpTimeout) {
        connectTimeoutMillis = 30_000
        requestTimeoutMillis = 60_000
        socketTimeoutMillis = 60_000
    }
    defaultRequest {
        authDataSource.currentAccessToken()?.let { token ->
            headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
