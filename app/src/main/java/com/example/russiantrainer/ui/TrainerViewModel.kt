package com.example.russiantrainer.ui

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.russiantrainer.data.WordRepository
import com.example.russiantrainer.domain.DailyLearnedStat
import com.example.russiantrainer.domain.GameQuestion
import com.example.russiantrainer.domain.RoundSummary
import com.example.russiantrainer.domain.SessionResultSummary
import com.example.russiantrainer.domain.WordProgressSummary
import com.example.russiantrainer.data.WordEntity
import com.example.russiantrainer.ui.theme.AppTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val currentBlock: VocabularyBlock = VocabularyBlock.BEGINNER_1000,
    val learnedCount: Int = 0,
    val activeCount: Int = 0,
    val playedRoundsCount: Int = 0,
    val roundSize: Int = 30,
    val isLoading: Boolean = true,
    val recentSessions: List<SessionResultSummary> = emptyList()
)

enum class LibraryFilter {
    ALL,
    LEARNING,
    LEARNED
}

data class LibraryState(
    val currentBlock: VocabularyBlock = VocabularyBlock.BEGINNER_1000,
    val words: List<WordProgressSummary> = emptyList(),
    val searchQuery: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL
)

data class StatisticsState(
    val currentBlock: VocabularyBlock = VocabularyBlock.BEGINNER_1000,
    val totalTimeMillis: Long = 0,
    val learnedToday: Int = 0,
    val learnedThisWeek: Int = 0,
    val dailyLearned: List<DailyLearnedStat> = emptyList(),
    val remainingWordsInBlock: Int = 0,
    val learnedWordsInBlock: Int = 0,
    val totalWordsInBlock: Int = 0
)

data class RoundState(
    val questions: List<GameQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOption: String? = null,
    val isCurrentAnswerCorrect: Boolean? = null,
    val wrongOptions: Set<String> = emptySet(),
    val isExitConfirmationVisible: Boolean = false,
    val learnedThisRound: Set<Long> = emptySet(),
    val correctAnswers: Int = 0,
    val startedAtEpochMillis: Long = System.currentTimeMillis()
) {
    val currentQuestion: GameQuestion? = questions.getOrNull(currentIndex)
}

