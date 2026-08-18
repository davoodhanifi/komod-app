package com.komod.api.data.repository

import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.model.ImageStatus
import com.komod.api.data.storage.StorageService
import com.komod.api.domain.model.UploadedImage
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

private val testJson = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

private fun buildApiService(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): WardrobeApiService {
    val client = HttpClient(MockEngine { request -> handler(request) }) {
        install(ContentNegotiation) { json(testJson) }
        defaultRequest { url("https://test.local/api/v1/") }
    }
    return WardrobeApiService(client)
}

// analyzeWardrobeItems/triggerAnalysisInBackground/refreshUploadedImages never touch
// storage — this just satisfies the constructor without any real Supabase project.
// Client construction alone performs no network I/O.
private fun testStorageService(): StorageService {
    val client = createSupabaseClient(supabaseUrl = "https://test.supabase.co", supabaseKey = "test-key") {
        install(Storage)
    }
    return StorageService(client)
}

class AddItemRepositoryImplTest {

    // triggerAnalysisInBackground fires on a background CoroutineScope independent of
    // the test dispatcher, so this waits for the store's real emission instead of
    // advancing virtual time (which wouldn't reach that scope).
    private suspend fun UploadedImageStore.awaitStatus(imageId: String, status: ImageStatus): UploadedImage =
        withTimeout(2_000) {
            uploadedImages.first { images -> images.any { it.imageId == imageId && it.status == status } }
                .first { it.imageId == imageId }
        }

    @Test
    fun `triggerAnalysisInBackground immediately reflects the queued Processing status`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"data":{"imageId":"img-1","status":"Processing"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val store = UploadedImageStore()
        store.add(
            UploadedImage(
                imageId = "img-1",
                storagePath = "originals/img-1.jpg",
                thumbnailStoragePath = "thumbs/img-1.jpg",
                status = ImageStatus.Pending,
            ),
        )
        val repository = AddItemRepositoryImpl(apiService, testStorageService(), store)

        repository.triggerAnalysisInBackground("img-1")

        assertEquals(ImageStatus.Processing, store.awaitStatus("img-1", ImageStatus.Processing).status)
    }

    @Test
    fun `412 from an already-queued analyze request is treated as Processing instead of a failure`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"type":"about:blank","title":"Conflict","status":412}""",
                status = HttpStatusCode.PreconditionFailed,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val store = UploadedImageStore()
        store.add(
            UploadedImage(
                imageId = "img-2",
                storagePath = "originals/img-2.jpg",
                thumbnailStoragePath = "thumbs/img-2.jpg",
                status = ImageStatus.Pending,
            ),
        )
        val repository = AddItemRepositoryImpl(apiService, testStorageService(), store)

        repository.triggerAnalysisInBackground("img-2")

        assertEquals(ImageStatus.Processing, store.awaitStatus("img-2", ImageStatus.Processing).status)
    }

    @Test
    fun `a genuine analyze failure leaves the image Pending for the next poll to recover`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"type":"about:blank","title":"boom","status":500}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val store = UploadedImageStore()
        store.add(
            UploadedImage(
                imageId = "img-3",
                storagePath = "originals/img-3.jpg",
                thumbnailStoragePath = "thumbs/img-3.jpg",
                status = ImageStatus.Pending,
            ),
        )
        val repository = AddItemRepositoryImpl(apiService, testStorageService(), store)

        repository.triggerAnalysisInBackground("img-3")

        // No transition ever happens on failure, so there's nothing to await — give the
        // background coroutine a moment to (not) run, then assert it stayed Pending.
        withTimeout(1_000) {
            var lastStatus = store.uploadedImages.value.first { it.imageId == "img-3" }.status
            repeat(5) {
                kotlinx.coroutines.delay(50)
                lastStatus = store.uploadedImages.value.first { it.imageId == "img-3" }.status
            }
            assertEquals(ImageStatus.Pending, lastStatus)
        }
    }

    // 2. Analysis capacity validation fails: the background analyze call is fire-and-forget,
    // so there's no synchronous error state to show — instead the image is marked Failed
    // (a status the Wardrobe screen's Upload Queue already renders) rather than left
    // Pending forever waiting for a poll that will keep hitting the same capacity limit.
    @Test
    fun `an analyze response reporting PlanLimitExceeded marks the image Failed`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"type":"about:blank","title":"Payment Required","status":402,"detail":"Your Komod is full or has items waiting for review.","code":"PlanLimitExceeded"}""",
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val store = UploadedImageStore()
        store.add(
            UploadedImage(
                imageId = "img-7",
                storagePath = "originals/img-7.jpg",
                thumbnailStoragePath = "thumbs/img-7.jpg",
                status = ImageStatus.Pending,
            ),
        )
        val repository = AddItemRepositoryImpl(apiService, testStorageService(), store)

        repository.triggerAnalysisInBackground("img-7")

        assertEquals(ImageStatus.Failed, store.awaitStatus("img-7", ImageStatus.Failed).status)
    }

    @Test
    fun `refreshUploadedImages upserts an Analyzed image together with its extracted items`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """
                    {"data":[{
                      "id":"img-4","uploadedAt":"2026-08-10T00:00:00Z","storagePath":"originals/img-4.jpg",
                      "thumbnailStoragePath":"thumbs/img-4.jpg","status":"Analyzed","itemCount":1,
                      "items":[{"id":"item-1","imageId":"img-4","status":0,"category":"top","confidence":0.9,"metadataJson":"{}","createdAt":"2026-08-10T00:00:00Z"}]
                    }]}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val store = UploadedImageStore()
        val repository = AddItemRepositoryImpl(apiService, testStorageService(), store)

        repository.refreshUploadedImages()

        val image = store.uploadedImages.value.first { it.imageId == "img-4" }
        assertEquals(ImageStatus.Analyzed, image.status)
        assertEquals(1, image.extractedItemCount)
    }

    @Test
    fun `refreshUploadedImages upserts a Processing image with zero items without error`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """
                    {"data":[{
                      "id":"img-5","uploadedAt":"2026-08-10T00:00:00Z","storagePath":"originals/img-5.jpg",
                      "thumbnailStoragePath":"thumbs/img-5.jpg","status":"Processing","itemCount":0,"items":[]
                    }]}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val store = UploadedImageStore()
        val repository = AddItemRepositoryImpl(apiService, testStorageService(), store)

        repository.refreshUploadedImages()

        val image = store.uploadedImages.value.first { it.imageId == "img-5" }
        assertEquals(ImageStatus.Processing, image.status)
        assertEquals(0, image.extractedItemCount)
    }

    @Test
    fun `a malformed 2xx analyze response does not silently disappear`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"unexpected":"shape"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val store = UploadedImageStore()
        store.add(
            UploadedImage(
                imageId = "img-6",
                storagePath = "originals/img-6.jpg",
                thumbnailStoragePath = "thumbs/img-6.jpg",
                status = ImageStatus.Pending,
            ),
        )
        val repository = AddItemRepositoryImpl(apiService, testStorageService(), store)

        repository.triggerAnalysisInBackground("img-6")

        // A parse failure on a successful HTTP response still promotes the image to
        // Processing (the backend did accept the request) rather than leaving it
        // stuck at Pending forever or crashing the background scope silently.
        assertEquals(ImageStatus.Processing, store.awaitStatus("img-6", ImageStatus.Processing).status)
    }
}
