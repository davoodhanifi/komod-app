package com.komod.api.data.api

import com.komod.api.core.error.PlanLimitCategory
import com.komod.api.core.error.PlanLimitExceededException
import com.komod.api.data.api.model.OutfitDto
import com.komod.api.data.api.model.OutfitGenerateRequest
import com.komod.api.data.api.model.OutfitGenerateResponse
import com.komod.api.data.api.model.OutfitOfTheDayResponseDto
import com.komod.api.data.api.model.ResponseData
import com.komod.api.data.api.model.SaveOutfitRequest
import com.komod.api.data.api.model.SaveOutfitResponse
import com.komod.api.data.api.model.SelectedOutfitItemsDto
import com.komod.api.data.api.model.WeatherContextDto
import com.komod.api.domain.model.WeatherCurrent
import com.komod.api.platform.getDeviceTimeZoneId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class OutfitApiService(
    private val httpClient: HttpClient,
) {
    suspend fun generateOutfits(
        occasion: String,
        style: String? = null,
        weather: WeatherCurrent? = null,
        selectedTopId: String? = null,
        selectedBottomId: String? = null,
        selectedShoesId: String? = null,
    ): OutfitGenerateResponse {
        // On success this is wrapped in ResponseData like the GET endpoints. On failure
        // it's a bare ProblemDetails payload with no "data" field, so the status is
        // checked explicitly first rather than letting a blind `.body()` call fail with
        // a confusing deserialization error instead of the real HTTP failure.
        val response = httpClient.post("outfits/generate") {
            contentType(ContentType.Application.Json)
            setBody(
                OutfitGenerateRequest(
                    occasion = occasion,
                    style = style,
                    weather = weather?.toWeatherContext(),
                    // Null (same as `style` above) when the device timezone can't be
                    // determined, so the backend falls back to UTC per its own default.
                    timeZoneId = getDeviceTimeZoneId(),
                    // Omitted entirely (not just all-null fields) when nothing was picked,
                    // matching the backend's "no constraints" contract.
                    selectedItems = selectedItemsDtoOrNull(selectedTopId, selectedBottomId, selectedShoesId),
                ),
            )
        }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            if (isPlanLimitExceeded(text)) {
                throw PlanLimitExceededException(PlanLimitCategory.DailyGenerationLimit)
            }
            throw if (response.status.value in 400..499) {
                ClientRequestException(response, text)
            } else {
                ServerResponseException(response, text)
            }
        }
        return response.body<ResponseData<OutfitGenerateResponse>>().data
    }

    // latitude/longitude are required on every call, even on a cache hit: the backend has
    // no stored location for the user and needs coordinates to resolve the local timezone
    // that determines the current 6-hour window.
    suspend fun getOutfitOfTheDay(latitude: Double, longitude: Double): OutfitOfTheDayResponseDto {
        val response = httpClient.get("outfits/today") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
        }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            if (isPlanLimitExceeded(text)) {
                // /outfits/today can trigger a fresh generation server-side when there's no
                // cached Outfit of the Day yet, drawing on the same daily generation
                // entitlement as POST /outfits/generate.
                throw PlanLimitExceededException(PlanLimitCategory.DailyGenerationLimit)
            }
            throw if (response.status.value in 400..499) {
                ClientRequestException(response, text)
            } else {
                ServerResponseException(response, text)
            }
        }
        return response.body<ResponseData<OutfitOfTheDayResponseDto>>().data
    }

    suspend fun saveOutfit(request: SaveOutfitRequest): String {
        val response = httpClient.post("outfits") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            throw if (response.status.value in 400..499) {
                ClientRequestException(response, text)
            } else {
                ServerResponseException(response, text)
            }
        }
        return response.body<ResponseData<SaveOutfitResponse>>().data.id
    }

    suspend fun deleteOutfit(id: String) {
        httpClient.delete("outfits/$id")
    }

    suspend fun getOutfits(): List<OutfitDto> {
        return httpClient.get("outfits")
            .body<ResponseData<List<OutfitDto>>>().data
    }
}

// Extracted so the "when do we send an object vs. omit it entirely" rule is unit-testable
// on its own (see OutfitApiServiceTest) — the backend contract treats a fully-absent
// selectedItems as "no constraints", not as an object of three nulls.
internal fun selectedItemsDtoOrNull(
    topId: String?,
    bottomId: String?,
    shoesId: String?,
): SelectedOutfitItemsDto? {
    if (topId == null && bottomId == null && shoesId == null) return null
    return SelectedOutfitItemsDto(top = topId, bottom = bottomId, shoes = shoesId)
}

private fun WeatherCurrent.toWeatherContext(): WeatherContextDto {
    return WeatherContextDto(
        temperatureC = temperatureC,
        feelsLikeC = feelsLikeC,
        condition = condition,
        windSpeedKmh = windSpeedKmh,
        isRaining = isRaining,
        isSnowing = isSnowing,
    )
}
