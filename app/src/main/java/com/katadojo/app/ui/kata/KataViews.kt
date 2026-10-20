package com.katadojo.app.ui.kata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.katadojo.app.data.XpReward
import com.katadojo.app.data.model.Kata
import com.katadojo.app.data.model.KataSession
import com.katadojo.app.ui.components.*
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters
import com.katadojo.app.utils.KataAnalyzer
import com.katadojo.app.utils.KataProgress
import com.katadojo.app.utils.SectionStat
import com.katadojo.app.utils.SessionAnalysis

@Composable
fun Bullet(text: String, color: androidx.compose.ui.graphics.Color = DojoRed) {
    Row(Modifier.padding(top = 8.dp)) {
        Box(Modifier.padding(top = 7.dp).size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Rice)
    }
}

/** "KATA SESSION COMPLETE" scoreboard. */
@Composable
fun SessionSummaryCard(session: KataSession, kata: Kata, analysis: SessionAnalysis) {
    DojoCard(accent = DojoRed) {
        Overline(if (session.isFullKata) "Kata session complete" else "Section practice complete", color = DojoRed)
        Spacer(Modifier.height(4.dp))
        Text(kata.name, style = MaterialTheme.typography.headlineMedium, color = Rice)
        Spacer(Modifier.height(14.dp))
        if (session.isFullKata) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingProgress(session.accuracy / 100f, Modifier.size(96.dp), strokeWidth = 8.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${session.accuracy}%", style = MaterialTheme.typography.headlineSmall, color = Rice)
                        Text("ACCURACY", style = MaterialTheme.typography.labelSmall, color = Stone)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Overline("Personal Kata Score")
                    ScoreBadge(session.score, large = true)
                }
            }
            Spacer(Modifier.height(14.dp))
            StatGrid(
                listOf(
                    "${session.confidence} / 10" to "Confidence",
                    "${analysis.completed} / ${analysis.totalSections}" to "Sections completed",
                    (session.needsPracticeSections + session.difficultSections).sorted()
                        .joinToString(", ") { "Section ${it + 1}" }.ifEmpty { "None" } to "Needs practice",
                    Formatters.duration(session.durationSec) to "Practice time"
                )
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Personal self-assessment metric, not an official instructor grade.",
                style = MaterialTheme.typography.bodySmall,
                color = StoneDark
            )
        } else {
            val result = session.results.firstOrNull()
            val section = kata.sections.getOrNull(session.focusSection ?: 0)
            Text("Section ${(session.focusSection ?: 0) + 1}: ${section?.title ?: ""}", style = MaterialTheme.typography.titleMedium, color = Rice)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(result?.status)
                Spacer(Modifier.width(10.dp))
                Text("Practice time ${Formatters.duration(session.durationSec)}", style = MaterialTheme.typography.bodySmall, color = Stone)
            }
        }
    }
}

/** Strong areas, needs practice, most difficult section and the next practice suggestion. */
@Composable
fun AnalysisCard(session: KataSession, analysis: SessionAnalysis) {
    DojoCard {
        Overline("Kata analysis")
        Spacer(Modifier.height(10.dp))
        if (session.isFullKata) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("STRONG AREAS", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                    Spacer(Modifier.height(4.dp))
                    Text(analysis.strongAreas.joinToString("\n").ifEmpty { "No standout area yet" }, style = MaterialTheme.typography.titleMedium, color = Rice)
                }
                Column(Modifier.weight(1f)) {
                    Text("NEEDS PRACTICE", style = MaterialTheme.typography.labelSmall, color = Amber)
                    Spacer(Modifier.height(4.dp))
                    Text(analysis.weakAreas.joinToString("\n").ifEmpty { "Nothing stands out" }, style = MaterialTheme.typography.titleMedium, color = Rice)
                }
            }
            Spacer(Modifier.height(12.dp))
            analysis.ratings.forEach { (label, value) ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = StoneLight, modifier = Modifier.width(88.dp))
                    ThickProgressBar(value / 10f, Modifier.weight(1f), color = if (value >= 8) SuccessGreen else if (value <= 5) ErrorRed else Beige, height = 8.dp)
                    Text("$value", style = MaterialTheme.typography.titleSmall, color = Rice, modifier = Modifier.width(32.dp).padding(start = 10.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        InfoRow("Most difficult section", analysis.mostDifficultSection?.let { "Section ${it + 1}" } ?: "None")
        InfoRow("Average section rating", Formatters.decimal(analysis.averageSectionRating) + " / 10")
        InfoRow("Practice time", Formatters.duration(analysis.practiceTimeSec))
        Spacer(Modifier.height(10.dp))
        DojoCard(containerColor = CharcoalLight, contentPadding = PaddingValues(14.dp)) {
            Overline("Your next practice suggestion", color = Beige)
            Spacer(Modifier.height(4.dp))
            Text(analysis.suggestion, style = MaterialTheme.typography.bodyLarge, color = Rice)
        }
    }
}

@Composable
fun SectionResultsList(kata: Kata, session: KataSession) {
    DojoCard {
        Overline("Section results")
        Spacer(Modifier.height(6.dp))
        session.results.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${r.sectionIndex + 1}", style = MaterialTheme.typography.titleMedium, color = Stone, modifier = Modifier.width(28.dp))
                Text(kata.sections.getOrNull(r.sectionIndex)?.title ?: "", style = MaterialTheme.typography.bodyMedium, color = Rice, modifier = Modifier.weight(1f))
                StatusPill(r.status)
            }
        }
    }
}

