package com.komod.api.data.location

sealed interface WeatherLocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
    ) : WeatherLocationResult

    data object PermissionRequired : WeatherLocationResult

    data class Error(
        val message: String,
    ) : WeatherLocationResult
}

expect class WeatherLocationService() {
    suspend fun getCurrentLocation(): WeatherLocationResult
}
