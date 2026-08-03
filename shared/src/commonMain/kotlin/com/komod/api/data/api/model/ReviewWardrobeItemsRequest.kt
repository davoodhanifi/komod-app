package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
enum class WardrobeItemReviewAction {
    Approve,
    Delete,
}

@Serializable
data class WardrobeItemReviewRequestItem(
    val id: String,
    val action: WardrobeItemReviewAction,
)

typealias ReviewWardrobeItemsRequest = List<WardrobeItemReviewRequestItem>
