package com.komod.api.data.api.model

import com.komod.api.domain.model.BoundingBox
import kotlinx.serialization.Serializable

@Serializable
data class BoundingBoxDto(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

fun BoundingBoxDto.toDomain(): BoundingBox = BoundingBox(
    x = x.toFloat(),
    y = y.toFloat(),
    width = width.toFloat(),
    height = height.toFloat(),
)

fun BoundingBox.toDto(): BoundingBoxDto = BoundingBoxDto(
    x = x.toDouble(),
    y = y.toDouble(),
    width = width.toDouble(),
    height = height.toDouble(),
)
