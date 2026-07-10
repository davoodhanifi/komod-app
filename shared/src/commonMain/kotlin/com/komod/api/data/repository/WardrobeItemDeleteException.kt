package com.komod.api.data.repository

class WardrobeItemDeleteNotFoundException : RuntimeException()

class WardrobeItemDeleteBadRequestException : RuntimeException()

class WardrobeItemDeleteNetworkException(
    cause: Throwable,
) : RuntimeException(cause)
