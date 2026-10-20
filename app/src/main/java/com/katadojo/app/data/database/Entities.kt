package com.katadojo.app.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "trainings")
data class TrainingEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val type: String,
    val style: String,
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val dojo: String,
    val dojoAddress: String,
    val instructor: String,
    val level: String,
    val capacity: Int,
    val booked: Int,
    val description: String,
    /** Items joined with '|'. */
    val whatToBring: String
)

@Entity(
    tableName = "registrations",
    indices = [Index(value = ["trainingId"], unique = true)]
)
data class RegistrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trainingId: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    val experience: String,
    val currentBelt: String?,
    val preferredType: String?,
    val comment: String,
    val createdAt: Long
)

@Entity(tableName = "kata_sessions", indices = [Index("kataId")])
data class KataSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kataId: String,
    val startedAt: Long,
    val durationSec: Int,
    val focusSection: Int?,
    val technique: Int,
    val balance: Int,
    val timing: Int,
    val confidence: Int,
    val perceivedDifficulty: String?,
    val score: Double,
    val accuracy: Int
)

@Entity(
    tableName = "section_results",
    foreignKeys = [ForeignKey(
        entity = KataSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class SectionResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val sectionIndex: Int,
    val status: String
)

data class KataSessionWithResults(
    @Embedded val session: KataSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val results: List<SectionResultEntity>
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val plannedMinutes: Int,
    val durationSec: Int,
    val effort: Int,
    val startedAt: Long
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val target: Int,
    val kataId: String?,
    val baseline: Double?,
    val createdAt: Long,
    val isCurrent: Boolean
)

@Entity(tableName = "xp_events", indices = [Index(value = ["eventKey"], unique = true)])
data class XpEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventKey: String,
    val type: String,
    val points: Int,
    val label: String,
    val createdAt: Long
)
