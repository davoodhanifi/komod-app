package com.komod.api.presentation.profile

import com.komod.api.domain.model.SubscriptionPlan

// The only place SubscriptionPlan is turned into user-facing copy — everywhere else
// (repository, ViewModel) works with the plan/limit values the backend returned as-is.
fun SubscriptionPlan.displayName(): String = when (this) {
    SubscriptionPlan.Rack -> "Komod Rack"
    SubscriptionPlan.OneDoor -> "Komod 1 Door"
    SubscriptionPlan.TwoDoors -> "Komod 2 Doors"
    SubscriptionPlan.ThreeDoors -> "Komod 3 Doors"
    SubscriptionPlan.WalkIn -> "Komod Walk-in"
    SubscriptionPlan.Unknown -> "Your Komod Plan"
}
