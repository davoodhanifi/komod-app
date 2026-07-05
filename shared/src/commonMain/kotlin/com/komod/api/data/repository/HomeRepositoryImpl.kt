package com.komod.api.data.repository

import com.komod.api.data.api.WardrobeApiService
import com.komod.api.domain.model.CategoryCount
import com.komod.api.domain.model.RecentItem
import com.komod.api.domain.model.WardrobeSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlin.time.Duration.Companion.hours

private const val WARDROBE_BUCKET = "wardrobe"

class HomeRepositoryImpl(
    private val wardrobeApiService: WardrobeApiService,
    private val supabaseClient: SupabaseClient,
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
}
