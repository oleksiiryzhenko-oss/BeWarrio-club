package com.katadojo.app.ui.dojo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.R
import com.katadojo.app.data.model.KarateTraining
import com.katadojo.app.data.model.workoutType
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.TrainingDetailViewModel

@Composable
fun TrainingDetailScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TrainingDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmCancel by remember { mutableStateOf(false) }
    val training = state.training

    Column(Modifier.fillMaxSize().background(Ink)) {
        DojoTopBar("Training Details", onBack = onBack)
        when {
            state.loading -> LoadingBox()
            training == null -> Box(Modifier.padding(16.dp)) {
                EmptyState(
                    title = "Session not found",
                    message = "This session is no longer in the schedule.",
                    actionLabel = "Back",
                    onAction = onBack
                )
            }
            else -> {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { DetailHeader(training) }
                    item { DetailFacts(training) }
                    item {
                        DojoCard {
                            Overline("About this session")
                            Spacer(Modifier.height(8.dp))
                            Text(training.description, style = MaterialTheme.typography.bodyLarge, color = Rice)
                        }
                    }
                    item {
                        DojoCard {
                            Overline("What to bring")
                            Spacer(Modifier.height(8.dp))
                            training.whatToBring.forEach { thing ->
                                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(DojoRed))
                                    Spacer(Modifier.width(12.dp))
                                    Text(thing, style = MaterialTheme.typography.bodyLarge, color = Rice)
                                }
                            }
                        }
                    }
                }
                DetailActions(
                    training = training,
                    onJoin = { onNavigate(Routes.register(training.id)) },
                    onStartWorkout = { onNavigate(Routes.workoutSetup(training.type.workoutType)) },
                    onCancel = { confirmCancel = true }
                )
            }
        }
    }

    if (confirmCancel) {
        ConfirmDialog(
            title = "Cancel registration?",
            text = "Your place will be released for other boxers. You can register again while places are available.",
            confirmLabel = "Cancel registration",
            dismissLabel = "Keep",
            onConfirm = {
                confirmCancel = false
                viewModel.cancelRegistration()
            },
            onDismiss = { confirmCancel = false }
        )
    }
}

@Composable
private fun DetailHeader(training: KarateTraining) {
    DojoHero(
        overline = training.type.label,
        title = training.title,
        subtitle = "${training.dojo} · ${training.instructor}",
        minHeight = 190.dp
    ) {
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TagPill(training.level.label, color = StoneLight)
            TrainingStatusPill(training)
        }
    }
}

@Composable
private fun DetailFacts(training: KarateTraining) {
    DojoCard {
        Row {
            StatBlock(Formatters.time(training.startTime), "Start", Modifier.weight(1f))
            StatBlock(Formatters.time(training.endTime), "End", Modifier.weight(1f))
            StatBlock("${training.durationMinutes} min", "Duration", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        val relative = Formatters.relativeDay(training.date)
        InfoRow(
            "Date",
            if (relative == "Today" || relative == "Tomorrow") "${Formatters.longDate(training.date)} ($relative)"
            else Formatters.longDate(training.date)
        )
        InfoRow("Time", Formatters.timeRange(training.startTime, training.endTime))
        InfoRow("Dojo", training.dojo)
        InfoRow("Address", training.dojoAddress)
        InfoRow("Instructor", training.instructor)
        InfoRow("Karate style", training.style.label)
        InfoRow("Skill level", training.level.label)
        InfoRow("Training type", training.type.label)
        Spacer(Modifier.height(10.dp))
        PlacesBar(training)
    }
}

@Composable
private fun DetailActions(
    training: KarateTraining,
    onJoin: () -> Unit,
    onStartWorkout: () -> Unit,
    onCancel: () -> Unit
) {
    val ended = training.hasEnded()
    Column(
        Modifier
            .fillMaxWidth()
            .background(Charcoal)
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when {
            training.isRegistered && !ended -> {
                Text(
                    "You're registered. See you on ${Formatters.relativeDay(training.date)} at ${Formatters.time(training.startTime)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SuccessGreen
                )
                PrimaryButton("Start Training", onClick = onStartWorkout, icon = painterResource(R.drawable.ic_timer))
                SecondaryButton("Cancel Registration", onClick = onCancel, height = 46.dp, color = StoneLight)
            }
            training.isRegistered -> {
                Text("You attended this session. Log the rounds you boxed:", style = MaterialTheme.typography.bodyMedium, color = StoneLight)
                PrimaryButton("Start Workout", onClick = onStartWorkout, icon = painterResource(R.drawable.ic_timer))
            }
            ended -> PrimaryButton("Session has ended", onClick = {}, enabled = false)
            training.isFull -> PrimaryButton("Session Full", onClick = {}, enabled = false)
            else -> PrimaryButton("Join Training", onClick = onJoin, icon = painterResource(R.drawable.ic_belt), height = 60.dp)
        }
    }
}
