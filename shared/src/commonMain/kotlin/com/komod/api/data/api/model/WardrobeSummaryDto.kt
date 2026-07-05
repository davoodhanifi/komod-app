package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class WardrobeSummaryDto(
    val totalItems: Int,
    val categories: List<CategoryCountDto>,
)

@Serializable
data class CategoryCountDto(
    val category: String,
    val count: Int,
)
