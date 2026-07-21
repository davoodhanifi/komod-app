package com.komod.api.platform

import android.content.Intent
import android.net.Uri
import android.provider.Settings

actual class AppSettingsOpener {
    actual fun openAppSettings() {
        val context = AndroidAppContextHolder.context
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
