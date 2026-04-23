package com.example.russiantrainer.ui

import androidx.annotation.StringRes
import com.example.russiantrainer.R

enum class VocabularyBlock(
    val id: String,
    val targetSize: Int,
    val displayWordCount: Int,
    val sourceRange: IntRange,
    @StringRes val titleResId: Int
) {
    BEGINNER_1000(
        id = "beginner_1000",
        targetSize = 1000,
        displayWordCount = 1000,
        sourceRange = 1..1000,
        titleResId = R.string.block_beginner_1000_title
    ),
    CORE_3000(
        id = "core_3000",
        targetSize = 3000,
        displayWordCount = 3000,
        sourceRange = 1001..4000,
        titleResId = R.string.block_core_3000_title
    ),
    EXTENDED_6000(
        id = "extended_6000",
        targetSize = 6000,
        displayWordCount = 6000,
        sourceRange = 4001..10000,
        titleResId = R.string.block_extended_6000_title
    ),
    FRUITS_VEGETABLES_120(
        id = "fruits_vegetables_120",
        targetSize = 120,
        displayWordCount = 120,
        sourceRange = 1..120,
        titleResId = R.string.block_fruits_vegetables_120_title
    ),
    SHOPPING_150(
        id = "shopping_150",
        targetSize = 150,
        displayWordCount = 150,
        sourceRange = 1..150,
        titleResId = R.string.block_shopping_150_title
    ),
    PROFESSIONS_40(
        id = "professions_40",
        targetSize = 100,
        displayWordCount = 100,
        sourceRange = 1..100,
        titleResId = R.string.block_professions_40_title
    ),
    CLOTHES_ACCESSORIES_100(
        id = "clothes_accessories_100",
        targetSize = 100,
        displayWordCount = 100,
        sourceRange = 1..100,
        titleResId = R.string.block_clothes_accessories_100_title
    ),
    APPEARANCE_80(
        id = "appearance_80",
        targetSize = 80,
        displayWordCount = 80,
        sourceRange = 1..80,
        titleResId = R.string.block_appearance_80_title
    ),
    HEALTH_100(
        id = "health_100",
        targetSize = 100,
        displayWordCount = 100,
        sourceRange = 1..100,
        titleResId = R.string.block_health_100_title
    ),
    SPORTS_60(
        id = "sports_60",
        targetSize = 60,
        displayWordCount = 60,
        sourceRange = 1..60,
        titleResId = R.string.block_sports_60_title
    ),
    HOME_APARTMENT_120(
        id = "home_apartment_120",
        targetSize = 120,
        displayWordCount = 120,
        sourceRange = 1..120,
        titleResId = R.string.block_home_apartment_120_title
    ),
    CONFECTIONERY_50(
        id = "confectionery_50",
        targetSize = 50,
        displayWordCount = 50,
        sourceRange = 1..50,
        titleResId = R.string.block_confectionery_50_title
    ),
    TRANSPORT_80(
        id = "transport_80",
        targetSize = 80,
        displayWordCount = 80,
        sourceRange = 1..80,
        titleResId = R.string.block_transport_80_title
    ),
    OUTDOOR_RECREATION_60(
        id = "outdoor_recreation_60",
        targetSize = 60,
        displayWordCount = 60,
        sourceRange = 1..60,
        titleResId = R.string.block_outdoor_recreation_60_title
    ),
    WEATHER_50(
        id = "weather_50",
        targetSize = 50,
        displayWordCount = 50,
        sourceRange = 1..50,
        titleResId = R.string.block_weather_50_title
    ),
    HUMAN_BODY_60(
        id = "human_body_60",
        targetSize = 60,
        displayWordCount = 60,
        sourceRange = 1..60,
        titleResId = R.string.block_human_body_60_title
    ),
    COMMON_VERBS_100(
        id = "common_verbs_100",
        targetSize = 100,
        displayWordCount = 100,
        sourceRange = 1..100,
        titleResId = R.string.block_common_verbs_100_title
    ),
    FREQUENT_VERBS_180(
        id = "frequent_verbs_180",
        targetSize = 180,
        displayWordCount = 180,
        sourceRange = 1..180,
        titleResId = R.string.block_frequent_verbs_180_title
    ),
    CAFE_50(
        id = "cafe_50",
        targetSize = 50,
        displayWordCount = 50,
        sourceRange = 1..50,
        titleResId = R.string.block_cafe_50_title
    ),
    ANIMALS_BIRDS_80(
        id = "animals_birds_80",
        targetSize = 80,
        displayWordCount = 80,
        sourceRange = 1..80,
        titleResId = R.string.block_animals_birds_80_title
    ),
    TIME_CALENDAR_80(
        id = "time_calendar_80",
        targetSize = 80,
        displayWordCount = 80,
        sourceRange = 1..80,
        titleResId = R.string.block_time_calendar_80_title
    ),
    ADVERBS_100(
        id = "adverbs_100",
        targetSize = 100,
        displayWordCount = 100,
        sourceRange = 1..100,
        titleResId = R.string.block_adverbs_100_title
    ),
    NUMBERS_50(
        id = "numbers_50",
        targetSize = 50,
        displayWordCount = 50,
        sourceRange = 1..50,
        titleResId = R.string.block_numbers_50_title
    ),
    PREPOSITIONS_35(
        id = "prepositions_35",
        targetSize = 35,
        displayWordCount = 35,
        sourceRange = 1..35,
        titleResId = R.string.block_prepositions_35_title
    ),
    PRONOUNS_60(
        id = "pronouns_60",
        targetSize = 60,
        displayWordCount = 60,
        sourceRange = 1..60,
        titleResId = R.string.block_pronouns_60_title
    ),
    PARK_TREES_40(
        id = "park_trees_40",
        targetSize = 40,
        displayWordCount = 40,
        sourceRange = 1..40,
        titleResId = R.string.block_park_trees_40_title
    );

    companion object {
        fun fromId(id: String?): VocabularyBlock {
            return entries.firstOrNull { it.id == id } ?: BEGINNER_1000
        }
    }
}
