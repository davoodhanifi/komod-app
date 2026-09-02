package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

// Pagination fields are null when the request omitted pageNumber/pageSize, matching the
// backend's opt-in pagination: data is still the full unpaginated list in that case.
@Serializable
data class PaginatedResponseData<T>(
    val data: T,
    val pageNumber: Int? = null,
    val pageSize: Int? = null,
    val totalCount: Int? = null,
    val totalPages: Int? = null,
    val hasNextPage: Boolean? = null,
)
