package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

/**
 * POST /wardrobe-items/analyze queues background analysis and returns immediately —
 * this is the queued status, not the extracted wardrobe items. Poll GET /images/uploaded
 * (keyed by [imageId]) for the eventual [ImageStatus.Analyzed]/[ImageStatus.Failed] outcome.
 */
@Serializable
data class AnalyzeWardrobeImageResultDto(
    val imageId: String,
    val status: ImageStatus,
)
