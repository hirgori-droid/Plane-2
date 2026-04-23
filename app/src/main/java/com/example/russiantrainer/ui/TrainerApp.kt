package com.example.russiantrainer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.russiantrainer.R
import com.example.russiantrainer.domain.GameQuestion
import com.example.russiantrainer.domain.SessionResultSummary
import com.example.russiantrainer.domain.WordProgressSummary
import com.example.russiantrainer.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private data class AccentPalette(
    val cardColors: List<Color>,
    val chartCardColor: Color,
    val chartTitleColor: Color,
    val chartSubtitleColor: Color,
    val chartBarColor: Color,
    val chartLabelColor: Color
)

@Composable
private fun rememberAccentPalette(): AccentPalette {
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = colors.background.red + colors.background.green + colors.background.blue < 1.2f

    return if (isDarkTheme) {
        AccentPalette(
            cardColors = listOf(
                colors.primaryContainer,
                colors.surfaceVariant,
                colors.secondaryContainer,
                colors.tertiaryContainer
            ),
            chartCardColor = colors.primaryContainer,
            chartTitleColor = colors.onPrimaryContainer,
            chartSubtitleColor = colors.onPrimaryContainer.copy(alpha = 0.82f),
            chartBarColor = colors.surface.copy(alpha = 0.92f),
            chartLabelColor = colors.onPrimaryContainer.copy(alpha = 0.88f)
        )
    } else {
        AccentPalette(
            cardColors = listOf(
                Color(0xFFF1E7FF),
                Color(0xFFEAF4FF),
                Color(0xFFFFE8F2),
                Color(0xFFEAFBF4)
            ),
            chartCardColor = Color(0xFF7EBBF0),
            chartTitleColor = Color(0xFF11263E),
            chartSubtitleColor = Color(0xFF26435F),
            chartBarColor = Color(0xFFF8FBFF),
            chartLabelColor = Color(0xFF26435F)
        )
    }
}

