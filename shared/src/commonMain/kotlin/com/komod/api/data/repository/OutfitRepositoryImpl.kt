package com.komod.api.data.repository

import com.komod.api.data.api.OutfitApiService
import com.komod.api.domain.model.Outfit
import com.komod.api.domain.model.OutfitItem
import com.komod.api.domain.model.WardrobeItem
import com.komod.api.domain.model.WardrobeItemDetail
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlin.time.Duration.Companion.hours

class OutfitRepositoryImpl(
    private val outfitApiService: OutfitApiService,
    private val supabaseClient: SupabaseClient,
    private val wardrobeItemRepository: WardrobeItemRepository,
    private val wardrobeItemCache: WardrobeItemCache,
) : OutfitRepository {
    override suspend fun generateOutfits(occasion: String, style: String): List<Outfit> {
        val response = outfitApiService.generateOutfits(occasion = occasion, style = style)

        return response.outfits.map { dto ->
            Outfit(
                name = dto.name,
                reason = dto.reason,
                matchScore = stableMatchScore(seed = dto.name + dto.wardrobeItemIds.joinToString()),
                wardrobeItemIds = dto.wardrobeItemIds,
                items = hydrateItems(dto.items, dto.wardrobeItemIds),
            )
        }
    }

    private suspend fun hydrateItems(
        apiItems: List<com.komod.api.data.api.model.OutfitGeneratedItemDto>,
        wardrobeItemIds: List<String>,
    ): List<OutfitItem> {
        if (apiItems.isNotEmpty()) {
            val metadata = resolveWardrobeMetadata(wardrobeItemIds)
            return coroutineScope {
                apiItems.mapIndexed { index, item ->
                    async {
                        val itemMeta = metadata.getOrNull(index)
                        OutfitItem(
                            imageUrl = extractStoragePath(item.imageUrl)?.let { storagePath ->
                                runCatching {
                                    supabaseClient.storage.from(WARDROBE_BUCKET).createSignedUrl(storagePath, 1.hours)
                                }.getOrNull()
                            } ?: item.imageUrl,
                            category = itemMeta?.category,
                            subcategory = itemMeta?.subcategory,
                        )
                    }
                }.awaitAll()
            }
        }

        return resolveFallbackItems(wardrobeItemIds)
    }

    private suspend fun resolveFallbackItems(wardrobeItemIds: List<String>): List<OutfitItem> = coroutineScope {
        wardrobeItemIds.distinct().map { id ->
            async {
                val wardrobeItem = wardrobeItemCache.getOrLoad(id) {
                    wardrobeItemRepository.getWardrobeItem(id).toWardrobeItem()
                }
                OutfitItem(
                    imageUrl = wardrobeItem.imageUrl,
                    category = wardrobeItem.category,
                    subcategory = wardrobeItem.subcategory,
                )
            }
        }.awaitAll()
    }

    private suspend fun resolveWardrobeMetadata(wardrobeItemIds: List<String>): List<WardrobeItem> = coroutineScope {
        wardrobeItemIds.map { id ->
            async {
                wardrobeItemCache.getOrLoad(id) {
                    wardrobeItemRepository.getWardrobeItem(id).toWardrobeItem()
                }
            }
        }.awaitAll()
    }

    private fun stableMatchScore(seed: String): Int {
        val bucket = seed.hashCode().absoluteValue % 10
        return 90 + bucket
    }
}

private val Int.absoluteValue: Int
    get() = if (this < 0) -this else this

private const val WARDROBE_BUCKET = "wardrobe"

private fun extractStoragePath(imageUrl: String): String? {
    val marker = "/wardrobe/"
    val index = imageUrl.indexOf(marker)
    if (index < 0) return null
    return imageUrl.substring(index + marker.length)
}

private fun WardrobeItemDetail.toWardrobeItem(): WardrobeItem {
    return WardrobeItem(
        id = id,
        imageId = imageId,
        itemName = itemName,
        category = category,
        subcategory = subcategory,
        primaryColor = primaryColor,
        dominantColorHex = null,
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
