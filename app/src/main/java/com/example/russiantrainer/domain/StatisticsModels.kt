package com.example.russiantrainer.domain

data class DailyLearnedStat(
    val label: String,
    val learnedWords: Int
)

data class StatisticsSummary(
    val totalTimeMillis: Long = 0,
    val learnedToday: Int = 0,
    val learnedThisWeek: Int = 0,
    val dailyLearned: List<DailyLearnedStat> = emptyList()
)
