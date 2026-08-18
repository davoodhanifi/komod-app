package com.komod.api.data.api

import com.komod.api.core.error.PlanLimitCategory
import com.komod.api.core.error.PlanLimitExceededException
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
): OutfitApiService {
    val client = HttpClient(MockEngine { request -> handler(request) }) {
        install(ContentNegotiation) { json(testJson) }
        defaultRequest { url("https://test.local/api/v1/") }
    }
    return OutfitApiService(client)
}

private const val PlanLimitBody =
    """{"type":"about:blank","title":"Payment Required","status":402,"detail":"Daily limit reached.","code":"PlanLimitExceeded"}"""

class OutfitApiServiceTest {

    // 3. Manual outfit generation hitting the daily generation limit.
    @Test
    fun `generateOutfits surfaces a PlanLimitExceeded body as DailyGenerationLimit`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = PlanLimitBody,
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val exception = assertFailsWith<PlanLimitExceededException> {
            apiService.generateOutfits(occasion = "casual")
        }
        assertEquals(PlanLimitCategory.DailyGenerationLimit, exception.category)
    }

    // 5. A normal server error on this same endpoint must still behave as before.
    @Test
    fun `generateOutfits on an unrelated server error still throws ServerResponseException`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"type":"about:blank","title":"boom","status":500}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertFailsWith<ServerResponseException> {
            apiService.generateOutfits(occasion = "casual")
        }
        Unit
    }

    // Outfit of the Day shares the same daily-generation entitlement as manual generation.
    @Test
    fun `getOutfitOfTheDay surfaces a PlanLimitExceeded body as DailyGenerationLimit`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = PlanLimitBody,
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val exception = assertFailsWith<PlanLimitExceededException> {
            apiService.getOutfitOfTheDay(latitude = 40.0, longitude = -73.0)
        }
        assertEquals(PlanLimitCategory.DailyGenerationLimit, exception.category)
    }

    @Test
    fun `getOutfitOfTheDay on an unrelated server error still throws ServerResponseException`() = runBlocking {
        val apiService = buildApiService {
            respond(
                content = """{"type":"about:blank","title":"boom","status":500}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertFailsWith<ServerResponseException> {
            apiService.getOutfitOfTheDay(latitude = 40.0, longitude = -73.0)
        }
        Unit
    }
}
