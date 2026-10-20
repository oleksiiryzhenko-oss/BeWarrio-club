package com.katadojo.app.ui.dojo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.data.model.ScheduleFilter
import com.katadojo.app.viewmodel.DojoViewModel
import com.katadojo.app.viewmodel.DojoTab

@Composable
fun DojoScreen(
    onNavigate: (String) -> Unit,
    viewModel: DojoViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenList(topBar = { BrandTopBar(onNavigate) }) {
        item {
            Column {
                Text("Dojo", style = MaterialTheme.typography.headlineLarge, color = Rice)
                Text(
                    "Karate classes at local dojos. No account needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Stone
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip(
                    "Sessions (${state.sessions.size})",
                    selected = state.tab == DojoTab.SESSIONS,
                    onClick = { viewModel.selectTab(DojoTab.SESSIONS) },
                    modifier = Modifier.weight(1f)
                )
                SelectChip(
                    "Registered (${state.upcomingRegistrations.size})",
                    selected = state.tab == DojoTab.REGISTERED,
                    onClick = { viewModel.selectTab(DojoTab.REGISTERED) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (state.loading) {
            item { LoadingBox(Modifier.height(240.dp)) }
            return@ScreenList
        }

        when (state.tab) {
            DojoTab.SESSIONS -> {
                item { FilterRow(ScheduleFilter.entries, state.filter, { it.label }, viewModel::selectFilter) }
                item {
                    SectionTitle(
                        if (state.filter == ScheduleFilter.ALL) "All upcoming sessions" else state.filter.label,
                        action = "Weekly schedule",
                        onAction = { onNavigate(Routes.SCHEDULE) }
                    )
                }
                if (state.sessions.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No ${if (state.filter == ScheduleFilter.ALL) "" else state.filter.label + " "}sessions coming up",
                            message = "Try another training type or check the weekly schedule.",
                            actionLabel = "Show all sessions",
                            onAction = { viewModel.selectFilter(ScheduleFilter.ALL) }
                        )
                    }
                } else {
                    items(state.sessions, key = { it.id }) { training ->
                        TrainingCard(training, onClick = { onNavigate(Routes.trainingDetail(training.id)) })
                    }
                }
            }
            DojoTab.REGISTERED -> {
                item { SectionTitle("Upcoming") }
                if (state.upcomingRegistrations.isEmpty()) {
                    item {
                        EmptyState(
                            title = "You have no upcoming dojo sessions.",
                            message = "Join a session and it will appear here with all the details.",
                            actionLabel = "Browse sessions",
                            onAction = { viewModel.selectTab(DojoTab.SESSIONS) }
                        )
                    }
                } else {
                    items(state.upcomingRegistrations, key = { "u" + it.registration.id }) { item ->
                        TrainingCard(item.training, onClick = { onNavigate(Routes.trainingDetail(item.training.id)) })
                    }
                }
                if (state.pastRegistrations.isNotEmpty()) {
                    item { SectionTitle("Past sessions") }
                    items(state.pastRegistrations, key = { "p" + it.registration.id }) { item ->
                        ScheduleRow(item.training, onClick = { onNavigate(Routes.trainingDetail(item.training.id)) })
                    }
                }
            }
        }
    }
}
