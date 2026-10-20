package com.katadojo.app.ui.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.data.model.GoalType
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.GoalItem
import com.katadojo.app.viewmodel.GoalsViewModel

private fun GoalType.shortLabel(): String = when (this) {
    GoalType.SESSIONS_WEEK -> "Weekly sessions"
    GoalType.KATA_WEEK -> "Weekly kata"
    GoalType.WORKOUTS_WEEK -> "Weekly workouts"
    GoalType.KATA_COUNT -> "Practice a kata"
    GoalType.IMPROVE_KATA -> "Improve a score"
    GoalType.WEAK_SECTION -> "Weak section"
    GoalType.MINUTES_WEEK -> "Weekly minutes"
    GoalType.STREAK -> "Training streak"
}

private fun GoalType.targetLabel(value: Int): String = when (this) {
    GoalType.IMPROVE_KATA -> "+" + Formatters.decimal(value / 10.0)
    GoalType.MINUTES_WEEK -> "$value min"
    GoalType.STREAK -> "$value days"
    else -> "$value×"
}

@Composable
fun GoalsScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: GoalsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    ScreenList(topBar = { DojoTopBar("Personal Goals", onBack = onBack) }) {
        item {
            DojoHero(
                overline = "Personal goals",
                title = "Set your intention",
                subtitle = "Goals update automatically from your saved sessions. Completing one earns +50 XP.",
                minHeight = 150.dp
            ) {
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Set Goal", onClick = { showCreate = true }, height = 50.dp)
            }
        }
        state.weakest?.let { (kata, section) ->
            item {
                DojoCard(accent = Amber) {
                    Overline("Your weakest section", color = Amber)
                    Text("${kata.name} · Section ${section.number}", style = MaterialTheme.typography.titleLarge, color = Rice)
                    Text(
                        "${section.title} · average ${Formatters.decimal(section.averageRating)} / 10 over ${section.attempts} attempts",
                        style = MaterialTheme.typography.bodySmall,
                        color = Stone
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton("Practice Section ${section.number} (+15 XP)", onClick = { onNavigate(Routes.kataCoach(kata.id, section.index)) }, height = 46.dp)
                }
            }
        }
        if (!state.loading && state.goals.isEmpty()) {
            item {
                EmptyState(
                    title = "No goals yet",
                    message = "Pick a goal such as practicing kata three times this week.",
                    actionLabel = "Set Goal",
                    onAction = { showCreate = true }
                )
            }
        }
        state.goals.forEach { goalItem ->
            item(key = goalItem.goal.id) {
                GoalCard(goalItem, onSetCurrent = { viewModel.setCurrent(goalItem.goal.id) }, onDelete = { deleteId = goalItem.goal.id })
            }
        }
        if (state.completedCount > 0) {
            item { Text("Goals completed so far: ${state.completedCount}", style = MaterialTheme.typography.bodySmall, color = Stone) }
        }
    }

    if (showCreate) {
        CreateGoalDialog(
            viewModel = viewModel,
            onDismiss = { showCreate = false },
            onCreate = { type, target, kataId ->
                viewModel.createGoal(type, target, kataId)
                showCreate = false
            }
        )
    }
    deleteId?.let { id ->
        ConfirmDialog(
            title = "Delete goal?",
            text = "The goal is removed. Training Points you already earned stay.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.delete(id)
                deleteId = null
            },
            onDismiss = { deleteId = null }
        )
    }
}

@Composable
private fun GoalCard(item: GoalItem, onSetCurrent: () -> Unit, onDelete: () -> Unit) {
    DojoCard(accent = if (item.goal.isCurrent) DojoRed else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline(if (item.goal.type.weekly) "Weekly goal" else "Goal", modifier = Modifier.weight(1f))
            if (item.goal.isCurrent) TagPill("Current", color = DojoRed, filled = true)
            if (item.achieved) {
                Spacer(Modifier.width(6.dp))
                TagPill(if (item.goal.type.weekly) "Done this week" else "Achieved", color = SuccessGreen)
            }
        }
        Spacer(Modifier.height(6.dp))
        GoalProgressBlock(item.goal.title, item.progress)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!item.goal.isCurrent) {
                SecondaryButton("Make Current", onClick = onSetCurrent, modifier = Modifier.weight(1f), height = 42.dp)
            }
            SecondaryButton("Delete", onClick = onDelete, modifier = Modifier.weight(1f), height = 42.dp, color = StoneLight)
        }
    }
}

@Composable
private fun CreateGoalDialog(viewModel: GoalsViewModel, onDismiss: () -> Unit, onCreate: (GoalType, Int, String?) -> Unit) {
    var type by rememberSaveable { mutableStateOf(GoalType.KATA_WEEK) }
    var target by rememberSaveable { mutableStateOf(GoalType.KATA_WEEK.defaultTarget) }
    var kataId by rememberSaveable { mutableStateOf("heian_shodan") }
    val kataName = viewModel.kataOptions.firstOrNull { it.id == kataId }?.name

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Charcoal,
        title = { Text("Set Goal", color = Rice) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Overline("Goal")
                ChipGrid(GoalType.entries, type, { it.shortLabel() }, {
                    type = it
                    target = it.defaultTarget
                }, columns = 2)
                Overline("Target")
                ChipGrid(type.targetOptions, target, { type.targetLabel(it) }, { target = it }, columns = 4)
                if (type.needsKata) {
                    Overline("Kata")
                    ChipGrid(viewModel.kataOptions.map { it.id }, kataId, { id -> viewModel.kataOptions.first { it.id == id }.name }, { kataId = it }, columns = 2)
                }
                DojoCard(containerColor = CharcoalLight, contentPadding = PaddingValues(12.dp)) {
                    Text(type.title(target, kataName), style = MaterialTheme.typography.titleMedium, color = Rice)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(type, target, if (type.needsKata) kataId else null) }) {
                Text("Set Goal", color = DojoRed)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = StoneLight) } }
    )
}
