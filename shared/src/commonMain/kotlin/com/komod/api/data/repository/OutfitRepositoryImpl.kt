package com.komod.api.data.repository

import com.komod.api.data.api.OutfitApiService
import com.komod.api.data.api.model.OutfitGeneratedDto
import com.komod.api.data.api.model.SaveOutfitRequest
import com.komod.api.domain.model.Outfit
import com.komod.api.domain.model.OutfitItem
import com.komod.api.domain.model.OutfitOfTheDay
import com.komod.api.domain.model.OutfitOfTheDayWeatherSnapshot
import com.komod.api.domain.model.WardrobeItem
import com.komod.api.domain.model.WardrobeItemDetail
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours

class OutfitRepositoryImpl(
    private val outfitApiService: OutfitApiService,
    private val supabaseClient: SupabaseClient,
    private val wardrobeItemRepository: WardrobeItemRepository,
    private val wardrobeItemCache: WardrobeItemCache,
) : OutfitRepository {
    override suspend fun generateOutfits(
        occasion: String,
        style: String?,
        weather: com.komod.api.domain.model.WeatherCurrent?,
        selectedTopId: String?,
        selectedBottomId: String?,
        selectedShoesId: String?,
    ): List<Outfit> {
        val response = outfitApiService.generateOutfits(
            occasion = occasion,
            style = style,
            weather = weather,
            selectedTopId = selectedTopId,
            selectedBottomId = selectedBottomId,
            selectedShoesId = selectedShoesId,
        )

        return response.outfits.map { it.toOutfit() }
    }

    override suspend fun getOutfitOfTheDay(latitude: Double, longitude: Double): OutfitOfTheDay {
        val response = outfitApiService.getOutfitOfTheDay(latitude, longitude)

        return OutfitOfTheDay(
            outfits = response.outfits.map { it.toOutfit() },
            weather = OutfitOfTheDayWeatherSnapshot(
                temperatureC = response.weather.temperatureC,
                condition = response.weather.condition,
                isRaining = response.weather.isRaining,
                isSnowing = response.weather.isSnowing,
                next6HourMinTemperatureC = response.weather.next6HourMinTemperatureC,
                next6HourMaxTemperatureC = response.weather.next6HourMaxTemperatureC,
            ),
        )
    }

    override suspend fun saveOutfit(outfit: Outfit): String {
        return outfitApiService.saveOutfit(
            SaveOutfitRequest(
                name = outfit.name,
                reason = outfit.reason,
                wardrobeItemIds = outfit.wardrobeItemIds,
            ),
        )
    }

    override suspend fun unsaveOutfit(savedOutfitId: String) {
        try {
            outfitApiService.deleteOutfit(savedOutfitId)
        } catch (error: ResponseException) {
            when (error.response.status) {
                HttpStatusCode.NotFound -> throw OutfitDeleteNotFoundException()
                HttpStatusCode.BadRequest -> throw OutfitDeleteBadRequestException()
                else -> throw OutfitDeleteNetworkException(error)
            }
        } catch (error: kotlinx.io.IOException) {
            throw OutfitDeleteNetworkException(error)
        }
    }

    // Shared by generateOutfits() and getOutfitOfTheDay(): both return the same
    // name/reason/wardrobeItemIds (+ optional items) shape per outfit.
    private suspend fun OutfitGeneratedDto.toOutfit(): Outfit = Outfit(
        id = randomOutfitId(),
        name = name,
        reason = reason,
        matchScore = stableMatchScore(seed = name + wardrobeItemIds.joinToString()),
        wardrobeItemIds = wardrobeItemIds,
        items = hydrateItems(items, wardrobeItemIds),
    )

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

        return hydrateOutfitItems(wardrobeItemIds)
    }

    override suspend fun hydrateOutfitItems(wardrobeItemIds: List<String>): List<OutfitItem> = coroutineScope {
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

    private fun randomOutfitId(): String {
        return Random.nextBytes(16).joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
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
