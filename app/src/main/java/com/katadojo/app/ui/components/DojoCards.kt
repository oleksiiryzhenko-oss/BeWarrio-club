package com.katadojo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.katadojo.app.data.model.KarateTraining
import com.katadojo.app.data.model.GoalProgress
import com.katadojo.app.data.model.TrainingType
import com.katadojo.app.utils.HistoryItem
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.DayActivity
import com.katadojo.app.utils.Formatters
import java.time.LocalDateTime

fun typeColor(type: TrainingType): Color = when (type) {
    TrainingType.FUNDAMENTALS -> StoneLight
    TrainingType.KATA -> DojoRed
    TrainingType.KUMITE -> Color(0xFFE0703A)
    TrainingType.COMPETITION -> Color(0xFFE5575F)
    TrainingType.FOOTWORK -> Indigo
    TrainingType.CONDITIONING -> Amber
    TrainingType.OPEN_DOJO -> SuccessGreen
    TrainingType.SEMINAR -> Beige
}

/** Status of a session for the current user. */
@Composable
fun TrainingStatusPill(training: KarateTraining) {
    val now = LocalDateTime.now()
    when {
        training.isRegistered && training.hasEnded(now) -> TagPill("Attended", color = Stone)
        training.isRegistered -> TagPill("Registered", color = SuccessGreen, filled = true)
        training.hasEnded(now) -> TagPill("Ended", color = Stone)
        training.isFull -> TagPill("Full", color = ErrorRed)
        training.placesLeft <= 2 -> TagPill("${training.placesLeft} left", color = Beige)
        else -> TagPill("Open", color = SuccessGreen)
    }
}

@Composable
fun TrainingCard(training: KarateTraining, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DojoCard(modifier, onClick = onClick, accent = typeColor(training.type)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TagPill(training.type.label, color = typeColor(training.type))
            Spacer(Modifier.weight(1f))
            TrainingStatusPill(training)
        }
        Spacer(Modifier.height(10.dp))
        Text(training.title, style = MaterialTheme.typography.titleLarge, color = Rice)
        Spacer(Modifier.height(4.dp))
        Text(
            "${Formatters.date(training.date)} · ${Formatters.relativeDay(training.date)}",
            style = MaterialTheme.typography.bodyMedium,
            color = StoneLight
        )
        Text(
            "${Formatters.timeRange(training.startTime, training.endTime)} · ${Formatters.minutes(training.durationMinutes)}",
            style = MaterialTheme.typography.titleMedium,
            color = Rice
        )
        Spacer(Modifier.height(10.dp))
        Text(training.dojo, style = MaterialTheme.typography.bodyMedium, color = Rice)
        Text(
            "${training.style.label} · ${training.level.label} · ${training.instructor}",
            style = MaterialTheme.typography.bodySmall,
            color = Stone
        )
        Spacer(Modifier.height(12.dp))
        PlacesBar(training)
        Spacer(Modifier.height(12.dp))
        SecondaryButton("View Details", onClick = onClick, height = 46.dp)
    }
}

@Composable
fun PlacesBar(training: KarateTraining) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${training.booked} / ${training.capacity} places",
            style = MaterialTheme.typography.titleSmall,
            color = Rice
        )
        Spacer(Modifier.weight(1f))
        Text(
            if (training.isFull) "Fully booked" else "${training.placesLeft} available",
            style = MaterialTheme.typography.bodySmall,
            color = if (training.isFull) ErrorRed else Stone
        )
    }
    Spacer(Modifier.height(6.dp))
    ThickProgressBar(
        fraction = if (training.capacity == 0) 0f else training.booked.toFloat() / training.capacity,
        color = if (training.isFull) ErrorRed else StoneLight,
        height = 6.dp
    )
}

/** Compact schedule row: time column + details. */
@Composable
fun ScheduleRow(training: KarateTraining, onClick: () -> Unit) {
    val ended = training.hasEnded()
    DojoCard(onClick = onClick, contentPadding = PaddingValues(14.dp), accent = typeColor(training.type)) {
        Row {
            Column(Modifier.width(64.dp)) {
                Text(
                    Formatters.time(training.startTime),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (ended) StoneDark else Rice
                )
                Text(Formatters.minutes(training.durationMinutes), style = MaterialTheme.typography.bodySmall, color = Stone)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    training.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (ended) Stone else Rice,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${training.type.label} · ${training.dojo}", style = MaterialTheme.typography.bodySmall, color = StoneLight)
                Text("${training.instructor} · ${training.style.label} · ${training.level.label}", style = MaterialTheme.typography.bodySmall, color = Stone)
            }
            TrainingStatusPill(training)
        }
    }
}

