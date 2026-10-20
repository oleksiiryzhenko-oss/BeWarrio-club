package com.katadojo.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrainingEntity::class, RegistrationEntity::class, KataSessionEntity::class, SectionResultEntity::class,
        WorkoutEntity::class, GoalEntity::class, XpEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KataDojoDatabase : RoomDatabase() {
    abstract fun trainingDao(): TrainingDao
    abstract fun registrationDao(): RegistrationDao
    abstract fun kataSessionDao(): KataSessionDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun goalDao(): GoalDao
    abstract fun xpDao(): XpDao

    companion object {
        fun build(context: Context): KataDojoDatabase =
            Room.databaseBuilder(context.applicationContext, KataDojoDatabase::class.java, "katadojo.db").build()
    }
}
