package com.katadojo.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katadojo.app.data.AppContainer
import com.katadojo.app.data.model.Belt
import com.katadojo.app.data.model.ExperienceLevel
import com.katadojo.app.data.model.KarateTraining
import com.katadojo.app.data.model.RegistrationWithTraining
import com.katadojo.app.data.model.ScheduleFilter
import com.katadojo.app.data.model.TrainingType
import com.katadojo.app.data.repository.RegistrationResult
import com.katadojo.app.utils.Validation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

enum class DojoTab { SESSIONS, REGISTERED }

data class DojoUiState(
    val loading: Boolean = true,
    val tab: DojoTab = DojoTab.SESSIONS,
    val filter: ScheduleFilter = ScheduleFilter.ALL,
    val sessions: List<KarateTraining> = emptyList(),
    val upcomingRegistrations: List<RegistrationWithTraining> = emptyList(),
    val pastRegistrations: List<RegistrationWithTraining> = emptyList()
)

class DojoViewModel(container: AppContainer) : ViewModel() {
    private val tab = MutableStateFlow(DojoTab.SESSIONS)
    private val filter = MutableStateFlow(ScheduleFilter.ALL)

    val uiState: StateFlow<DojoUiState> = combine(
        container.trainingRepository.trainings, container.trainingRepository.registrations, tab, filter
    ) { trainings, registrations, t, f ->
        val now = LocalDateTime.now()
        val (past, upcoming) = registrations.sortedBy { it.training.startDateTime }.partition { it.training.hasEnded(now) }
        DojoUiState(
            loading = false, tab = t, filter = f,
            sessions = trainings.filter { !it.hasEnded(now) && f.matches(it) }.sortedBy { it.startDateTime },
            upcomingRegistrations = upcoming,
            pastRegistrations = past.reversed()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DojoUiState())

    fun selectTab(value: DojoTab) { tab.value = value }
    fun selectFilter(value: ScheduleFilter) { filter.value = value }
}

data class TrainingDetailUiState(val loading: Boolean = true, val training: KarateTraining? = null)

class TrainingDetailViewModel(savedStateHandle: SavedStateHandle, container: AppContainer) : ViewModel() {
    private val trainingId: Long = savedStateHandle.get<Long>("trainingId") ?: -1L
    private val repository = container.trainingRepository

    val uiState: StateFlow<TrainingDetailUiState> = repository.observeTraining(trainingId)
        .map { TrainingDetailUiState(false, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainingDetailUiState())

    fun cancelRegistration() {
        viewModelScope.launch { repository.cancelRegistration(trainingId) }
    }
}

data class RegistrationForm(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val experience: ExperienceLevel? = null,
    val belt: Belt? = null,
    val preferredType: TrainingType? = null,
    val comment: String = "",
    val consent: Boolean = false
)

data class RegistrationErrors(
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val experience: String? = null,
    val comment: String? = null,
    val consent: String? = null
) {
    val hasErrors: Boolean get() = listOf(fullName, email, phone, experience, comment, consent).any { it != null }
}

class RegistrationViewModel(savedStateHandle: SavedStateHandle, container: AppContainer) : ViewModel() {
    private val trainingId: Long = savedStateHandle.get<Long>("trainingId") ?: -1L
    private val repository = container.trainingRepository

    val training: StateFlow<KarateTraining?> = repository.observeTraining(trainingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var form by mutableStateOf(RegistrationForm())
        private set
    var errors by mutableStateOf(RegistrationErrors())
        private set
    var submitting by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var confirmedRegistrationId by mutableStateOf<Long?>(null)
        private set

    private var attempted = false

    init {
        viewModelScope.launch {
            val last = repository.latestRegistration()
            val profile = container.preferences.profile.first()
            if (form == RegistrationForm()) {
                form = if (last != null) RegistrationForm(
                    fullName = last.fullName, email = last.email, phone = last.phone,
                    experience = last.experience, belt = last.currentBelt, preferredType = last.preferredType
                ) else RegistrationForm(experience = profile.experience, belt = profile.belt)
            }
        }
    }

    fun update(transform: (RegistrationForm) -> RegistrationForm) {
        form = transform(form)
        message = null
        if (attempted) errors = validate(form)
    }

    fun submit() {
        if (submitting) return
        attempted = true
        val result = validate(form)
        errors = result
        if (result.hasErrors) {
            message = "Please check the highlighted fields."
            return
        }
        submitting = true
        viewModelScope.launch {
            val outcome = repository.register(
                trainingId, form.fullName, form.email, form.phone, form.experience ?: ExperienceLevel.BEGINNER,
                form.belt, form.preferredType, form.comment
            )
            submitting = false
            when (outcome) {
                is RegistrationResult.Success -> confirmedRegistrationId = outcome.registrationId
                RegistrationResult.AlreadyRegistered -> message = "You are already registered for this training."
                RegistrationResult.Full -> message = "This training is full. Try another session in the schedule."
                RegistrationResult.Ended -> message = "This training has already taken place."
                RegistrationResult.NotFound -> message = "This training is no longer available."
            }
        }
    }

    fun onConfirmationShown() { confirmedRegistrationId = null }

    private fun validate(form: RegistrationForm) = RegistrationErrors(
        fullName = Validation.fullName(form.fullName),
        email = Validation.email(form.email),
        phone = Validation.phone(form.phone),
        experience = if (form.experience == null) "Select your experience level" else null,
        comment = Validation.comment(form.comment),
        consent = if (form.consent) null else "Please accept to continue"
    )
}

data class RegistrationConfirmedUiState(val loading: Boolean = true, val data: RegistrationWithTraining? = null)

class RegistrationConfirmedViewModel(savedStateHandle: SavedStateHandle, container: AppContainer) : ViewModel() {
    private val registrationId: Long = savedStateHandle.get<Long>("registrationId") ?: -1L

    var uiState by mutableStateOf(RegistrationConfirmedUiState())
        private set

    init {
        viewModelScope.launch {
            uiState = RegistrationConfirmedUiState(false, container.trainingRepository.getRegistration(registrationId))
        }
    }
}

data class ScheduleDay(val date: LocalDate, val sessions: List<KarateTraining>)

data class ScheduleUiState(
    val loading: Boolean = true,
    val filter: ScheduleFilter = ScheduleFilter.ALL,
    val days: List<ScheduleDay> = emptyList(),
    val total: Int = 0
)

class ScheduleViewModel(container: AppContainer) : ViewModel() {
    private val filter = MutableStateFlow(ScheduleFilter.ALL)

    val uiState: StateFlow<ScheduleUiState> = combine(container.trainingRepository.trainings, filter) { trainings, f ->
        val today = LocalDate.now()
        val days = (0L..6L).map { offset ->
            val date = today.plusDays(offset)
            ScheduleDay(date, trainings.filter { it.date == date && f.matches(it) }.sortedBy { it.startTime })
        }
        ScheduleUiState(false, f, days, days.sumOf { it.sessions.size })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    fun selectFilter(value: ScheduleFilter) { filter.value = value }
}
