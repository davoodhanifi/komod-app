package com.komod.api.presentation.outfits

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberWeatherPermissionController(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): WeatherPermissionController {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted() else onDenied()
    }

    return remember(context, launcher) {
        object : WeatherPermissionController {
            override val hasPermission: Boolean
                get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

            override fun requestPermission() {
                if (hasPermission) onGranted() else launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
}
