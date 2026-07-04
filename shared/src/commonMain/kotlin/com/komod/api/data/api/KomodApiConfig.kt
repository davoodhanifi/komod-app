package com.komod.api.data.api

object KomodApiConfig {
    // Uses adb reverse tcp:7250 tcp:7250 to tunnel the emulator's localhost to the host machine.
    // Run: adb reverse tcp:7250 tcp:7250 before launching the app.
    const val BASE_URL = "https://komod.app/api/v1/"
}
