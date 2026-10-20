package com.katadojo.app.ui.progress

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.Stone
import com.katadojo.app.utils.HistoryItem
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.HistoryViewModel

private enum class HistoryFilter(val label: String) { ALL("All"), KATA("Kata"), WORKOUTS("Workouts") }

@Composable
fun HistoryScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onTab: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
    val visible = state.items.filter {
        when (filter) {
            HistoryFilter.ALL -> true
            HistoryFilter.KATA -> it is HistoryItem.Kata
            HistoryFilter.WORKOUTS -> it is HistoryItem.Workout
        }
    }

    ScreenList(topBar = { DojoTopBar("Training History", onBack = onBack) }) {
        item { FilterRow(HistoryFilter.entries, filter, { it.label }, { filter = it }) }
        when {
            state.loading -> item { LoadingBox(Modifier.height(300.dp)) }
            state.items.isEmpty() -> item {
                EmptyState(
                    title = "Your kata journey starts here.",
                    message = "Complete your first session to see your progress.",
                    actionLabel = "Open Kata Library",
                    onAction = { onTab(Routes.KATA) }
                )
            }
            visible.isEmpty() -> item {
                EmptyState(
                    title = if (filter == HistoryFilter.WORKOUTS) "No workouts yet" else "No kata sessions yet",
                    message = "Complete your first training session to unlock statistics.",
                    actionLabel = "Show all",
                    onAction = { filter = HistoryFilter.ALL }
                )
            }
            else -> {
                item { Text("${visible.size} sessions", style = MaterialTheme.typography.bodySmall, color = Stone) }
                items(visible, key = { if (it is HistoryItem.Kata) "k${it.session.id}" else "w${(it as HistoryItem.Workout).workout.id}" }) { entry ->
                    when (entry) {
                        is HistoryItem.Kata -> HistoryRow(entry) { onNavigate(Routes.kataSession(entry.session.id)) }
                        is HistoryItem.Workout -> HistoryRow(entry) { onNavigate(Routes.workoutDetail(entry.workout.id)) }
                    }
                }
            }
        }
    }
}
