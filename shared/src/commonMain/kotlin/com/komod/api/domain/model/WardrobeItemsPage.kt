package com.komod.api.domain.model

data class WardrobeItemsPage(
    val items: List<WardrobeItem>,
    val hasNextPage: Boolean,
    val totalCount: Int?,
)
