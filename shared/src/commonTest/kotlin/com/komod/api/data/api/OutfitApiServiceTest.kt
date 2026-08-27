package com.komod.api.data.api

import com.komod.api.core.error.PlanLimitCategory
import com.komod.api.core.error.PlanLimitExceededException
import com.komod.api.data.api.model.OutfitGenerateRequest
import com.komod.api.data.api.model.SelectedOutfitItemsDto
import com.komod.api.platform.getDeviceTimeZoneId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
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
import kotlin.test.assertTrue

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

    // The device timezone can vary by host, so this compares against the same
    // getDeviceTimeZoneId() the production code calls rather than a fixed literal — the
    // point under test is that whatever it returns reaches the request body unchanged.
    @Test
    fun `generateOutfits sends the device timezone in the request body`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val apiService = buildApiService { request ->
            capturedRequest = request
            respond(
                content = """{"data":{"outfits":[]}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        apiService.generateOutfits(occasion = "casual")

        val sentBody = testJson.decodeFromString<OutfitGenerateRequest>(
            capturedRequest!!.body.toByteArray().decodeToString(),
        )
        assertEquals(getDeviceTimeZoneId(), sentBody.timeZoneId)
    }

    private suspend fun captureSelectedItems(
        selectedTopId: String? = null,
        selectedBottomId: String? = null,
        selectedShoesId: String? = null,
    ): SelectedOutfitItemsDto? {
        var capturedRequest: HttpRequestData? = null
        val apiService = buildApiService { request ->
            capturedRequest = request
            respond(
                content = """{"data":{"outfits":[]}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        apiService.generateOutfits(
            occasion = "casual",
            selectedTopId = selectedTopId,
            selectedBottomId = selectedBottomId,
            selectedShoesId = selectedShoesId,
        )

        return testJson.decodeFromString<OutfitGenerateRequest>(
            capturedRequest!!.body.toByteArray().decodeToString(),
        ).selectedItems
    }

    // Existing generation without selected items must keep behaving exactly as before —
    // the backend treats a fully-absent selectedItems as "no constraints", not an object
    // of three nulls, so it must be omitted entirely rather than sent as {}.
    @Test
    fun `generateOutfits with no selected items omits selectedItems entirely`() = runBlocking {
        assertEquals(null, captureSelectedItems())
    }

    @Test
    fun `generateOutfits with only a top selected sends just the top id`() = runBlocking {
        assertEquals(
            SelectedOutfitItemsDto(top = "TOP_UUID", bottom = null, shoes = null),
            captureSelectedItems(selectedTopId = "TOP_UUID"),
        )
    }

    @Test
    fun `generateOutfits with only a bottom selected sends just the bottom id`() = runBlocking {
        assertEquals(
            SelectedOutfitItemsDto(top = null, bottom = "BOTTOM_UUID", shoes = null),
            captureSelectedItems(selectedBottomId = "BOTTOM_UUID"),
        )
    }

    @Test
    fun `generateOutfits with only shoes selected sends just the shoes id`() = runBlocking {
        assertEquals(
            SelectedOutfitItemsDto(top = null, bottom = null, shoes = "SHOES_UUID"),
            captureSelectedItems(selectedShoesId = "SHOES_UUID"),
        )
    }

    @Test
    fun `generateOutfits with top and bottom selected sends both ids`() = runBlocking {
        assertEquals(
            SelectedOutfitItemsDto(top = "TOP_UUID", bottom = "BOTTOM_UUID", shoes = null),
            captureSelectedItems(selectedTopId = "TOP_UUID", selectedBottomId = "BOTTOM_UUID"),
        )
    }

    @Test
    fun `generateOutfits with top and shoes selected sends both ids`() = runBlocking {
        assertEquals(
            SelectedOutfitItemsDto(top = "TOP_UUID", bottom = null, shoes = "SHOES_UUID"),
            captureSelectedItems(selectedTopId = "TOP_UUID", selectedShoesId = "SHOES_UUID"),
        )
    }

    @Test
    fun `generateOutfits with bottom and shoes selected sends both ids`() = runBlocking {
        assertEquals(
            SelectedOutfitItemsDto(top = null, bottom = "BOTTOM_UUID", shoes = "SHOES_UUID"),
            captureSelectedItems(selectedBottomId = "BOTTOM_UUID", selectedShoesId = "SHOES_UUID"),
        )
    }

    @Test
    fun `generateOutfits with top, bottom, and shoes selected sends all three ids`() = runBlocking {
        assertEquals(
            SelectedOutfitItemsDto(top = "TOP_UUID", bottom = "BOTTOM_UUID", shoes = "SHOES_UUID"),
            captureSelectedItems(
                selectedTopId = "TOP_UUID",
                selectedBottomId = "BOTTOM_UUID",
                selectedShoesId = "SHOES_UUID",
            ),
        )
    }

    // Confirms the wire format matches the backend contract literally — not just that
    // decoding round-trips, but that the actual field names on the wire are
    // "selectedItems"/"top"/"bottom"/"shoes", not renamed by serialization config.
    @Test
    fun `generateOutfits serializes selectedItems using the exact backend field names`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val apiService = buildApiService { request ->
            capturedRequest = request
            respond(
                content = """{"data":{"outfits":[]}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        apiService.generateOutfits(
            occasion = "casual",
            selectedTopId = "TOP_UUID",
            selectedBottomId = "BOTTOM_UUID",
            selectedShoesId = "SHOES_UUID",
        )

        val rawJson = capturedRequest!!.body.toByteArray().decodeToString()
        assertTrue(rawJson.contains("\"selectedItems\""))
        assertTrue(rawJson.contains("\"top\":\"TOP_UUID\""))
        assertTrue(rawJson.contains("\"bottom\":\"BOTTOM_UUID\""))
        assertTrue(rawJson.contains("\"shoes\":\"SHOES_UUID\""))
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

    // Outfit of the Day derives its timezone from the user's location server-side, so
    // unlike generateOutfits() it must never send a client-supplied timezone.
    @Test
    fun `getOutfitOfTheDay does not send a timezone query parameter`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val apiService = buildApiService { request ->
            capturedRequest = request
            respond(
                content = """
                    {"data":{
                      "outfits":[],
                      "weather":{
                        "temperatureC":20.0,
                        "feelsLikeC":19.0,
                        "condition":"Clear",
                        "windSpeedKmh":5.0,
                        "isRaining":false,
                        "isSnowing":false,
                        "next6HourMinTemperatureC":18.0,
                        "next6HourMaxTemperatureC":22.0
                      }
                    }}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        apiService.getOutfitOfTheDay(latitude = 40.0, longitude = -73.0)

        assertEquals(null, capturedRequest?.url?.parameters?.get("timeZoneId"))
    }
}
