package com.katadojo.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katadojo.app.data.AppContainer
import com.katadojo.app.data.model.GoalProgress
import com.katadojo.app.data.model.GoalType
import com.katadojo.app.data.model.Kata
import com.katadojo.app.data.model.KataLibrary
import com.katadojo.app.data.model.PersonalGoal
import com.katadojo.app.data.model.XpEvent
import com.katadojo.app.utils.CareerStats
import com.katadojo.app.utils.ChartPoint
import com.katadojo.app.utils.DayActivity
import com.katadojo.app.utils.GoalProgressCalculator
import com.katadojo.app.utils.HistoryItem
import com.katadojo.app.utils.KataAnalyzer
import com.katadojo.app.utils.LevelProgress
import com.katadojo.app.utils.SectionStat
import com.katadojo.app.utils.StatisticsCalculator
import com.katadojo.app.utils.XpCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProgressUiState(
    val loading: Boolean = true,
    val stats: CareerStats = CareerStats(),
    val level: LevelProgress = XpCalculator.level(0),
    val minutesPerWeek: List<ChartPoint> = emptyList(),
    val kataScores: List<ChartPoint> = emptyList(),
    val workoutFrequency: List<ChartPoint> = emptyList(),
    val recentDays: List<DayActivity> = emptyList(),
    val xpEvents: List<XpEvent> = emptyList()
) {
    val isEmpty: Boolean get() = !loading && stats.workouts == 0 && stats.kataSessions == 0
}

class ProgressViewModel(container: AppContainer) : ViewModel() {
    val uiState: StateFlow<ProgressUiState> = combine(
        container.kataRepository.sessions, container.workoutRepository.workouts, container.xpRepository.events
    ) { kata, workouts, events ->
        val today = LocalDate.now()
        val xp = events.sumOf { it.points }
        ProgressUiState(
            loading = false,
            stats = StatisticsCalculator.career(kata, workouts, xp, today),
            level = XpCalculator.level(xp),
            minutesPerWeek = StatisticsCalculator.minutesPerWeek(kata, workouts, today),
            kataScores = StatisticsCalculator.kataScoreTrend(kata),
            workoutFrequency = StatisticsCalculator.workoutFrequency(workouts, today),
            recentDays = StatisticsCalculator.recentDays(kata, workouts, today),
            xpEvents = events.take(6)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
}

data class HistoryUiState(val loading: Boolean = true, val items: List<HistoryItem> = emptyList())

class HistoryViewModel(container: AppContainer) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = combine(container.kataRepository.sessions, container.workoutRepository.workouts) { k, w ->
        HistoryUiState(false, StatisticsCalculator.history(k, w))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}

data class GoalItem(val goal: PersonalGoal, val progress: GoalProgress, val achieved: Boolean)

data class GoalsUiState(
    val loading: Boolean = true,
    val goals: List<GoalItem> = emptyList(),
    val weakest: Pair<Kata, SectionStat>? = null,
    val completedCount: Int = 0
)

class GoalsViewModel(private val container: AppContainer) : ViewModel() {
    val uiState: StateFlow<GoalsUiState> = combine(
        container.goalRepository.goals, container.kataRepository.sessions,
        container.workoutRepository.workouts, container.xpRepository.events
    ) { goals, kata, workouts, events ->
        val today = LocalDate.now()
        val keys = events.map { it.key }.toSet()
        val items = goals.map { g ->
            val p = GoalProgressCalculator.progress(g, kata, workouts, today)
            GoalItem(g, p, p.completed || "goal:${g.id}:${GoalProgressCalculator.periodKey(g, today)}" in keys)
        }
        GoalsUiState(false, items, KataAnalyzer.weakestOverall(kata), events.count { it.key.startsWith("goal:") })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    /** "Set Goal": creates a local goal and makes it the current goal. */
    fun createGoal(type: GoalType, target: Int, kataId: String?) {
        viewModelScope.launch {
            val baseline = if (type == GoalType.IMPROVE_KATA) {
                container.kataRepository.sessions.first()
                    .filter { it.kataId == kataId && it.isFullKata }.maxOfOrNull { it.score }
            } else null
            container.goalRepository.create(
                PersonalGoal(type = type, target = target, kataId = if (type.needsKata) kataId else null,
                    baseline = baseline, createdAt = System.currentTimeMillis())
            )
            container.progressEngine.evaluate()
        }
    }

    fun setCurrent(id: Long) { viewModelScope.launch { container.goalRepository.setCurrent(id) } }
    fun delete(id: Long) { viewModelScope.launch { container.goalRepository.delete(id) } }

    val kataOptions: List<Kata> = KataLibrary.all
}

