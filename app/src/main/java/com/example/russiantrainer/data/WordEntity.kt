package com.example.russiantrainer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockId: String,
    val russian: String,
    val transcription: String,
    val english: String,
    val translationsData: String = "",
    val translationsFrData: String = "",
    val translationsEsData: String = "",
    val translationsPtData: String = "",
    val translationsArData: String = "",
    val translationsZhData: String = "",
    val examplesData: String = "",
    val correctStreak: Int = 0,
    val timesShown: Int = 0,
    val timesCorrect: Int = 0,
    val isLearned: Boolean = false,
    val learnedAtEpochMillis: Long? = null
)
