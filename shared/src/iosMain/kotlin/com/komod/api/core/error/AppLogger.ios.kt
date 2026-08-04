package com.komod.api.core.error

import platform.Foundation.NSLog

actual object AppLogger {
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        val detail = throwable?.let { " | ${it::class.simpleName}: ${it.message}\n${it.stackTraceToString()}" }.orEmpty()
        NSLog("[$tag] $message$detail")
    }
}
