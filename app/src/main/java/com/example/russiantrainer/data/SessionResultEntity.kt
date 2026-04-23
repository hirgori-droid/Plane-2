package com.example.russiantrainer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_results")
data class SessionResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playedAtEpochMillis: Long,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val learnedWordsCount: Int,
    val durationMillis: Long
)