sealed interface ScreenState {
    data object Home : ScreenState
    data object BlockSelection : ScreenState
    data object Statistics : ScreenState
    data object Library : ScreenState
    data class Playing(val roundState: RoundState) : ScreenState
    data class Results(
        val summary: RoundSummary,
        val learnedWords: List<GameQuestion>,
        val selectedLearnedWordIds: Set<Long>
    ) : ScreenState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrainerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WordRepository.getInstance(application)
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Home)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()
    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()
    private val _selectedBlock = MutableStateFlow(loadBlock())
    private val _librarySearchQuery = MutableStateFlow("")
    private val _libraryFilter = MutableStateFlow(LibraryFilter.ALL)
    private val _isSeeding = MutableStateFlow(true)

    private val learnedCountFlow = _selectedBlock.flatMapLatest { block ->
        repository.observeLearnedCount(block.id)
    }

    private val activeCountFlow = _selectedBlock.flatMapLatest { block ->
        repository.observeActiveCount(block.id)
    }

    private val libraryWordsFlow = _selectedBlock.flatMapLatest { block ->
        _language.flatMapLatest { language ->
            repository.observeWordLibrary(block.id, language.tag)
        }
    }

    private val dashboardMetricsFlow = combine(
        learnedCountFlow,
        activeCountFlow,
        repository.observePlayedRoundsCount(),
        repository.observeRecentSessionResults(),
        _isSeeding
    ) { learned, active, rounds, recentSessions, isSeeding ->
        DashboardState(
            learnedCount = learned,
            activeCount = active,
            playedRoundsCount = rounds,
            roundSize = 30,
            isLoading = isSeeding,
            recentSessions = recentSessions
        )
    }

    val dashboardState: StateFlow<DashboardState> = combine(
        _selectedBlock,
        dashboardMetricsFlow
    ) { block, metrics ->
        metrics.copy(currentBlock = block)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardState()
    )

    val statisticsState: StateFlow<StatisticsState> = combine(
        _selectedBlock,
        learnedCountFlow,
        activeCountFlow,
        repository.observeAllSessionResults(),
        repository.observeAllWordsAcrossBlocks()
    ) { block, learnedCount, activeCount, sessionResults, words ->
        buildStatisticsState(
            block = block,
            learnedCount = learnedCount,
            activeCount = activeCount,
            sessionResults = sessionResults,
            words = words
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsState()
    )

    val libraryState: StateFlow<LibraryState> = combine(
        _selectedBlock,
        _librarySearchQuery,
        _libraryFilter,
        libraryWordsFlow
    ) { block, query, filter, words ->
        val normalizedQuery = query.trim().lowercase()
        val filteredWords = words
            .asSequence()
            .filter { word ->
                when (filter) {
                    LibraryFilter.ALL -> true
                    LibraryFilter.LEARNING -> !word.isLearned
                    LibraryFilter.LEARNED -> word.isLearned
                }
            }
            .filter { word ->
                normalizedQuery.isBlank() ||
                    word.russian.lowercase().contains(normalizedQuery) ||
                    word.english.lowercase().contains(normalizedQuery) ||
                    word.transcription.lowercase().contains(normalizedQuery) ||
                    word.translations.any { it.lowercase().contains(normalizedQuery) }
            }
            .toList()

        LibraryState(
            currentBlock = block,
            words = filteredWords,
            searchQuery = query,
            filter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryState()
    )

    init {
        applyLanguage(_language.value)
        viewModelScope.launch {
            try {
                repository.seedIfNeeded()
            } finally {
                _isSeeding.value = false
            }
        }
    }

    fun selectLanguage(language: AppLanguage) {
        if (_language.value == language) return
        _language.value = language
        prefs.edit().putString("language", language.tag).apply()
        applyLanguage(language)
    }

    fun selectTheme(theme: AppTheme) {
        if (_theme.value == theme) return
        _theme.value = theme
        prefs.edit().putString("theme", theme.key).apply()
    }

    fun selectBlock(block: VocabularyBlock) {
        _selectedBlock.value = block
        prefs.edit().putString("vocabulary_block", block.id).apply()
        _librarySearchQuery.value = ""
        _libraryFilter.value = LibraryFilter.ALL
        _screenState.value = ScreenState.Home
    }

    fun openBlockSelection() {
        _screenState.value = ScreenState.BlockSelection
    }

    fun openStatistics() {
        _screenState.value = ScreenState.Statistics
    }

    fun startRound() {
        viewModelScope.launch {
            val questions = repository.buildRound(
                blockId = _selectedBlock.value.id,
                languageTag = _language.value.tag,
                roundSize = 30
            )
            if (questions.isNotEmpty()) {
                _screenState.value = ScreenState.Playing(
                    RoundState(
                        questions = questions,
                        startedAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun openLibrary() {
        _screenState.value = ScreenState.Library
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _libraryFilter.value = filter
    }

    fun updateLibrarySearchQuery(query: String) {
        _librarySearchQuery.value = query
    }

    fun submitAnswer(option: String) {
        val current = _screenState.value as? ScreenState.Playing ?: return
        val question = current.roundState.currentQuestion ?: return
        if (current.roundState.selectedOption != null) return
        if (option in current.roundState.wrongOptions) return

        val isCorrect = option == question.correctEnglish
        if (!isCorrect) {
            _screenState.value = ScreenState.Playing(
                current.roundState.copy(
                    isCurrentAnswerCorrect = false,
                    wrongOptions = current.roundState.wrongOptions + option
                )
            )
            return
        }

        viewModelScope.launch {
            val learnedNow = repository.recordAnswer(question, true)
            _screenState.value = ScreenState.Playing(
                current.roundState.copy(
                    selectedOption = option,
                    isCurrentAnswerCorrect = true,
                    correctAnswers = current.roundState.correctAnswers + 1,
                    learnedThisRound = current.roundState.learnedThisRound + if (learnedNow) setOf(question.wordId) else emptySet()
                )
            )
        }
    }

    fun moveNext() {
        val current = _screenState.value as? ScreenState.Playing ?: return
        val roundState = current.roundState
        if (roundState.selectedOption == null) return

        val nextIndex = roundState.currentIndex + 1
        if (nextIndex >= roundState.questions.size) {
            val learnedQuestions = roundState.questions.filter { it.wordId in roundState.learnedThisRound }
            val summary = RoundSummary(
                totalQuestions = roundState.questions.size,
                correctAnswers = roundState.correctAnswers,
                newlyLearnedWordIds = roundState.learnedThisRound.toList(),
                durationMillis = System.currentTimeMillis() - roundState.startedAtEpochMillis
            )
            _screenState.value = ScreenState.Results(
                summary = summary,
                learnedWords = learnedQuestions,
                selectedLearnedWordIds = learnedQuestions.map { it.wordId }.toSet()
            )
        } else {
            _screenState.value = ScreenState.Playing(
                roundState.copy(
                    currentIndex = nextIndex,
                    selectedOption = null,
                    isCurrentAnswerCorrect = null,
                    wrongOptions = emptySet(),
                    isExitConfirmationVisible = false
                )
            )
        }
    }

    fun requestExitFromRound() {
        val current = _screenState.value as? ScreenState.Playing ?: return
        _screenState.value = ScreenState.Playing(
            current.roundState.copy(isExitConfirmationVisible = true)
        )
    }

    fun dismissExitFromRound() {
        val current = _screenState.value as? ScreenState.Playing ?: return
        _screenState.value = ScreenState.Playing(
            current.roundState.copy(isExitConfirmationVisible = false)
        )
    }

    fun confirmExitFromRound() {
        _screenState.value = ScreenState.Home
    }

    fun toggleLearnedWord(wordId: Long, checked: Boolean) {
        val current = _screenState.value as? ScreenState.Results ?: return
        val updated = current.selectedLearnedWordIds.toMutableSet().apply {
            if (checked) add(wordId) else remove(wordId)
        }
        _screenState.value = current.copy(selectedLearnedWordIds = updated)
    }

    fun confirmLearnedWords() {
        val current = _screenState.value as? ScreenState.Results ?: return
        viewModelScope.launch {
            repository.confirmRound(current.summary, current.selectedLearnedWordIds.toList())
            _screenState.value = ScreenState.Home
        }
    }

    fun returnHome() {
        _screenState.value = ScreenState.Home
    }

    fun resetAllLocalProgress() {
        viewModelScope.launch {
            repository.resetAllLocalProgress()
            _screenState.value = ScreenState.Home
        }
    }

    fun resetWordToPractice(wordId: Long) {
        viewModelScope.launch {
            repository.resetWordToPractice(wordId)
        }
    }

    private fun loadLanguage(): AppLanguage {
        return AppLanguage.fromTag(prefs.getString("language", AppLanguage.RU.tag))
    }

    private fun loadTheme(): AppTheme {
        return AppTheme.fromKey(prefs.getString("theme", AppTheme.BLOSSOM.key))
    }

    private fun loadBlock(): VocabularyBlock {
        return VocabularyBlock.fromId(prefs.getString("vocabulary_block", VocabularyBlock.BEGINNER_1000.id))
    }

    private fun applyLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    private fun buildStatisticsState(
        block: VocabularyBlock,
        learnedCount: Int,
        activeCount: Int,
        sessionResults: List<SessionResultSummary>,
        words: List<WordEntity>
    ): StatisticsState {
        val zoneId = ZoneId.systemDefault()
        val today = Instant.now().atZone(zoneId).toLocalDate()
        val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val lastSevenDays = (6L downTo 0L).map { today.minusDays(it) }

        val sessionsByDate = sessionResults.groupBy {
            Instant.ofEpochMilli(it.playedAtEpochMillis).atZone(zoneId).toLocalDate()
        }
        val sessionLearnedByDate = sessionsByDate.mapValues { (_, items) ->
            items.sumOf { it.learnedWordsCount }
        }

        val learnedWordsByDate = words
            .asSequence()
            .filter { it.isLearned }
            .mapNotNull { word ->
                word.learnedAtEpochMillis?.let { learnedAt ->
                    Instant.ofEpochMilli(learnedAt).atZone(zoneId).toLocalDate()
                }
            }
            .groupingBy { it }
            .eachCount()

        val dailyLearned = lastSevenDays.map { date ->
            DailyLearnedStat(
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                learnedWords = maxOf(
                    learnedWordsByDate[date] ?: 0,
                    sessionLearnedByDate[date] ?: 0
                )
            )
        }

        val learnedToday = maxOf(
            learnedWordsByDate[today] ?: 0,
            sessionLearnedByDate[today] ?: 0
        )

        val learnedThisWeek = lastSevenDays
            .filter { it >= startOfWeek }
            .sumOf { date ->
                maxOf(
                    learnedWordsByDate[date] ?: 0,
                    sessionLearnedByDate[date] ?: 0
                )
            }

        return StatisticsState(
            currentBlock = block,
            totalTimeMillis = sessionResults.sumOf { it.durationMillis },
            learnedToday = learnedToday,
            learnedThisWeek = learnedThisWeek,
            dailyLearned = dailyLearned,
            remainingWordsInBlock = activeCount,
            learnedWordsInBlock = learnedCount,
            totalWordsInBlock = activeCount + learnedCount
        )
    }
}
