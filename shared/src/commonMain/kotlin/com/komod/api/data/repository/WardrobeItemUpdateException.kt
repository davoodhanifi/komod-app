package com.komod.api.data.repository

class WardrobeItemUpdateNotFoundException : RuntimeException()

class WardrobeItemUpdateBadRequestException : RuntimeException()

class WardrobeItemUpdateNetworkException(
    cause: Throwable,
) : RuntimeException(cause)
