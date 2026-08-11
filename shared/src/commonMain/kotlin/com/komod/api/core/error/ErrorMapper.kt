package com.komod.api.core.error

import com.komod.api.data.repository.OutfitDeleteBadRequestException
import com.komod.api.data.repository.OutfitDeleteNetworkException
import com.komod.api.data.repository.OutfitDeleteNotFoundException
import com.komod.api.data.repository.UploadReviewConflictException
import com.komod.api.data.repository.UploadReviewForbiddenException
import com.komod.api.data.repository.UploadReviewNetworkException
import com.komod.api.data.repository.UploadReviewNotFoundException
import com.komod.api.data.repository.UploadReviewUnauthorizedException
import com.komod.api.data.repository.UploadedImageDeleteConflictException
import com.komod.api.data.repository.UploadedImageDeleteForbiddenException
import com.komod.api.data.repository.UploadedImageDeleteNetworkException
import com.komod.api.data.repository.UploadedImageDeleteNotFoundException
import com.komod.api.data.repository.UploadedImageNotFoundException
import com.komod.api.data.repository.UploadedItemNotFoundException
import com.komod.api.data.repository.WardrobeItemsBulkDeleteBadRequestException
import com.komod.api.data.repository.WardrobeItemsBulkDeleteNetworkException
import com.komod.api.data.repository.WardrobeItemDeleteBadRequestException
import com.komod.api.data.repository.WardrobeItemDeleteNetworkException
import com.komod.api.data.repository.WardrobeItemDeleteNotFoundException
import com.komod.api.data.repository.WardrobeItemUpdateBadRequestException
import com.komod.api.data.repository.WardrobeItemUpdateConflictException
import com.komod.api.data.repository.WardrobeItemUpdateNetworkException
import com.komod.api.data.repository.WardrobeItemUpdateNotFoundException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException

/**
 * Single place that turns any [Throwable] raised by a repository, API service, or
 * platform call into a short, user-facing message. The returned string is always one
 * of the fixed messages in [UserFacingMessages]/[ErrorContext] or a plain, hand-written
 * sentence below — never anything derived from the throwable itself (message, class
 * name, HTTP status code, response body).
 *
 * Every call also logs the original throwable via [AppLogger] first, so the technical
 * detail needed for debugging is never lost, even though it never reaches the UI.
 *
 * ViewModels and repositories should use this instead of reading `throwable.message`
 * or building their own error strings.
 */
object ErrorMapper {

    fun toUserMessage(
        throwable: Throwable,
        tag: String,
        context: ErrorContext = ErrorContext.Generic,
    ): String {
        AppLogger.e(tag, "Operation failed (${throwable::class.simpleName})", throwable)
        return mapToMessage(throwable, context)
    }

    private fun mapToMessage(throwable: Throwable, context: ErrorContext): String = when (throwable) {
        is UploadReviewUnauthorizedException -> UserFacingMessages.SessionExpired
        is UploadReviewForbiddenException -> "You don't have permission to do this."
        is UploadReviewNotFoundException,
        is UploadedImageNotFoundException -> "This upload could not be found."
        is UploadedItemNotFoundException -> "This item could not be found."
        is UploadReviewConflictException -> "This upload was already reviewed."
        is UploadReviewNetworkException -> mapCauseOrFallback(throwable, context)

        is WardrobeItemDeleteNotFoundException,
        is WardrobeItemUpdateNotFoundException -> "This item no longer exists."
        is WardrobeItemDeleteBadRequestException,
        is WardrobeItemUpdateBadRequestException -> "Couldn't save your changes. Please try again."
        is WardrobeItemUpdateConflictException -> "This item was updated elsewhere. Please go back and try again."
        is WardrobeItemDeleteNetworkException -> mapCauseOrFallback(throwable, context)
        is WardrobeItemUpdateNetworkException -> mapCauseOrFallback(throwable, context)

        is OutfitDeleteNotFoundException -> "This outfit no longer exists."
        is OutfitDeleteBadRequestException -> "Couldn't update this outfit. Please try again."
        is OutfitDeleteNetworkException -> mapCauseOrFallback(throwable, context)

        is UploadedImageDeleteNotFoundException -> "This upload could not be found."
        is UploadedImageDeleteForbiddenException -> "You don't have permission to do this."
        is UploadedImageDeleteConflictException -> "This item has already been processed and can't be removed this way."
        is UploadedImageDeleteNetworkException -> mapCauseOrFallback(throwable, context)

        is WardrobeItemsBulkDeleteBadRequestException -> "Couldn't delete these items. Please try again."
        is WardrobeItemsBulkDeleteNetworkException -> mapCauseOrFallback(throwable, context)

        is ClientRequestException ->
            if (throwable.response.status == HttpStatusCode.Unauthorized) {
                UserFacingMessages.SessionExpired
            } else {
                context.fallbackMessage
            }

        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException -> UserFacingMessages.Timeout

        is ResponseException -> context.fallbackMessage

        is IOException -> UserFacingMessages.NoInternet

        else -> context.fallbackMessage
    }

    // *NetworkException wrappers always carry the original cause (Ktor/IO exception),
    // so recurse into it to preserve the more specific network/timeout messaging.
    private fun mapCauseOrFallback(throwable: Throwable, context: ErrorContext): String {
        val cause = throwable.cause
        return if (cause != null && cause !== throwable) mapToMessage(cause, context) else context.fallbackMessage
    }
}
