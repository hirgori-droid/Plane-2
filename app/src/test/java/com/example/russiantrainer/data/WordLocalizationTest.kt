package com.example.russiantrainer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordLocalizationTest {
    @Test
    fun normalizeEnglishText_removesOnceSuffixAndCollapsesSpaces() {
        val normalized = WordLocalization.normalizeEnglishText("  to take   once  ")

        assertEquals("to take", normalized)
    }

    @Test
    fun translationsForLanguage_returnsLocalizedValuesWhenPresent() {
        val word = sampleWord(
            english = "to take",
            translationsData = "to take | take",
            translationsFrData = "prendre | emporter"
        )

        val translations = WordLocalization.translationsForLanguage(word, "fr")

        assertEquals(listOf("prendre", "emporter"), translations)
    }

    @Test
    fun translationsForLanguage_fallsBackToEnglishWhenLocalizedMissing() {
        val word = sampleWord(
            english = "to finish",
            translationsData = "to finish | finish",
            translationsZhData = ""
        )

        val translations = WordLocalization.translationsForLanguage(word, "ar")

        assertEquals(listOf("to finish", "finish"), translations)
    }

    @Test
    fun translationsForLanguage_supportsChinese() {
        val word = sampleWord(
            english = "to take",
            translationsData = "to take",
            translationsZhData = "拿 | 取"
        )

        val translations = WordLocalization.translationsForLanguage(word, "zh")

        assertEquals(listOf("拿", "取"), translations)
    }

    @Test
    fun primaryTranslationForLanguage_usesFirstLocalizedTranslation() {
        val word = sampleWord(
            english = "to order",
            translationsData = "to order",
            translationsEsData = "pedir | ordenar"
        )

        val primary = WordLocalization.primaryTranslationForLanguage(word, "es")

        assertEquals("pedir", primary)
    }

    @Test
    fun encodeAndDecodeList_removeDuplicatesAndEmptyValues() {
        val encoded = WordLocalization.encodeList(listOf("one", " ", "two", "one"))
        val decoded = WordLocalization.decodeList(encoded)

        assertEquals("one | two", encoded)
        assertEquals(listOf("one", "two"), decoded)
    }

    @Test
    fun fallbackTranscription_transliteratesCyrillicText() {
        val transcription = WordLocalization.fallbackTranscription("привет")

        assertEquals("/privyet/", transcription)
        assertTrue(transcription.startsWith("/"))
        assertTrue(transcription.endsWith("/"))
    }

    private fun sampleWord(
        english: String,
        translationsData: String,
        translationsFrData: String = "",
        translationsEsData: String = "",
        translationsPtData: String = "",
        translationsArData: String = "",
        translationsZhData: String = ""
    ): WordEntity {
        return WordEntity(
            blockId = "common_verbs_100",
            russian = "тест",
            transcription = "/test/",
            english = english,
            translationsData = translationsData,
            translationsFrData = translationsFrData,
            translationsEsData = translationsEsData,
            translationsPtData = translationsPtData,
            translationsArData = translationsArData,
            translationsZhData = translationsZhData
        )
    }
}
