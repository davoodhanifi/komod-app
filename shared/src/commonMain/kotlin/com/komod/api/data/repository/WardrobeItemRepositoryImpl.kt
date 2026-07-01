package com.komod.api.data.repository

import com.komod.api.data.api.WardrobeApiService
import com.komod.api.domain.model.WardrobeItemDetail
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlin.time.Duration.Companion.hours

private const val WARDROBE_BUCKET = "wardrobe"

class WardrobeItemRepositoryImpl(
    private val wardrobeApiService: WardrobeApiService,
    private val supabaseClient: SupabaseClient,
) : WardrobeItemRepository {

    override suspend fun getWardrobeItem(id: String): WardrobeItemDetail {
        val dto = wardrobeApiService.getWardrobeItemById(id)

        val imageUrl = dto.croppedImageStoragePath
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->
                runCatching {
                    supabaseClient.storage.from(WARDROBE_BUCKET).createSignedUrl(path, 1.hours)
                }.getOrNull()
            }
            ?: runCatching {
                val image = wardrobeApiService.getImage(dto.imageId)
                supabaseClient.storage.from(WARDROBE_BUCKET).createSignedUrl(image.storagePath, 1.hours)
            }.getOrNull()

        return WardrobeItemDetail(
            id = dto.id,
            imageId = dto.imageId,
            imageUrl = imageUrl,
            status = dto.status,
            category = dto.category,
            subcategory = dto.subcategory,
            itemName = dto.itemName,
            bodyRegion = dto.bodyRegion,
            layer = dto.layer,
            primaryColor = dto.primaryColor,
            secondaryColors = dto.secondaryColors,
            accentColors = dto.accentColors,
            pattern = dto.pattern,
            material = dto.material,
            texture = dto.texture,
            fit = dto.fit,
            silhouette = dto.silhouette,
            sleeveLength = dto.sleeveLength,
            pantLength = dto.pantLength,
            neckline = dto.neckline,
            collarType = dto.collarType,
            closure = dto.closure,
            formality = dto.formality,
            style = dto.style,
            season = dto.season,
            occasion = dto.occasion,
            genderStyle = dto.genderStyle,
            weatherMinTempC = dto.weatherMinTempC,
            weatherMaxTempC = dto.weatherMaxTempC,
            weatherRainFriendly = dto.weatherRainFriendly,
            warmthLevel = dto.warmthLevel,
            features = dto.features,
            recommendedPairings = dto.recommendedPairings,
            embeddingDescription = dto.embeddingDescription,
            confidence = dto.confidence,
            createdAt = dto.createdAt,
        )
    }
}
