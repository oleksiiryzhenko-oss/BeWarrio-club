package com.katadojo.app.utils

import com.katadojo.app.data.model.Kata
import com.katadojo.app.data.model.KataLibrary
import com.katadojo.app.data.model.KataSession
import com.katadojo.app.data.model.SectionResult
import com.katadojo.app.data.model.SectionStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object KataScoring {
    /** Personal Kata Score: the average of the four self-assessment ratings, one decimal. */
    fun personalScore(technique: Int, balance: Int, timing: Int, confidence: Int): Double =
        ((technique + balance + timing + confidence) / 4.0 * 10).roundToInt() / 10.0

    /** Accuracy: Completed = 100 %, Needs Practice = 60 %, Difficult = 30 % per section. */
    fun accuracy(results: List<SectionResult>): Int =
        if (results.isEmpty()) 0 else results.map { it.status.rating * 10 }.average().roundToInt()
}

data class SessionAnalysis(
    val averageSectionRating: Double,
    val completed: Int,
    val needsPractice: Int,
    val difficult: Int,
    val totalSections: Int,
    val ratings: List<Pair<String, Int>>,
    val strongAreas: List<String>,
    val weakAreas: List<String>,
    val mostDifficultSection: Int?,
    val practiceTimeSec: Int,
    val suggestion: String
)

data class SectionStat(
    val index: Int,
    val title: String,
    val attempts: Int,
    val averageRating: Double,
    val completedCount: Int,
    val needsPracticeCount: Int,
    val difficultCount: Int,
    val lastStatus: SectionStatus?
) {
    val number: Int get() = index + 1
}

data class KataProgress(
    val fullSessions: Int,
    val sectionPractices: Int,
    val currentScore: Double?,
    val bestScore: Double?,
    val progressPercent: Int?,
    val practiceStreak: Int,
    val lastPracticed: LocalDate?,
    val masteryPercent: Int,
    val weakestSection: SectionStat?,
    val strongestSection: SectionStat?,
    val insight: String?
)

/** Deterministic, local kata analysis. Nothing here is estimated or invented. */
object KataAnalyzer {
    const val MIN_SESSIONS_FOR_INSIGHTS = 3
    const val NOT_ENOUGH_DATA = "Complete more kata sessions to unlock personal insights."

    fun analyze(session: KataSession, kata: Kata, history: List<KataSession>): SessionAnalysis {
        val results = session.results
        val ratings = if (session.isFullKata) listOf(
            "Technique" to session.technique,
            "Balance" to session.balance,
            "Timing" to session.timing,
            "Confidence" to session.confidence
        ) else emptyList()
        val max = ratings.maxOfOrNull { it.second } ?: 0
        val min = ratings.minOfOrNull { it.second } ?: 0
        val strong = ratings.filter { it.second == max && max >= 7 }.map { it.first }
        val weak = when {
            ratings.isEmpty() -> emptyList()
            min < max -> ratings.filter { it.second == min }.map { it.first }
            min <= 5 -> ratings.map { it.first }
            else -> emptyList()
        }

        val stats = sectionStats(kata, history.filter { it.kataId == kata.id && it.id != session.id })
            .associateBy { it.index }
        val worst = results.filter { it.status != SectionStatus.COMPLETED }
            .minWithOrNull(
                compareBy<SectionResult> { it.status.rating }
                    .thenBy { stats[it.sectionIndex]?.averageRating ?: 10.0 }
                    .thenBy { it.sectionIndex }
            )
        val worstNumber = worst?.sectionIndex?.plus(1)

        val suggestion = when {
            worst?.status == SectionStatus.DIFFICULT ->
                "Break Section $worstNumber into slow repetitions, then repeat it before your next full kata."
            worst != null -> "Repeat Section $worstNumber before your next full kata."
            weak.isNotEmpty() -> "All sections completed. Focus on ${weak.first().lowercase()} in your next session."
            else -> "Clean session. Try a timed run next time to build rhythm."
        }

        return SessionAnalysis(
            averageSectionRating = if (results.isEmpty()) 0.0 else results.map { it.status.rating }.average(),
            completed = results.count { it.status == SectionStatus.COMPLETED },
            needsPractice = results.count { it.status == SectionStatus.NEEDS_PRACTICE },
            difficult = results.count { it.status == SectionStatus.DIFFICULT },
            totalSections = results.size,
            ratings = ratings,
            strongAreas = strong,
            weakAreas = weak,
            mostDifficultSection = worst?.sectionIndex,
            practiceTimeSec = session.durationSec,
            suggestion = suggestion
        )
    }

