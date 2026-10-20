package com.katadojo.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {
    @Query("SELECT * FROM trainings ORDER BY dateEpochDay, startMinuteOfDay")
    fun observeAll(): Flow<List<TrainingEntity>>

    @Query("SELECT * FROM trainings WHERE id = :id")
    fun observeById(id: Long): Flow<TrainingEntity?>

    @Query("SELECT * FROM trainings WHERE id = :id")
    suspend fun getById(id: Long): TrainingEntity?

    @Query("SELECT MAX(dateEpochDay) FROM trainings")
    suspend fun latestDate(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TrainingEntity>)

    @Query("UPDATE trainings SET booked = MAX(0, MIN(capacity, booked + :delta)) WHERE id = :id")
    suspend fun adjustBooked(id: Long, delta: Int)

    @Query("DELETE FROM trainings")
    suspend fun clear()
}

@Dao
interface RegistrationDao {
    @Query("SELECT * FROM registrations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE id = :id")
    suspend fun getById(id: Long): RegistrationEntity?

    @Query("SELECT * FROM registrations WHERE trainingId = :trainingId LIMIT 1")
    suspend fun findByTraining(trainingId: Long): RegistrationEntity?

    @Query("SELECT * FROM registrations ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(): RegistrationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: RegistrationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RegistrationEntity>)

    @Query("DELETE FROM registrations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM registrations")
    suspend fun clear()
}

@Dao
abstract class KataSessionDao {
    @Transaction
    @Query("SELECT * FROM kata_sessions ORDER BY startedAt DESC")
    abstract fun observeAll(): Flow<List<KataSessionWithResults>>

    @Transaction
    @Query("SELECT * FROM kata_sessions WHERE id = :id")
    abstract fun observeById(id: Long): Flow<KataSessionWithResults?>

    @Insert
    abstract suspend fun insertSession(session: KataSessionEntity): Long

    @Insert
    abstract suspend fun insertResults(results: List<SectionResultEntity>)

    @Transaction
    open suspend fun insertWithResults(session: KataSessionEntity, results: List<SectionResultEntity>): Long {
        val id = insertSession(session)
        insertResults(results.map { it.copy(sessionId = id) })
        return id
    }

    @Query("DELETE FROM kata_sessions WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM section_results")
    abstract suspend fun clearResults()

    @Query("DELETE FROM kata_sessions")
    abstract suspend fun clearSessions()

    @Transaction
    open suspend fun clearAll() {
        clearResults()
        clearSessions()
    }
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutEntity?>

    @Insert
    suspend fun insert(item: WorkoutEntity): Long

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM workouts")
    suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY isCurrent DESC, createdAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Insert
    suspend fun insert(item: GoalEntity): Long

    @Query("UPDATE goals SET isCurrent = (id = :id)")
    suspend fun setCurrent(id: Long)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM goals")
    suspend fun clear()
}

@Dao
interface XpDao {
    @Query("SELECT * FROM xp_events ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<XpEventEntity>>

    /** Returns -1 when the event was already awarded (unique key). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: XpEventEntity): Long

    @Query("DELETE FROM xp_events")
    suspend fun clear()
}
