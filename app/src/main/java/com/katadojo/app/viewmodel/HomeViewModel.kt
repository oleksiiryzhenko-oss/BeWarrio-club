package com.katadojo.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katadojo.app.data.AppContainer
import com.katadojo.app.data.model.GoalProgress
import com.katadojo.app.data.model.KarateTraining
import com.katadojo.app.data.model.Kata
import com.katadojo.app.data.model.KataLibrary
import com.katadojo.app.data.model.PersonalGoal
import com.katadojo.app.data.model.UserProfile
import com.katadojo.app.utils.CareerStats
import com.katadojo.app.utils.DayActivity
import com.katadojo.app.utils.GoalProgressCalculator
import com.katadojo.app.utils.HistoryItem
import com.katadojo.app.utils.KataAnalyzer
import com.katadojo.app.utils.KataProgress
import com.katadojo.app.utils.LevelProgress
import com.katadojo.app.utils.StatisticsCalculator
import com.katadojo.app.utils.XpCalculator
import com.katadojo.app.utils.weekStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class HomeUiState(
    val loading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val level: LevelProgress = XpCalculator.level(0),
    val nextTraining: KarateTraining? = null,
    val nextIsRegistered: Boolean = false,
    val currentGoal: PersonalGoal? = null,
    val goalProgress: GoalProgress? = null,
    val kataOfWeek: Kata = KataLibrary.all.first(),
    val kataOfWeekProgress: KataProgress? = null,
    val recent: HistoryItem? = null,
    val stats: CareerStats = CareerStats(),
    val week: List<DayActivity> = emptyList()
)

private data class HomeData(
    val trainings: List<KarateTraining>,
    val kata: List<com.katadojo.app.data.model.KataSession>,
    val workouts: List<com.katadojo.app.data.model.WorkoutSession>,
    val goals: List<PersonalGoal>,
    val xp: Int
)

class HomeViewModel(container: AppContainer) : ViewModel() {
    private val data = combine(
        container.trainingRepository.trainings,
        container.kataRepository.sessions,
        container.workoutRepository.workouts,
        container.goalRepository.goals,
        container.xpRepository.total
    ) { t, k, w, g, xp -> HomeData(t, k, w, g, xp) }

    val uiState: StateFlow<HomeUiState> = combine(data, container.preferences.profile) { d, profile ->
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val upcoming = d.trainings.filter { !it.hasEnded(now) }.sortedBy { it.startDateTime }
        val registered = upcoming.firstOrNull { it.isRegistered }
        val goal = d.goals.firstOrNull { it.isCurrent } ?: d.goals.firstOrNull()
        val kataOfWeek = KataLibrary.kataOfWeek(ChronoUnit.WEEKS.between(LocalDate.of(2024, 1, 1), today.weekStart()))
        HomeUiState(
            loading = false,
            profile = profile,
            level = XpCalculator.level(d.xp),
            nextTraining = registered ?: upcoming.firstOrNull(),
            nextIsRegistered = registered != null,
            currentGoal = goal,
            goalProgress = goal?.let { GoalProgressCalculator.progress(it, d.kata, d.workouts, today) },
            kataOfWeek = kataOfWeek,
            kataOfWeekProgress = KataAnalyzer.progress(kataOfWeek, d.kata, today),
            recent = StatisticsCalculator.history(d.kata, d.workouts).firstOrNull(),
            stats = StatisticsCalculator.career(d.kata, d.workouts, d.xp, today),
            week = StatisticsCalculator.week(d.kata, d.workouts, today)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
