package com.komod.api.data.repository

import com.komod.api.domain.model.CurrentSubscription

interface SubscriptionRepository {
    suspend fun getCurrentSubscription(): CurrentSubscription
}
