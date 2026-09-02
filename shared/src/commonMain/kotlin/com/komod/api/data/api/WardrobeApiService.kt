package com.komod.api.data.api

import com.komod.api.core.error.PlanLimitCategory
import com.komod.api.core.error.PlanLimitExceededException
import com.komod.api.data.api.model.AnalyzeWardrobeImageResultDto
import com.komod.api.data.api.model.AnalyzeWardrobeRequest
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.api.model.DeleteWardrobeItemsRequest
import com.komod.api.data.api.model.ImageDto
import com.komod.api.data.api.model.PaginatedResponseData
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
import io.ktor.client.request.parameter
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
    // Status is checked explicitly before parsing the body, same reasoning as
    // analyzeWardrobeItems below: an error response (e.g. capacity exceeded) has a
    // ProblemDetails-shaped body, not a ResponseData<CreateImageResponse> one, and a blind
    // .body<T>() call would throw an unrelated SerializationException instead of the real
    // HTTP failure — which would also hide the "code" field this needs to detect a
    // PlanLimitExceeded response.
    suspend fun createImage(): CreateImageResponse {
        val response = httpClient.post("images") {
            contentType(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            if (isPlanLimitExceeded(text)) {
                throw PlanLimitExceededException(PlanLimitCategory.PendingCapacity)
            }
            throw if (response.status.value in 400..499) {
                ClientRequestException(response, text)
            } else {
                ServerResponseException(response, text)
            }
        }
        return response.body<ResponseData<CreateImageResponse>>().data
    }

    // Queues background analysis and returns immediately with the queued status — the
    // wardrobe items themselves are extracted later and only show up via
    // getUploadedImages(). Callers must not treat this response as containing item IDs.
    //
    // The status is checked explicitly *before* attempting to parse the body as the
    // success envelope: an error response (e.g. 412 for an already-queued image) has a
    // ProblemDetails-shaped body, not a ResponseData<AnalyzeWardrobeImageResultDto> one,
    // and letting .body<T>() run against it first throws an unrelated
    // SerializationException instead of the expected ClientRequestException/
    // ServerResponseException — silently miscategorizing a normal HTTP error as a parse
    // failure.
    suspend fun analyzeWardrobeItems(imageId: String): AnalyzeWardrobeImageResultDto {
        val response = httpClient.post("wardrobe-items/analyze") {
            contentType(ContentType.Application.Json)
            setBody(AnalyzeWardrobeRequest(imageId = imageId))
        }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            if (isPlanLimitExceeded(text)) {
                throw PlanLimitExceededException(PlanLimitCategory.PendingCapacity)
            }
            throw if (response.status.value in 400..499) {
                ClientRequestException(response, text)
            } else {
                ServerResponseException(response, text)
            }
        }
        return response.body<ResponseData<AnalyzeWardrobeImageResultDto>>().data
    }

    // pageNumber/pageSize are opt-in and must be passed together — omitting both keeps
    // the pre-pagination behavior of returning the full wardrobe in one response.
    suspend fun getWardrobeItems(
        pageNumber: Int? = null,
        pageSize: Int? = null,
    ): PaginatedResponseData<List<WardrobeItemDto>> {
        return httpClient.get("wardrobe-items") {
            pageNumber?.let { parameter("pageNumber", it) }
            pageSize?.let { parameter("pageSize", it) }
        }.body<PaginatedResponseData<List<WardrobeItemDto>>>()
    }

    // Same reasoning as analyzeWardrobeItems: the backend returns 404 once an item's
    // status moves away from Active, and that error body isn't shaped like
    // ResponseData<WardrobeItemDto> — checking the status first keeps that a
    // ClientRequestException callers can catch, instead of an unrelated
    // SerializationException.
    suspend fun getWardrobeItemById(id: String): WardrobeItemDto {
        val response = httpClient.get("wardrobe-items/$id")
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            throw if (response.status.value in 400..499) {
                ClientRequestException(response, text)
            } else {
                ServerResponseException(response, text)
            }
        }
        return response.body<ResponseData<WardrobeItemDto>>().data
    }

    suspend fun deleteWardrobeItem(id: String) {
        httpClient.delete("wardrobe-items/$id")
    }

    suspend fun deleteWardrobeItems(request: DeleteWardrobeItemsRequest) {
        httpClient.delete("wardrobe-items") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteUploadedImage(imageId: String) {
        httpClient.delete("images/$imageId")
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

    // Same reasoning as analyzeWardrobeItems: check the status before parsing the body,
    // so a transient server error during polling surfaces as a proper ResponseException
    // rather than an unrelated SerializationException.
    suspend fun getUploadedImages(): List<UploadedImageDto> {
        val response = httpClient.get("images/uploaded")
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            throw if (response.status.value in 400..499) {
                ClientRequestException(response, text)
            } else {
                ServerResponseException(response, text)
            }
        }
        return response.body<ResponseData<List<UploadedImageDto>>>().data
    }

    // Same reasoning as createImage/analyzeWardrobeItems: the status is checked explicitly
    // so a PlanLimitExceeded body (returned when approving would exceed the wardrobe's
    // capacity) can be detected before any implicit body-parsing miscategorizes it.
    suspend fun reviewWardrobeItems(request: ReviewWardrobeItemsRequest) {
        val response = httpClient.patch("wardrobe-items/review") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            if (isPlanLimitExceeded(text)) {
                throw PlanLimitExceededException(PlanLimitCategory.WardrobeCapacity)
            }
            throw if (response.status.value in 400..499) {
                ClientRequestException(response, text)
            } else {
                ServerResponseException(response, text)
            }
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
