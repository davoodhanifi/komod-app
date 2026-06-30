package com.komod.api.data.upload

import com.komod.api.data.auth.SupabaseAuthDataSource
import com.komod.api.httpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class UploadService(
    private val supabaseUrl: String,
    private val authDataSource: SupabaseAuthDataSource,
) {
    private val httpClient = HttpClient(httpClientEngine()) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
        }
    }

    /**
     * Uploads image bytes directly to Supabase Storage using the storagePath
     * returned by POST /api/v1/images (e.g. "wardrobe/{userId}/originals/{imageId}.jpg").
     */
    suspend fun upload(
        storagePath: String,
        bytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
    ) {
        val uploadUrl = "$supabaseUrl/storage/v1/object/$storagePath"
        val token = authDataSource.currentAccessToken()

        onProgress(0f)
        httpClient.put(uploadUrl) {
            contentType(ContentType.parse(mimeType))
            header("x-upsert", "true")
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(bytes)
        }
        onProgress(1f)
    }
}
