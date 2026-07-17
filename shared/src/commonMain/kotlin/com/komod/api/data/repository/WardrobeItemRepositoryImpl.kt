package com.komod.api.data.repository

import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.model.WardrobeItemUpdateRequest
import com.komod.api.domain.model.WardrobeItemDetail
import com.komod.api.domain.model.WardrobeItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlin.time.Duration.Companion.hours

private const val WARDROBE_BUCKET = "wardrobe"

class WardrobeItemRepositoryImpl(
    private val wardrobeApiService: WardrobeApiService,
    private val supabaseClient: SupabaseClient,
    private val wardrobeItemCache: WardrobeItemCache,
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
        ).also { wardrobeItemCache.putAll(listOf(it.toWardrobeItem())) }
    }

    override suspend fun deleteWardrobeItem(id: String) {
        try {
            wardrobeApiService.deleteWardrobeItem(id)
            wardrobeItemCache.remove(id)
        } catch (error: ResponseException) {
            when (error.response.status) {
                HttpStatusCode.NotFound -> throw WardrobeItemDeleteNotFoundException()
                HttpStatusCode.BadRequest -> throw WardrobeItemDeleteBadRequestException()
                else -> throw WardrobeItemDeleteNetworkException(error)
            }
        } catch (error: kotlinx.io.IOException) {
            throw WardrobeItemDeleteNetworkException(error)
        }
    }

    override suspend fun updateWardrobeItem(
        id: String,
        request: WardrobeItemUpdateRequest,
    ): WardrobeItemDetail {
        try {
            wardrobeApiService.updateWardrobeItem(id, request)
            return getWardrobeItem(id)
        } catch (error: ResponseException) {
            when (error.response.status) {
                HttpStatusCode.NotFound -> throw WardrobeItemUpdateNotFoundException()
                HttpStatusCode.BadRequest -> throw WardrobeItemUpdateBadRequestException()
                else -> throw WardrobeItemUpdateNetworkException(error)
            }
        } catch (error: kotlinx.io.IOException) {
            throw WardrobeItemUpdateNetworkException(error)
        }
    }
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
        imageUrl = imageUrl,
        createdAt = createdAt,
    )
}
