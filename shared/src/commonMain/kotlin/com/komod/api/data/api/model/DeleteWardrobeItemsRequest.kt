package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class DeleteWardrobeItemsRequest(
    val ids: List<String>,
)
