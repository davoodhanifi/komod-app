package com.komod.api.data.repository

import com.komod.api.domain.model.UploadedImageDetail

interface UploadReviewRepository {
    suspend fun getUploadedImageDetail(imageId: String): UploadedImageDetail
}
