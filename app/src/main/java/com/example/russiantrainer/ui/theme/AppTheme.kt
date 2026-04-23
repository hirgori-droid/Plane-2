package com.example.russiantrainer.ui.theme

import com.example.russiantrainer.R

enum class AppTheme(
    val key: String,
    val labelResId: Int
) {
    BLOSSOM("blossom", R.string.theme_blossom),
    GROVE("grove", R.string.theme_grove),
    MIDNIGHT("midnight", R.string.theme_midnight);

    companion object {
        fun fromKey(key: String?): AppTheme {
            return entries.firstOrNull { it.key == key } ?: BLOSSOM
        }
    }
}
