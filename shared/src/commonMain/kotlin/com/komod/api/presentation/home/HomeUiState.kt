package com.komod.api.presentation.home

import com.komod.api.domain.model.RecentItem
import com.komod.api.domain.model.SavedOutfit
import com.komod.api.domain.model.WardrobeSummary

sealed interface WardrobeSummaryState {
    data object Loading : WardrobeSummaryState
    data class Success(val summary: WardrobeSummary) : WardrobeSummaryState
    data class Error(val message: String) : WardrobeSummaryState
}

sealed interface RecentItemsState {
    data object Loading : RecentItemsState
    data class Success(val items: List<RecentItem>) : RecentItemsState
    data class Error(val message: String) : RecentItemsState
}

sealed interface SavedOutfitsState {
    data object Loading : SavedOutfitsState
    data class Success(val outfits: List<SavedOutfit>) : SavedOutfitsState
    // No message: per spec, a failed load hides the section entirely rather than
    // surfacing a backend error, so there's nothing for the UI to display.
    data object Error : SavedOutfitsState
}

data class HomeUiState(
    val summaryState: WardrobeSummaryState = WardrobeSummaryState.Loading,
    val recentItemsState: RecentItemsState = RecentItemsState.Loading,
    val savedOutfitsState: SavedOutfitsState = SavedOutfitsState.Loading,
    val selectedOccasion: String? = "Outdoor",
    // Null means "not determined yet / not shown" (e.g. wardrobe summary itself hasn't
    // resolved, or generation failed) — distinct from OutfitOfTheDayState.Loading, which
    // renders a skeleton because we're actively generating today's outfit.
    val outfitOfTheDayState: OutfitOfTheDayState? = null,
)
