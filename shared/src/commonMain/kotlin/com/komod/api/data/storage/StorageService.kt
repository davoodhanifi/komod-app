package com.komod.api.data.storage

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType

private const val BUCKET = "wardrobe"

/**
 * Uploads images to Supabase Storage using the authenticated Supabase session.
 *
 * The [storagePath] returned by the backend is the object path within the bucket
 * (e.g. "{userId}/originals/{imageId}.jpg"). The bucket name ("wardrobe") is
 * fixed and NOT included in the path.
 *
 * The resulting REST call targets:
 *   /storage/v1/object/wardrobe/{userId}/originals/{imageId}.jpg
 */
class StorageService(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun upload(
        storagePath: String,
        bytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
    ) {
        require(!storagePath.startsWith("$BUCKET/")) {
            "storagePath must not contain bucket prefix: $storagePath"
        }
        onProgress(0f)
        supabaseClient.storage.from(BUCKET).upload(path = storagePath, data = bytes) {
            contentType = ContentType.parse(mimeType)
        }
        onProgress(1f)
    }
}