/** Long-term progress for one kata; insights appear only once enough sessions exist. */
@Composable
fun KataProgressCard(progress: KataProgress) {
    DojoCard {
        Overline("Kata progress")
        Spacer(Modifier.height(10.dp))
        StatGrid(
            listOf(
                (progress.currentScore?.let { Formatters.score(it) } ?: "-") to "Current / 10",
                (progress.bestScore?.let { Formatters.score(it) } ?: "-") to "Best / 10",
                "${progress.fullSessions}" to "Sessions",
                (progress.progressPercent?.let { (if (it >= 0) "+" else "") + "$it%" } ?: "-") to "Progress",
                "${progress.practiceStreak}" to "Practice streak",
                "${progress.sectionPractices}" to "Section practices"
            ),
            columns = 3
        )
        Spacer(Modifier.height(10.dp))
        InfoRow("Weakest section", progress.weakestSection?.let { "Section ${it.number}" } ?: "Not enough data")
        InfoRow("Strongest section", progress.strongestSection?.let { "Section ${it.number}" } ?: "Not enough data")
        Spacer(Modifier.height(8.dp))
        Text(
            progress.insight ?: "Practice this kata a few more times to unlock personal insights.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (progress.insight != null) Beige else Stone
        )
    }
}

@Composable
fun SectionBreakdownCard(stats: List<SectionStat>, lastSession: KataSession?) {
    DojoCard {
        Overline("Section breakdown")
        Text(
            if (lastSession != null) "Latest session, with averages from all practice" else "Practice this kata to see results per section",
            style = MaterialTheme.typography.bodySmall,
            color = Stone
        )
        Spacer(Modifier.height(6.dp))
        stats.forEach { s ->
            val latest = lastSession?.results?.firstOrNull { it.sectionIndex == s.index }?.status
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Section ${s.number}", style = MaterialTheme.typography.titleSmall, color = Rice)
                    Text(
                        s.title + if (s.attempts > 0) " · avg ${Formatters.decimal(s.averageRating)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Stone
                    )
                }
                StatusPill(latest)
            }
        }
    }
}

@Composable
fun RewardsCard(rewards: List<XpReward>) {
    if (rewards.isEmpty()) return
    DojoCard(accent = Beige) {
        Overline("Training Points earned", color = Beige)
        rewards.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(r.label, style = MaterialTheme.typography.bodyMedium, color = Rice, modifier = Modifier.weight(1f))
                Text("+${r.points} XP", style = MaterialTheme.typography.titleSmall, color = Beige)
            }
        }
    }
}

@Composable
fun HistoricalInsight(progress: KataProgress) {
    DojoCard(containerColor = CharcoalLight, contentPadding = PaddingValues(14.dp)) {
        Overline("Personal insight")
        Spacer(Modifier.height(4.dp))
        Text(progress.insight ?: KataAnalyzer.NOT_ENOUGH_DATA, style = MaterialTheme.typography.bodyMedium, color = if (progress.insight != null) Rice else Stone)
    }
}
