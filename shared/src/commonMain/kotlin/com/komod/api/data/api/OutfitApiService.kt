package com.komod.api.data.api

import com.komod.api.data.api.model.OutfitGenerateRequest
import com.komod.api.data.api.model.OutfitGenerateResponse
import com.komod.api.data.api.model.ResponseData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class OutfitApiService(
    private val httpClient: HttpClient,
) {
    suspend fun generateOutfits(occasion: String, style: String): OutfitGenerateResponse {
        return httpClient.post("outfits/generate") {
            contentType(ContentType.Application.Json)
            setBody(
                OutfitGenerateRequest(
                    occasion = occasion,
                    style = style,
                ),
            )
        }.body<ResponseData<OutfitGenerateResponse>>().data
    }
}
