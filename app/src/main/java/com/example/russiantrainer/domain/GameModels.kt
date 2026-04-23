package com.example.russiantrainer.domain

data class GameQuestion(
    val wordId: Long,
    val russian: String,
    val transcription: String,
    val correctEnglish: String,
    val translations: List<String>,
    val examples: List<String>,
    val options: List<String>,
    val streakBeforeAnswer: Int
)

data class RoundSummary(
    val totalQuestions: Int,
    val correctAnswers: Int,
    val newlyLearnedWordIds: List<Long>,
    val durationMillis: Long
) {
    val accuracyPercent: Int = if (totalQuestions == 0) 0 else (correctAnswers * 100) / totalQuestions
}
