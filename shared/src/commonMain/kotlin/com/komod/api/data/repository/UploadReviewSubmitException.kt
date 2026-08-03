package com.komod.api.data.repository

class UploadReviewUnauthorizedException : RuntimeException()

class UploadReviewForbiddenException : RuntimeException()

class UploadReviewNotFoundException : RuntimeException()

class UploadReviewConflictException : RuntimeException()

class UploadReviewNetworkException(
    cause: Throwable,
) : RuntimeException(cause)
