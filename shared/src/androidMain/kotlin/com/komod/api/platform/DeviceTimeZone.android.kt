package com.komod.api.platform

import java.util.TimeZone

actual fun getDeviceTimeZoneId(): String? = runCatching { TimeZone.getDefault().id }.getOrNull()