    /** Per-section statistics from every full session and section practice of this kata. */
    fun sectionStats(kata: Kata, sessions: List<KataSession>): List<SectionStat> {
        val ofKata = sessions.filter { it.kataId == kata.id }.sortedBy { it.startedAt }
        return kata.sections.map { section ->
            val results = ofKata.flatMap { s -> s.results.filter { it.sectionIndex == section.index } }
            SectionStat(
                index = section.index,
                title = section.title,
                attempts = results.size,
                averageRating = if (results.isEmpty()) 0.0 else results.map { it.status.rating }.average(),
                completedCount = results.count { it.status == SectionStatus.COMPLETED },
                needsPracticeCount = results.count { it.status == SectionStatus.NEEDS_PRACTICE },
                difficultCount = results.count { it.status == SectionStatus.DIFFICULT },
                lastStatus = results.lastOrNull()?.status
            )
        }
    }

    fun progress(kata: Kata, sessions: List<KataSession>, today: LocalDate = LocalDate.now()): KataProgress {
        val ofKata = sessions.filter { it.kataId == kata.id }.sortedBy { it.startedAt }
        val full = ofKata.filter { it.isFullKata }
        val stats = sectionStats(kata, ofKata).filter { it.attempts > 0 }
        val enough = full.size >= 2 && stats.isNotEmpty()
        val weakest = if (enough) stats.minWithOrNull(compareBy<SectionStat> { it.averageRating }.thenBy { it.index }) else null
        val strongest = if (enough) stats.maxWithOrNull(compareBy<SectionStat> { it.averageRating }.thenBy { -it.index }) else null
        val first = full.firstOrNull()?.score
        val current = full.lastOrNull()?.score

        val insight = when {
            full.size < MIN_SESSIONS_FOR_INSIGHTS -> null
            weakest != null && strongest != null && weakest.averageRating < strongest.averageRating ->
                "Section ${weakest.number} is currently your main practice area."
            else -> "Your sections are consistently even. Work on timing and rhythm across the whole kata."
        }

        return KataProgress(
            fullSessions = full.size,
            sectionPractices = ofKata.size - full.size,
            currentScore = current,
            bestScore = full.maxOfOrNull { it.score },
            progressPercent = if (full.size >= 2 && first != null && current != null && first > 0)
                ((current - first) / first * 100).roundToInt() else null,
            practiceStreak = practiceStreak(ofKata.map { it.date }, today),
            lastPracticed = ofKata.lastOrNull()?.date,
            masteryPercent = full.lastOrNull()?.accuracy ?: 0,
            weakestSection = weakest,
            strongestSection = strongest,
            insight = insight
        )
    }

    /** Sessions in a row (newest first) with no gap longer than 3 days, counted only if still current. */
    fun practiceStreak(dates: List<LocalDate>, today: LocalDate): Int {
        val sorted = dates.sortedDescending()
        if (sorted.isEmpty() || ChronoUnit.DAYS.between(sorted.first(), today) > 3) return 0
        var count = 1
        for (i in 1 until sorted.size) {
            if (ChronoUnit.DAYS.between(sorted[i], sorted[i - 1]) > 3) break
            count++
        }
        return count
    }

    /** The weakest section across all kata with at least two attempts. */
    fun weakestOverall(sessions: List<KataSession>): Pair<Kata, SectionStat>? =
        KataLibrary.all.flatMap { kata -> sectionStats(kata, sessions).filter { it.attempts >= 2 }.map { kata to it } }
            .filter { it.second.averageRating < SectionStatus.COMPLETED.rating }
            .minByOrNull { it.second.averageRating }
}
