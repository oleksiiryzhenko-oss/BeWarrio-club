package com.katadojo.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katadojo.app.data.AppContainer
import com.katadojo.app.data.model.AppSettings
import com.katadojo.app.data.model.UserProfile
import com.katadojo.app.utils.CareerStats
import com.katadojo.app.utils.LevelProgress
import com.katadojo.app.utils.StatisticsCalculator
import com.katadojo.app.utils.Validation
import com.katadojo.app.utils.XpCalculator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val stats: CareerStats = CareerStats(),
    val level: LevelProgress = XpCalculator.level(0),
    val registrations: Int = 0
)

class ProfileViewModel(private val container: AppContainer) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = combine(
        container.preferences.profile, container.kataRepository.sessions, container.workoutRepository.workouts,
        container.xpRepository.total, container.trainingRepository.registrations
    ) { profile, kata, workouts, xp, regs ->
        ProfileUiState(false, profile, StatisticsCalculator.career(kata, workouts, xp, LocalDate.now()), XpCalculator.level(xp), regs.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    var draft by mutableStateOf<UserProfile?>(null)
        private set
    var nameError by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun startEditing() {
        viewModelScope.launch {
            draft = container.preferences.profile.first()
            nameError = null
        }
    }

    fun updateDraft(transform: (UserProfile) -> UserProfile) {
        val current = draft ?: return
        val next = transform(current)
        draft = next
        if (nameError != null) nameError = Validation.displayName(next.displayName)
    }

    fun cancelEditing() {
        draft = null
        nameError = null
    }

    fun saveDraft() {
        val current = draft ?: return
        val error = Validation.displayName(current.displayName)
        nameError = error
        if (error != null) return
        viewModelScope.launch {
            container.preferences.saveProfile(current)
            draft = null
            message = "Profile saved"
        }
    }

    fun resetLocalData() = runBusy("All local data was deleted") { container.resetLocalData() }
    fun loadDemoData() = runBusy("Demo data loaded") { container.loadDemoData() }
    fun clearMessage() { message = null }

    private fun runBusy(done: String, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            withContext(NonCancellable) { block() }
            busy = false
            message = done
        }
    }
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = container.preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(settings.value)
        viewModelScope.launch { container.preferences.saveSettings(next) }
    }
}
