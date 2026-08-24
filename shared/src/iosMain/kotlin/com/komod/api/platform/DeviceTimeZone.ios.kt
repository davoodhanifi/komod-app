package com.komod.api.platform

import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.Foundation.resetSystemTimeZone

// NSTimeZone.systemTimeZone is cached by Foundation for the life of the process, so a bare
// read would keep returning the timezone at app launch even after the user travels or a DST
// boundary passes. resetSystemTimeZone() clears that cache before every read so the value
// stays live, matching the current Settings > General > Date & Time selection.
actual fun getDeviceTimeZoneId(): String? = runCatching {
    NSTimeZone.resetSystemTimeZone()
    NSTimeZone.localTimeZone.name
}.getOrNull()
