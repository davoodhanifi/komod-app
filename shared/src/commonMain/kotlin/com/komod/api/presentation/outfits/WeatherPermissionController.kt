package com.komod.api.presentation.outfits

import androidx.compose.runtime.Composable

interface WeatherPermissionController {
    val hasPermission: Boolean

    fun requestPermission()
}

@Composable
expect fun rememberWeatherPermissionController(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): WeatherPermissionController
