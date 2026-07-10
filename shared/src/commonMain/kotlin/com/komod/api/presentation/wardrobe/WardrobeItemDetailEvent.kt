package com.komod.api.presentation.wardrobe

sealed interface WardrobeItemDetailEvent {
    data object DeleteClicked : WardrobeItemDetailEvent
    data object DeleteDismissed : WardrobeItemDetailEvent
    data object DeleteConfirmed : WardrobeItemDetailEvent
}
