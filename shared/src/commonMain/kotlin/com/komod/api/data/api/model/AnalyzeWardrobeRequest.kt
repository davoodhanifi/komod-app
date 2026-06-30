package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzeWardrobeRequest(
    val imageId: String,
)