@Composable
fun TrainerApp(viewModel: TrainerViewModel) {
    val screenState by viewModel.screenState.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()
    val libraryState by viewModel.libraryState.collectAsState()
    val statisticsState by viewModel.statisticsState.collectAsState()
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()

    BackHandler(enabled = screenState != ScreenState.Home) {
        when (screenState) {
            ScreenState.Home -> Unit
            ScreenState.BlockSelection,
            ScreenState.Statistics,
            ScreenState.Library,
            is ScreenState.Results -> viewModel.returnHome()
            is ScreenState.Playing -> viewModel.requestExitFromRound()
        }
    }

    Scaffold { padding ->
        when (val state = screenState) {
            ScreenState.Home -> HomeScreen(
                modifier = Modifier.padding(padding),
                dashboardState = dashboardState,
                onStart = viewModel::startRound,
                onOpenLibrary = viewModel::openLibrary,
                onOpenBlockSelection = viewModel::openBlockSelection,
                onOpenStatistics = viewModel::openStatistics,
                selectedLanguage = language,
                onSelectLanguage = viewModel::selectLanguage,
                selectedTheme = theme,
                onSelectTheme = viewModel::selectTheme,
                onResetProgress = viewModel::resetAllLocalProgress
            )

            ScreenState.BlockSelection -> BlockSelectionScreen(
                modifier = Modifier.padding(padding),
                currentBlock = dashboardState.currentBlock,
                onBack = viewModel::returnHome,
                onSelectBlock = viewModel::selectBlock
            )

            ScreenState.Statistics -> StatisticsScreen(
                modifier = Modifier.padding(padding),
                state = statisticsState,
                onBack = viewModel::returnHome
            )

            ScreenState.Library -> LibraryScreen(
                modifier = Modifier.padding(padding),
                state = libraryState,
                onBack = viewModel::returnHome,
                onSearchQueryChanged = viewModel::updateLibrarySearchQuery,
                onFilterChanged = viewModel::setLibraryFilter,
                onResetWord = viewModel::resetWordToPractice
            )

            is ScreenState.Playing -> RoundScreen(
                modifier = Modifier.padding(padding),
                state = state.roundState,
                onAnswer = viewModel::submitAnswer,
                onNext = viewModel::moveNext,
                onBack = viewModel::requestExitFromRound,
                onDismissExit = viewModel::dismissExitFromRound,
                onConfirmExit = viewModel::confirmExitFromRound
            )

            is ScreenState.Results -> ResultsScreen(
                modifier = Modifier.padding(padding),
                state = state,
                onToggleWord = viewModel::toggleLearnedWord,
                onConfirm = viewModel::confirmLearnedWords,
                onPlayAgain = viewModel::startRound,
                onBack = viewModel::returnHome
            )
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    dashboardState: DashboardState,
    onStart: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenBlockSelection: () -> Unit,
    onOpenStatistics: () -> Unit,
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    selectedTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
    onResetProgress: () -> Unit
) {
    val hasAnyWords = dashboardState.learnedCount + dashboardState.activeCount > 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                LanguageMenuButton(
                    selectedLanguage = selectedLanguage,
                    onSelectLanguage = onSelectLanguage
                )
                SettingsMenuButton(
                    selectedTheme = selectedTheme,
                    onSelectTheme = onSelectTheme
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.hero_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.current_block_label) + ": " + stringResource(dashboardState.currentBlock.titleResId),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ElevatedButton(
                            onClick = onStart,
                            enabled = hasAnyWords,
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        ) {
                            Text(stringResource(R.string.start_game))
                        }
                        IconButton(
                            onClick = onOpenLibrary,
                            modifier = Modifier
                                .size(52.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.open_word_library)
                            )
                        }
                    }
                }
            }
        }
        item {
            BlockSelectorCard(
                currentBlock = dashboardState.currentBlock,
                onOpenBlockSelection = onOpenBlockSelection
            )
        }
        item {
            StatisticsEntryCard(onOpenStatistics = onOpenStatistics)
        }
        if (dashboardState.recentSessions.isNotEmpty()) {
            item {
                SessionHistoryCard(sessions = dashboardState.recentSessions)
            }
        }
        item {
            OutlinedButton(
                onClick = onResetProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.reset_local_progress))
            }
        }
        if (!dashboardState.isLoading) {
            val emptyMessageRes = when {
                !hasAnyWords -> R.string.empty_unseeded_block
                dashboardState.activeCount == 0 -> R.string.empty_word_pool
                else -> null
            }
            if (emptyMessageRes != null) {
                item {
                    Card {
                        Text(
                            text = stringResource(emptyMessageRes),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockSelectorCard(
    currentBlock: VocabularyBlock,
    onOpenBlockSelection: () -> Unit
) {
    Card(onClick = onOpenBlockSelection) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.block_selector_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.block_selector_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.block_selector_current,
                            stringResource(currentBlock.titleResId),
                            currentBlock.displayWordCount
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun LanguageMenuButton(
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            androidx.compose.material3.Icon(
                Icons.Rounded.Language,
                contentDescription = stringResource(R.string.language_menu_title)
            )
            Spacer(Modifier.width(4.dp))
            Text(selectedLanguage.nativeLabel)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.nativeLabel) },
                    onClick = {
                        expanded = false
                        onSelectLanguage(language)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuButton(
    selectedTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            androidx.compose.material3.Icon(
                Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings_menu_title)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.theme_menu_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                onClick = {}
            )
            AppTheme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = {
                        val suffix = if (theme == selectedTheme) {
                            " • " + stringResource(R.string.selected_theme_suffix)
                        } else {
                            ""
                        }
                        Text(stringResource(theme.labelResId) + suffix)
                    },
                    onClick = {
                        expanded = false
                        onSelectTheme(theme)
                    }
                )
            }
        }
    }
}

@Composable
private fun BlockSelectionScreen(
    modifier: Modifier = Modifier,
    currentBlock: VocabularyBlock,
    onBack: () -> Unit,
    onSelectBlock: (VocabularyBlock) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back_home)
                    )
                }
                Text(
                    text = stringResource(R.string.block_selection_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.block_selection_screen_body),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        itemsIndexed(VocabularyBlock.entries, key = { _, block -> block.id }) { index, block ->
            BlockOptionCard(
                block = block,
                colorIndex = index,
                isSelected = block == currentBlock,
                onSelectBlock = onSelectBlock
            )
        }
    }
}

