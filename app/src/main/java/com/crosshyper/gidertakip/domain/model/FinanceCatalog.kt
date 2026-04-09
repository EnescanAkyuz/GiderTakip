package com.crosshyper.gidertakip.domain.model

data class CategoryPreset(
    val name: String,
    val defaultBudgetLimit: Double
)

data class CardPreset(
    val name: String,
    val dueDayOfMonth: Int?
)

object FinanceCatalog {
    val categories = listOf(
        CategoryPreset("Market", 5000.0),
        CategoryPreset("Yemek", 4500.0),
        CategoryPreset("Ulasim", 2500.0),
        CategoryPreset("Faturalar", 3200.0),
        CategoryPreset("Ev", 6500.0),
        CategoryPreset("Saglik", 2000.0),
        CategoryPreset("Eglence", 2500.0),
        CategoryPreset("Egitim", 3000.0),
        CategoryPreset("Kisisel", 2200.0),
        CategoryPreset("Diger", 1800.0)
    )

    val cards = listOf(
        CardPreset("Nakit", null),
        CardPreset("Akbank Platinum", 12),
        CardPreset("Enpara Kredi Karti", 18),
        CardPreset("Yapi Kredi World", 24),
        CardPreset("Garanti Bonus", 9)
    )

    fun defaultBudgetLimits(): Map<String, Double> {
        return categories.associate { it.name to it.defaultBudgetLimit }
    }

    fun dueDayForCard(cardName: String): Int? {
        return cards.firstOrNull { it.name == cardName }?.dueDayOfMonth
    }
}
