package com.katadojo.app.utils

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object Formatters {
    private val monthDay = DateTimeFormatter.ofPattern("MMMM d", Locale.US)
    private val shortMonthDay = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val longDate = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)
    private val clockTime = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun clock(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
    }

    /** Countdown display rounds up so the clock shows 00:01 until the phase actually ends. */
    fun countdown(remainingMs: Long): String = clock(((remainingMs.coerceAtLeast(0) + 999) / 1000).toInt())

    fun score(value: Double): String = String.format(Locale.US, "%.1f", value)
    fun decimal(value: Double): String = String.format(Locale.US, "%.1f", value)
    fun thousands(value: Int): String = String.format(Locale.US, "%,d", value)

    fun date(d: LocalDate): String = d.format(monthDay)
    fun shortDate(d: LocalDate): String = d.format(shortMonthDay)
    fun longDate(d: LocalDate): String = d.format(longDate)
    fun weekday(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    fun weekdayShort(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
    fun time(t: LocalTime): String = t.format(clockTime)
    fun timeRange(start: LocalTime, end: LocalTime): String = "${time(start)} - ${time(end)}"

    fun relativeDay(d: LocalDate, today: LocalDate = LocalDate.now()): String = when {
        d == today -> "Today"
        d == today.plusDays(1) -> "Tomorrow"
        d == today.minusDays(1) -> "Yesterday"
        d.isAfter(today) && d.isBefore(today.plusDays(7)) -> weekday(d)
        else -> date(d)
    }

    fun minutes(total: Int): String = when {
        total < 60 -> "$total min"
        total % 60 == 0 -> "${total / 60} h"
        else -> "${total / 60} h ${total % 60} min"
    }

    /** "18 min", "2:41" for short practices, or "Not timed". */
    fun duration(sec: Int): String = when {
        sec <= 0 -> "Not timed"
        sec < 600 -> clock(sec)
        else -> "${(sec + 30) / 60} min"
    }
}

fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
