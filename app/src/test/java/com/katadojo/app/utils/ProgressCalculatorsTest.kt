package com.katadojo.app.utils

import com.katadojo.app.data.model.GoalType
import com.katadojo.app.data.model.KataSession
import com.katadojo.app.data.model.PersonalGoal
import com.katadojo.app.data.model.ProgressLevel
import com.katadojo.app.data.model.SectionResult
import com.katadojo.app.data.model.SectionStatus
import com.katadojo.app.data.model.WorkoutSession
import com.katadojo.app.data.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ProgressCalculatorsTest {
    private val today = LocalDate.of(2026, 9, 24) // Thursday
    private fun at(d: LocalDate) = d.atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun kata(d: LocalDate, id: String = "heian_shodan", focus: Int? = null, score: Double = 8.0) = KataSession(
        kataId = id, startedAt = at(d), durationSec = 600, focusSection = focus,
        results = listOf(SectionResult(focus ?: 0, SectionStatus.COMPLETED)), score = score
    )

    private fun workout(d: LocalDate, minutes: Int = 45) =
        WorkoutSession(type = WorkoutType.TECHNIQUE, plannedMinutes = 45, durationSec = minutes * 60, effort = 7, startedAt = at(d))

    @Test
    fun levelsMatchThresholds() {
        val green = XpCalculator.level(740)
        assertEquals(ProgressLevel.GREEN, green.level)
        assertEquals(1000, green.nextAt)
        assertEquals(260, green.remaining)
        assertEquals(ProgressLevel.WHITE, XpCalculator.level(0).level)
        assertEquals(ProgressLevel.BLACK, XpCalculator.level(5000).level)
        assertEquals(0, XpCalculator.level(5000).remaining)
    }

    @Test
    fun streaksCountConsecutiveDays() {
        val days = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3), today.minusDays(10), today.minusDays(11))
        assertEquals(3, StatisticsCalculator.currentStreak(days, today))
        assertEquals(3, StatisticsCalculator.longestStreak(days))
        assertEquals(0, StatisticsCalculator.currentStreak(days, today.plusDays(2)))
        assertEquals(2, StatisticsCalculator.runEndingOn(days, today.minusDays(10)))
    }

    @Test
    fun weeklySessionGoalCountsCurrentWeekOnly() {
        val goal = PersonalGoal(id = 1, type = GoalType.SESSIONS_WEEK, target = 3, createdAt = at(today.minusDays(30)))
        val kata = listOf(kata(today.minusDays(1)), kata(today.minusDays(7)))
        val workouts = listOf(workout(today))
        val p = GoalProgressCalculator.progress(goal, kata, workouts, today)
        assertEquals(2.0, p.current, 1e-9)
        assertFalse(p.completed)
        assertEquals(66, p.percent)
    }

    @Test
    fun kataCountGoalCountsSinceCreation() {
        val created = at(today.minusDays(5))
        val goal = PersonalGoal(id = 2, type = GoalType.KATA_COUNT, target = 3, kataId = "heian_shodan", createdAt = created)
        val kata = listOf(
            kata(today.minusDays(8)), kata(today.minusDays(4)), kata(today.minusDays(2)),
            kata(today.minusDays(1)), kata(today.minusDays(1), id = "heian_nidan"), kata(today, focus = 2)
        )
        val p = GoalProgressCalculator.progress(goal, kata, emptyList(), today)
        assertEquals(3.0, p.current, 1e-9)
        assertTrue(p.completed)
    }

    @Test
    fun improveGoalUsesBaseline() {
        val goal = PersonalGoal(id = 3, type = GoalType.IMPROVE_KATA, target = 5, kataId = "heian_shodan", baseline = 7.8, createdAt = at(today.minusDays(3)))
        val p = GoalProgressCalculator.progress(goal, listOf(kata(today.minusDays(1), score = 8.1)), emptyList(), today)
        assertFalse(p.completed)
        val done = GoalProgressCalculator.progress(goal, listOf(kata(today, score = 8.4)), emptyList(), today)
        assertTrue(done.completed)
    }

    @Test
    fun minutesGoalSumsWorkoutsAndKata() {
        val goal = PersonalGoal(id = 4, type = GoalType.MINUTES_WEEK, target = 60, createdAt = at(today.minusDays(20)))
        val p = GoalProgressCalculator.progress(goal, listOf(kata(today)), listOf(workout(today.minusDays(1), 50)), today)
        assertEquals(60.0, p.current, 1e-9)
        assertTrue(p.completed)
    }
}
