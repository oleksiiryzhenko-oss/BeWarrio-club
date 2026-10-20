package com.katadojo.app.ui.kata

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.R
import com.katadojo.app.data.model.PerceivedDifficulty
import com.katadojo.app.data.model.SectionStatus
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.utils.KataScoring
import com.katadojo.app.utils.StopwatchState
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.CoachPhase
import com.katadojo.app.viewmodel.CoachUiState
import com.katadojo.app.viewmodel.KataCoachViewModel

@Composable
fun KataCoachScreen(
    onExit: () -> Unit,
    onDone: (kataId: String) -> Unit,
    viewModel: KataCoachViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state = viewModel.uiState
    val kata = state.kata
    var confirmExit by rememberSaveable { mutableStateOf(false) }

    val inProgress = state.phase != CoachPhase.RESULT &&
        (state.selections.any { it != null } || state.timerState != StopwatchState.IDLE)
    BackHandler { if (inProgress) confirmExit = true else onExit() }

    val view = LocalView.current
    val keepOn = state.settings.keepScreenOn && state.phase != CoachPhase.RESULT
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    Column(Modifier.fillMaxSize().background(Ink)) {
        DojoTopBar(
            title = if (state.isSectionPractice) "Section Practice" else "Kata Session",
            onBack = { if (inProgress) confirmExit = true else onExit() }
        )
        if (kata == null) {
            Box(Modifier.padding(16.dp)) { EmptyState("Kata not found", "Choose a kata from the library.", actionLabel = "Back", onAction = onExit) }
            return@Column
        }
        when (state.phase) {
            CoachPhase.SECTIONS -> SectionsPhase(state, viewModel)
            CoachPhase.ASSESSMENT -> AssessmentPhase(state, viewModel)
            CoachPhase.RESULT -> ResultPhase(state, onViewKata = { onDone(kata.id) }, onExit = onExit)
        }
    }

    if (confirmExit) {
        ConfirmDialog(
            title = "Leave this session?",
            text = "Your section results and timer for this session will not be saved.",
            confirmLabel = "Leave",
            dismissLabel = "Keep practicing",
            onConfirm = {
                confirmExit = false
                onExit()
            },
            onDismiss = { confirmExit = false }
        )
    }
}

@Composable
private fun ColumnScope.SectionsPhase(state: CoachUiState, viewModel: KataCoachViewModel) {
    val kata = state.kata ?: return
    val section = state.currentSection ?: return
    val haptics = LocalHapticFeedback.current

    LazyColumn(
        Modifier.weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Overline(if (state.isSectionPractice) "Section practice" else "Kata session", color = DojoRed)
                Text(kata.name, style = MaterialTheme.typography.headlineLarge, color = Rice)
                Text(
                    if (state.isSectionPractice) "Section ${section.number} of ${kata.sectionCount}"
                    else "Section ${state.position + 1} / ${state.sectionIndexes.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = StoneLight
                )
            }
        }
        if (!state.isSectionPractice) {
            item { SectionDots(state, onSelect = viewModel::goTo) }
        }
        item { TimerCard(state, viewModel) }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Charcoal)
            ) {
                TatamiTexture(Modifier.matchParentSize(), alpha = 0.04f)
                Enso(Modifier.size(120.dp).align(Alignment.TopEnd).offset(x = 24.dp, y = (-18).dp), alpha = 0.25f)
                Column(Modifier.padding(20.dp)) {
                    Overline("Section ${section.number}", color = Beige)
                    Spacer(Modifier.height(4.dp))
                    Text(section.title, style = MaterialTheme.typography.headlineMedium, color = Rice)
                    Spacer(Modifier.height(8.dp))
                    Text(section.cue, style = MaterialTheme.typography.bodyMedium, color = StoneLight)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionStatus.entries.forEach { status ->
                    StatusButton(status, selected = state.currentSelection == status) {
                        if (state.settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.select(status)
                    }
                }
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().background(Charcoal).navigationBarsPadding().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!state.isSectionPractice) {
            SecondaryButton("Previous", onClick = viewModel::previous, modifier = Modifier.weight(1f), enabled = state.position > 0)
        }
        when {
            state.isSectionPractice -> PrimaryButton(
                if (state.saving) "Saving..." else "Finish Practice",
                onClick = viewModel::finish,
                modifier = Modifier.weight(1f),
                enabled = state.currentSelection != null && !state.saving
            )
            state.isLast -> PrimaryButton(
                "Rate Kata",
                onClick = viewModel::next,
                modifier = Modifier.weight(1.4f),
                enabled = state.allSelected
            )
            else -> PrimaryButton(
                "Next Section",
                onClick = viewModel::next,
                modifier = Modifier.weight(1.4f),
                enabled = state.currentSelection != null
            )
        }
    }
}

