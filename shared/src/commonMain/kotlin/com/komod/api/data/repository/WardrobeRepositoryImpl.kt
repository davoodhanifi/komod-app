package com.komod.api.data.repository

import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.model.DeleteWardrobeItemsRequest
import com.komod.api.domain.model.WardrobeItem
import com.komod.api.domain.model.WardrobeItemsPage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlin.time.Duration.Companion.hours

private const val WARDROBE_BUCKET = "wardrobe"

class WardrobeRepositoryImpl(
    private val wardrobeApiService: WardrobeApiService,
    private val supabaseClient: SupabaseClient,
    private val wardrobeItemCache: WardrobeItemCache,
) : WardrobeRepository {
    override suspend fun getWardrobeItems(pageNumber: Int?, pageSize: Int?): WardrobeItemsPage {
        val response = wardrobeApiService.getWardrobeItems(pageNumber = pageNumber, pageSize = pageSize)
        val items = response.data.map { dto ->
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
                isFavorite = dto.isFavorite,
                imageUrl = imageUrl,
                createdAt = dto.createdAt,
            )
        }

        wardrobeItemCache.putAll(items)
        return WardrobeItemsPage(
            items = items,
            hasNextPage = response.hasNextPage ?: false,
            totalCount = response.totalCount,
        )
    }

    override suspend fun deleteWardrobeItems(ids: List<String>) {
        try {
            wardrobeApiService.deleteWardrobeItems(DeleteWardrobeItemsRequest(ids = ids.distinct()))
            ids.distinct().forEach { id -> wardrobeItemCache.remove(id) }
        } catch (error: ResponseException) {
            when (error.response.status) {
                HttpStatusCode.BadRequest -> throw WardrobeItemsBulkDeleteBadRequestException()
                else -> throw WardrobeItemsBulkDeleteNetworkException(error)
            }
        } catch (error: kotlinx.io.IOException) {
            throw WardrobeItemsBulkDeleteNetworkException(error)
        }
    }
}
