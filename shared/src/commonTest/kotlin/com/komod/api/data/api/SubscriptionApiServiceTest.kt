package com.komod.api.data.api

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
}
