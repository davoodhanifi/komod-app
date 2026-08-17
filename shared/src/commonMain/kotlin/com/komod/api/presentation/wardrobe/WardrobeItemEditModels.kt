package com.komod.api.presentation.wardrobe

import com.komod.api.data.api.model.WardrobeItemStatus
import com.komod.api.data.api.model.WardrobeItemUpdateRequest
import com.komod.api.domain.model.WardrobeItemDetail

data class WardrobeItemEditFormState(
    val category: String = "",
    val originalCategory: String = "",
    val subcategory: String = "",
    val name: String = "",
    val brand: String = "",
    val material: String = "",
    val primaryColor: String = "",
    val formality: String = "",
    val originalFormality: String = "",
    val seasons: Set<String> = emptySet(),
    val originalSeasons: Set<String> = emptySet(),
    val occasions: Set<String> = emptySet(),
    val originalOccasions: Set<String> = emptySet(),
    val status: WardrobeItemStatus = WardrobeItemStatus.Active,
    val originalStatus: WardrobeItemStatus = WardrobeItemStatus.Active,
)

data class WardrobeItemEditContent(
    val item: WardrobeItemDetail,
    val form: WardrobeItemEditFormState,
    val isSaving: Boolean = false,
)

sealed interface WardrobeItemEditUiState {
    data object Loading : WardrobeItemEditUiState
    data class Ready(val content: WardrobeItemEditContent) : WardrobeItemEditUiState
    data class Error(val message: String) : WardrobeItemEditUiState
}

sealed interface WardrobeItemEditEffect {
    data object Saved : WardrobeItemEditEffect
    data object ItemNotFound : WardrobeItemEditEffect
    data class SaveFailed(val message: String) : WardrobeItemEditEffect
}

internal fun WardrobeItemDetail.toEditContent(): WardrobeItemEditContent {
    return WardrobeItemEditContent(
        item = this,
        form = WardrobeItemEditFormState(
            category = category.normalizeWardrobeValue(),
            originalCategory = category.normalizeWardrobeValue(),
            subcategory = subcategory.orEmpty(),
            name = itemName.orEmpty(),
            brand = brand.orEmpty(),
            material = material.orEmpty(),
            primaryColor = primaryColor.orEmpty(),
            formality = formality.orEmpty().normalizeWardrobeValue(),
            originalFormality = formality.orEmpty().normalizeWardrobeValue(),
            seasons = season
                .orEmpty()
                .map { it.normalizeWardrobeValue() }
                .toSet(),
            originalSeasons = season.orEmpty().map { it.normalizeWardrobeValue() }.toSet(),
            occasions = occasion
                .orEmpty()
                .map { it.normalizeWardrobeValue() }
                .toSet(),
            originalOccasions = occasion.orEmpty().map { it.normalizeWardrobeValue() }.toSet(),
            status = status.toWardrobeItemStatus(),
            originalStatus = status.toWardrobeItemStatus(),
        ),
    )
}

internal fun WardrobeItemEditFormState.toUpdateRequest(): WardrobeItemUpdateRequest {
    return WardrobeItemUpdateRequest(
        category = category.takeIf { it != originalCategory && it in WardrobeCategoryOptions },
        subcategory = subcategory.takeIf { it.isNotBlank() },
        name = name.takeIf { it.isNotBlank() },
        brand = brand.takeIf { it.isNotBlank() },
        material = material.takeIf { it.isNotBlank() },
        primaryColor = primaryColor.takeIf { it.isNotBlank() },
        formality = formality.takeIf { it != originalFormality && it in WardrobeFormalityOptions },
        season = seasons.takeIf { it != originalSeasons && it.all { value -> value in WardrobeSeasonOptions } }?.toList(),
        occasion = occasions.takeIf { it != originalOccasions && it.all { value -> value in WardrobeOccasionOptions } }?.toList(),
        status = status.takeIf { it != originalStatus }?.ordinal,
    )
}

internal fun WardrobeItemEditFormState.validate(): String? {
    if (category != originalCategory && category !in WardrobeCategoryOptions) {
        return "Choose a valid category."
    }
    if (formality != originalFormality && formality !in WardrobeFormalityOptions) {
        return "Choose a valid formality."
    }
    if (seasons != originalSeasons && seasons.any { it !in WardrobeSeasonOptions }) {
        return "Choose valid season values."
    }
    if (occasions != originalOccasions && occasions.any { it !in WardrobeOccasionOptions }) {
        return "Choose valid occasion values."
    }
    if (status != originalStatus && status !in WardrobeItemStatus.entries) {
        return "Choose a valid status."
    }
    return null
}

internal fun WardrobeItemStatus.toDisplayLabel(): String = name.toWardrobeLabel()

internal fun String.normalizeWardrobeValue(): String = lowercase().trim()
    .replace(" ", "-")
    .replace("_", "-")
    .let { value -> if (value == "fall") "autumn" else value }

internal fun String.toWardrobeLabel(): String = split('-', '_', ' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { part -> part.replaceFirstChar { char -> char.uppercase() } }

internal fun Int.toWardrobeItemStatus(): WardrobeItemStatus = WardrobeItemStatus.entries.getOrNull(this)
    ?: WardrobeItemStatus.Active
