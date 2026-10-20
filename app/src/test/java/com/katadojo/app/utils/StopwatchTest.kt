package com.katadojo.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchTest {
    private var now = 0L
    private val watch = Stopwatch { now }

    @Test
    fun measuresElapsedTimeAcrossPauses() {
        watch.start()
        now = 10_000L
        watch.pause()
        now = 60_000L
        assertEquals(10_000L, watch.elapsedMs())
        watch.resume()
        now = 75_000L
        assertEquals(25_000L, watch.elapsedMs())
        assertEquals(StopwatchState.RUNNING, watch.state)
    }

    @Test
    fun finishFreezesTime() {
        watch.start()
        now = 5_000L
        watch.finish()
        now = 50_000L
        assertEquals(5_000L, watch.elapsedMs())
        watch.start()
        assertEquals(StopwatchState.FINISHED, watch.state)
    }

    @Test
    fun restartKeepsRunningFromZero() {
        watch.start()
        now = 8_000L
        watch.restart()
        now = 11_000L
        assertEquals(3_000L, watch.elapsedMs())
        assertEquals(StopwatchState.RUNNING, watch.state)
    }

    @Test
    fun snapshotRestoresRunningWatch() {
        watch.start()
        now = 4_000L
        val copy = Stopwatch { now }
        copy.restore(watch.snapshot())
        now = 9_000L
        assertEquals(9_000L, copy.elapsedMs())
        assertEquals(StopwatchState.RUNNING, copy.state)
    }
}
