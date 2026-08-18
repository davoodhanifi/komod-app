package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

// The backend's error envelope for non-2xx responses (RFC 7807 ProblemDetails plus a
// machine-readable error-code extension, e.g. {"type":"...","title":"...","status":402,
// "detail":"...","code":"PlanLimitExceeded"}). The exact field name for that code hasn't
// been confirmed against the real backend yet, so both common spellings are read —
// whichever is present is what lets the app recognize specific backend error codes (like
// PlanLimitExceeded) without string-matching the English "detail" text.
@Serializable
data class ProblemDetailsDto(
    val code: String? = null,
    val errorCode: String? = null,
)
