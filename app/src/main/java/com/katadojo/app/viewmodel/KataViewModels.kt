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
import com.katadojo.app.data.model.Kata
import com.katadojo.app.data.model.KataDifficulty
import com.katadojo.app.data.model.KataLibrary
import com.katadojo.app.data.model.KataSession
import com.katadojo.app.data.model.PerceivedDifficulty
import com.katadojo.app.data.model.SectionResult
import com.katadojo.app.data.model.SectionStatus
import com.katadojo.app.utils.KataAnalyzer
import com.katadojo.app.utils.KataProgress
import com.katadojo.app.utils.KataScoring
import com.katadojo.app.utils.SectionStat
import com.katadojo.app.utils.SessionAnalysis
import com.katadojo.app.utils.Stopwatch
import com.katadojo.app.utils.StopwatchState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

// ------------------------------------------------------------------ library

data class KataCardState(val kata: Kata, val progress: KataProgress)

data class KataLibraryUiState(
    val loading: Boolean = true,
    val groups: List<Pair<KataDifficulty, List<KataCardState>>> = emptyList(),
    val totalSessions: Int = 0
)

class KataLibraryViewModel(container: AppContainer) : ViewModel() {
    val uiState: StateFlow<KataLibraryUiState> = container.kataRepository.sessions.map { sessions ->
        val today = LocalDate.now()
        val cards = KataLibrary.all.map { KataCardState(it, KataAnalyzer.progress(it, sessions, today)) }
        KataLibraryUiState(
            loading = false,
            groups = KataDifficulty.entries.map { d -> d to cards.filter { it.kata.difficulty == d } },
            totalSessions = sessions.count { it.isFullKata }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KataLibraryUiState())
}

// ------------------------------------------------------------------ detail

data class KataDetailUiState(
    val loading: Boolean = true,
    val kata: Kata? = null,
    val progress: KataProgress? = null,
    val sectionStats: List<SectionStat> = emptyList(),
    val lastFull: KataSession? = null,
    val recent: List<KataSession> = emptyList()
)

class KataDetailViewModel(savedStateHandle: SavedStateHandle, container: AppContainer) : ViewModel() {
    private val kata = KataLibrary.get(savedStateHandle.get<String>("kataId"))

    val uiState: StateFlow<KataDetailUiState> = container.kataRepository.sessions.map { sessions ->
        val k = kata ?: return@map KataDetailUiState(loading = false)
        val ofKata = sessions.filter { it.kataId == k.id }
        KataDetailUiState(
            loading = false,
            kata = k,
            progress = KataAnalyzer.progress(k, sessions),
            sectionStats = KataAnalyzer.sectionStats(k, ofKata),
            lastFull = ofKata.firstOrNull { it.isFullKata },
            recent = ofKata.take(6)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KataDetailUiState(kata = kata))
}

// ------------------------------------------------------------------ Kata Coach

enum class CoachPhase { SECTIONS, ASSESSMENT, RESULT }

data class CoachResult(
    val session: KataSession,
    val analysis: SessionAnalysis,
    val progress: KataProgress,
    val rewards: List<XpReward>
)

data class CoachUiState(
    val kata: Kata?,
    val isSectionPractice: Boolean,
    val sectionIndexes: List<Int>,
    val position: Int,
    val selections: List<SectionStatus?>,
    val phase: CoachPhase,
    val timerState: StopwatchState,
    val elapsedMs: Long,
    val technique: Int,
    val balance: Int,
    val timing: Int,
    val confidence: Int,
    val difficulty: PerceivedDifficulty?,
    val saving: Boolean,
    val result: CoachResult?,
    val settings: AppSettings
) {
    val currentSection get() = kata?.sections?.getOrNull(sectionIndexes.getOrElse(position) { 0 })
    val isLast: Boolean get() = position == sectionIndexes.lastIndex
    val currentSelection: SectionStatus? get() = selections.getOrNull(position)
    val allSelected: Boolean get() = selections.isNotEmpty() && selections.all { it != null }
    val canFinish: Boolean get() = difficulty != null
}

/**
 * Kata Coach: walks through a kata section by section, times the practice with an
 * elapsed-time stopwatch, collects the self-assessment and saves the session locally.
 * All progress is kept in SavedStateHandle so rotation or process death cannot lose it.
 */
class KataCoachViewModel(private val savedStateHandle: SavedStateHandle, private val container: AppContainer) : ViewModel() {
    private val kata: Kata? = KataLibrary.get(savedStateHandle.get<String>("kataId"))
    private val focus: Int? = savedStateHandle.get<Int>("focus")?.takeIf { it >= 0 && kata != null && it < kata.sectionCount }
    private val indexes: List<Int> = focus?.let { listOf(it) } ?: kata?.sections?.map { it.index } ?: emptyList()

