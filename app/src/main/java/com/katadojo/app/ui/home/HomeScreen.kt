package com.katadojo.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.katadojo.app.R
import com.katadojo.app.navigation.Routes
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.utils.HistoryItem
import com.katadojo.app.viewmodel.AppViewModelProvider
import com.katadojo.app.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onTab: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenList(topBar = { BrandTopBar(onNavigate) }) {
        if (state.loading) {
            item { LoadingBox(Modifier.height(300.dp)) }
            return@ScreenList
        }

        item {
            DojoHero(
                overline = "Osu, ${state.profile.displayName}",
                title = "Train with focus.\nPractice with purpose.",
                subtitle = "${state.stats.currentStreak}-day streak · ${Formatters.thousands(state.level.xp)} Training Points"
            ) {
                Spacer(Modifier.height(18.dp))
                PrimaryButton(
                    "Quick Start Kata",
                    onClick = { onNavigate(Routes.kataCoach(state.kataOfWeek.id)) },
                    icon = painterResource(R.drawable.ic_enso),
                    height = 52.dp
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Quick Start Workout", onClick = { onNavigate(Routes.WORKOUT_SETUP_DEFAULT) }, height = 46.dp)
            }
        }

        item { SectionTitle("Next Training", action = "Schedule", onAction = { onNavigate(Routes.SCHEDULE) }) }
        item {
            val next = state.nextTraining
            if (next == null) {
                EmptyState(
                    title = "You have no upcoming dojo sessions.",
                    message = "Browse karate classes at local dojos and join one in a few taps.",
                    actionLabel = "Find a Dojo Session",
                    onAction = { onTab(Routes.DOJO) }
                )
            } else {
                DojoCard(accent = DojoRed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Overline(if (state.nextIsRegistered) "Your next training" else "Next open session", color = DojoRed, modifier = Modifier.weight(1f))
                        if (state.nextIsRegistered) TagPill("Registered", color = SuccessGreen, filled = true)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(next.title, style = MaterialTheme.typography.headlineMedium, color = Rice)
                    Spacer(Modifier.height(10.dp))
                    Text(Formatters.relativeDay(next.date), style = MaterialTheme.typography.titleMedium, color = StoneLight)
                    Text(Formatters.timeRange(next.startTime, next.endTime), style = MaterialTheme.typography.displaySmall, color = Rice)
                    Spacer(Modifier.height(8.dp))
                    Text(next.dojo, style = MaterialTheme.typography.titleMedium, color = Rice)
                    Text("${next.level.label} · ${next.style.label} · ${next.instructor}", style = MaterialTheme.typography.bodyMedium, color = Stone)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("View Training", onClick = { onNavigate(Routes.trainingDetail(next.id)) })
                }
            }
        }

        item { SectionTitle("App Progress", action = "Details", onAction = { onTab(Routes.PROGRESS) }) }
        item {
            val level = state.level
            DojoCard(onClick = { onTab(Routes.PROGRESS) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Overline("App progress level")
                        Text("${level.level.label} level", style = MaterialTheme.typography.headlineMedium, color = Rice)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Overline("Your belt")
                        Text(state.profile.belt.label, style = MaterialTheme.typography.titleMedium, color = beltColor(state.profile.belt.color).takeIf { state.profile.belt.name != "BLACK" } ?: Rice)
                    }
                }
                Spacer(Modifier.height(10.dp))
                BeltProgress(level.fraction, beltColor(level.level.color))
                Text(
                    if (level.next != null) "${Formatters.thousands(level.xp)} / ${Formatters.thousands(level.nextAt ?: 0)} XP · ${level.remaining} XP to ${level.next.label}"
                    else "${Formatters.thousands(level.xp)} XP · highest app level reached",
                    style = MaterialTheme.typography.bodySmall,
                    color = Stone
                )
            }
        }

        val goal = state.currentGoal
        val progress = state.goalProgress
        item { SectionTitle("Current Goal", action = "Goals", onAction = { onNavigate(Routes.GOALS) }) }
        item {
            if (goal == null || progress == null) {
                EmptyState(
                    title = "No goal set yet",
                    message = "Pick a weekly goal to give every session a purpose.",
                    actionLabel = "Set a Goal",
                    onAction = { onNavigate(Routes.GOALS) }
                )
            } else {
                DojoCard(onClick = { onNavigate(Routes.GOALS) }) { GoalProgressBlock(goal.title, progress) }
            }
        }

        item { SectionTitle("Kata of the Week", action = "Library", onAction = { onTab(Routes.KATA) }) }
        item {
            val kata = state.kataOfWeek
            val kp = state.kataOfWeekProgress
            DojoCard(onClick = { onNavigate(Routes.kataDetail(kata.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingProgress((kp?.masteryPercent ?: 0) / 100f, Modifier.size(72.dp), strokeWidth = 7.dp) {
                        Text("${kp?.masteryPercent ?: 0}%", style = MaterialTheme.typography.titleMedium, color = Rice)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(kata.name, style = MaterialTheme.typography.headlineMedium, color = Rice)
                        Text("${kata.difficulty.label} · ${kata.sectionCount} sections · ${kata.durationText}", style = MaterialTheme.typography.bodySmall, color = Stone)
                        Text(
                            kp?.bestScore?.let { "Best ${Formatters.score(it)} / 10" } ?: "Not practiced yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = Beige
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Start Kata Session", onClick = { onNavigate(Routes.kataCoach(kata.id)) }, height = 48.dp)
            }
        }

        item { SectionTitle("This Week") }
        item {
            DojoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Overline("Current streak")
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${state.stats.currentStreak}", style = MaterialTheme.typography.displayMedium, color = Rice)
                            Text(
                                if (state.stats.currentStreak == 1) " training day" else " training days",
                                style = MaterialTheme.typography.titleSmall,
                                color = Stone,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                    }
                    Enso(Modifier.size(58.dp), alpha = if (state.stats.currentStreak > 0) 1f else 0.3f)
                }
                Spacer(Modifier.height(14.dp))
                WeekStrip(state.week)
            }
        }

        item { SectionTitle("Recent Training", action = "History", onAction = { onNavigate(Routes.HISTORY) }) }
        item {
            when (val recent = state.recent) {
                null -> EmptyState(
                    title = "Your kata journey starts here.",
                    message = "Complete your first session to see your progress.",
                    actionLabel = "Start a Kata",
                    onAction = { onTab(Routes.KATA) }
                )
                is HistoryItem.Kata -> HistoryRow(recent) { onNavigate(Routes.kataSession(recent.session.id)) }
                is HistoryItem.Workout -> HistoryRow(recent) { onNavigate(Routes.workoutDetail(recent.workout.id)) }
            }
        }
    }
}
