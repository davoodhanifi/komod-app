package com.komod.api.data.api

import com.komod.api.data.api.model.AnalyzeWardrobeRequest
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.api.model.ImageDto
import com.komod.api.data.api.model.RecentItemDto
import com.komod.api.data.api.model.ResponseData
import com.komod.api.data.api.model.ReviewWardrobeItemsRequest
import com.komod.api.data.api.model.UploadedImageDto
import com.komod.api.data.api.model.WardrobeFavoriteRequest
import com.komod.api.data.api.model.WardrobeItemDto
import com.komod.api.data.api.model.WardrobeItemUpdateRequest
import com.komod.api.data.api.model.WardrobeSummaryDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

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

    suspend fun updateWardrobeItem(
        id: String,
        request: WardrobeItemUpdateRequest,
    ) {
        httpClient.patch("wardrobe-items/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun uploadWardrobeItemCroppedImage(
        id: String,
        imageBytes: ByteArray,
    ) {
        // No caller needs the response body — the item is refetched separately to pick
        // up the new cropped image — and its shape isn't guaranteed to stay in sync with
        // the backend, so only the HTTP status is checked. Parsing an unused body here
        // was turning successful saves into a user-facing "something went wrong" error
        // whenever that shape drifted.
        val response = httpClient.patch("wardrobe-items/$id/cropped-image") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "image",
                            value = imageBytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"crop.jpg\"")
                            },
                        )
                    },
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
    }

    suspend fun setWardrobeItemFavorite(
        id: String,
        favorite: Boolean,
    ) {
        val requestBody = WardrobeFavoriteRequest(favorite = favorite)

        val postResponse = httpClient.post("wardrobe-items/$id/favorite") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        if (postResponse.status.isSuccess()) return

        val patchResponse = httpClient.patch("wardrobe-items/$id/favorite") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        if (patchResponse.status.isSuccess()) return

        val absolutePostResponse = httpClient.post("/v1/wardrobe-items/$id/favorite") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        if (absolutePostResponse.status.isSuccess()) return

        val absolutePatchResponse = httpClient.patch("/v1/wardrobe-items/$id/favorite") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        if (absolutePatchResponse.status.isSuccess()) return

        error(
            "Failed to update favorite. Statuses: post=${postResponse.status}, patch=${patchResponse.status}, " +
                "absolutePost=${absolutePostResponse.status}, absolutePatch=${absolutePatchResponse.status}"
        )
    }

    suspend fun getImage(imageId: String): ImageDto {
        return httpClient.get("images/$imageId")
            .body<ResponseData<ImageDto>>().data
    }

    suspend fun getUploadedImages(): List<UploadedImageDto> {
        return httpClient.get("images/uploaded")
            .body<ResponseData<List<UploadedImageDto>>>().data
    }

    suspend fun reviewWardrobeItems(request: ReviewWardrobeItemsRequest) {
        httpClient.patch("wardrobe-items/review") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getWardrobeSummary(): WardrobeSummaryDto {
        return httpClient.get("wardrobe-items/summary")
            .body<ResponseData<WardrobeSummaryDto>>().data
    }

    suspend fun getRecentItems(): List<RecentItemDto> {
        return httpClient.get("wardrobe-items/recent")
            .body<ResponseData<List<RecentItemDto>>>().data
    }
}
