package com.katadojo.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.katadojo.app.data.model.AppSettings
import com.katadojo.app.data.model.Belt
import com.katadojo.app.data.model.ExperienceLevel
import com.katadojo.app.data.model.KarateStyle
import com.katadojo.app.data.model.UserProfile
import com.katadojo.app.data.model.enumOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.kataDojoStore: DataStore<Preferences> by preferencesDataStore(name = "katadojo_preferences")

class PreferencesRepository(context: Context) {
    private val store = context.applicationContext.kataDojoStore

    private object Keys {
        val NAME = stringPreferencesKey("profile_name")
        val STYLE = stringPreferencesKey("profile_style")
        val EXPERIENCE = stringPreferencesKey("profile_experience")
        val BELT = stringPreferencesKey("profile_belt")
        val GOAL_TEXT = stringPreferencesKey("profile_training_goal")
        val TIMER_SOUNDS = booleanPreferencesKey("settings_timer_sounds")
        val HAPTICS = booleanPreferencesKey("settings_haptics")
        val KEEP_SCREEN_ON = booleanPreferencesKey("settings_keep_screen_on")
        val AUTO_ADVANCE = booleanPreferencesKey("settings_auto_advance")
        val SEEDED = booleanPreferencesKey("demo_seeded")
    }

    private val data: Flow<Preferences> = store.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val profile: Flow<UserProfile> = data.map { p ->
        val d = UserProfile()
        UserProfile(
            displayName = p[Keys.NAME] ?: d.displayName,
            style = enumOrNull<KarateStyle>(p[Keys.STYLE]) ?: d.style,
            experience = enumOrNull<ExperienceLevel>(p[Keys.EXPERIENCE]) ?: d.experience,
            belt = enumOrNull<Belt>(p[Keys.BELT]) ?: d.belt,
            trainingGoal = p[Keys.GOAL_TEXT] ?: d.trainingGoal
        )
    }.distinctUntilChanged()

    val settings: Flow<AppSettings> = data.map { p ->
        val d = AppSettings()
        AppSettings(
            timerSounds = p[Keys.TIMER_SOUNDS] ?: d.timerSounds,
            haptics = p[Keys.HAPTICS] ?: d.haptics,
            keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: d.keepScreenOn,
            autoAdvanceSections = p[Keys.AUTO_ADVANCE] ?: d.autoAdvanceSections
        )
    }.distinctUntilChanged()

    suspend fun saveProfile(profile: UserProfile) {
        store.edit { p ->
            p[Keys.NAME] = profile.displayName.trim()
            p[Keys.STYLE] = profile.style.name
            p[Keys.EXPERIENCE] = profile.experience.name
            p[Keys.BELT] = profile.belt.name
            p[Keys.GOAL_TEXT] = profile.trainingGoal.trim()
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        store.edit { p ->
            p[Keys.TIMER_SOUNDS] = settings.timerSounds
            p[Keys.HAPTICS] = settings.haptics
            p[Keys.KEEP_SCREEN_ON] = settings.keepScreenOn
            p[Keys.AUTO_ADVANCE] = settings.autoAdvanceSections
        }
    }

    suspend fun isSeeded(): Boolean = data.first()[Keys.SEEDED] ?: false

    suspend fun setSeeded(value: Boolean) {
        store.edit { it[Keys.SEEDED] = value }
    }

    /** Clears everything except the seeded flag, so demo data is not re-created after a reset. */
    suspend fun clearUserPreferences() {
        store.edit { p ->
            val seeded = p[Keys.SEEDED]
            p.clear()
            if (seeded != null) p[Keys.SEEDED] = seeded
        }
    }
}
