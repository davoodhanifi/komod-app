package com.komod.api.domain.model

data class SavedOutfitsPage(
    val outfits: List<SavedOutfit>,
    val hasNextPage: Boolean,
    val totalCount: Int?,
)
