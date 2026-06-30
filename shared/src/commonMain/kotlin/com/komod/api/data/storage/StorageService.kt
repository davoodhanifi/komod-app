package com.komod.api.data.storage

import com.komod.api.httpClientEngine
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

/**
 * Uploads images directly to Supabase Storage using the authenticated Supabase session.
 *
 * Uses a dedicated Ktor client (not the Supabase SDK's internal client) so that
 * upload-specific timeouts can be configured explicitly.
 *
 * The [storagePath] returned by the backend includes the bucket name as the first path segment
 * (e.g. "wardrobe/{userId}/originals/{imageId}.jpg") — no bucket names or URLs are hardcoded.
 */
class StorageService(
    private val supabaseUrl: String,
    private val supabaseClient: SupabaseClient,
) {
    private val httpClient = HttpClient(httpClientEngine()) {
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 120_000
            socketTimeoutMillis = 120_000
        }
    }

    suspend fun upload(
        storagePath: String,
        bytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
    ) {
        require(storagePath.contains("/")) {
            "Invalid storagePath — expected '<bucket>/<object-path>', got: $storagePath"
        }

        val uploadUrl = "${supabaseUrl.trimEnd('/')}/storage/v1/object/$storagePath"
        val token = supabaseClient.auth.currentSessionOrNull()?.accessToken

        onProgress(0f)
        val response = httpClient.put(uploadUrl) {
            contentType(ContentType.parse(mimeType))
            header("x-upsert", "true")
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(bytes)
        }
        check(response.status.value in 200..299) {
            "Storage upload failed — HTTP ${response.status.value}: ${response.status.description}"
        }
        onProgress(1f)
    }
}
