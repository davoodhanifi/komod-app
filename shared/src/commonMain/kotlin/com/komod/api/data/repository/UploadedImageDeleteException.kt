package com.komod.api.data.repository

class UploadedImageDeleteNotFoundException : RuntimeException()

class UploadedImageDeleteForbiddenException : RuntimeException()

// 412 Precondition Failed — the image has already been analyzed, so it must be
// removed via its extracted wardrobe items instead.
class UploadedImageDeleteConflictException : RuntimeException()

class UploadedImageDeleteNetworkException(
    cause: Throwable,
) : RuntimeException(cause)
