package com.katadojo.app.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class KarateTraining(
    val id: Long,
    val title: String,
    val type: TrainingType,
    val style: KarateStyle,
    val date: LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val dojo: String,
    val dojoAddress: String,
    val instructor: String,
    val level: SkillLevel,
    val capacity: Int,
    val booked: Int,
    val description: String,
    val whatToBring: List<String>,
    val isRegistered: Boolean = false
) {
    val endTime: LocalTime get() = startTime.plusMinutes(durationMinutes.toLong())
    val startDateTime: LocalDateTime get() = LocalDateTime.of(date, startTime)
    val placesLeft: Int get() = (capacity - booked).coerceAtLeast(0)
    val isFull: Boolean get() = booked >= capacity
    fun hasEnded(now: LocalDateTime = LocalDateTime.now()): Boolean =
        startDateTime.plusMinutes(durationMinutes.toLong()).isBefore(now)
}

data class TrainingRegistration(
    val id: Long = 0,
    val trainingId: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    val experience: ExperienceLevel,
    val currentBelt: Belt?,
    val preferredType: TrainingType?,
    val comment: String,
    val createdAt: Long
)

data class RegistrationWithTraining(val registration: TrainingRegistration, val training: KarateTraining)

data class SectionResult(val sectionIndex: Int, val status: SectionStatus)

data class KataSession(
    val id: Long = 0,
    val kataId: String,
    val startedAt: Long,
    val durationSec: Int,
    /** Null for a full kata; the section index for a single-section practice. */
    val focusSection: Int? = null,
    val results: List<SectionResult>,
    val technique: Int = 0,
    val balance: Int = 0,
    val timing: Int = 0,
    val confidence: Int = 0,
    val perceivedDifficulty: PerceivedDifficulty? = null,
    val score: Double = 0.0,
    val accuracy: Int = 0
) {
    val isFullKata: Boolean get() = focusSection == null
    val date: LocalDate get() = startedAt.toDate()
    val kata: Kata? get() = KataLibrary.get(kataId)
    val completedCount: Int get() = results.count { it.status == SectionStatus.COMPLETED }
    val needsPracticeSections: List<Int> get() = results.filter { it.status == SectionStatus.NEEDS_PRACTICE }.map { it.sectionIndex }
    val difficultSections: List<Int> get() = results.filter { it.status == SectionStatus.DIFFICULT }.map { it.sectionIndex }
}

data class WorkoutSession(
    val id: Long = 0,
    val type: WorkoutType,
    val plannedMinutes: Int,
    val durationSec: Int,
    val effort: Int,
    val startedAt: Long
) {
    val date: LocalDate get() = startedAt.toDate()
    val minutes: Int get() = (durationSec + 30) / 60
}

enum class GoalType(val weekly: Boolean, val needsKata: Boolean, val defaultTarget: Int) {
    SESSIONS_WEEK(true, false, 3),
    KATA_WEEK(true, false, 3),
    WORKOUTS_WEEK(true, false, 4),
    KATA_COUNT(false, true, 5),
    IMPROVE_KATA(false, true, 5),
    WEAK_SECTION(false, false, 5),
    MINUTES_WEEK(true, false, 60),
    STREAK(false, false, 7);

    fun title(target: Int, kataName: String?): String = when (this) {
        SESSIONS_WEEK -> "Complete $target karate sessions this week"
        KATA_WEEK -> "Practice kata $target times this week"
        WORKOUTS_WEEK -> "Complete $target karate workouts this week"
        KATA_COUNT -> "Practice ${kataName ?: "a kata"} $target times"
        IMPROVE_KATA -> "Improve ${kataName ?: "kata"} score by ${String.format(java.util.Locale.US, "%.1f", target / 10.0)}"
        WEAK_SECTION -> "Practice weakest kata section $target times"
        MINUTES_WEEK -> "Train for $target minutes this week"
        STREAK -> "Maintain a $target-day training streak"
    }

    /** Selectable targets. IMPROVE_KATA targets are tenths of a point (5 = +0.5). */
    val targetOptions: List<Int>
        get() = when (this) {
            SESSIONS_WEEK, KATA_WEEK -> listOf(2, 3, 4, 5)
            WORKOUTS_WEEK -> listOf(2, 3, 4, 5)
            KATA_COUNT, WEAK_SECTION -> listOf(3, 5, 8, 10)
            IMPROVE_KATA -> listOf(3, 5, 10)
            MINUTES_WEEK -> listOf(60, 90, 120, 180)
            STREAK -> listOf(3, 5, 7)
        }
}

data class PersonalGoal(
    val id: Long = 0,
    val type: GoalType,
    val target: Int,
    val kataId: String? = null,
    /** Best score of the kata when an IMPROVE_KATA goal was set. */
    val baseline: Double? = null,
    val createdAt: Long,
    val isCurrent: Boolean = false
) {
    val kata: Kata? get() = KataLibrary.get(kataId)
    val title: String get() = type.title(target, kata?.name)
}

data class GoalProgress(
    val current: Double,
    val target: Double,
    val completed: Boolean,
    val currentLabel: String,
    val targetLabel: String,
    val detail: String
) {
    val fraction: Float get() = if (target <= 0) 0f else (current / target).toFloat().coerceIn(0f, 1f)
    val percent: Int get() = (fraction * 100).toInt()
}

enum class XpType(val points: Int, val label: String) {
    WORKOUT(20, "Workout completed"),
    KATA(30, "Kata session completed"),
    SECTION_PRACTICE(15, "Weak section practiced"),
    GOAL(50, "Goal completed"),
    STREAK(75, "7-day training streak")
}

data class XpEvent(val id: Long = 0, val key: String, val type: XpType, val points: Int, val label: String, val createdAt: Long)

/** App progress levels. These are motivational levels, not official karate ranks. */
enum class ProgressLevel(val label: String, val minXp: Int, val color: Long) {
    WHITE("White", 0, 0xFFF5F1EA), YELLOW("Yellow", 150, 0xFFE8C547), ORANGE("Orange", 350, 0xFFE08A3C),
    GREEN("Green", 600, 0xFF4E9A5B), BLUE("Blue", 1000, 0xFF3E6FB0), BROWN("Brown", 1500, 0xFF7A5234),
    BLACK("Black", 2200, 0xFF101010)
}

data class UserProfile(
    val displayName: String = "Karateka",
    val style: KarateStyle = KarateStyle.SHOTOKAN,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val belt: Belt = Belt.WHITE,
    val trainingGoal: String = ""
)

data class AppSettings(
    val timerSounds: Boolean = true,
    val haptics: Boolean = true,
    val keepScreenOn: Boolean = true,
    val autoAdvanceSections: Boolean = true
)

fun Long.toDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
