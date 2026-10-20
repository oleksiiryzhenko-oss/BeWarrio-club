package com.katadojo.app.utils

import com.katadojo.app.data.model.KataLibrary
import com.katadojo.app.data.model.KataSession
import com.katadojo.app.data.model.SectionResult
import com.katadojo.app.data.model.SectionStatus
import com.katadojo.app.data.model.SectionStatus.COMPLETED
import com.katadojo.app.data.model.SectionStatus.DIFFICULT
import com.katadojo.app.data.model.SectionStatus.NEEDS_PRACTICE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class KataAnalyzerTest {
    private val kata = KataLibrary.get("heian_shodan")!!
    private val today = LocalDate.of(2026, 9, 25)

    private fun at(daysAgo: Long) = today.minusDays(daysAgo).atTime(19, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun session(id: Long, daysAgo: Long, statuses: List<SectionStatus>, r: List<Int>) = KataSession(
        id = id, kataId = kata.id, startedAt = at(daysAgo), durationSec = 161,
        results = statuses.mapIndexed { i, s -> SectionResult(i, s) },
        technique = r[0], balance = r[1], timing = r[2], confidence = r[3],
        score = KataScoring.personalScore(r[0], r[1], r[2], r[3]),
        accuracy = KataScoring.accuracy(statuses.mapIndexed { i, s -> SectionResult(i, s) })
    )

    @Test
    fun personalScoreIsAverageOfRatings() {
        assertEquals(8.0, KataScoring.personalScore(8, 7, 9, 8), 1e-9)
        assertEquals(7.3, KataScoring.personalScore(7, 7, 8, 7), 1e-9)
    }

    @Test
    fun accuracyWeighsSectionStatus() {
        val results = listOf(COMPLETED, COMPLETED, NEEDS_PRACTICE, COMPLETED, COMPLETED).mapIndexed { i, s -> SectionResult(i, s) }
        assertEquals(92, KataScoring.accuracy(results))
    }

    @Test
    fun analysisFindsStrongAreasAndMostDifficultSection() {
        val s = session(1, 0, listOf(COMPLETED, COMPLETED, DIFFICULT, NEEDS_PRACTICE, COMPLETED), listOf(8, 7, 9, 9))
        val a = KataAnalyzer.analyze(s, kata, listOf(s))
        assertEquals(listOf("Timing", "Confidence"), a.strongAreas)
        assertEquals(listOf("Balance"), a.weakAreas)
        assertEquals(2, a.mostDifficultSection)
        assertTrue(a.suggestion.contains("Section 3"))
        assertEquals(3, a.completed)
    }

    @Test
    fun suggestionRepeatsWeakSectionWhenNeedsPractice() {
        val s = session(1, 0, listOf(COMPLETED, COMPLETED, NEEDS_PRACTICE, COMPLETED, COMPLETED), listOf(8, 8, 8, 8))
        val a = KataAnalyzer.analyze(s, kata, listOf(s))
        assertEquals("Repeat Section 3 before your next full kata.", a.suggestion)
    }

    @Test
    fun insightNeedsEnoughSessions() {
        val two = listOf(
            session(1, 3, listOf(COMPLETED, COMPLETED, NEEDS_PRACTICE, COMPLETED, COMPLETED), listOf(7, 7, 7, 7)),
            session(2, 1, listOf(COMPLETED, COMPLETED, NEEDS_PRACTICE, COMPLETED, COMPLETED), listOf(8, 8, 8, 8))
        )
        assertNull(KataAnalyzer.progress(kata, two, today).insight)

        val three = two + session(3, 0, listOf(COMPLETED, COMPLETED, DIFFICULT, COMPLETED, COMPLETED), listOf(8, 8, 9, 8))
        val p = KataAnalyzer.progress(kata, three, today)
        assertEquals("Section 3 is currently your main practice area.", p.insight)
        assertEquals(3, p.weakestSection?.number)
        assertEquals(3, p.fullSessions)
        assertEquals(3, p.practiceStreak)
        assertEquals(8.3, p.currentScore!!, 1e-9)
        assertEquals(19, p.progressPercent)
    }
}
