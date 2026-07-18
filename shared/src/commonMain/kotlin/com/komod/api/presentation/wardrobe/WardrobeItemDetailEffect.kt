package com.komod.api.presentation.wardrobe

sealed interface WardrobeItemDetailEffect {
    data object FavoriteUpdated : WardrobeItemDetailEffect
    data class FavoriteUpdateFailed(val message: String) : WardrobeItemDetailEffect
    data object DeleteSucceeded : WardrobeItemDetailEffect
    data object ItemMissing : WardrobeItemDetailEffect
    data class DeleteFailed(val message: String) : WardrobeItemDetailEffect
}
