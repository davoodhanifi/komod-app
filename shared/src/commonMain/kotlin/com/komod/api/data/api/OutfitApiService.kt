package com.komod.api.data.api

import com.komod.api.data.api.model.OutfitGenerateRequest
import com.komod.api.data.api.model.OutfitGenerateResponse
import com.komod.api.data.api.model.ResponseData
import com.komod.api.data.api.model.WeatherContextDto
import com.komod.api.domain.model.WeatherCurrent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
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
                ),
            )
        }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            throw if (response.status.value in 400..499) {
                ClientRequestException(response, text)
            } else {
                ServerResponseException(response, text)
            }
        }
        return response.body<ResponseData<OutfitGenerateResponse>>().data
    }
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
