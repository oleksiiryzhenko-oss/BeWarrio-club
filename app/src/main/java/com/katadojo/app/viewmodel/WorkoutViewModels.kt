package com.katadojo.app.viewmodel

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katadojo.app.data.AppContainer
import com.katadojo.app.data.XpReward
import com.katadojo.app.data.model.AppSettings
import com.katadojo.app.data.model.WorkoutSession
import com.katadojo.app.data.model.WorkoutType
import com.katadojo.app.data.model.enumOrNull
import com.katadojo.app.utils.Stopwatch
import com.katadojo.app.utils.StopwatchState
import com.katadojo.app.utils.TonePlayer
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutSetupViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    companion object {
        val DURATIONS = listOf(30, 45, 60, 90)
    }

    var type by mutableStateOf(enumOrNull<WorkoutType>(savedStateHandle.get<String>("type")) ?: WorkoutType.TECHNIQUE)
    var minutes by mutableStateOf(45)
}

data class WorkoutUiState(
    val type: WorkoutType,
    val plannedMinutes: Int,
    val timerState: StopwatchState,
    val elapsedMs: Long,
    val reviewing: Boolean,
    val effort: Int,
    val saving: Boolean,
    val savedId: Long?,
    val rewards: List<XpReward>,
    val settings: AppSettings
) {
    val targetMs: Long get() = plannedMinutes * 60_000L
    val fraction: Float get() = (elapsedMs.toFloat() / targetMs).coerceIn(0f, 1f)
    val targetReached: Boolean get() = elapsedMs >= targetMs
    val minutes: Int get() = ((elapsedMs / 1000 + 30) / 60).toInt()
}

/** Workout timer based on elapsed real time. State survives rotation and process death. */
class WorkoutViewModel(private val savedStateHandle: SavedStateHandle, private val container: AppContainer) : ViewModel() {
    private val type = enumOrNull<WorkoutType>(savedStateHandle.get<String>("type")) ?: WorkoutType.MIXED
    private val planned = (savedStateHandle.get<Int>("minutes") ?: 45).coerceIn(5, 240)
    private val stopwatch = Stopwatch { SystemClock.elapsedRealtime() }
    private var reviewing = savedStateHandle.get<Boolean>(KEY_REVIEW) ?: false
    private var effort = savedStateHandle.get<Int>(KEY_EFFORT) ?: 7
    private var savedId = savedStateHandle.get<Long>(KEY_SAVED)
    private var startedAt = savedStateHandle.get<Long>(KEY_STARTED) ?: 0L
    private var targetAnnounced = savedStateHandle.get<Boolean>(KEY_ANNOUNCED) ?: false
    private var saving = false
    private var rewards: List<XpReward> = emptyList()
    private var settings = AppSettings()
    private var tones: TonePlayer? = null

    var uiState by mutableStateOf(build())
        private set

    init {
        stopwatch.restore(savedStateHandle.get<LongArray>(KEY_TIMER))
        viewModelScope.launch { container.preferences.settings.collect { settings = it; publish() } }
        viewModelScope.launch {
            while (isActive) {
                delay(250)
                if (stopwatch.isRunning) {
                    if (!targetAnnounced && stopwatch.elapsedMs() >= planned * 60_000L) {
                        targetAnnounced = true
                        savedStateHandle[KEY_ANNOUNCED] = true
                        if (settings.timerSounds) (tones ?: TonePlayer().also { tones = it }).done()
                    }
                    publish()
                }
            }
        }
        publish()
    }

    fun start() {
        if (startedAt == 0L) {
            startedAt = System.currentTimeMillis()
            savedStateHandle[KEY_STARTED] = startedAt
        }
        stopwatch.start()
        persist()
    }

    fun pause() { stopwatch.pause(); persist() }
    fun resume() { stopwatch.resume(); persist() }

    fun reset() {
        stopwatch.restart()
        stopwatch.pause()
        targetAnnounced = false
        savedStateHandle[KEY_ANNOUNCED] = false
        persist()
    }

    fun finish() {
        if (stopwatch.elapsedMs() < 1_000) return
        stopwatch.finish()
        reviewing = true
        persist()
    }

    fun backToTimer() {
        if (savedId != null) return
        reviewing = false
        stopwatch.restore(longArrayOf(stopwatch.elapsedMs(), -1L, StopwatchState.PAUSED.ordinal.toLong()))
        persist()
    }

    fun setEffort(value: Int) {
        effort = value.coerceIn(1, 10)
        persist()
    }

    fun save() {
        if (saving || savedId != null) return
        saving = true
        publish()
        val workout = WorkoutSession(
            type = type,
            plannedMinutes = planned,
            durationSec = (stopwatch.elapsedMs() / 1000).toInt(),
            effort = effort,
            startedAt = if (startedAt == 0L) System.currentTimeMillis() else startedAt
        )
        viewModelScope.launch {
            val (id, gained) = withContext(NonCancellable) {
                val id = container.workoutRepository.save(workout)
                id to container.progressEngine.onWorkoutSaved(workout.copy(id = id))
            }
            savedId = id
            rewards = gained
            saving = false
            persist()
        }
    }

    private fun persist() {
        savedStateHandle[KEY_TIMER] = stopwatch.snapshot()
        savedStateHandle[KEY_REVIEW] = reviewing
        savedStateHandle[KEY_EFFORT] = effort
        savedId?.let { savedStateHandle[KEY_SAVED] = it }
        publish()
    }

    private fun build() = WorkoutUiState(
        type, planned, stopwatch.state, stopwatch.elapsedMs(), reviewing, effort, saving, savedId, rewards, settings
    )

    private fun publish() {
        uiState = build()
    }

    override fun onCleared() {
        tones?.release()
        super.onCleared()
    }

    private companion object {
        const val KEY_TIMER = "workout_timer"
        const val KEY_REVIEW = "workout_review"
        const val KEY_EFFORT = "workout_effort"
        const val KEY_SAVED = "workout_saved"
        const val KEY_STARTED = "workout_started"
        const val KEY_ANNOUNCED = "workout_announced"
    }
}

data class WorkoutDetailUiState(val loading: Boolean = true, val workout: WorkoutSession? = null)

class WorkoutDetailViewModel(savedStateHandle: SavedStateHandle, container: AppContainer) : ViewModel() {
    private val id: Long = savedStateHandle.get<Long>("workoutId") ?: -1L
    val uiState: StateFlow<WorkoutDetailUiState> = container.workoutRepository.workout(id)
        .map { WorkoutDetailUiState(false, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailUiState())
}
