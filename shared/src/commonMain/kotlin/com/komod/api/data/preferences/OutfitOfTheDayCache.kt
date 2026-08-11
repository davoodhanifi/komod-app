package com.komod.api.data.preferences

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CachedOutfitOfTheDay(
    val date: String,
    val occasion: String,
    val id: String,
    val name: String,
    val reason: String,
    val matchScore: Int,
    val wardrobeItemIds: List<String>,
    val temperatureC: Double?,
    val condition: String?,
    val weatherCode: Int?,
    val isRaining: Boolean,
    val isSnowing: Boolean,
    val locationPermissionDenied: Boolean,
)

private val cacheJson = Json { ignoreUnknownKeys = true }

expect class OutfitOfTheDayCache() {
    fun getRawValue(): String?
    fun setRawValue(value: String)
}

fun OutfitOfTheDayCache.get(): CachedOutfitOfTheDay? {
    val raw = getRawValue() ?: return null
    return runCatching { cacheJson.decodeFromString<CachedOutfitOfTheDay>(raw) }.getOrNull()
}

fun OutfitOfTheDayCache.save(entry: CachedOutfitOfTheDay) {
    setRawValue(cacheJson.encodeToString(entry))
}
