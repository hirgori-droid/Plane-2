package com.example.russiantrainer.domain

data class SessionResultSummary(
    val id: Long,
    val playedAtEpochMillis: Long,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val learnedWordsCount: Int,
    val durationMillis: Long
) {
    val accuracyPercent: Int = if (totalQuestions == 0) 0 else (correctAnswers * 100) / totalQuestions
}
