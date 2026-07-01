package com.komod.api.data.api

import com.komod.api.data.api.model.AnalyzeWardrobeRequest
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.api.model.ImageDto
import com.komod.api.data.api.model.ResponseData
import com.komod.api.data.api.model.WardrobeItemDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class WardrobeApiService(
    private val httpClient: HttpClient,
) {
    suspend fun createImage(): CreateImageResponse {
        return httpClient.post("images") {
            contentType(ContentType.Application.Json)
        }.body<ResponseData<CreateImageResponse>>().data
    }

    suspend fun analyzeWardrobeItems(imageId: String) {
        httpClient.post("wardrobe-items/analyze") {
            contentType(ContentType.Application.Json)
            setBody(AnalyzeWardrobeRequest(imageId = imageId))
        }
    }

    suspend fun getWardrobeItems(): List<WardrobeItemDto> {
        return httpClient.get("wardrobe-items")
            .body<ResponseData<List<WardrobeItemDto>>>().data
    }

    suspend fun getWardrobeItemById(id: String): WardrobeItemDto {
        return httpClient.get("wardrobe-items/$id")
            .body<ResponseData<WardrobeItemDto>>().data
    }

    suspend fun deleteWardrobeItem(id: String) {
        httpClient.delete("wardrobe-items/$id")
    }

    suspend fun getImage(imageId: String): ImageDto {
        return httpClient.get("images/$imageId")
            .body<ResponseData<ImageDto>>().data
    }
}