@Composable
private fun BlockOptionCard(
    block: VocabularyBlock,
    colorIndex: Int,
    isSelected: Boolean,
    onSelectBlock: (VocabularyBlock) -> Unit
) {
    val accentPalette = rememberAccentPalette()
    val baseColor = accentPalette.cardColors[colorIndex % accentPalette.cardColors.size]
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onSelectBlock(block) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            } else {
                baseColor
            }
        )
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(block.titleResId),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.block_word_count_label, block.displayWordCount),
                style = MaterialTheme.typography.bodyLarge
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.block_selected_badge),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatisticsEntryCard(onOpenStatistics: () -> Unit, colorIndex: Int = 0) {
    val accentPalette = rememberAccentPalette()
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenStatistics,
        colors = CardDefaults.cardColors(
            containerColor = accentPalette.cardColors[colorIndex % accentPalette.cardColors.size]
        )
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.statistics_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.statistics_entry_body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatsCard(learnedCount: Int, activeCount: Int, playedRoundsCount: Int, roundSize: Int) {
    val total = max(learnedCount + activeCount, 1)
    val progress = learnedCount.toFloat() / total.toFloat()
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.current_progress), style = MaterialTheme.typography.titleLarge)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.words_mastered) + ": $learnedCount")
                Text(stringResource(R.string.words_left) + ": $activeCount")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.round_length) + ": $roundSize")
                Text(stringResource(R.string.played_rounds_count, playedRoundsCount))
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(sessions: List<SessionResultSummary>) {
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.recent_rounds_title),
                style = MaterialTheme.typography.titleLarge
            )
            sessions.forEach { session ->
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val accuracy = if (session.totalQuestions == 0) 0 else (session.correctAnswers * 100 / session.totalQuestions)
                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatter.format(Date(session.playedAtEpochMillis)), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.session_history_duration, formatDuration(session.durationMillis)))
                        Text(
                            text = stringResource(
                                R.string.session_history_score,
                                session.correctAnswers,
                                session.totalQuestions,
                                accuracy
                            )
                        )
                        Text(
                            text = stringResource(
                                R.string.session_history_learned,
                                session.learnedWordsCount
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsScreen(
    modifier: Modifier = Modifier,
    state: StatisticsState,
    onBack: () -> Unit
) {
    val accentPalette = rememberAccentPalette()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null
                    )
                }
                Text(
                    text = stringResource(R.string.statistics_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.statistics_total_time_short),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDurationClock(state.totalTimeMillis),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.current_block_label) + ": " + stringResource(state.currentBlock.titleResId),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.statistics_all_bases_caption),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticsMiniCard(
                    modifier = Modifier.weight(1f),
                    value = state.learnedToday.toString(),
                    label = stringResource(R.string.statistics_learned_today_short)
                )
                StatisticsMiniCard(
                    modifier = Modifier.weight(1f),
                    value = state.learnedThisWeek.toString(),
                    label = stringResource(R.string.statistics_learned_week_short)
                )
            }
        }
        item {
            Text(
                text = stringResource(
                    R.string.statistics_current_base_caption,
                    stringResource(state.currentBlock.titleResId)
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticsMiniCard(
                    modifier = Modifier.weight(1f),
                    value = state.remainingWordsInBlock.toString(),
                    label = stringResource(R.string.statistics_remaining_words_short)
                )
                StatisticsMiniCard(
                    modifier = Modifier.weight(1f),
                    value = "${state.learnedWordsInBlock}/${state.totalWordsInBlock}",
                    label = stringResource(R.string.statistics_progress_short)
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = accentPalette.chartCardColor
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.statistics_daily_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accentPalette.chartTitleColor
                        )
                        Text(
                            text = stringResource(R.string.statistics_daily_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = accentPalette.chartSubtitleColor
                        )
                    }
                    StatisticsBarChart(
                        state = state,
                        barColor = accentPalette.chartBarColor,
                        labelColor = accentPalette.chartLabelColor
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsMiniCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatisticsBarChart(state: StatisticsState) {
    StatisticsBarChart(
        state = state,
        barColor = Color(0xFFF8FBFF),
        labelColor = Color(0xFF26435F)
    )
}

@Composable
private fun StatisticsBarChart(
    state: StatisticsState,
    barColor: Color,
    labelColor: Color
) {
    val maxValue = (state.dailyLearned.maxOfOrNull { it.learnedWords } ?: 1).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        state.dailyLearned.forEach { stat ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stat.learnedWords.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((28 + (92f * stat.learnedWords / maxValue)).dp),
                        shape = RoundedCornerShape(14.dp),
                        color = barColor
                    ) {}
                }
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    modifier: Modifier = Modifier,
    state: LibraryState,
    onBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFilterChanged: (LibraryFilter) -> Unit,
    onResetWord: (Long) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.word_library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back_home))
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.current_block_label) + ": " + stringResource(state.currentBlock.titleResId),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.word_library_search)) }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filter == LibraryFilter.ALL,
                    onClick = { onFilterChanged(LibraryFilter.ALL) },
                    label = { Text(stringResource(R.string.filter_all_words)) }
                )
                FilterChip(
                    selected = state.filter == LibraryFilter.LEARNING,
                    onClick = { onFilterChanged(LibraryFilter.LEARNING) },
                    label = { Text(stringResource(R.string.filter_learning_words)) }
                )
                FilterChip(
                    selected = state.filter == LibraryFilter.LEARNED,
                    onClick = { onFilterChanged(LibraryFilter.LEARNED) },
                    label = { Text(stringResource(R.string.filter_learned_words)) }
                )
            }
        }
        if (state.words.isEmpty()) {
            val emptyMessageRes = if (
                state.searchQuery.isBlank() &&
                state.filter == LibraryFilter.ALL
            ) {
                R.string.empty_unseeded_block
            } else {
                R.string.word_library_empty
            }
            item {
                Card {
                    Text(
                        text = stringResource(emptyMessageRes),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.words, key = { it.id }) { word ->
                WordLibraryCard(word = word, onResetWord = onResetWord)
            }
        }
    }
}

