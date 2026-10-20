package com.katadojo.app.utils

enum class StopwatchState { IDLE, RUNNING, PAUSED, FINISHED }

/**
 * Elapsed-time stopwatch. Time is always computed from a monotonic clock
 * (accumulated time + time since the last start), never by counting UI ticks,
 * so recomposition, rotation or a slow frame cannot make it drift.
 */
class Stopwatch(private val clock: () -> Long) {
    private var accumulatedMs = 0L
    private var runningSince: Long? = null
    var state: StopwatchState = StopwatchState.IDLE
        private set

    val isRunning: Boolean get() = state == StopwatchState.RUNNING

    fun elapsedMs(): Long = accumulatedMs + (runningSince?.let { (clock() - it).coerceAtLeast(0) } ?: 0L)

    fun start() {
        if (state == StopwatchState.RUNNING || state == StopwatchState.FINISHED) return
        runningSince = clock()
        state = StopwatchState.RUNNING
    }

    fun pause() {
        val since = runningSince ?: return
        accumulatedMs += (clock() - since).coerceAtLeast(0)
        runningSince = null
        state = StopwatchState.PAUSED
    }

    fun resume() = start()

    /** Back to 00:00. Keeps running if it was running. */
    fun restart() {
        val wasRunning = isRunning
        accumulatedMs = 0
        runningSince = null
        state = StopwatchState.IDLE
        if (wasRunning) start()
    }

    fun finish() {
        pause()
        state = StopwatchState.FINISHED
    }

    fun snapshot(): LongArray = longArrayOf(accumulatedMs, runningSince ?: -1L, state.ordinal.toLong())

    fun restore(values: LongArray?) {
        if (values == null || values.size < 3) return
        accumulatedMs = values[0]
        runningSince = values[1].takeIf { it >= 0 }
        state = StopwatchState.entries.getOrElse(values[2].toInt()) { StopwatchState.IDLE }
    }
}
