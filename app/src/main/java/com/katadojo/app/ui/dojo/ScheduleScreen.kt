package com.katadojo.app.ui.dojo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.data.model.ScheduleFilter
import com.katadojo.app.viewmodel.ScheduleViewModel
import java.time.LocalDate

@Composable
fun ScheduleScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    ScreenList(topBar = { DojoTopBar("Weekly Schedule", onBack = onBack) }) {
        item {
            Text(
                "The next seven days across all dojos. Filters change the list below.",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone
            )
        }
        item { FilterRow(ScheduleFilter.entries, state.filter, { it.label }, viewModel::selectFilter) }
        item {
            Text(
                "${state.total} ${if (state.total == 1) "session" else "sessions"}" +
                    (if (state.filter == ScheduleFilter.ALL) "" else " · ${state.filter.label}"),
                style = MaterialTheme.typography.titleSmall,
                color = StoneLight
            )
        }
        if (state.loading) {
            item { LoadingBox(Modifier.height(200.dp)) }
            return@ScreenList
        }
        state.days.forEach { day ->
            item(key = "h" + day.date.toEpochDay()) {
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text(Formatters.weekday(day.date), style = MaterialTheme.typography.titleLarge, color = Rice)
                    Spacer(Modifier.width(8.dp))
                    Text(Formatters.date(day.date), style = MaterialTheme.typography.bodyMedium, color = Stone)
                    Spacer(Modifier.weight(1f))
                    if (day.date == today) TagPill("Today", color = DojoRed, filled = true)
                }
            }
            if (day.sessions.isEmpty()) {
                item(key = "e" + day.date.toEpochDay()) {
                    Text(
                        if (state.filter == ScheduleFilter.ALL) "No dojo sessions scheduled" else "No ${state.filter.label} sessions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StoneDark,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                items(day.sessions, key = { it.id }) { training ->
                    ScheduleRow(training, onClick = { onNavigate(Routes.trainingDetail(training.id)) })
                }
            }
        }
    }
}
