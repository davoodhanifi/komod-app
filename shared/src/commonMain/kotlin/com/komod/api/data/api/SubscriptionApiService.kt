package com.komod.api.data.api

import com.komod.api.data.api.model.CurrentSubscriptionDto
import com.komod.api.data.api.model.ResponseData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class SubscriptionApiService(
    private val httpClient: HttpClient,
) {
    // Same reasoning as the other GET endpoints in this file's siblings (WardrobeApiService,
    // OutfitApiService): the status is checked explicitly before parsing the body, so a
    // non-2xx response (a ProblemDetails-shaped body, not ResponseData<CurrentSubscriptionDto>)
    // surfaces as a proper ClientRequestException/ServerResponseException instead of an
    // unrelated SerializationException.
    suspend fun getCurrentSubscription(): CurrentSubscriptionDto {
        val response = httpClient.get("subscription/current")
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            throw if (response.status.value in 400..499) {
                ClientRequestException(response, text)
            } else {
                ServerResponseException(response, text)
            }
        }
        return response.body<ResponseData<CurrentSubscriptionDto>>().data
    }
}
