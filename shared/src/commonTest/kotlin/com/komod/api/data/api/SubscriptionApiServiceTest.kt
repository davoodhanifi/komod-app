package com.komod.api.data.api

import com.komod.api.platform.getDeviceTimeZoneId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
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
): SubscriptionApiService {
    val client = HttpClient(MockEngine { request -> handler(request) }) {
        install(ContentNegotiation) { json(testJson) }
        defaultRequest { url("https://test.local/api/v1/") }
    }
    return SubscriptionApiService(client)
}

class SubscriptionApiServiceTest {

    @Test
    fun `getCurrentSubscription decodes a finite-limit plan from the ResponseData envelope`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """
                    {"data":{
                      "plan":"Komod2Doors",
                      "wardrobeItemLimit":150,
                      "dailyOutfitGenerationLimit":30,
                      "currentWardrobeItemCount":87,
                      "todayOutfitGenerationCount":12
                    }}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val dto = apiService.getCurrentSubscription()

        assertEquals("Komod2Doors", dto.plan)
        assertEquals(150, dto.wardrobeItemLimit)
        assertEquals(30, dto.dailyOutfitGenerationLimit)
        assertEquals(87, dto.currentWardrobeItemCount)
        assertEquals(12, dto.todayOutfitGenerationCount)
    }

    // 3. Walk-in with null limits.
    @Test
    fun `getCurrentSubscription decodes null limits for an unlimited plan`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """
                    {"data":{
                      "plan":"KomodWalkIn",
                      "wardrobeItemLimit":null,
                      "dailyOutfitGenerationLimit":null,
                      "currentWardrobeItemCount":87,
                      "todayOutfitGenerationCount":12
                    }}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val dto = apiService.getCurrentSubscription()

        assertEquals("KomodWalkIn", dto.plan)
        assertEquals(null, dto.wardrobeItemLimit)
        assertEquals(null, dto.dailyOutfitGenerationLimit)
    }

    // The device timezone can vary by host, so this compares against the same
    // getDeviceTimeZoneId() the production code calls rather than a fixed literal — the
    // point under test is that whatever it returns reaches the query string unchanged
    // (and is simply absent when it returns null).
    @Test
    fun `getCurrentSubscription sends the device timezone as a query parameter`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val apiService = buildApiService { request ->
            capturedRequest = request
            respond(
                content = """
                    {"data":{
                      "plan":"Komod2Doors",
                      "wardrobeItemLimit":150,
                      "dailyOutfitGenerationLimit":30,
                      "currentWardrobeItemCount":87,
                      "todayOutfitGenerationCount":12
                    }}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        apiService.getCurrentSubscription()

        assertEquals(getDeviceTimeZoneId(), capturedRequest?.url?.parameters?.get("timeZoneId"))
    }

    // 6. API failure surfaces as a real exception — never a synthesized default DTO.
    @Test
    fun `getCurrentSubscription on a server error throws rather than returning a default`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"type":"about:blank","title":"boom","status":500}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertFailsWith<ServerResponseException> {
            apiService.getCurrentSubscription()
        }
        Unit
    }

    @Test
    fun `syncSubscription decodes the full ResponseData envelope including a cancelled-but-not-yet-expired plan`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """
                    {"data":{
                      "plan":"Komod3Doors",
                      "status":"Cancelled",
                      "expiresAt":"2026-12-01T00:00:00Z",
                      "willRenew":false,
                      "store":"APP_STORE",
                      "productId":"komod_3doors_monthly"
                    }}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val dto = apiService.syncSubscription()

        assertEquals("Komod3Doors", dto.plan)
        assertEquals("Cancelled", dto.status)
        assertEquals("2026-12-01T00:00:00Z", dto.expiresAt)
        assertEquals(false, dto.willRenew)
        assertEquals("APP_STORE", dto.store)
        assertEquals("komod_3doors_monthly", dto.productId)
    }

    // POST /subscription/sync takes no request body — the backend derives the user from the
    // auth token and reads RevenueCat itself.
    @Test
    fun `syncSubscription sends no request body`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val apiService = buildApiService { request ->
            capturedRequest = request
            respond(
                content = """
                    {"data":{"plan":"KomodRack","status":"Active","willRenew":false}}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        apiService.syncSubscription()

        assertEquals(0L, capturedRequest?.body?.contentLength ?: 0L)
    }

    @Test
    fun `syncSubscription on a server error throws rather than returning a default`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"type":"about:blank","title":"boom","status":500}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertFailsWith<ServerResponseException> {
            apiService.syncSubscription()
        }
        Unit
    }
}