@Composable
private fun WordLibraryCard(word: WordProgressSummary, onResetWord: (Long) -> Unit) {
    val accuracy = if (word.timesShown == 0) 0 else (word.timesCorrect * 100 / word.timesShown)
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(word.russian, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(word.transcription, style = MaterialTheme.typography.bodyMedium)
            Text(word.translations.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
            if (word.examples.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.word_examples_label) + ": " + word.examples.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = if (word.isLearned) stringResource(R.string.word_status_learned) else stringResource(R.string.word_status_learning),
                color = if (word.isLearned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Text(stringResource(R.string.word_library_streak, word.correctStreak))
            Text(stringResource(R.string.word_library_stats, word.timesCorrect, word.timesShown, accuracy))
            if (word.isLearned) {
                OutlinedButton(onClick = { onResetWord(word.id) }) {
                    Text(stringResource(R.string.return_word_to_practice))
                }
            }
        }
    }
}

@Composable
private fun RoundScreen(
    modifier: Modifier = Modifier,
    state: RoundState,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onDismissExit: () -> Unit,
    onConfirmExit: () -> Unit
) {
    val question = state.currentQuestion ?: return
    if (state.isExitConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onDismissExit,
            title = { Text(stringResource(R.string.exit_game_title)) },
            text = { Text(stringResource(R.string.exit_game_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmExit) {
                    Text(text = stringResource(R.string.exit_game_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExit) {
                    Text(text = stringResource(R.string.exit_game_dismiss))
                }
            }
        )
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            RoundHeader(
                progressText = stringResource(R.string.round_progress, state.currentIndex + 1, state.questions.size),
                onBack = onBack
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(question.russian, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(question.transcription, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        items(question.options.chunked(2)) { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowOptions.forEach { option ->
                    val isSelected = state.selectedOption == option
                    val isCorrectOption = option == question.correctEnglish
                    val isWrongChosen = option in state.wrongOptions
                    val defaultTextColor = MaterialTheme.colorScheme.onSurface
                    val correctContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    val wrongContainerColor = MaterialTheme.colorScheme.errorContainer
                    val containerColor = when {
                        isCorrectOption && state.selectedOption != null -> correctContainerColor
                        isWrongChosen -> wrongContainerColor
                        else -> MaterialTheme.colorScheme.surface
                    }
                    Button(
                        onClick = { onAnswer(option) },
                        enabled = !isSelected && !isWrongChosen,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = defaultTextColor,
                            disabledContainerColor = containerColor,
                            disabledContentColor = defaultTextColor
                        )
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = defaultTextColor
                        )
                    }
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            ElevatedButton(
                onClick = onNext,
                enabled = state.selectedOption != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
                    disabledContentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.88f)
                )
            ) {
                Text(
                    text = stringResource(R.string.next_word),
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
        if (state.isCurrentAnswerCorrect != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (state.isCurrentAnswerCorrect == true) {
                            stringResource(R.string.correct_answer)
                        } else {
                            stringResource(R.string.try_again_answer)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.selectedOption != null) {
                        Text(
                            text = question.translations.joinToString(", "),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (state.selectedOption != null && question.examples.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.word_examples_label) + ": " + question.examples.joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsScreen(
    modifier: Modifier = Modifier,
    state: ScreenState.Results,
    onToggleWord: (Long, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit
) {
    val accuracy = if (state.summary.totalQuestions == 0) 0 else (state.summary.correctAnswers * 100 / state.summary.totalQuestions)
    val encouragementRes = when {
        accuracy >= 95 -> R.string.encouragement_top
        accuracy >= 80 -> R.string.encouragement_high
        accuracy >= 60 -> R.string.encouragement_mid
        else -> R.string.encouragement_low
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.round_results),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.game_finished), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.score_line, state.summary.correctAnswers, state.summary.totalQuestions))
                    Text(stringResource(R.string.accuracy_line, accuracy))
                    Text(stringResource(R.string.newly_mastered_count, state.summary.newlyLearnedWordIds.size))
                    Text(stringResource(encouragementRes))
                }
            }
        }
        if (state.learnedWords.isNotEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.review_mastered_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(stringResource(R.string.review_mastered_body))
                        state.learnedWords.forEach { word ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(word.russian, fontWeight = FontWeight.SemiBold)
                                    Text(word.transcription)
                                    Text(word.translations.joinToString(", "))
                                }
                                Checkbox(
                                    checked = word.wordId in state.selectedLearnedWordIds,
                                    onCheckedChange = { checked -> onToggleWord(word.wordId, checked) }
                                )
                            }
                        }
                        ElevatedButton(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.confirm_mastered))
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ElevatedButton(onClick = onPlayAgain) {
                    Text(stringResource(R.string.play_again))
                }
                OutlinedButton(onClick = onBack) {
                    Text(stringResource(R.string.back_home))
                }
            }
        }
    }
}

@Composable
private fun RoundHeader(progressText: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = progressText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val seconds = (durationMillis / 1_000) % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        totalMinutes > 0 -> "${totalMinutes}m"
        else -> "${seconds}s"
    }
}

private fun formatDurationClock(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "%02d:%02d".format(hours, minutes)
}
