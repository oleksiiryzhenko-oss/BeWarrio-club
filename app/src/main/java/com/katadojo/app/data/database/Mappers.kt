package com.katadojo.app.data.database

import com.katadojo.app.data.model.Belt
import com.katadojo.app.data.model.ExperienceLevel
import com.katadojo.app.data.model.GoalType
import com.katadojo.app.data.model.KarateStyle
import com.katadojo.app.data.model.KarateTraining
import com.katadojo.app.data.model.KataSession
import com.katadojo.app.data.model.PerceivedDifficulty
import com.katadojo.app.data.model.PersonalGoal
import com.katadojo.app.data.model.SectionResult
import com.katadojo.app.data.model.SectionStatus
import com.katadojo.app.data.model.SkillLevel
import com.katadojo.app.data.model.TrainingRegistration
import com.katadojo.app.data.model.TrainingType
import com.katadojo.app.data.model.WorkoutSession
import com.katadojo.app.data.model.WorkoutType
import com.katadojo.app.data.model.XpEvent
import com.katadojo.app.data.model.XpType
import com.katadojo.app.data.model.enumOrNull
import java.time.LocalDate
import java.time.LocalTime

fun TrainingEntity.toModel(isRegistered: Boolean) = KarateTraining(
    id = id,
    title = title,
    type = enumOrNull<TrainingType>(type) ?: TrainingType.FUNDAMENTALS,
    style = enumOrNull<KarateStyle>(style) ?: KarateStyle.MIXED,
    date = LocalDate.ofEpochDay(dateEpochDay),
    startTime = LocalTime.of(startMinuteOfDay / 60, startMinuteOfDay % 60),
    durationMinutes = durationMinutes,
    dojo = dojo,
    dojoAddress = dojoAddress,
    instructor = instructor,
    level = enumOrNull<SkillLevel>(level) ?: SkillLevel.ALL_LEVELS,
    capacity = capacity,
    booked = booked,
    description = description,
    whatToBring = whatToBring.split('|').filter { it.isNotBlank() },
    isRegistered = isRegistered
)

fun KarateTraining.toEntity() = TrainingEntity(
    id = id,
    title = title,
    type = type.name,
    style = style.name,
    dateEpochDay = date.toEpochDay(),
    startMinuteOfDay = startTime.hour * 60 + startTime.minute,
    durationMinutes = durationMinutes,
    dojo = dojo,
    dojoAddress = dojoAddress,
    instructor = instructor,
    level = level.name,
    capacity = capacity,
    booked = booked,
    description = description,
    whatToBring = whatToBring.joinToString("|")
)

fun RegistrationEntity.toModel() = TrainingRegistration(
    id = id,
    trainingId = trainingId,
    fullName = fullName,
    email = email,
    phone = phone,
    experience = enumOrNull<ExperienceLevel>(experience) ?: ExperienceLevel.BEGINNER,
    currentBelt = enumOrNull<Belt>(currentBelt),
    preferredType = enumOrNull<TrainingType>(preferredType),
    comment = comment,
    createdAt = createdAt
)

fun KataSessionWithResults.toModel() = KataSession(
    id = session.id,
    kataId = session.kataId,
    startedAt = session.startedAt,
    durationSec = session.durationSec,
    focusSection = session.focusSection,
    results = results.sortedBy { it.sectionIndex }.mapNotNull { r ->
        enumOrNull<SectionStatus>(r.status)?.let { SectionResult(r.sectionIndex, it) }
    },
    technique = session.technique,
    balance = session.balance,
    timing = session.timing,
    confidence = session.confidence,
    perceivedDifficulty = enumOrNull<PerceivedDifficulty>(session.perceivedDifficulty),
    score = session.score,
    accuracy = session.accuracy
)

fun KataSession.toEntity() = KataSessionEntity(
    id = id,
    kataId = kataId,
    startedAt = startedAt,
    durationSec = durationSec,
    focusSection = focusSection,
    technique = technique,
    balance = balance,
    timing = timing,
    confidence = confidence,
    perceivedDifficulty = perceivedDifficulty?.name,
    score = score,
    accuracy = accuracy
)

fun KataSession.resultEntities() = results.map { SectionResultEntity(sessionId = 0, sectionIndex = it.sectionIndex, status = it.status.name) }

fun WorkoutEntity.toModel() = WorkoutSession(
    id = id,
    type = enumOrNull<WorkoutType>(type) ?: WorkoutType.MIXED,
    plannedMinutes = plannedMinutes,
    durationSec = durationSec,
    effort = effort,
    startedAt = startedAt
)

fun WorkoutSession.toEntity() = WorkoutEntity(id, type.name, plannedMinutes, durationSec, effort, startedAt)

fun GoalEntity.toModel(): PersonalGoal? = enumOrNull<GoalType>(type)?.let {
    PersonalGoal(id, it, target, kataId, baseline, createdAt, isCurrent)
}

fun PersonalGoal.toEntity() = GoalEntity(id, type.name, target, kataId, baseline, createdAt, isCurrent)

fun XpEventEntity.toModel(): XpEvent? = enumOrNull<XpType>(type)?.let { XpEvent(id, eventKey, it, points, label, createdAt) }

fun XpEvent.toEntity() = XpEventEntity(id, key, type.name, points, label, createdAt)
