package com.komod.api.presentation.outfits

import com.komod.api.domain.model.WardrobeItem
import kotlin.test.Test
import kotlin.test.assertEquals

// selectedItemIds() is extracted out of OutfitUiState specifically so this state
// transition — clearing a slot drops its id from what would be sent to the backend — is
// unit-testable without constructing OutfitViewModel (see OutfitViewModelPlanLimitTest
// for why that's not possible in a JVM test target).
class OutfitUiStateSelectedItemsTest {

    private fun wardrobeItem(id: String) = WardrobeItem(
        id = id,
        imageId = "image-$id",
        itemName = "Item $id",
        category = "top",
        subcategory = null,
        primaryColor = null,
        dominantColorHex = null,
        style = null,
        season = null,
        occasion = null,
        material = null,
        formality = null,
        isFavorite = false,
        imageUrl = null,
        createdAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `no items selected yields all-null ids`() {
        assertEquals(Triple(null, null, null), OutfitUiState().selectedItemIds())
    }

    @Test
    fun `selecting all three slots yields their ids`() {
        val state = OutfitUiState(
            selectedTopItem = wardrobeItem("top-1"),
            selectedBottomItem = wardrobeItem("bottom-1"),
            selectedShoesItem = wardrobeItem("shoes-1"),
        )
        assertEquals(Triple("top-1", "bottom-1", "shoes-1"), state.selectedItemIds())
    }

    // Mirrors what OutfitViewModel.clearTopItem() does: copy(selectedTopItem = null).
    @Test
    fun `clearing a previously selected top drops its id but keeps the others`() {
        val withAllSelected = OutfitUiState(
            selectedTopItem = wardrobeItem("top-1"),
            selectedBottomItem = wardrobeItem("bottom-1"),
            selectedShoesItem = wardrobeItem("shoes-1"),
        )
        val afterClearingTop = withAllSelected.copy(selectedTopItem = null)

        assertEquals(Triple(null, "bottom-1", "shoes-1"), afterClearingTop.selectedItemIds())
    }
}
