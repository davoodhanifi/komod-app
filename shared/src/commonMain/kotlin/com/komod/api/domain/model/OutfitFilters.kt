package com.komod.api.domain.model

enum class OutfitOccasion(
    val apiValue: String,
    val label: String,
) {
    All("all", "All"),
    Office("office", "Office"),
    Casual("daily", "Casual"),
    Date("date", "Date"),
    Travel("travel", "Travel"),
    Outdoor("outdoor", "Outdoor"),
    Sport("sport", "Sport"),
    Party("party", "Party"),
    Business("business", "Business"),
    Wedding("wedding", "Wedding"),
    Holiday("holiday", "Holiday"),
}

enum class OutfitStyle(
    val apiValue: String,
    val label: String,
) {
    Minimalist("minimalist", "Minimalist"),
    Classic("classic", "Classic"),
    Streetwear("streetwear", "Streetwear"),
    Business("business", "Business"),
    Preppy("preppy", "Preppy"),
    Athleisure("athleisure", "Athleisure"),
    Vintage("vintage", "Vintage"),
    Modern("modern", "Modern"),
    Elegant("elegant", "Elegant"),
    Outdoor("outdoor", "Outdoor"),
}