@Composable
private fun SectionDots(state: CoachUiState, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        state.selections.forEachIndexed { i, status ->
            val current = i == state.position
            Box(
                Modifier
                    .size(if (current) 34.dp else 28.dp)
                    .clip(CircleShape)
                    .background(if (status != null) statusColor(status) else CharcoalLight)
                    .border(if (current) 2.dp else 0.dp, if (current) Rice else Color.Transparent, CircleShape)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = if (status != null) Ink else StoneLight)
            }
        }
    }
}

@Composable
private fun TimerCard(state: CoachUiState, viewModel: KataCoachViewModel) {
    DojoCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Overline("Kata timer · optional")
                Text(
                    Formatters.clock((state.elapsedMs / 1000).toInt()),
                    style = MaterialTheme.typography.displayMedium,
                    color = when (state.timerState) {
                        StopwatchState.RUNNING -> Rice
                        StopwatchState.FINISHED -> Beige
                        else -> StoneLight
                    }
                )
                Text(
                    when (state.timerState) {
                        StopwatchState.IDLE -> "Not started"
                        StopwatchState.RUNNING -> "Running"
                        StopwatchState.PAUSED -> "Paused"
                        StopwatchState.FINISHED -> "Finished"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.timerState == StopwatchState.RUNNING) DojoRed else Stone
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state.timerState) {
                StopwatchState.IDLE -> PrimaryButton("Start", onClick = viewModel::startTimer, modifier = Modifier.weight(1f), icon = painterResource(R.drawable.ic_timer), height = 46.dp)
                StopwatchState.RUNNING -> {
                    SecondaryButton("Pause", onClick = viewModel::pauseTimer, modifier = Modifier.weight(1f), icon = painterResource(R.drawable.ic_pause), height = 46.dp)
                    SecondaryButton("Restart", onClick = viewModel::restartTimer, modifier = Modifier.weight(1f), height = 46.dp, color = StoneLight)
                    SecondaryButton("Finish", onClick = viewModel::finishTimer, modifier = Modifier.weight(1f), height = 46.dp, color = Beige)
                }
                StopwatchState.PAUSED -> {
                    PrimaryButton("Resume", onClick = viewModel::resumeTimer, modifier = Modifier.weight(1f), height = 46.dp)
                    SecondaryButton("Restart", onClick = viewModel::restartTimer, modifier = Modifier.weight(1f), height = 46.dp, color = StoneLight)
                    SecondaryButton("Finish", onClick = viewModel::finishTimer, modifier = Modifier.weight(1f), height = 46.dp, color = Beige)
                }
                StopwatchState.FINISHED -> SecondaryButton("Restart Timer", onClick = viewModel::restartTimer, modifier = Modifier.weight(1f), height = 46.dp, color = StoneLight)
            }
        }
    }
}

