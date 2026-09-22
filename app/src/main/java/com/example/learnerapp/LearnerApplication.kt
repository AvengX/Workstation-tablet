package com.example.learnerapp

import android.app.Application
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.repository.LearnerRepository
import com.example.learnerapp.data.repository.RoomLearnerRepository
import com.example.learnerapp.staff.repository.RoomTaskRepository
import com.example.learnerapp.staff.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base Application class for Learner Workstation.
 * Initializes the Room database and repository singleton.
 */
class LearnerApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: LearnerRepository by lazy {
        RoomLearnerRepository(database)
    }

    val taskRepository: TaskRepository by lazy {
        RoomTaskRepository(database)
    }

    val scheduleRepository: com.example.learnerapp.staff.repository.ScheduleRepository by lazy {
        com.example.learnerapp.staff.repository.RoomScheduleRepository(database)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CoroutineScope(Dispatchers.IO).launch {
            repository.seedIfEmpty()
        }
    }

    companion object {
        var instance: LearnerApplication? = null
            private set
    }
}
