package com.komod.api.data.api

import com.komod.api.core.error.PlanLimitCategory
import com.komod.api.core.error.PlanLimitExceededException
import com.komod.api.data.api.model.ReviewWardrobeItemsRequest
import com.komod.api.data.api.model.WardrobeItemReviewAction
import com.komod.api.data.api.model.WardrobeItemReviewRequestItem
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

private const val PlanLimitBody =
    """{"type":"about:blank","title":"Payment Required","status":402,"detail":"Your Komod is full.","code":"PlanLimitExceeded"}"""

class WardrobeApiServiceTest {

    // 1. CreateImage / upload capacity full.
    @Test
    fun `createImage surfaces a PlanLimitExceeded body as PendingCapacity`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = PlanLimitBody,
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val exception = assertFailsWith<PlanLimitExceededException> {
            apiService.createImage()
        }
        assertEquals(PlanLimitCategory.PendingCapacity, exception.category)
    }

    // 5. Existing generic errors (e.g. a plain server error) must still map as before —
    // i.e. still surface as the normal ResponseException subtypes, not as PlanLimitExceeded.
    @Test
    fun `createImage on an unrelated server error still throws ServerResponseException`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"type":"about:blank","title":"boom","status":500}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertFailsWith<ServerResponseException> {
            apiService.createImage()
        }
        Unit
    }

    @Test
    fun `createImage on success still decodes the created image normally`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"data":{"imageId":"img-1","storagePath":"originals/img-1.jpg","thumbnailStoragePath":"thumbs/img-1.jpg"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = apiService.createImage()

        assertEquals("img-1", result.imageId)
    }

    // 1. Approving wardrobe items when the wardrobe is full.
    @Test
    fun `reviewWardrobeItems surfaces a PlanLimitExceeded body as WardrobeCapacity`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = PlanLimitBody,
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val request: ReviewWardrobeItemsRequest =
            listOf(WardrobeItemReviewRequestItem(id = "item-1", action = WardrobeItemReviewAction.Approve))

        val exception = assertFailsWith<PlanLimitExceededException> {
            apiService.reviewWardrobeItems(request)
        }
        assertEquals(PlanLimitCategory.WardrobeCapacity, exception.category)
    }

    // 5. A normal 404 on this same endpoint must still behave exactly as before.
    @Test
    fun `reviewWardrobeItems on a 404 still throws ClientRequestException`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"type":"about:blank","title":"Not Found","status":404}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val request: ReviewWardrobeItemsRequest =
            listOf(WardrobeItemReviewRequestItem(id = "item-1", action = WardrobeItemReviewAction.Approve))

        val exception = assertFailsWith<ClientRequestException> {
            apiService.reviewWardrobeItems(request)
        }
        assertEquals(HttpStatusCode.NotFound, exception.response.status)
    }

    @Test
    fun `reviewWardrobeItems on success does not throw`() = runBlocking {
        val apiService = buildApiService {
            respond(content = "", status = HttpStatusCode.NoContent)
        }
        val request: ReviewWardrobeItemsRequest =
            listOf(WardrobeItemReviewRequestItem(id = "item-1", action = WardrobeItemReviewAction.Approve))

        apiService.reviewWardrobeItems(request)
    }

    // 2. Analysis capacity validation fails (too many pending/processing photos).
    @Test
    fun `analyzeWardrobeItems surfaces a PlanLimitExceeded body as PendingCapacity`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = PlanLimitBody,
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val exception = assertFailsWith<PlanLimitExceededException> {
            apiService.analyzeWardrobeItems("img-1")
        }
        assertEquals(PlanLimitCategory.PendingCapacity, exception.category)
    }
}
