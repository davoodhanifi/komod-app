package com.komod.api.presentation.wardrobe

sealed interface WardrobeItemDetailEffect {
    data object DeleteSucceeded : WardrobeItemDetailEffect
    data object ItemMissing : WardrobeItemDetailEffect
    data class DeleteFailed(val message: String) : WardrobeItemDetailEffect
}