    private val stopwatch = Stopwatch { SystemClock.elapsedRealtime() }
    private val tones = TonePlayerHolder()

    private var position = savedStateHandle.get<Int>(KEY_POSITION) ?: 0
    private val selections: Array<SectionStatus?> = savedStateHandle.get<IntArray>(KEY_SELECTIONS)
        ?.takeIf { it.size == indexes.size }
        ?.map { i -> SectionStatus.entries.getOrNull(i) }?.toTypedArray()
        ?: arrayOfNulls(indexes.size)
    private var phase = CoachPhase.entries.getOrElse(savedStateHandle.get<Int>(KEY_PHASE) ?: 0) { CoachPhase.SECTIONS }
    private var ratings = savedStateHandle.get<IntArray>(KEY_RATINGS) ?: intArrayOf(7, 7, 7, 7)
    private var difficulty = PerceivedDifficulty.entries.getOrNull(savedStateHandle.get<Int>(KEY_DIFFICULTY) ?: -1)
    private val startedAt = savedStateHandle.get<Long>(KEY_STARTED) ?: System.currentTimeMillis().also { savedStateHandle[KEY_STARTED] = it }
    private var savedId: Long? = savedStateHandle.get<Long>(KEY_SAVED_ID)
    private var saving = false
    private var result: CoachResult? = null
    private var settings = AppSettings()

    var uiState by mutableStateOf(build())
        private set

    init {
        stopwatch.restore(savedStateHandle.get<LongArray>(KEY_TIMER))
        viewModelScope.launch {
            container.preferences.settings.collect {
                settings = it
                publish()
            }
        }
        // Rebuild the result screen after process death.
        val id = savedId
        if (phase == CoachPhase.RESULT && id != null) {
            viewModelScope.launch {
                container.kataRepository.session(id).first()?.let { session -> result = buildResult(session, emptyList()) }
                publish()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(200)
                if (stopwatch.isRunning) publish()
            }
        }
        publish()
    }

    // ---------------------------------------------------------- sections

    fun select(status: SectionStatus) {
        if (phase != CoachPhase.SECTIONS) return
        selections[position] = status
        if (settings.haptics) tones.tick()
        if (settings.autoAdvanceSections && position < indexes.lastIndex) position++
        persist()
    }

    fun next() {
        if (selections[position] == null) return
        if (position < indexes.lastIndex) position++ else if (focus == null) phase = CoachPhase.ASSESSMENT
        persist()
    }

    fun previous() {
        when {
            phase == CoachPhase.ASSESSMENT -> phase = CoachPhase.SECTIONS
            position > 0 -> position--
        }
        persist()
    }

    fun goTo(index: Int) {
        if (phase == CoachPhase.RESULT) return
        position = index.coerceIn(0, indexes.lastIndex)
        phase = CoachPhase.SECTIONS
        persist()
    }

    // ---------------------------------------------------------- timer

    fun startTimer() { stopwatch.start(); persist() }
    fun pauseTimer() { stopwatch.pause(); persist() }
    fun resumeTimer() { stopwatch.resume(); persist() }
    fun restartTimer() { stopwatch.restart(); persist() }
    fun finishTimer() { stopwatch.finish(); if (settings.timerSounds) tones.done(); persist() }

    // ---------------------------------------------------------- assessment

    fun setRating(index: Int, value: Int) {
        ratings = ratings.copyOf().also { it[index] = value.coerceIn(1, 10) }
        persist()
    }

    fun setDifficulty(value: PerceivedDifficulty) {
        difficulty = value
        persist()
    }