@Composable
private fun StatusButton(status: SectionStatus, selected: Boolean, onClick: () -> Unit) {
    val color = statusColor(status)
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.18f) else Charcoal)
            .border(if (selected) 2.dp else 1.dp, if (selected) color else CharcoalBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(status.label.uppercase(), style = MaterialTheme.typography.labelLarge, color = Rice)
            Text(
                when (status) {
                    SectionStatus.COMPLETED -> "Clean and confident"
                    SectionStatus.NEEDS_PRACTICE -> "Mostly right, some errors"
                    SectionStatus.DIFFICULT -> "Lost the sequence or balance"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Stone
            )
        }
        if (selected) Text("✓", style = MaterialTheme.typography.titleLarge, color = color)
    }
}

@Composable
private fun ColumnScope.AssessmentPhase(state: CoachUiState, viewModel: KataCoachViewModel) {
    val kata = state.kata ?: return
    val preview = KataScoring.personalScore(state.technique, state.balance, state.timing, state.confidence)
    LazyColumn(
        Modifier.weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Overline("Self-assessment", color = DojoRed)
                Text(kata.name, style = MaterialTheme.typography.headlineLarge, color = Rice)
                Text(
                    "${state.selections.count { it == SectionStatus.COMPLETED }} of ${state.selections.size} sections completed · " +
                        Formatters.duration((state.elapsedMs / 1000).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Stone
                )
            }
        }
        item {
            DojoCard {
                RatingSlider("Technique", state.technique) { viewModel.setRating(0, it) }
                RatingSlider("Balance", state.balance) { viewModel.setRating(1, it) }
                RatingSlider("Timing", state.timing) { viewModel.setRating(2, it) }
                RatingSlider("Confidence", state.confidence) { viewModel.setRating(3, it) }
            }
        }
        item {
            DojoCard {
                Overline("How difficult was this kata?")
                Spacer(Modifier.height(10.dp))
                ChipGrid(
                    options = PerceivedDifficulty.entries,
                    selected = state.difficulty,
                    label = { it.label },
                    onSelect = viewModel::setDifficulty
                )
            }
        }
        item {
            DojoCard(containerColor = CharcoalLight) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Overline("Personal Kata Score")
                        Text("Average of your four ratings", style = MaterialTheme.typography.bodySmall, color = Stone)
                    }
                    ScoreBadge(preview)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "This is a personal self-assessment metric, not an official instructor grade.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StoneDark
                )
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().background(Charcoal).navigationBarsPadding().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SecondaryButton("Sections", onClick = viewModel::previous, modifier = Modifier.weight(1f))
        PrimaryButton(
            if (state.saving) "Saving..." else "Finish Kata",
            onClick = viewModel::finish,
            modifier = Modifier.weight(1.4f),
            enabled = state.canFinish && !state.saving
        )
    }
}

@Composable
private fun RatingSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Rice, modifier = Modifier.weight(1f))
            Text("$value / 10", style = MaterialTheme.typography.titleMedium, color = scoreColor(value.toDouble()))
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = Rice,
                activeTrackColor = DojoRed,
                inactiveTrackColor = CharcoalBorder,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun ColumnScope.ResultPhase(state: CoachUiState, onViewKata: () -> Unit, onExit: () -> Unit) {
    val kata = state.kata ?: return
    val result = state.result
    LazyColumn(
        Modifier.weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (result == null) {
            item { LoadingBox(Modifier.height(240.dp)) }
        } else {
            item { SessionSummaryCard(result.session, kata, result.analysis) }
            item { AnalysisCard(result.session, result.analysis) }
            if (result.session.isFullKata) item { SectionResultsList(kata, result.session) }
            item { HistoricalInsight(result.progress) }
            item { RewardsCard(result.rewards) }
            item {
                Text(
                    "Saved to your training history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Stone,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    Column(
        Modifier.fillMaxWidth().background(Charcoal).navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PrimaryButton("View Kata Progress", onClick = onViewKata)
        SecondaryButton("Done", onClick = onExit, height = 46.dp)
    }
}
