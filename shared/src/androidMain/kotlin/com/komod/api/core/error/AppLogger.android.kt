package com.komod.api.core.error

import android.util.Log

actual object AppLogger {
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}
