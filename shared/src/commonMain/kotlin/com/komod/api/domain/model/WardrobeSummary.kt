package com.komod.api.domain.model

data class WardrobeSummary(
    val totalItems: Int,
    val categories: List<CategoryCount>,
)

data class CategoryCount(
    val category: String,
    val count: Int,
)
