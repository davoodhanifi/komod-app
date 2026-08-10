package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

// Serialized by name (kotlinx.serialization's default enum handling), matching the
// backend's string wire format (e.g. "Processing") rather than an ordinal int — the
// backend's ImageStatus enum serializes as its name, not a numeric code.
@Serializable
enum class ImageStatus {
    Pending,
    Processing,
    Analyzed,
    Failed,
}
