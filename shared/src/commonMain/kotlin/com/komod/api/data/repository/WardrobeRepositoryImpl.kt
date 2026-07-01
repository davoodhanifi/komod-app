package com.komod.api.data.repository

import com.komod.api.data.api.WardrobeApiService
import com.komod.api.domain.model.WardrobeItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlin.time.Duration.Companion.hours

private const val WARDROBE_BUCKET = "wardrobe"

class WardrobeRepositoryImpl(
    private val wardrobeApiService: WardrobeApiService,
    private val supabaseClient: SupabaseClient,
) : WardrobeRepository {
    override suspend fun getWardrobeItems(): List<WardrobeItem> {
        return wardrobeApiService.getWardrobeItems().map { dto ->
            val imageUrl = dto.croppedImageStoragePath
                ?.takeIf { it.isNotBlank() }
                ?.let { path ->
                    runCatching {
                        supabaseClient.storage.from(WARDROBE_BUCKET).createSignedUrl(path, 1.hours)
                    }.getOrNull()
                }

            WardrobeItem(
                id = dto.id,
                imageId = dto.imageId,
                itemName = dto.itemName,
                category = dto.category,
                subcategory = dto.subcategory,
                primaryColor = dto.primaryColor,
                dominantColorHex = dto.dominantColorHex,
                style = dto.style,
                season = dto.season,
                occasion = dto.occasion,
                material = dto.material,
                formality = dto.formality,
                imageUrl = imageUrl,
                createdAt = dto.createdAt,
            )
        }
    }
}