    /** Finish Kata (or Finish Practice for a single section): calculates the score and saves the session. */
    fun finish() {
        val k = kata ?: return
        if (saving || savedId != null || selections.any { it == null }) return
        if (focus == null && difficulty == null) return
        saving = true
        stopwatch.finish()
        publish()
        val results = indexes.mapIndexed { i, sectionIndex -> SectionResult(sectionIndex, selections[i]!!) }
        val session = if (focus == null) KataSession(
            kataId = k.id,
            startedAt = startedAt,
            durationSec = (stopwatch.elapsedMs() / 1000).toInt(),
            results = results,
            technique = ratings[0], balance = ratings[1], timing = ratings[2], confidence = ratings[3],
            perceivedDifficulty = difficulty,
            score = KataScoring.personalScore(ratings[0], ratings[1], ratings[2], ratings[3]),
            accuracy = KataScoring.accuracy(results)
        ) else KataSession(
            kataId = k.id,
            startedAt = startedAt,
            durationSec = (stopwatch.elapsedMs() / 1000).toInt(),
            focusSection = focus,
            results = results,
            score = results.first().status.rating.toDouble(),
            accuracy = KataScoring.accuracy(results)
        )
        viewModelScope.launch {
            val saved = withContext(NonCancellable) {
                val id = container.kataRepository.save(session)
                val s = session.copy(id = id)
                s to container.progressEngine.onKataSaved(s)
            }
            savedId = saved.first.id
            result = buildResult(saved.first, saved.second)
            phase = CoachPhase.RESULT
            saving = false
            if (settings.timerSounds) tones.done()
            persist()
        }
    }

    private suspend fun buildResult(session: KataSession, rewards: List<XpReward>): CoachResult {
        val k = kata ?: KataLibrary.all.first()
        val all = container.kataRepository.sessions.first()
        return CoachResult(
            session = session,
            analysis = KataAnalyzer.analyze(session, k, all),
            progress = KataAnalyzer.progress(k, all),
            rewards = rewards
        )
    }

    private fun persist() {
        savedStateHandle[KEY_POSITION] = position
        savedStateHandle[KEY_SELECTIONS] = selections.map { it?.ordinal ?: -1 }.toIntArray()
        savedStateHandle[KEY_PHASE] = phase.ordinal
        savedStateHandle[KEY_RATINGS] = ratings
        savedStateHandle[KEY_DIFFICULTY] = difficulty?.ordinal ?: -1
        savedStateHandle[KEY_TIMER] = stopwatch.snapshot()
        savedId?.let { savedStateHandle[KEY_SAVED_ID] = it }
        publish()
    }

    private fun build() = CoachUiState(
        kata = kata,
        isSectionPractice = focus != null,
        sectionIndexes = indexes,
        position = position,
        selections = selections.toList(),
        phase = phase,
        timerState = stopwatch.state,
        elapsedMs = stopwatch.elapsedMs(),
        technique = ratings[0],
        balance = ratings[1],
        timing = ratings[2],
        confidence = ratings[3],
        difficulty = difficulty,
        saving = saving,
        result = result,
        settings = settings
    )

    private fun publish() {
        uiState = build()
    }

    override fun onCleared() {
        tones.release()
        super.onCleared()
    }

    private companion object {
        const val KEY_POSITION = "coach_position"
        const val KEY_SELECTIONS = "coach_selections"
        const val KEY_PHASE = "coach_phase"
        const val KEY_RATINGS = "coach_ratings"
        const val KEY_DIFFICULTY = "coach_difficulty"
        const val KEY_TIMER = "coach_timer"
        const val KEY_STARTED = "coach_started"
        const val KEY_SAVED_ID = "coach_saved_id"
    }
}

/** Lazily creates the tone generator only when a sound is actually played. */
private class TonePlayerHolder {
    private var player: com.katadojo.app.utils.TonePlayer? = null
    private fun get() = player ?: com.katadojo.app.utils.TonePlayer().also { player = it }
    fun tick() = Unit
    fun done() = get().done()
    fun release() { player?.release() }
}

// ------------------------------------------------------------------ session detail

data class KataSessionDetailUiState(
    val loading: Boolean = true,
    val session: KataSession? = null,
    val analysis: SessionAnalysis? = null,
    val progress: KataProgress? = null
)

class KataSessionDetailViewModel(savedStateHandle: SavedStateHandle, container: AppContainer) : ViewModel() {
    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: -1L

    val uiState: StateFlow<KataSessionDetailUiState> = container.kataRepository.sessions.map { all ->
        val session = all.firstOrNull { it.id == sessionId }
        val kata = session?.kata
        if (session == null || kata == null) KataSessionDetailUiState(loading = false)
        else {
            val upTo = all.filter { it.startedAt <= session.startedAt }
            KataSessionDetailUiState(
                loading = false,
                session = session,
                analysis = KataAnalyzer.analyze(session, kata, upTo),
                progress = KataAnalyzer.progress(kata, all)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KataSessionDetailUiState())
}
