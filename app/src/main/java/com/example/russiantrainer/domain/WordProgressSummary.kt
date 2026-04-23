package com.example.russiantrainer.domain

data class WordProgressSummary(
    val id: Long,
    val russian: String,
    val transcription: String,
    val english: String,
    val translations: List<String>,
    val examples: List<String>,
    val correctStreak: Int,
    val isLearned: Boolean,
    val timesShown: Int,
    val timesCorrect: Int
) {
    val accuracyPercent: Int = if (timesShown == 0) 0 else (timesCorrect * 100) / timesShown
}
