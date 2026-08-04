package com.komod.api.core.error

/**
 * Thin platform logging seam used by [ErrorMapper] to record full exception detail
 * (type, message, stack trace) for debugging, independent of whatever short message
 * is ultimately shown to the user.
 */
expect object AppLogger {
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
