package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class OutfitGenerateRequest(
    val occasion: String,
    val style: String,
    val weather: WeatherContextDto? = null,
)
