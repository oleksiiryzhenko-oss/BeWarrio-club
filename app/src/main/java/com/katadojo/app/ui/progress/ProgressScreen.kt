package com.katadojo.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.R
import com.katadojo.app.data.model.ProgressLevel
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.DayActivity
import com.katadojo.app.utils.Formatters
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.ProgressViewModel

@Composable
fun ProgressScreen(
    onNavigate: (String) -> Unit,
    onTab: (String) -> Unit,
    viewModel: ProgressViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = state.stats
    val level = state.level

    ScreenList(topBar = { BrandTopBar(onNavigate) }) {
        item {
            Column {
                Text("Progress", style = MaterialTheme.typography.headlineLarge, color = Rice)
                Text("Every number comes from your saved sessions.", style = MaterialTheme.typography.bodyMedium, color = Stone)
            }
        }
        if (state.loading) {
            item { LoadingBox(Modifier.height(300.dp)) }
            return@ScreenList
        }

        item {
            DojoCard(accent = beltColor(level.level.color)) {
                Overline("App progress")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(level.level.label, style = MaterialTheme.typography.displaySmall, color = Rice)
                    Text(" level", style = MaterialTheme.typography.titleLarge, color = Stone, modifier = Modifier.padding(bottom = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
                BeltProgress(level.fraction, beltColor(level.level.color), height = 16.dp)
                Row {
                    Text(
                        "XP ${Formatters.thousands(level.xp)}" + (level.nextAt?.let { " / ${Formatters.thousands(it)}" } ?: ""),
                        style = MaterialTheme.typography.titleSmall,
                        color = Rice,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        level.next?.let { "${level.remaining} XP to ${it.label}" } ?: "Highest level",
                        style = MaterialTheme.typography.titleSmall,
                        color = Beige
                    )
                }
                Spacer(Modifier.height(12.dp))
                LevelLadder(level.level)
                Spacer(Modifier.height(10.dp))
                Text(
                    "App Progress Level — a motivational level for this app, not an official karate rank or grading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StoneDark
                )
            }
        }

        if (state.isEmpty) {
            item {
                EmptyState(
                    title = "No training yet",
                    message = "Complete your first training session to unlock statistics.",
                    actionLabel = "Start a Workout",
                    onAction = { onNavigate(Routes.WORKOUT_SETUP_DEFAULT) }
                )
            }
        }

        item {
            DojoCard {
                StatGrid(
                    listOf(
                        "${stats.workouts}" to "Workouts completed",
                        "${stats.kataSessions}" to "Kata sessions",
                        Formatters.thousands(stats.trainingMinutes) to "Training minutes",
                        "${stats.currentStreak} days" to "Current streak",
                        "${stats.longestStreak} days" to "Longest streak",
                        Formatters.thousands(stats.xp) to "Training Points",
                        (if (stats.kataSessions > 0) Formatters.score(stats.averageKataScore) else "-") to "Average kata score",
                        (if (stats.kataSessions > 0) Formatters.score(stats.bestKataScore) else "-") to "Best kata score"
                    )
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Start Workout", onClick = { onNavigate(Routes.WORKOUT_SETUP_DEFAULT) }, modifier = Modifier.weight(1f), icon = painterResource(R.drawable.ic_timer), height = 50.dp)
                SecondaryButton("Kata", onClick = { onTab(Routes.KATA) }, modifier = Modifier.weight(1f), icon = painterResource(R.drawable.ic_enso), height = 50.dp)
            }
        }

        item {
            DojoCard {
                Overline("Training minutes per week")
                Spacer(Modifier.height(12.dp))
                BarChart(state.minutesPerWeek)
            }
        }
        item {
            DojoCard {
                Overline("Kata score over time")
                Spacer(Modifier.height(12.dp))
                LineChart(state.kataScores, minValue = 0f, maxValue = 10f)
            }
        }
        item {
            DojoCard {
                Overline("Workout frequency")
                Spacer(Modifier.height(12.dp))
                BarChart(state.workoutFrequency, barColor = Beige, height = 140.dp)
            }
        }
        item {
            DojoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Overline("Training streak · last 14 days", modifier = Modifier.weight(1f))
                    Text("${stats.currentStreak} now · ${stats.longestStreak} best", style = MaterialTheme.typography.labelMedium, color = Beige)
                }
                Spacer(Modifier.height(12.dp))
                StreakChart(state.recentDays)
            }
        }

        item { SectionTitle("Recent Training Points", action = "History", onAction = { onNavigate(Routes.HISTORY) }) }
        if (state.xpEvents.isEmpty()) {
            item { Text("Complete a workout or kata to earn your first Training Points.", style = MaterialTheme.typography.bodyMedium, color = Stone) }
        } else {
            item {
                DojoCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)) {
                    state.xpEvents.forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.label, style = MaterialTheme.typography.bodyMedium, color = Rice)
                                Text(Formatters.relativeDay(java.time.Instant.ofEpochMilli(e.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()), style = MaterialTheme.typography.bodySmall, color = Stone)
                            }
                            Text("+${e.points} XP", style = MaterialTheme.typography.titleSmall, color = Beige)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Training History", onClick = { onNavigate(Routes.HISTORY) }, modifier = Modifier.weight(1f), height = 48.dp)
                SecondaryButton("Personal Goals", onClick = { onNavigate(Routes.GOALS) }, modifier = Modifier.weight(1f), height = 48.dp)
            }
        }
    }
}

@Composable
private fun LevelLadder(current: ProgressLevel) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ProgressLevel.entries.forEach { l ->
            val reached = l.ordinal <= current.ordinal
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (reached) beltColor(l.color) else CharcoalBorder)
                )
                Spacer(Modifier.height(4.dp))
                Text(l.label.take(3).uppercase(), style = MaterialTheme.typography.labelSmall, color = if (l == current) Rice else StoneDark)
            }
        }
    }
}

@Composable
private fun StreakChart(days: List<DayActivity>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        days.forEach { d ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                d.sessions >= 2 -> DojoRed
                                d.sessions == 1 -> DojoRed.copy(alpha = 0.55f)
                                else -> CharcoalLight
                            }
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    Formatters.weekdayShort(d.date).take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (d.isToday) Rice else StoneDark
                )
            }
        }
    }
}
