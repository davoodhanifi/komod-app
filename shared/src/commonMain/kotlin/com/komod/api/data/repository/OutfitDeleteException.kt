package com.komod.api.data.repository

class OutfitDeleteNotFoundException : RuntimeException()

class OutfitDeleteBadRequestException : RuntimeException()

class OutfitDeleteNetworkException(
    cause: Throwable,
) : RuntimeException(cause)
