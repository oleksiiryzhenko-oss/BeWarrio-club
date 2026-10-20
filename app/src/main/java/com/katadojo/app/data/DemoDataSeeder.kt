package com.katadojo.app.data

import androidx.room.withTransaction
import com.katadojo.app.data.database.GoalEntity
import com.katadojo.app.data.database.KataDojoDatabase
import com.katadojo.app.data.database.RegistrationEntity
import com.katadojo.app.data.database.toEntity
import com.katadojo.app.data.model.Belt
import com.katadojo.app.data.model.ExperienceLevel
import com.katadojo.app.data.model.GoalType
import com.katadojo.app.data.model.KarateStyle
import com.katadojo.app.data.model.KarateTraining
import com.katadojo.app.data.model.KataSession
import com.katadojo.app.data.model.PerceivedDifficulty
import com.katadojo.app.data.model.SectionResult
import com.katadojo.app.data.model.SectionStatus
import com.katadojo.app.data.model.SkillLevel
import com.katadojo.app.data.model.TrainingType
import com.katadojo.app.data.model.UserProfile
import com.katadojo.app.data.model.WorkoutSession
import com.katadojo.app.data.model.WorkoutType
import com.katadojo.app.data.repository.KataRepository
import com.katadojo.app.data.repository.PreferencesRepository
import com.katadojo.app.data.repository.WorkoutRepository
import com.katadojo.app.utils.KataScoring
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Realistic local demo content, generated relative to today, so the app looks complete on first launch. */
class DemoDataSeeder(
    private val db: KataDojoDatabase,
    private val preferences: PreferencesRepository,
    private val kataRepository: KataRepository,
    private val workoutRepository: WorkoutRepository,
    private val engine: ProgressEngine
) {
    private enum class Dojo(val title: String, val address: String) {
        CENTRAL("Central Karate Dojo", "12 Market Square"),
        KYOKUSHIN("Kyokushin Dojo East", "48 East Mill Road"),
        HARBOR("Harbor Goju-ryu Club", "5 Harbor Street"),
        NORTHSIDE("Northside Wado Dojo", "210 North Avenue")
    }

    private data class Template(
        val day: Int, val start: LocalTime, val minutes: Int, val title: String, val type: TrainingType,
        val style: KarateStyle, val dojo: Dojo, val instructor: String, val level: SkillLevel,
        val capacity: Int, val booked: Int, val description: String, val bring: List<String>
    )

    private val gi = listOf("Karate gi", "Belt", "Water", "Towel")

    private val templates = listOf(
        Template(0, LocalTime.of(18, 30), 90, "Traditional Karate Fundamentals", TrainingType.FUNDAMENTALS, KarateStyle.KYOKUSHIN,
            Dojo.KYOKUSHIN, "Coach Daniel Hart", SkillLevel.INTERMEDIATE, 20, 13,
            "Stances, blocks and strikes practiced with traditional counting, followed by basic combinations and partner drills.", gi),
        Template(0, LocalTime.of(7, 0), 60, "Conditioning Session", TrainingType.CONDITIONING, KarateStyle.GOJU_RYU,
            Dojo.HARBOR, "Coach Elena Ruiz", SkillLevel.ALL_LEVELS, 18, 11,
            "Karate-specific conditioning: stance holds, core work, explosive footwork and breathing exercises.",
            listOf("Training clothes or gi", "Water", "Towel")),
        Template(2, LocalTime.of(18, 30), 90, "Kata Training", TrainingType.KATA, KarateStyle.SHOTOKAN,
            Dojo.CENTRAL, "Coach Daniel Hart", SkillLevel.INTERMEDIATE, 20, 11,
            "Detailed work on the Heian kata: rhythm, stance transitions and applications, then full runs with feedback.", gi),
        Template(1, LocalTime.of(17, 30), 60, "Beginner Karate", TrainingType.FUNDAMENTALS, KarateStyle.WADO_RYU,
            Dojo.NORTHSIDE, "Sensei Aiko Sato", SkillLevel.BEGINNER, 16, 9,
            "A friendly first class: etiquette, basic stances, blocks and punches, and a short introduction to kata.",
            listOf("Comfortable training clothes", "Water", "Towel")),
        Template(1, LocalTime.of(19, 0), 90, "Kumite Fundamentals", TrainingType.KUMITE, KarateStyle.KYOKUSHIN,
            Dojo.KYOKUSHIN, "Coach Tom Becker", SkillLevel.INTERMEDIATE, 14, 10,
            "Distance, timing and footwork for controlled sparring, with pad drills and light partner work.",
            gi + listOf("Mouthguard", "Hand pads")),
        Template(3, LocalTime.of(10, 0), 120, "Open Dojo Practice", TrainingType.OPEN_DOJO, KarateStyle.SHOTOKAN,
            Dojo.CENTRAL, "Coach Daniel Hart", SkillLevel.ALL_LEVELS, 30, 9,
            "Free practice time on the dojo floor. Work on your own kata and techniques with an instructor available for questions.", gi),
        Template(3, LocalTime.of(13, 0), 180, "Weekend Karate Seminar", TrainingType.SEMINAR, KarateStyle.MIXED,
            Dojo.HARBOR, "Sensei Yuki Mori", SkillLevel.ALL_LEVELS, 40, 28,
            "A three-hour seminar on kata applications (bunkai) open to all styles, with a short Q&A at the end.",
            gi + listOf("Notebook")),
        Template(4, LocalTime.of(18, 0), 75, "Footwork & Stance Training", TrainingType.FOOTWORK, KarateStyle.SHOTOKAN,
            Dojo.CENTRAL, "Sensei Aiko Sato", SkillLevel.BEGINNER, 16, 6,
            "Deep, stable stances and smooth transitions. Build the foundation that makes every kata stronger.", gi),
        Template(5, LocalTime.of(19, 0), 90, "Advanced Karate", TrainingType.FUNDAMENTALS, KarateStyle.KYOKUSHIN,
            Dojo.KYOKUSHIN, "Coach Tom Becker", SkillLevel.ADVANCED, 12, 10,
            "High-intensity technique session for experienced karateka: complex combinations, advanced kata and conditioning.", gi),
        Template(6, LocalTime.of(18, 30), 120, "Competition Preparation", TrainingType.COMPETITION, KarateStyle.SHOTOKAN,
            Dojo.CENTRAL, "Coach Daniel Hart", SkillLevel.ADVANCED, 12, 11,
            "Tournament-style kata and kumite rounds with scoring rules, timing and mental preparation.",
            gi + listOf("Mouthguard", "Competition mitts"))
    )

    private val demoRegistrations = listOf(0, 2)

    fun sessions(today: LocalDate, withDemoRegistrations: Boolean): List<KarateTraining> =
        templates.mapIndexed { index, t ->
            val extra = if (withDemoRegistrations && index in demoRegistrations) 1 else 0
            KarateTraining(
                id = today.toEpochDay() * 100 + index, title = t.title, type = t.type, style = t.style,
                date = today.plusDays(t.day.toLong()), startTime = t.start, durationMinutes = t.minutes,
                dojo = t.dojo.title, dojoAddress = t.dojo.address, instructor = t.instructor, level = t.level,
                capacity = t.capacity, booked = (t.booked + extra).coerceAtMost(t.capacity),
                description = t.description, whatToBring = t.bring
            )
        }

    suspend fun seedIfNeeded(today: LocalDate = LocalDate.now()) {
        if (!preferences.isSeeded()) loadDemo(today) else ensureUpcomingSessions(today)
    }

    private suspend fun clearTables() {
        db.registrationDao().clear()
        db.kataSessionDao().clearAll()
        db.workoutDao().clear()
        db.goalDao().clear()
        db.xpDao().clear()
        db.trainingDao().clear()
    }

    private fun millis(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    suspend fun loadDemo(today: LocalDate = LocalDate.now()) {
        val now = System.currentTimeMillis()
        val trainings = sessions(today, withDemoRegistrations = true)
        db.withTransaction {
            clearTables()
            db.trainingDao().insertAll(trainings.map { it.toEntity() })
            db.registrationDao().insertAll(demoRegistrations.mapIndexed { i, index ->
                RegistrationEntity(
                    trainingId = trainings[index].id, fullName = "Sam Rivera", email = "sam.rivera@example.com",
                    phone = "+1 555 0187", experience = ExperienceLevel.INTERMEDIATE.name, currentBelt = Belt.GREEN.name,
                    preferredType = TrainingType.KATA.name, comment = "", createdAt = now - (i + 2) * 86_400_000L
                )
            })
            val weekly = millis(today.minusDays(25), 9)
            val recent = millis(today.minusDays(6), 9)
            db.goalDao().insert(GoalEntity(0, GoalType.SESSIONS_WEEK.name, 3, null, null, weekly, false))
            db.goalDao().insert(GoalEntity(0, GoalType.MINUTES_WEEK.name, 90, null, null, weekly, false))
            db.goalDao().insert(GoalEntity(0, GoalType.STREAK.name, 7, null, null, weekly, false))
            db.goalDao().insert(GoalEntity(0, GoalType.WEAK_SECTION.name, 5, null, null, recent, false))
            val current = db.goalDao().insert(GoalEntity(0, GoalType.KATA_COUNT.name, 5, "heian_shodan", null, recent, false))
            db.goalDao().setCurrent(current)
        }

        // Replay the demo history in order so XP, goals and streaks are awarded exactly as in real use.
        val C = SectionStatus.COMPLETED
        val N = SectionStatus.NEEDS_PRACTICE
        val D = SectionStatus.DIFFICULT
        data class K(val day: Int, val hour: Int, val kata: String, val statuses: List<SectionStatus>, val r: List<Int>, val min: Int, val diff: PerceivedDifficulty)
        data class S(val day: Int, val hour: Int, val kata: String, val section: Int, val status: SectionStatus, val min: Int)
        data class W(val day: Int, val hour: Int, val type: WorkoutType, val planned: Int, val min: Int, val effort: Int)

        val activities: List<Pair<Long, suspend () -> Unit>> = buildList {
            fun kata(k: K) = add(millis(today.minusDays(k.day.toLong()), k.hour) to suspend {
                val results = k.statuses.mapIndexed { i, s -> SectionResult(i, s) }
                val session = KataSession(
                    kataId = k.kata, startedAt = millis(today.minusDays(k.day.toLong()), k.hour), durationSec = k.min * 60 + 17,
                    results = results, technique = k.r[0], balance = k.r[1], timing = k.r[2], confidence = k.r[3],
                    perceivedDifficulty = k.diff, score = KataScoring.personalScore(k.r[0], k.r[1], k.r[2], k.r[3]),
                    accuracy = KataScoring.accuracy(results)
                )
                val id = kataRepository.save(session)
                engine.onKataSaved(session.copy(id = id), session.startedAt + session.durationSec * 1000L)
                Unit
            })
            fun section(s: S) = add(millis(today.minusDays(s.day.toLong()), s.hour, 30) to suspend {
                val results = listOf(SectionResult(s.section, s.status))
                val session = KataSession(
                    kataId = s.kata, startedAt = millis(today.minusDays(s.day.toLong()), s.hour, 30), durationSec = s.min * 60,
                    focusSection = s.section, results = results, score = s.status.rating.toDouble(),
                    accuracy = KataScoring.accuracy(results)
                )
                val id = kataRepository.save(session)
                engine.onKataSaved(session.copy(id = id), session.startedAt + session.durationSec * 1000L)
                Unit
            })
            fun workout(w: W) = add(millis(today.minusDays(w.day.toLong()), w.hour) to suspend {
                val workout = WorkoutSession(
                    type = w.type, plannedMinutes = w.planned, durationSec = w.min * 60 + 23, effort = w.effort,
                    startedAt = millis(today.minusDays(w.day.toLong()), w.hour)
                )
                val id = workoutRepository.save(workout)
                engine.onWorkoutSaved(workout.copy(id = id), workout.startedAt + workout.durationSec * 1000L)
                Unit
            })
            val hs = "heian_shodan"
            workout(W(18, 18, WorkoutType.TECHNIQUE, 45, 44, 7))
            kata(K(18, 20, "taikyoku_shodan", listOf(C, C, N, C), listOf(7, 7, 6, 7), 9, PerceivedDifficulty.EASY))
            kata(K(17, 19, hs, listOf(C, N, D, N, C), listOf(7, 6, 7, 6), 12, PerceivedDifficulty.MODERATE))
            workout(W(16, 18, WorkoutType.CONDITIONING, 30, 31, 8))
            kata(K(15, 19, "heian_nidan", listOf(C, C, N, C, N), listOf(7, 8, 7, 7), 10, PerceivedDifficulty.MODERATE))
            workout(W(14, 18, WorkoutType.KUMITE, 60, 58, 8))
            kata(K(13, 19, hs, listOf(C, C, N, C, C), listOf(8, 7, 7, 7), 14, PerceivedDifficulty.MODERATE))
            workout(W(12, 18, WorkoutType.FLEXIBILITY, 30, 30, 6))
            kata(K(4, 19, hs, listOf(C, C, N, C, C), listOf(8, 7, 8, 8), 16, PerceivedDifficulty.MODERATE))
            section(S(4, 19, hs, 2, N, 6))
            workout(W(3, 18, WorkoutType.TECHNIQUE, 60, 57, 8))
            section(S(3, 20, hs, 2, C, 5))
            kata(K(2, 19, hs, listOf(C, C, N, C, C), listOf(8, 7, 9, 8), 18, PerceivedDifficulty.MODERATE))
            kata(K(2, 20, "tekki_shodan", listOf(C, N, D, C), listOf(7, 6, 7, 7), 11, PerceivedDifficulty.DIFFICULT))
            kata(K(1, 19, hs, listOf(C, C, N, C, C), listOf(9, 7, 9, 8), 18, PerceivedDifficulty.EASY))
            section(S(1, 19, hs, 2, N, 7))
            workout(W(1, 18, WorkoutType.KATA, 45, 46, 7))
        }
        activities.sortedBy { it.first }.forEach { it.second() }

        preferences.clearUserPreferences()
        preferences.saveProfile(
            UserProfile("Sam", KarateStyle.SHOTOKAN, ExperienceLevel.INTERMEDIATE, Belt.GREEN, "Pass my next grading with a clean Heian Shodan")
        )
        preferences.setSeeded(true)
    }

    /** Clears user data and settings. The dojo schedule stays available. */
    suspend fun resetLocalData(today: LocalDate = LocalDate.now()) {
        db.withTransaction {
            clearTables()
            db.trainingDao().insertAll(sessions(today, false).map { it.toEntity() })
        }
        preferences.clearUserPreferences()
        preferences.setSeeded(true)
    }

    private suspend fun ensureUpcomingSessions(today: LocalDate) {
        val latest = db.trainingDao().latestDate()
        if (latest == null || latest < today.plusDays(4).toEpochDay()) {
            db.trainingDao().insertAll(sessions(today, false).map { it.toEntity() })
        }
    }
}
