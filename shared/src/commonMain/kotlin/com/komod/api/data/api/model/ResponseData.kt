package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ResponseData<T>(val data: T)
