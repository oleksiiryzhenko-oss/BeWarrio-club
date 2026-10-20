package com.katadojo.app.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.R
import com.katadojo.app.data.model.WorkoutType
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.kata.RewardsCard
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.utils.StopwatchState
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.WorkoutDetailViewModel
import com.katadojo.app.viewmodel.WorkoutSetupViewModel
import com.katadojo.app.viewmodel.WorkoutViewModel

private fun workoutHint(type: WorkoutType): String = when (type) {
    WorkoutType.TECHNIQUE -> "Basics in the air: stances, blocks, punches and kicks with full focus."
    WorkoutType.KATA -> "Full kata runs and slow repetitions of difficult parts."
    WorkoutType.KUMITE -> "Distance, timing and footwork drills with a partner or shadow sparring."
    WorkoutType.CONDITIONING -> "Strength and endurance for karate: core, legs and explosive movement."
    WorkoutType.FOOTWORK -> "Stance transitions, shifting and angles."
    WorkoutType.FLEXIBILITY -> "Mobility and stretching for higher, safer kicks."
    WorkoutType.MIXED -> "A bit of everything: warm-up, technique, kata and cool-down."
}

@Composable
fun WorkoutSetupScreen(
    onBack: () -> Unit,
    onStart: (WorkoutType, Int) -> Unit,
    viewModel: WorkoutSetupViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    Column(Modifier.fillMaxSize().background(Ink)) {
        DojoTopBar("New Workout", onBack = onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DojoHero(overline = "Workout tracker", title = "Karate workout", subtitle = "Pick a focus and a target duration. The timer tracks real elapsed time.", minHeight = 150.dp)
            }
            item { SectionTitle("Workout type") }
            item {
                ChipGrid(WorkoutType.entries, viewModel.type, { it.label }, { viewModel.type = it })
            }
            item { Text(workoutHint(viewModel.type), style = MaterialTheme.typography.bodyMedium, color = Stone) }
            item { SectionTitle("Duration") }
            item {
                ChipGrid(WorkoutSetupViewModel.DURATIONS, viewModel.minutes, { "$it min" }, { viewModel.minutes = it }, columns = 4)
            }
        }
        Box(Modifier.fillMaxWidth().background(Charcoal).navigationBarsPadding().padding(16.dp)) {
            PrimaryButton(
                "Start ${viewModel.minutes} min ${viewModel.type.label}",
                onClick = { onStart(viewModel.type, viewModel.minutes) },
                icon = painterResource(R.drawable.ic_timer),
                height = 58.dp
            )
        }
    }
}

@Composable
fun WorkoutScreen(
    onExit: () -> Unit,
    onDone: () -> Unit,
    viewModel: WorkoutViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state = viewModel.uiState
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val active = state.savedId == null && (state.timerState != StopwatchState.IDLE || state.elapsedMs > 0)
    BackHandler { if (active) confirmExit = true else onExit() }

    val view = LocalView.current
    val keepOn = state.settings.keepScreenOn && state.timerState == StopwatchState.RUNNING
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    Column(Modifier.fillMaxSize().background(Ink)) {
        DojoTopBar("${state.type.label} Workout", onBack = { if (active) confirmExit = true else onExit() })
        if (!state.reviewing) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Overline("Workout timer", color = DojoRed)
                Spacer(Modifier.height(16.dp))
                RingProgress(
                    state.fraction,
                    Modifier.size(260.dp),
                    color = if (state.targetReached) SuccessGreen else DojoRed,
                    strokeWidth = 14.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Formatters.clock((state.elapsedMs / 1000).toInt()), style = MaterialTheme.typography.displayLarge, color = Rice)
                        Text("of ${state.plannedMinutes} min", style = MaterialTheme.typography.titleMedium, color = Stone)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    when {
                        state.targetReached -> "Target reached. Finish when you are done."
                        state.timerState == StopwatchState.RUNNING -> "Training in progress"
                        state.timerState == StopwatchState.PAUSED -> "Paused"
                        else -> "Press Start when you begin training"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.targetReached) SuccessGreen else StoneLight,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(workoutHint(state.type), style = MaterialTheme.typography.bodySmall, color = Stone, textAlign = TextAlign.Center)
            }
            Column(
                Modifier.fillMaxWidth().background(Charcoal).navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (state.timerState) {
                    StopwatchState.IDLE -> PrimaryButton("Start", onClick = viewModel::start, icon = painterResource(R.drawable.ic_timer), height = 60.dp)
                    StopwatchState.RUNNING -> PrimaryButton("Pause", onClick = viewModel::pause, icon = painterResource(R.drawable.ic_pause), height = 60.dp, color = CharcoalLight)
                    StopwatchState.PAUSED, StopwatchState.FINISHED -> PrimaryButton("Resume", onClick = viewModel::resume, height = 60.dp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        "Reset",
                        onClick = viewModel::reset,
                        modifier = Modifier.weight(1f),
                        enabled = state.elapsedMs > 0,
                        icon = painterResource(R.drawable.ic_restart),
                        color = StoneLight
                    )
                    SecondaryButton(
                        "Finish",
                        onClick = viewModel::finish,
                        modifier = Modifier.weight(1f),
                        enabled = state.elapsedMs >= 1_000,
                        icon = painterResource(R.drawable.ic_stop),
                        color = Beige
                    )
                }
            }
        } else {
            WorkoutReview(state, viewModel, onDone)
        }
    }

    if (confirmExit) {
        ConfirmDialog(
            title = "Leave this workout?",
            text = "The timer will stop and this workout will not be saved.",
            confirmLabel = "Leave",
            dismissLabel = "Keep training",
            onConfirm = {
                confirmExit = false
                onExit()
            },
            onDismiss = { confirmExit = false }
        )
    }
}

