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

// Marketing copy for the Paywall's plan cards — the four purchasable tiers' wardrobe limits.
// These are fixed at build time, not sourced from the backend (unlike CurrentSubscription's
// wardrobeItemLimit, which reflects the signed-in user's actual current plan).
fun SubscriptionPlan.wardrobeLimitDescription(): String = when (this) {
    SubscriptionPlan.OneDoor -> "Up to 100 wardrobe items"
    SubscriptionPlan.TwoDoors -> "Up to 200 wardrobe items"
    SubscriptionPlan.ThreeDoors -> "Up to 300 wardrobe items"
    SubscriptionPlan.WalkIn -> "Unlimited wardrobe items"
    SubscriptionPlan.Rack, SubscriptionPlan.Unknown -> ""
}
