package com.katadojo.app.data.repository

import androidx.room.withTransaction
import com.katadojo.app.data.database.GoalEntity
import com.katadojo.app.data.database.KataDojoDatabase
import com.katadojo.app.data.database.RegistrationEntity
import com.katadojo.app.data.database.XpEventEntity
import com.katadojo.app.data.database.resultEntities
import com.katadojo.app.data.database.toEntity
import com.katadojo.app.data.database.toModel
import com.katadojo.app.data.model.Belt
import com.katadojo.app.data.model.ExperienceLevel
import com.katadojo.app.data.model.KarateTraining
import com.katadojo.app.data.model.KataSession
import com.katadojo.app.data.model.PersonalGoal
import com.katadojo.app.data.model.RegistrationWithTraining
import com.katadojo.app.data.model.TrainingRegistration
import com.katadojo.app.data.model.TrainingType
import com.katadojo.app.data.model.WorkoutSession
import com.katadojo.app.data.model.XpEvent
import com.katadojo.app.data.model.XpType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

sealed interface RegistrationResult {
    data class Success(val registrationId: Long) : RegistrationResult
    data object AlreadyRegistered : RegistrationResult
    data object Full : RegistrationResult
    data object Ended : RegistrationResult
    data object NotFound : RegistrationResult
}

class TrainingRepository(private val db: KataDojoDatabase) {
    private val trainingDao = db.trainingDao()
    private val registrationDao = db.registrationDao()

    val trainings: Flow<List<KarateTraining>> =
        combine(trainingDao.observeAll(), registrationDao.observeAll()) { trainings, registrations ->
            val registered = registrations.map { it.trainingId }.toSet()
            trainings.map { it.toModel(it.id in registered) }
        }

    val registrations: Flow<List<RegistrationWithTraining>> =
        combine(registrationDao.observeAll(), trainingDao.observeAll()) { registrations, trainings ->
            val byId = trainings.associateBy { it.id }
            registrations.mapNotNull { r -> byId[r.trainingId]?.let { RegistrationWithTraining(r.toModel(), it.toModel(true)) } }
        }

    fun observeTraining(id: Long): Flow<KarateTraining?> =
        combine(trainingDao.observeById(id), registrationDao.observeAll()) { t, regs ->
            t?.toModel(regs.any { it.trainingId == id })
        }

    suspend fun getTraining(id: Long): KarateTraining? {
        val entity = trainingDao.getById(id) ?: return null
        return entity.toModel(registrationDao.findByTraining(id) != null)
    }

    suspend fun getRegistration(id: Long): RegistrationWithTraining? {
        val r = registrationDao.getById(id) ?: return null
        val t = trainingDao.getById(r.trainingId) ?: return null
        return RegistrationWithTraining(r.toModel(), t.toModel(true))
    }

    suspend fun latestRegistration(): TrainingRegistration? = registrationDao.latest()?.toModel()

    suspend fun register(
        trainingId: Long,
        fullName: String,
        email: String,
        phone: String,
        experience: ExperienceLevel,
        currentBelt: Belt?,
        preferredType: TrainingType?,
        comment: String,
        now: Long = System.currentTimeMillis()
    ): RegistrationResult = db.withTransaction {
        val entity = trainingDao.getById(trainingId) ?: return@withTransaction RegistrationResult.NotFound
        val training = entity.toModel(false)
        when {
            registrationDao.findByTraining(trainingId) != null -> RegistrationResult.AlreadyRegistered
            training.hasEnded() -> RegistrationResult.Ended
            training.isFull -> RegistrationResult.Full
            else -> {
                val id = registrationDao.insert(
                    RegistrationEntity(
                        trainingId = trainingId,
                        fullName = fullName.trim(),
                        email = email.trim(),
                        phone = phone.trim(),
                        experience = experience.name,
                        currentBelt = currentBelt?.name,
                        preferredType = preferredType?.name,
                        comment = comment.trim(),
                        createdAt = now
                    )
                )
                trainingDao.adjustBooked(trainingId, 1)
                RegistrationResult.Success(id)
            }
        }
    }

    suspend fun cancelRegistration(trainingId: Long) {
        db.withTransaction {
            val existing = registrationDao.findByTraining(trainingId) ?: return@withTransaction
            registrationDao.delete(existing.id)
            trainingDao.adjustBooked(trainingId, -1)
        }
    }
}

class KataRepository(db: KataDojoDatabase) {
    private val dao = db.kataSessionDao()

    /** All kata sessions (full and single-section), newest first. */
    val sessions: Flow<List<KataSession>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    fun session(id: Long): Flow<KataSession?> = dao.observeById(id).map { it?.toModel() }

    suspend fun save(session: KataSession): Long = dao.insertWithResults(session.toEntity(), session.resultEntities())
}

class WorkoutRepository(db: KataDojoDatabase) {
    private val dao = db.workoutDao()

    val workouts: Flow<List<WorkoutSession>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    fun workout(id: Long): Flow<WorkoutSession?> = dao.observeById(id).map { it?.toModel() }

    suspend fun save(workout: WorkoutSession): Long = dao.insert(workout.toEntity())
}

class GoalRepository(private val db: KataDojoDatabase) {
    private val dao = db.goalDao()

    val goals: Flow<List<PersonalGoal>> = dao.observeAll().map { list -> list.mapNotNull { it.toModel() } }

    /** Creates a goal and makes it the current goal. */
    suspend fun create(goal: PersonalGoal): Long = db.withTransaction {
        val id = dao.insert(goal.copy(id = 0, isCurrent = false).toEntity())
        dao.setCurrent(id)
        id
    }

    suspend fun setCurrent(id: Long) = dao.setCurrent(id)

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun insertRaw(goal: GoalEntity): Long = dao.insert(goal)
}

class XpRepository(db: KataDojoDatabase) {
    private val dao = db.xpDao()

    val events: Flow<List<XpEvent>> = dao.observeAll().map { list -> list.mapNotNull { it.toModel() } }

    val total: Flow<Int> = events.map { list -> list.sumOf { it.points } }

    /** Awards XP once per [key]. Returns true when the award is new. */
    suspend fun award(key: String, type: XpType, label: String, at: Long = System.currentTimeMillis()): Boolean =
        dao.insert(XpEventEntity(eventKey = key, type = type.name, points = type.points, label = label, createdAt = at)) > 0
}
