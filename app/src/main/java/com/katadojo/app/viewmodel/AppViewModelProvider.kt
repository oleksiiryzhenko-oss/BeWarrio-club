package com.katadojo.app.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.katadojo.app.KataDojoApp
import com.katadojo.app.data.AppContainer

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { HomeViewModel(container()) }
        initializer { DojoViewModel(container()) }
        initializer { TrainingDetailViewModel(createSavedStateHandle(), container()) }
        initializer { RegistrationViewModel(createSavedStateHandle(), container()) }
        initializer { RegistrationConfirmedViewModel(createSavedStateHandle(), container()) }
        initializer { ScheduleViewModel(container()) }
        initializer { KataLibraryViewModel(container()) }
        initializer { KataDetailViewModel(createSavedStateHandle(), container()) }
        initializer { KataCoachViewModel(createSavedStateHandle(), container()) }
        initializer { KataSessionDetailViewModel(createSavedStateHandle(), container()) }
        initializer { WorkoutSetupViewModel(createSavedStateHandle()) }
        initializer { WorkoutViewModel(createSavedStateHandle(), container()) }
        initializer { WorkoutDetailViewModel(createSavedStateHandle(), container()) }
        initializer { ProgressViewModel(container()) }
        initializer { HistoryViewModel(container()) }
        initializer { GoalsViewModel(container()) }
        initializer { ProfileViewModel(container()) }
        initializer { SettingsViewModel(container()) }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KataDojoApp).container
