package com.komod.api.presentation.outfits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberWeatherPermissionController(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): WeatherPermissionController {
    return remember {
        object : WeatherPermissionController {
            override val hasPermission: Boolean = true

            override fun requestPermission() {
                onGranted()
            }
        }
    }
}