/** One entry of the training history: a kata session or a workout. */
@Composable
fun HistoryRow(item: HistoryItem, onClick: () -> Unit) {
    val date = when (item) {
        is HistoryItem.Kata -> item.session.date
        is HistoryItem.Workout -> item.workout.date
    }
    DojoCard(onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CharcoalLight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, color = Rice)
                Text(Formatters.shortDate(date).substringBefore(' ').uppercase(), style = MaterialTheme.typography.labelSmall, color = DojoRed)
            }
            Spacer(Modifier.width(14.dp))
            when (item) {
                is HistoryItem.Kata -> {
                    val s = item.session
                    val kata = s.kata
                    Column(Modifier.weight(1f)) {
                        Text(if (s.isFullKata) "Kata Practice" else "Section Practice", style = MaterialTheme.typography.labelMedium, color = Stone)
                        Text(kata?.name ?: "Kata", style = MaterialTheme.typography.titleMedium, color = Rice, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val detail = if (s.isFullKata) {
                            val weak = (s.difficultSections + s.needsPracticeSections).sorted().firstOrNull()
                            "${Formatters.duration(s.durationSec)} · " + (weak?.let { "Needs practice: Section ${it + 1}" } ?: "All sections completed")
                        } else "${Formatters.duration(s.durationSec)} · Section ${(s.focusSection ?: 0) + 1}: ${s.results.firstOrNull()?.status?.label ?: ""}"
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = StoneLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (s.isFullKata) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Formatters.score(s.score), style = MaterialTheme.typography.headlineSmall, color = scoreColor(s.score))
                            Text("/ 10", style = MaterialTheme.typography.labelSmall, color = Stone)
                        }
                    }
                }
                is HistoryItem.Workout -> {
                    val w = item.workout
                    Column(Modifier.weight(1f)) {
                        Text("Workout", style = MaterialTheme.typography.labelMedium, color = Stone)
                        Text(w.type.label, style = MaterialTheme.typography.titleMedium, color = Rice)
                        Text("${w.minutes} min · planned ${w.plannedMinutes} min", style = MaterialTheme.typography.bodySmall, color = StoneLight)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${w.effort}", style = MaterialTheme.typography.headlineSmall, color = Beige)
                        Text("EFFORT", style = MaterialTheme.typography.labelSmall, color = Stone)
                    }
                }
            }
        }
    }
}

/** Monday - Sunday strip showing which days had a workout. */
@Composable
fun WeekStrip(days: List<DayActivity>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            val trained = day.sessions > 0
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    Formatters.weekdayShort(day.date).take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) Rice else Stone
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                trained -> DojoRed
                                day.isFuture -> Color.Transparent
                                else -> CharcoalLight
                            }
                        )
                        .border(
                            width = if (day.isToday) 2.dp else 1.dp,
                            color = if (day.isToday) Rice else CharcoalBorder,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (trained) "✓" else day.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (trained) Rice else Stone
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        trained -> "Trained"
                        day.isFuture -> "-"
                        else -> "Rest"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
                    color = if (trained) Rice else StoneDark,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Large "2 / 3" goal progress block. */
@Composable
fun GoalProgressBlock(title: String, progress: GoalProgress, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Rice)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(progress.currentLabel, style = MaterialTheme.typography.displaySmall, color = Rice)
            Text(
                " / ${progress.targetLabel}",
                style = MaterialTheme.typography.titleLarge,
                color = Stone,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (progress.completed) "DONE" else "${progress.percent}%",
                style = MaterialTheme.typography.headlineSmall,
                color = if (progress.completed) SuccessGreen else DojoRed
            )
        }
        Spacer(Modifier.height(8.dp))
        ThickProgressBar(progress.fraction, color = if (progress.completed) SuccessGreen else DojoRed)
        if (progress.detail.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(progress.detail, style = MaterialTheme.typography.bodySmall, color = Stone)
        }
    }
}
