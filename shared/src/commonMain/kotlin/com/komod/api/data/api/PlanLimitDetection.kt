package com.komod.api.data.api

import com.komod.api.core.error.AppLogger
import com.komod.api.data.api.model.ProblemDetailsDto
import kotlinx.serialization.json.Json

private const val PLAN_LIMIT_EXCEEDED_CODE = "PlanLimitExceeded"

private val problemDetailsJson = Json { ignoreUnknownKeys = true; isLenient = true }

// Best-effort: a body that isn't valid JSON just means this wasn't a structured
// ProblemDetails response at all — never worth crashing over, since the real HTTP
// failure is about to be thrown right after this check either way.
//
// The exact field name the backend uses for its error code hasn't been confirmed yet, so
// this checks the two most likely field names first, then falls back to a raw substring
// match on the literal code (still the stable identifier, not the translatable English
// message) so detection keeps working even if the field name turns out to be something
// else entirely. Every non-match is logged with the full raw body so the real shape can
// be captured from a device log next time this fires.
internal fun isPlanLimitExceeded(responseBodyText: String): Boolean {
    val problem = runCatching { problemDetailsJson.decodeFromString<ProblemDetailsDto>(responseBodyText) }.getOrNull()
    val matched = problem?.code == PLAN_LIMIT_EXCEEDED_CODE ||
        problem?.errorCode == PLAN_LIMIT_EXCEEDED_CODE ||
        responseBodyText.contains(PLAN_LIMIT_EXCEEDED_CODE)

    if (!matched) {
        // A logging failure must never affect detection's result — this runs on every
        // non-matching error response, including totally unrelated ones (a plain 404), so
        // it must be exactly as safe as the check itself.
        runCatching {
            AppLogger.e("PlanLimitDetection", "Non-success response did not match PlanLimitExceeded. Raw body: $responseBodyText")
        }
    }

    return matched
}