@Composable
private fun ColumnScope.WorkoutReview(state: com.katadojo.app.viewmodel.WorkoutUiState, viewModel: WorkoutViewModel, onDone: () -> Unit) {
    val saved = state.savedId != null
    LazyColumn(
        Modifier.weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DojoCard(accent = DojoRed) {
                Overline("Workout complete", color = DojoRed)
                Spacer(Modifier.height(10.dp))
                StatGrid(
                    listOf(
                        "${state.minutes} min" to "Duration",
                        state.type.label to "Training type",
                        "${state.effort} / 10" to "Personal effort",
                        "${state.plannedMinutes} min" to "Target"
                    )
                )
            }
        }
        if (!saved) {
            item {
                DojoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Personal effort", style = MaterialTheme.typography.titleMedium, color = Rice, modifier = Modifier.weight(1f))
                        Text("${state.effort} / 10", style = MaterialTheme.typography.titleMedium, color = Beige)
                    }
                    Slider(
                        value = state.effort.toFloat(),
                        onValueChange = { viewModel.setEffort(it.toInt()) },
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
                    Text("1 = very light · 10 = maximum effort", style = MaterialTheme.typography.bodySmall, color = Stone)
                }
            }
        } else {
            item { RewardsCard(state.rewards) }
            item {
                Text("Workout saved to your training history.", style = MaterialTheme.typography.bodyMedium, color = SuccessGreen, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    Column(
        Modifier.fillMaxWidth().background(Charcoal).navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!saved) {
            PrimaryButton(if (state.saving) "Saving..." else "Save Workout", onClick = viewModel::save, enabled = !state.saving, height = 58.dp)
            SecondaryButton("Back to Timer", onClick = viewModel::backToTimer, height = 46.dp, color = StoneLight)
        } else {
            PrimaryButton("View Progress", onClick = onDone, height = 58.dp)
        }
    }
}

@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val w = state.workout
    ScreenList(topBar = { DojoTopBar(w?.let { Formatters.longDate(it.date) } ?: "Workout", onBack = onBack) }) {
        when {
            state.loading -> item { LoadingBox(Modifier.height(300.dp)) }
            w == null -> item { EmptyState("Workout not found", "It may have been deleted.", actionLabel = "Back", onAction = onBack) }
            else -> {
                item { DojoHero(overline = "Workout", title = w.type.label, subtitle = Formatters.longDate(w.date), minHeight = 150.dp) }
                item {
                    DojoCard {
                        StatGrid(
                            listOf(
                                "${w.minutes} min" to "Duration",
                                "${w.plannedMinutes} min" to "Target",
                                "${w.effort} / 10" to "Personal effort",
                                (if (w.durationSec >= w.plannedMinutes * 60) "Reached" else "${(w.durationSec * 100 / (w.plannedMinutes * 60).coerceAtLeast(1))}%") to "Target progress"
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        InfoRow("Exact time", Formatters.clock(w.durationSec))
                        InfoRow("Started", Formatters.time(java.time.Instant.ofEpochMilli(w.startedAt).atZone(java.time.ZoneId.systemDefault()).toLocalTime()))
                    }
                }
                item { Text(workoutHint(w.type), style = MaterialTheme.typography.bodyMedium, color = Stone) }
            }
        }
    }
}
