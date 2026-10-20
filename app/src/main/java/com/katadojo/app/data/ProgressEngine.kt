package com.katadojo.app.data

import com.katadojo.app.data.model.KataSession
import com.katadojo.app.data.model.WorkoutSession
import com.katadojo.app.data.model.XpType
import com.katadojo.app.data.model.toDate
import com.katadojo.app.data.repository.GoalRepository
import com.katadojo.app.data.repository.KataRepository
import com.katadojo.app.data.repository.WorkoutRepository
import com.katadojo.app.data.repository.XpRepository
import com.katadojo.app.utils.GoalProgressCalculator
import com.katadojo.app.utils.StatisticsCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

data class XpReward(val label: String, val points: Int)

/**
 * Awards Training Points (XP) after every saved activity: the activity itself, any goal
 * it completes and a 7-day streak. Every award has a unique key, so it is given only once.
 */
class ProgressEngine(
    private val kataRepository: KataRepository,
    private val workoutRepository: WorkoutRepository,
    private val goalRepository: GoalRepository,
    private val xpRepository: XpRepository
) {
    suspend fun onKataSaved(session: KataSession, now: Long = System.currentTimeMillis()): List<XpReward> {
        val rewards = mutableListOf<XpReward>()
        val name = session.kata?.name ?: "Kata"
        if (session.isFullKata) {
            award(rewards, "kata:${session.id}", XpType.KATA, "Completed $name", now)
        } else {
            val section = (session.focusSection ?: 0) + 1
            award(rewards, "section:${session.id}", XpType.SECTION_PRACTICE, "Practiced $name, Section $section", now)
        }
        rewards += evaluate(session.date, now)
        return rewards
    }

    suspend fun onWorkoutSaved(workout: WorkoutSession, now: Long = System.currentTimeMillis()): List<XpReward> {
        val rewards = mutableListOf<XpReward>()
        award(rewards, "workout:${workout.id}", XpType.WORKOUT, "${workout.type.label} workout", now)
        rewards += evaluate(workout.date, now)
        return rewards
    }

    /** Checks goals and the streak as of [date]. */
    suspend fun evaluate(date: LocalDate = LocalDate.now(), now: Long = System.currentTimeMillis()): List<XpReward> {
        val rewards = mutableListOf<XpReward>()
        val kata = kataRepository.sessions.first().filter { !it.date.isAfter(date) }
        val workouts = workoutRepository.workouts.first().filter { !it.date.isAfter(date) }

        goalRepository.goals.first()
            .filter { !it.createdAt.toDate().isAfter(date) }
            .forEach { goal ->
                val progress = GoalProgressCalculator.progress(goal, kata, workouts, date)
                if (progress.completed) {
                    val key = "goal:${goal.id}:${GoalProgressCalculator.periodKey(goal, date)}"
                    award(rewards, key, XpType.GOAL, "Goal: ${goal.title}", now)
                }
            }

        val days = StatisticsCalculator.activityDays(kata, workouts)
        val run = StatisticsCalculator.runEndingOn(days, date)
        if (run >= 7) {
            val start = date.minusDays((run - 1).toLong())
            award(rewards, "streak:${start.toEpochDay()}", XpType.STREAK, "7-day training streak", now)
        }
        return rewards
    }

    private suspend fun award(into: MutableList<XpReward>, key: String, type: XpType, label: String, now: Long) {
        if (xpRepository.award(key, type, label, now)) into += XpReward(label, type.points)
    }
}
