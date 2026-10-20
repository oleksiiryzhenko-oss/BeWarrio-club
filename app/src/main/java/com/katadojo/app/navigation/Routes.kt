package com.katadojo.app.navigation

import com.katadojo.app.data.model.WorkoutType

object Routes {
    const val HOME = "home"
    const val DOJO = "dojo"
    const val KATA = "kata"
    const val PROGRESS = "progress"

    const val TRAINING_DETAIL = "training/{trainingId}"
    const val REGISTER = "register/{trainingId}"
    const val REGISTRATION_CONFIRMED = "registration/{registrationId}/confirmed"
    const val SCHEDULE = "schedule"
    const val KATA_DETAIL = "kata_detail/{kataId}"
    const val KATA_COACH = "coach/{kataId}/{focus}"
    const val KATA_SESSION = "kata_session/{sessionId}"
    const val WORKOUT_SETUP = "workout_setup/{type}"
    const val WORKOUT = "workout/{type}/{minutes}"
    const val WORKOUT_DETAIL = "workout_detail/{workoutId}"
    const val HISTORY = "history"
    const val GOALS = "goals"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val TERMS = "terms"

    const val WORKOUT_SETUP_DEFAULT = "workout_setup/NONE"
    const val FULL_KATA = -1

    val topLevel = setOf(HOME, DOJO, KATA, PROGRESS)

    fun trainingDetail(id: Long) = "training/$id"
    fun register(id: Long) = "register/$id"
    fun registrationConfirmed(id: Long) = "registration/$id/confirmed"
    fun kataDetail(kataId: String) = "kata_detail/$kataId"
    fun kataCoach(kataId: String, focusSection: Int? = null) = "coach/$kataId/${focusSection ?: FULL_KATA}"
    fun kataSession(id: Long) = "kata_session/$id"
    fun workoutSetup(type: WorkoutType?) = "workout_setup/${type?.name ?: "NONE"}"
    fun workout(type: WorkoutType, minutes: Int) = "workout/${type.name}/$minutes"
    fun workoutDetail(id: Long) = "workout_detail/$id"
}
