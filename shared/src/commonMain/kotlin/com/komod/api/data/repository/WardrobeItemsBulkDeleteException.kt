package com.komod.api.data.repository

class WardrobeItemsBulkDeleteBadRequestException : RuntimeException()

class WardrobeItemsBulkDeleteNetworkException(
    cause: Throwable,
) : RuntimeException(cause)
