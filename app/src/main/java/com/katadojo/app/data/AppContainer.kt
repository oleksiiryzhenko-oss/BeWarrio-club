package com.katadojo.app.data

import android.content.Context
import android.util.Log
import com.katadojo.app.data.database.KataDojoDatabase
import com.katadojo.app.data.repository.GoalRepository
import com.katadojo.app.data.repository.KataRepository
import com.katadojo.app.data.repository.PreferencesRepository
import com.katadojo.app.data.repository.TrainingRepository
import com.katadojo.app.data.repository.WorkoutRepository
import com.katadojo.app.data.repository.XpRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Manual dependency container, created once by [com.katadojo.app.KataDojoApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database = KataDojoDatabase.build(appContext)
    val preferences = PreferencesRepository(appContext)
    val trainingRepository = TrainingRepository(database)
    val kataRepository = KataRepository(database)
    val workoutRepository = WorkoutRepository(database)
    val goalRepository = GoalRepository(database)
    val xpRepository = XpRepository(database)
    val progressEngine = ProgressEngine(kataRepository, workoutRepository, goalRepository, xpRepository)

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val seeder = DemoDataSeeder(database, preferences, kataRepository, workoutRepository, progressEngine)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun start() {
        applicationScope.launch {
            try {
                seeder.seedIfNeeded()
                progressEngine.evaluate()
            } catch (e: Exception) {
                Log.e("KataDojo", "Could not prepare local data", e)
            } finally {
                _ready.value = true
            }
        }
    }

    suspend fun resetLocalData() = seeder.resetLocalData()

    suspend fun loadDemoData() = seeder.loadDemo()
}
