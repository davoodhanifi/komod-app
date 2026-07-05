package com.komod.api.presentation.home

import com.komod.api.domain.model.RecentItem
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

data class HomeUiState(
    val summaryState: WardrobeSummaryState = WardrobeSummaryState.Loading,
    val recentItemsState: RecentItemsState = RecentItemsState.Loading,
    val selectedOccasion: String = "Outdoor",
)
