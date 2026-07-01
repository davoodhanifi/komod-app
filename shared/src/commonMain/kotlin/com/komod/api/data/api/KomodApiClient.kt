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
        contentType(ContentType.Application.Json)
        headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
        authDataSource.currentAccessToken()?.let { token ->
            headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
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
