package com.example.russiantrainer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words WHERE blockId = :blockId")
    suspend fun countByBlock(blockId: String): Int

    @Query("SELECT * FROM words WHERE blockId = :blockId ORDER BY russian COLLATE NOCASE ASC")
    suspend fun getAllWordsForBlock(blockId: String): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words WHERE blockId = :blockId AND isLearned = 1")
    fun observeLearnedCount(blockId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE blockId = :blockId AND isLearned = 0")
    fun observeActiveCount(blockId: String): Flow<Int>

    @Query(
        """
        SELECT * FROM words
        WHERE blockId = :blockId AND isLearned = 0
        ORDER BY correctStreak ASC, timesShown ASC, russian COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun getActiveWords(blockId: String, limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WordEntity

    @Query(
        """
        SELECT * FROM words
        WHERE blockId = :blockId
        ORDER BY isLearned DESC, correctStreak DESC, russian COLLATE NOCASE ASC
        """
    )
    fun observeAllWords(blockId: String): Flow<List<WordEntity>>

    @Query(
        """
        SELECT * FROM words
        ORDER BY blockId ASC, russian COLLATE NOCASE ASC
        """
    )
    fun observeAllWordsAcrossBlocks(): Flow<List<WordEntity>>

    @Update
    suspend fun updateWord(word: WordEntity)

    @Query("DELETE FROM words WHERE blockId = :blockId")
    suspend fun deleteWordsByBlock(blockId: String)

    @Query(
        "UPDATE words SET isLearned = 0, correctStreak = 0, learnedAtEpochMillis = NULL WHERE id IN (:ids)"
    )
    suspend fun resetWordsToPractice(ids: List<Long>)

    @Query("UPDATE words SET isLearned = 0, correctStreak = 0, timesShown = 0, timesCorrect = 0, learnedAtEpochMillis = NULL")
    suspend fun resetAllWordProgress()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionResult(result: SessionResultEntity)

    @Query(
        """
        SELECT * FROM session_results
        ORDER BY playedAtEpochMillis DESC
        LIMIT :limit
        """
    )
    fun observeRecentSessionResults(limit: Int): Flow<List<SessionResultEntity>>

    @Query("SELECT COUNT(*) FROM session_results")
    fun observePlayedRoundsCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM session_results
        ORDER BY playedAtEpochMillis DESC
        """
    )
    fun observeAllSessionResults(): Flow<List<SessionResultEntity>>

    @Query("DELETE FROM session_results")
    suspend fun clearSessionHistory()
}
