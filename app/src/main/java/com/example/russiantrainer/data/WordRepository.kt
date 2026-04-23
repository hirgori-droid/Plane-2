package com.example.russiantrainer.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.russiantrainer.domain.GameQuestion
import com.example.russiantrainer.domain.RoundSummary
import com.example.russiantrainer.domain.SessionResultSummary
import com.example.russiantrainer.domain.WordProgressSummary
import com.example.russiantrainer.ui.VocabularyBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.random.Random

class WordRepository private constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val random: Random = Random.Default
) {
    private val dao = database.wordDao()
    private val seedAssetNames = listOf("words_seed.csv", "theme_blocks_seed.csv")
    private val localizedTranslationsAssetName = "localized_translations.csv"
    private val seedPrefs = context.getSharedPreferences("seed_metadata", MODE_PRIVATE)

    companion object {
        const val BEGINNER_BLOCK_ID = "beginner_1000"
        private const val SEED_VERSION = 2

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE words ADD COLUMN learnedAtEpochMillis INTEGER"
                )
            }
        }

        @Volatile
        private var instance: WordRepository? = null

        fun getInstance(context: Context): WordRepository {
            return instance ?: synchronized(this) {
                instance ?: WordRepository(
                    context = context.applicationContext,
                    database = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "russian_trainer.db"
                    )
                        .addMigrations(MIGRATION_6_7)
                        .fallbackToDestructiveMigration()
                        .build()
                ).also { instance = it }
            }
        }
    }

    fun observeLearnedCount(blockId: String = BEGINNER_BLOCK_ID): Flow<Int> = dao.observeLearnedCount(blockId)
    fun observeActiveCount(blockId: String = BEGINNER_BLOCK_ID): Flow<Int> = dao.observeActiveCount(blockId)
    fun observePlayedRoundsCount(): Flow<Int> = dao.observePlayedRoundsCount()
    fun observeAllSessionResults(): Flow<List<SessionResultSummary>> =
        dao.observeAllSessionResults().map { items -> items.map { it.toSummary() } }

    fun observeRecentSessionResults(limit: Int = 5): Flow<List<SessionResultSummary>> =
        dao.observeRecentSessionResults(limit).map { items -> items.map { it.toSummary() } }

    fun observeAllWordsAcrossBlocks(): Flow<List<WordEntity>> = dao.observeAllWordsAcrossBlocks()

    fun observeWordLibrary(
        blockId: String = BEGINNER_BLOCK_ID,
        languageTag: String = "en"
    ): Flow<List<WordProgressSummary>> =
        dao.observeAllWords(blockId).map { words ->
            words.map { word ->
                val localizedTranslations = WordLocalization.translationsForLanguage(word, languageTag)
                WordProgressSummary(
                    id = word.id,
                    russian = word.russian,
                    transcription = word.transcription,
                    english = word.english,
                    translations = localizedTranslations,
                    examples = WordLocalization.decodeList(word.examplesData),
                    correctStreak = word.correctStreak,
                    isLearned = word.isLearned,
                    timesShown = word.timesShown,
                    timesCorrect = word.timesCorrect
                )
            }
        }

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        val blockEntries = loadSeedWords().groupBy { it.blockId }
        if (blockEntries.isEmpty()) return@withContext
        val storedSeedVersion = seedPrefs.getInt("seed_version", 0)
        val forceRefreshAllSeedBlocks = storedSeedVersion != SEED_VERSION

        database.withTransaction {
            blockEntries.forEach { (blockId, words) ->
                val existingCount = dao.countByBlock(blockId)
                val shouldReplaceBlock = when {
                    forceRefreshAllSeedBlocks -> true
                    existingCount == 0 -> true
                    existingCount != words.size -> true
                    else -> hasBlockContentChanged(blockId, words)
                }

                if (shouldReplaceBlock) {
                    dao.deleteWordsByBlock(blockId)
                    dao.insertAll(words)
                }
            }
        }
        if (forceRefreshAllSeedBlocks) {
            seedPrefs.edit().putInt("seed_version", SEED_VERSION).apply()
        }
    }

    private suspend fun hasBlockContentChanged(blockId: String, words: List<WordEntity>): Boolean {
        val storedPairs = dao.getAllWordsForBlock(blockId)
            .map {
                listOf(
                    it.russian,
                    it.transcription,
                    it.english,
                    it.translationsData,
                    it.translationsFrData,
                    it.translationsEsData,
                    it.translationsPtData,
                    it.translationsArData,
                    it.translationsZhData,
                    it.examplesData
                )
            }
            .sortedBy { it.joinToString("::") }
        val seedPairs = words
            .map {
                listOf(
                    it.russian,
                    it.transcription,
                    it.english,
                    it.translationsData,
                    it.translationsFrData,
                    it.translationsEsData,
                    it.translationsPtData,
                    it.translationsArData,
                    it.translationsZhData,
                    it.examplesData
                )
            }
            .sortedBy { it.joinToString("::") }
        return storedPairs != seedPairs
    }

    private fun loadSeedWords(): List<WordEntity> {
        val localizedTranslations = loadLocalizedTranslations()
        return seedAssetNames.flatMap { assetName ->
            loadSeedWordsFromAsset(assetName, localizedTranslations)
        }
    }

    private fun loadSeedWordsFromAsset(
        assetName: String,
        localizedTranslations: Map<WordKey, LocalizedTranslations>
    ): List<WordEntity> {
        return try {
            context.assets.open(assetName)
                .bufferedReader()
                .useLines { lines ->
                    val iterator = lines.iterator()
                    if (!iterator.hasNext()) {
                        return@useLines emptyList()
                    }
                    val header = iterator.next().trim()

                    iterator.asSequence().mapNotNull { line ->
                        val parts = line.split(';', limit = 6)
                        when {
                            parts.size >= 6 -> {
                                val russian = parts[1].trim()
                                val primaryTranslation = WordLocalization.normalizeEnglishText(parts[3])
                                val wordKey = WordKey(parts[0].trim(), russian)
                                val localized = localizedTranslations[wordKey]
                                WordEntity(
                                    blockId = wordKey.blockId,
                                    russian = russian,
                                    transcription = parts[2].trim().ifBlank { WordLocalization.fallbackTranscription(russian) },
                                    english = primaryTranslation,
                                    translationsData = WordLocalization.encodeList(
                                        WordLocalization.decodeList(parts[4]).map(WordLocalization::normalizeEnglishText).ifEmpty {
                                            listOf(primaryTranslation)
                                        }
                                    ),
                                    translationsFrData = WordLocalization.encodeList(localized?.fr.orEmpty()),
                                    translationsEsData = WordLocalization.encodeList(localized?.es.orEmpty()),
                                    translationsPtData = WordLocalization.encodeList(localized?.pt.orEmpty()),
                                    translationsArData = WordLocalization.encodeList(localized?.ar.orEmpty()),
                                    translationsZhData = WordLocalization.encodeList(localized?.zh.orEmpty()),
                                    examplesData = WordLocalization.encodeList(WordLocalization.decodeList(parts[5]))
                                )
                            }

                            header == "block;russian;english;examples" && parts.size >= 4 -> {
                                val russian = parts[1].trim()
                                val english = WordLocalization.normalizeEnglishText(parts[2])
                                val wordKey = WordKey(parts[0].trim(), russian)
                                val localized = localizedTranslations[wordKey]
                                WordEntity(
                                    blockId = wordKey.blockId,
                                    russian = russian,
                                    transcription = WordLocalization.fallbackTranscription(russian),
                                    english = english,
                                    translationsData = WordLocalization.encodeList(listOf(english)),
                                    translationsFrData = WordLocalization.encodeList(localized?.fr.orEmpty()),
                                    translationsEsData = WordLocalization.encodeList(localized?.es.orEmpty()),
                                    translationsPtData = WordLocalization.encodeList(localized?.pt.orEmpty()),
                                    translationsArData = WordLocalization.encodeList(localized?.ar.orEmpty()),
                                    translationsZhData = WordLocalization.encodeList(localized?.zh.orEmpty()),
                                    examplesData = WordLocalization.encodeList(WordLocalization.decodeList(parts[3]))
                                )
                            }

                            parts.size >= 3 -> {
                                val russian = parts[1].trim()
                                val english = WordLocalization.normalizeEnglishText(parts[2])
                                val wordKey = WordKey(parts[0].trim(), russian)
                                val localized = localizedTranslations[wordKey]
                                WordEntity(
                                    blockId = wordKey.blockId,
                                    russian = russian,
                                    transcription = WordLocalization.fallbackTranscription(russian),
                                    english = english,
                                    translationsData = WordLocalization.encodeList(listOf(english)),
                                    translationsFrData = WordLocalization.encodeList(localized?.fr.orEmpty()),
                                    translationsEsData = WordLocalization.encodeList(localized?.es.orEmpty()),
                                    translationsPtData = WordLocalization.encodeList(localized?.pt.orEmpty()),
                                    translationsArData = WordLocalization.encodeList(localized?.ar.orEmpty()),
                                    translationsZhData = WordLocalization.encodeList(localized?.zh.orEmpty()),
                                    examplesData = ""
                                )
                            }

                            else -> null
                        }
                    }.toList()
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadLocalizedTranslations(): Map<WordKey, LocalizedTranslations> {
        return try {
            context.assets.open(localizedTranslationsAssetName)
                .bufferedReader()
                .useLines { lines ->
                    lines.drop(1)
                        .mapNotNull { line ->
                            val parts = line.split(';', limit = 7)
                            if (parts.size != 7) return@mapNotNull null
                            val blockId = parts[0].trim()
                            val russian = parts[1].trim()
                            if (blockId.isBlank() || russian.isBlank()) return@mapNotNull null

                            WordKey(blockId, russian) to LocalizedTranslations(
                                fr = WordLocalization.decodeList(parts[2]),
                                es = WordLocalization.decodeList(parts[3]),
                                pt = WordLocalization.decodeList(parts[4]),
                                ar = WordLocalization.decodeList(parts[5]),
                                zh = WordLocalization.decodeList(parts[6])
                            )
                        }
                        .toMap()
                }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun buildRound(
        blockId: String = BEGINNER_BLOCK_ID,
        languageTag: String = "en",
        roundSize: Int = 30
    ): List<GameQuestion> = withContext(Dispatchers.IO) {
        val expectedPoolSize = VocabularyBlock.fromId(blockId).targetSize.coerceAtLeast(30)
        val candidates = dao.getActiveWords(blockId, roundSize.coerceAtLeast(30))
        if (candidates.isEmpty()) return@withContext emptyList()

        val allWordsInBlock = dao.getAllWordsForBlock(blockId).take(expectedPoolSize)
        val allTranslations = allWordsInBlock
            .map { WordLocalization.primaryTranslationForLanguage(it, languageTag) }
            .distinct()

        candidates.shuffled(random).take(roundSize).mapNotNull { word ->
            val correctTranslation = WordLocalization.primaryTranslationForLanguage(word, languageTag)
            val incorrect = allTranslations
                .asSequence()
                .filter { it != correctTranslation }
                .shuffled(random)
                .take(5)
                .toList()

            if (incorrect.size < 5) {
                return@mapNotNull null
            }

            GameQuestion(
                wordId = word.id,
                russian = word.russian,
                transcription = word.transcription,
                correctEnglish = correctTranslation,
                translations = WordLocalization.translationsForLanguage(word, languageTag),
                examples = WordLocalization.decodeList(word.examplesData),
                options = (incorrect + correctTranslation).distinct().shuffled(random),
                streakBeforeAnswer = word.correctStreak
            )
        }
    }

    suspend fun recordAnswer(question: GameQuestion, isCorrect: Boolean): Boolean = withContext(Dispatchers.IO) {
        val word = dao.getById(question.wordId)
        val nextStreak = if (isCorrect) 1 else 0
        val learnedNow = isCorrect && !word.isLearned
        val learnedAt = if (learnedNow) System.currentTimeMillis() else word.learnedAtEpochMillis
        dao.updateWord(
            word.copy(
                correctStreak = nextStreak,
                timesShown = word.timesShown + 1,
                timesCorrect = word.timesCorrect + if (isCorrect) 1 else 0,
                isLearned = word.isLearned || isCorrect,
                learnedAtEpochMillis = learnedAt
            )
        )
        learnedNow
    }

    suspend fun confirmRound(summary: RoundSummary, keepLearnedIds: List<Long>) = withContext(Dispatchers.IO) {
        val idsToReset = summary.newlyLearnedWordIds.filterNot { it in keepLearnedIds }
        if (idsToReset.isNotEmpty()) {
            dao.resetWordsToPractice(idsToReset)
        }
        dao.insertSessionResult(
            SessionResultEntity(
                playedAtEpochMillis = System.currentTimeMillis(),
                totalQuestions = summary.totalQuestions,
                correctAnswers = summary.correctAnswers,
                learnedWordsCount = keepLearnedIds.size,
                durationMillis = summary.durationMillis
            )
        )
    }

    suspend fun resetAllLocalProgress() = withContext(Dispatchers.IO) {
        dao.resetAllWordProgress()
        dao.clearSessionHistory()
    }

    suspend fun resetWordToPractice(wordId: Long) = withContext(Dispatchers.IO) {
        dao.resetWordsToPractice(listOf(wordId))
    }

    private fun SessionResultEntity.toSummary(): SessionResultSummary {
        return SessionResultSummary(
            id = id,
            playedAtEpochMillis = playedAtEpochMillis,
            totalQuestions = totalQuestions,
            correctAnswers = correctAnswers,
            learnedWordsCount = learnedWordsCount,
            durationMillis = durationMillis
        )
    }

    private data class WordKey(
        val blockId: String,
        val russian: String
    )

    private data class LocalizedTranslations(
        val fr: List<String>,
        val es: List<String>,
        val pt: List<String>,
        val ar: List<String>,
        val zh: List<String>
    )

}
