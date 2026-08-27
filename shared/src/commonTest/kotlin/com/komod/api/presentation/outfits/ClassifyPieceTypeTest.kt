package com.komod.api.presentation.outfits

import kotlin.test.Test
import kotlin.test.assertEquals

// Regression coverage for classifyPieceType(), which both OutfitItem.pieceType() (outfit
// card collage layout) and WardrobeItem.matchesSlot() (starting-item picker filtering)
// rely on. A short-sleeve top was previously misclassified as Bottom because "short"
// (meant to catch the garment "shorts") also matches inside "short sleeve" — see the
// long-sleeve keys in HomeScreen.getCategoryIcon for the sibling "Short Sleeve"
// subcategory this taxonomy uses.
class ClassifyPieceTypeTest {

    @Test
    fun `a short-sleeve top is classified as Top, not Bottom`() {
        assertEquals(
            OutfitPieceType.Top,
            classifyPieceType(category = "Tops", subcategory = "Short Sleeve"),
        )
    }

    @Test
    fun `a hyphenated short-sleeve subcategory is classified as Top`() {
        assertEquals(
            OutfitPieceType.Top,
            classifyPieceType(category = "Shirts", subcategory = "Short-Sleeve"),
        )
    }

    @Test
    fun `a polo shirt is classified as Top`() {
        assertEquals(
            OutfitPieceType.Top,
            classifyPieceType(category = "Tops", subcategory = "Polo Shirt"),
        )
    }

    @Test
    fun `a t-shirt is classified as Top`() {
        assertEquals(
            OutfitPieceType.Top,
            classifyPieceType(category = "Tops", subcategory = "T-Shirt"),
        )
    }

    @Test
    fun `shorts the garment are still classified as Bottom`() {
        assertEquals(
            OutfitPieceType.Bottom,
            classifyPieceType(category = "Bottoms", subcategory = "Shorts"),
        )
    }

    @Test
    fun `cargo shorts are still classified as Bottom`() {
        assertEquals(
            OutfitPieceType.Bottom,
            classifyPieceType(category = "Bottoms", subcategory = "Cargo Shorts"),
        )
    }

    @Test
    fun `jeans are classified as Bottom`() {
        assertEquals(
            OutfitPieceType.Bottom,
            classifyPieceType(category = "Bottoms", subcategory = "Jeans"),
        )
    }

    @Test
    fun `sneakers are classified as Footwear`() {
        assertEquals(
            OutfitPieceType.Footwear,
            classifyPieceType(category = "Shoes", subcategory = "Sneakers"),
        )
    }
}
