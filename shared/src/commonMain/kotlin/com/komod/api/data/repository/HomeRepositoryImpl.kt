package com.komod.api.data.repository

import com.komod.api.data.api.OutfitApiService
import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.model.OutfitWardrobeItemDto
import com.komod.api.domain.model.CategoryCount
import com.komod.api.domain.model.OutfitItem
import com.komod.api.domain.model.RecentItem
import com.komod.api.domain.model.SavedOutfit
import com.komod.api.domain.model.WardrobeItem
import com.komod.api.domain.model.WardrobeSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlin.time.Duration.Companion.hours

private const val WARDROBE_BUCKET = "wardrobe"

class HomeRepositoryImpl(
    private val wardrobeApiService: WardrobeApiService,
    private val outfitApiService: OutfitApiService,
    private val supabaseClient: SupabaseClient,
    private val wardrobeItemCache: WardrobeItemCache,
) : HomeRepository {
    override suspend fun getWardrobeSummary(): WardrobeSummary {
        val dto = wardrobeApiService.getWardrobeSummary()
        return WardrobeSummary(
            totalItems = dto.totalItems,
            categories = dto.categories.map { CategoryCount(it.category, it.count) },
        )
    }

    override suspend fun getRecentItems(): List<RecentItem> {
        val dtos = wardrobeApiService.getRecentItems()
        return dtos.map { dto ->
            val imageUrl = dto.croppedImageStoragePath
                ?.takeIf { it.isNotBlank() }
                ?.let { path ->
                    runCatching {
                        supabaseClient.storage.from(WARDROBE_BUCKET).createSignedUrl(path, 1.hours)
                    }.getOrNull()
                }

            RecentItem(
                id = dto.id,
                itemName = dto.itemName,
                imageUrl = imageUrl,
                createdAt = dto.createdAt,
            )
        }
    }

    override suspend fun getSavedOutfits(): List<SavedOutfit> {
        val dtos = outfitApiService.getOutfits()
        return dtos
            .sortedByDescending { it.createdAt }
            .map { dto ->
                // The backend embeds full wardrobe item details (in display order) directly
                // on the outfit, so items are resolved here without touching WardrobeItemCache
                // or any per-item endpoint — only a signed URL lookup per image, same as
                // getRecentItems above. The cache is still warmed as a side effect so other
                // screens that key off it (e.g. wardrobe item detail) benefit too.
                val wardrobeItems = dto.wardrobeItems.map { itemDto -> itemDto.toWardrobeItem() }
                wardrobeItemCache.putAll(wardrobeItems)

                SavedOutfit(
                    id = dto.id,
                    name = dto.name,
                    reason = dto.reason,
                    wardrobeItemIds = wardrobeItems.map { it.id },
                    isFavorite = dto.isFavorite,
                    createdAt = dto.createdAt,
                    items = wardrobeItems.map { item ->
                        OutfitItem(
                            imageUrl = item.imageUrl,
                            category = item.category,
                            subcategory = item.subcategory,
                        )
                    },
                )
            }
    }

    private suspend fun OutfitWardrobeItemDto.toWardrobeItem(): WardrobeItem {
        // thumbnailStoragePath is a thumbnail of the original uploaded photo (Image.ThumbnailStoragePath),
        // not of this specific item's crop, so it's a last resort — croppedImageStoragePath is the
        // actual per-item image and must win whenever it's present.
        val imageUrl = (croppedImageStoragePath ?: originalImageStoragePath ?: thumbnailStoragePath)
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->
                runCatching {
                    supabaseClient.storage.from(WARDROBE_BUCKET).createSignedUrl(path, 1.hours)
                }.getOrNull()
            }

        return WardrobeItem(
            id = id,
            imageId = imageId,
            itemName = itemName,
            category = category,
            subcategory = subcategory,
            primaryColor = primaryColor,
            dominantColorHex = dominantColorHex,
            style = style,
            season = season,
            occasion = occasion,
            material = material,
            formality = formality,
            isFavorite = isFavorite,
            imageUrl = imageUrl,
            createdAt = createdAt,
        )
    }
}
