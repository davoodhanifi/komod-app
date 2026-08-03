package com.komod.api.data.api.model

enum class ImageStatus {
    Pending,
    Processing,
    Analyzed,
    Failed,
}

fun Int.toImageStatus(): ImageStatus = ImageStatus.entries.getOrNull(this) ?: ImageStatus.Pending
