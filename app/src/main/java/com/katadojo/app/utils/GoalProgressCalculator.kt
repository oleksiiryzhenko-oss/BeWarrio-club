package com.katadojo.app.utils

import com.katadojo.app.data.model.GoalProgress
import com.katadojo.app.data.model.GoalType
import com.katadojo.app.data.model.KataSession
import com.katadojo.app.data.model.PersonalGoal
import com.katadojo.app.data.model.WorkoutSession
import com.katadojo.app.data.model.toDate
import java.time.LocalDate
import java.util.Locale

object GoalProgressCalculator {

    /** Weekly goals restart every Monday; other goals count from the day they were set. */
    fun periodKey(goal: PersonalGoal, date: LocalDate): Long =
        if (goal.type.weekly) date.weekStart().toEpochDay() else 0L

    fun progress(
        goal: PersonalGoal,
        kata: List<KataSession>,
        workouts: List<WorkoutSession>,
        today: LocalDate = LocalDate.now()
    ): GoalProgress {
        val weekStart = today.weekStart()
        val weekEnd = weekStart.plusDays(6)
        val since = goal.createdAt
        fun inWeek(d: LocalDate) = d in weekStart..weekEnd && !d.isAfter(today)
        val fullKata = kata.filter { it.isFullKata }

        fun count(current: Int, detail: String) = GoalProgress(
            current = current.toDouble(),
            target = goal.target.toDouble(),
            completed = current >= goal.target,
            currentLabel = current.coerceAtMost(goal.target).toString(),
            targetLabel = goal.target.toString(),
            detail = detail
        )

        return when (goal.type) {
            GoalType.SESSIONS_WEEK -> count(
                fullKata.count { inWeek(it.date) } + workouts.count { inWeek(it.date) },
                "Kata sessions and workouts this week"
            )
            GoalType.KATA_WEEK -> count(fullKata.count { inWeek(it.date) }, "Full kata sessions this week")
            GoalType.WORKOUTS_WEEK -> count(workouts.count { inWeek(it.date) }, "Workouts this week")
            GoalType.KATA_COUNT -> count(
                fullKata.count { it.kataId == goal.kataId && it.startedAt >= since && !it.date.isAfter(today) },
                "Full sessions of ${goal.kata?.name ?: "this kata"} since the goal was set"
            )
            GoalType.WEAK_SECTION -> count(
                kata.count { !it.isFullKata && it.startedAt >= since && !it.date.isAfter(today) },
                "Single-section practices since the goal was set"
            )
            GoalType.MINUTES_WEEK -> {
                val minutes = (kata.filter { inWeek(it.date) }.sumOf { it.durationSec } +
                    workouts.filter { inWeek(it.date) }.sumOf { it.durationSec }) / 60
                GoalProgress(
                    minutes.toDouble(), goal.target.toDouble(), minutes >= goal.target,
                    minutes.coerceAtMost(goal.target).toString(), "${goal.target} min", "Training minutes this week"
                )
            }
            GoalType.STREAK -> {
                val days = StatisticsCalculator.activityDays(
                    kata.filter { !it.date.isAfter(today) }, workouts.filter { !it.date.isAfter(today) }
                )
                count(StatisticsCalculator.currentStreak(days, today), "Consecutive training days")
            }
            GoalType.IMPROVE_KATA -> {
                val after = fullKata.filter { it.kataId == goal.kataId && it.startedAt >= since && !it.date.isAfter(today) }
                val baseline = goal.baseline ?: after.minByOrNull { it.startedAt }?.score
                val best = after.maxOfOrNull { it.score }
                val gain = if (baseline != null && best != null) (best - baseline).coerceAtLeast(0.0) else 0.0
                val needed = goal.target / 10.0
                GoalProgress(
                    current = gain,
                    target = needed,
                    completed = baseline != null && gain >= needed - 1e-9,
                    currentLabel = String.format(Locale.US, "+%.1f", gain.coerceAtMost(needed)),
                    targetLabel = String.format(Locale.US, "+%.1f", needed),
                    detail = if (baseline == null) "Complete a session of ${goal.kata?.name ?: "this kata"} to set your starting score"
                    else String.format(Locale.US, "Starting score %.1f", baseline)
                )
            }
        }
    }

    fun isActiveOn(goal: PersonalGoal, date: LocalDate): Boolean = !goal.createdAt.toDate().isAfter(date)
}
