package com.katadojo.app.utils

import com.katadojo.app.data.model.KataSession
import com.katadojo.app.data.model.WorkoutSession
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ChartPoint(val label: String, val value: Float)

data class DayActivity(val date: LocalDate, val sessions: Int, val isToday: Boolean, val isFuture: Boolean)

data class CareerStats(
    val workouts: Int = 0,
    val kataSessions: Int = 0,
    val sectionPractices: Int = 0,
    val trainingMinutes: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val xp: Int = 0,
    val averageKataScore: Double = 0.0,
    val bestKataScore: Double = 0.0
) {
    val trainingSessions: Int get() = workouts + kataSessions
}

/** One entry of the combined training history. */
sealed interface HistoryItem {
    val startedAt: Long
    data class Kata(val session: KataSession) : HistoryItem { override val startedAt: Long get() = session.startedAt }
    data class Workout(val workout: WorkoutSession) : HistoryItem { override val startedAt: Long get() = workout.startedAt }
}

object StatisticsCalculator {

    fun activityDays(kata: List<KataSession>, workouts: List<WorkoutSession>): Set<LocalDate> =
        (kata.map { it.date } + workouts.map { it.date }).toSet()

    /** Consecutive training days ending today (or yesterday, so the streak is not lost before today's session). */
    fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in days) today else today.minusDays(1)
        var count = 0
        while (day in days) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** Length of the run of consecutive training days that ends exactly on [date]. */
    fun runEndingOn(days: Set<LocalDate>, date: LocalDate): Int {
        var day = date
        var count = 0
        while (day in days) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun longestStreak(days: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (d in days.sorted()) {
            run = if (previous != null && ChronoUnit.DAYS.between(previous, d) == 1L) run + 1 else 1
            best = maxOf(best, run)
            previous = d
        }
        return best
    }

    fun minutes(kata: List<KataSession>, workouts: List<WorkoutSession>): Int =
        (kata.sumOf { it.durationSec } + workouts.sumOf { it.durationSec }) / 60

    fun career(kata: List<KataSession>, workouts: List<WorkoutSession>, xp: Int, today: LocalDate): CareerStats {
        val full = kata.filter { it.isFullKata }
        val days = activityDays(kata, workouts)
        return CareerStats(
            workouts = workouts.size,
            kataSessions = full.size,
            sectionPractices = kata.size - full.size,
            trainingMinutes = minutes(kata, workouts),
            currentStreak = currentStreak(days, today),
            longestStreak = longestStreak(days),
            xp = xp,
            averageKataScore = if (full.isEmpty()) 0.0 else full.map { it.score }.average(),
            bestKataScore = full.maxOfOrNull { it.score } ?: 0.0
        )
    }

    fun history(kata: List<KataSession>, workouts: List<WorkoutSession>): List<HistoryItem> =
        (kata.map { HistoryItem.Kata(it) } + workouts.map { HistoryItem.Workout(it) }).sortedByDescending { it.startedAt }

    /** Monday - Sunday of the current week. */
    fun week(kata: List<KataSession>, workouts: List<WorkoutSession>, today: LocalDate): List<DayActivity> {
        val start = today.weekStart()
        val counts = (kata.map { it.date } + workouts.map { it.date }).groupingBy { it }.eachCount()
        return (0L..6L).map { i ->
            val d = start.plusDays(i)
            DayActivity(d, counts[d] ?: 0, d == today, d.isAfter(today))
        }
    }

    /** The last [count] days, oldest first, for the streak chart. */
    fun recentDays(kata: List<KataSession>, workouts: List<WorkoutSession>, today: LocalDate, count: Int = 14): List<DayActivity> {
        val counts = (kata.map { it.date } + workouts.map { it.date }).groupingBy { it }.eachCount()
        return (count - 1 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            DayActivity(d, counts[d] ?: 0, d == today, false)
        }
    }

    private fun weekLabel(start: LocalDate, today: LocalDate): String =
        if (start == today.weekStart()) "This wk" else Formatters.shortDate(start)

    fun minutesPerWeek(kata: List<KataSession>, workouts: List<WorkoutSession>, today: LocalDate, weeks: Int = 6): List<ChartPoint> {
        val current = today.weekStart()
        return (weeks - 1 downTo 0).map { back ->
            val start = current.minusWeeks(back.toLong())
            val end = start.plusDays(6)
            val secs = kata.filter { it.date in start..end }.sumOf { it.durationSec } +
                workouts.filter { it.date in start..end }.sumOf { it.durationSec }
            ChartPoint(weekLabel(start, today), (secs / 60).toFloat())
        }
    }

    fun workoutFrequency(workouts: List<WorkoutSession>, today: LocalDate, weeks: Int = 6): List<ChartPoint> {
        val current = today.weekStart()
        return (weeks - 1 downTo 0).map { back ->
            val start = current.minusWeeks(back.toLong())
            ChartPoint(weekLabel(start, today), workouts.count { it.date in start..start.plusDays(6) }.toFloat())
        }
    }

    fun kataScoreTrend(kata: List<KataSession>, last: Int = 8): List<ChartPoint> =
        kata.filter { it.isFullKata }.sortedBy { it.startedAt }.takeLast(last)
            .map { ChartPoint(Formatters.shortDate(it.date), it.score.toFloat()) }
}
