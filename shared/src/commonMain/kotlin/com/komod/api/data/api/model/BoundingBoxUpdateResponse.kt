package com.komod.api.data.api.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BoundingBoxUpdateResponse(
    @JsonNames("CroppedImageStoragePath")
    val croppedImageStoragePath: String,
    @JsonNames("CroppedImageId")
    val croppedImageId: String,
)
