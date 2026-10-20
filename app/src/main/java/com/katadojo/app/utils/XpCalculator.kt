package com.katadojo.app.utils

import com.katadojo.app.data.model.ProgressLevel

data class LevelProgress(
    val level: ProgressLevel,
    val next: ProgressLevel?,
    val xp: Int,
    val nextAt: Int?,
    val remaining: Int,
    val fraction: Float
)

object XpCalculator {
    fun level(xp: Int): LevelProgress {
        val levels = ProgressLevel.entries
        val current = levels.last { xp >= it.minXp }
        val next = levels.getOrNull(current.ordinal + 1)
        val span = (next?.minXp ?: current.minXp) - current.minXp
        val fraction = if (next == null || span == 0) 1f else ((xp - current.minXp).toFloat() / span).coerceIn(0f, 1f)
        return LevelProgress(current, next, xp, next?.minXp, next?.let { (it.minXp - xp).coerceAtLeast(0) } ?: 0, fraction)
    }
}
