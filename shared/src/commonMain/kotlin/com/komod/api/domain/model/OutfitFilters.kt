package com.komod.api.domain.model

enum class OutfitOccasion(
    val apiValue: String,
    val label: String,
) {
    All("all", "All"),
    Office("office", "Office"),
    Casual("casual", "Casual"),
    Date("date", "Date"),
    Travel("travel", "Travel"),
    Outdoor("outdoor", "Outdoor"),
    Sport("sport", "Sport"),
    Party("party", "Party"),
}

enum class OutfitStyle(
    val apiValue: String,
    val label: String,
) {
    Casual("casual", "Casual"),
    Minimalist("minimalist", "Minimalist"),
    Outdoor("outdoor", "Outdoor"),
    Business("business", "Business"),
    Streetwear("streetwear", "Streetwear"),
    Classic("classic", "Classic"),
}
