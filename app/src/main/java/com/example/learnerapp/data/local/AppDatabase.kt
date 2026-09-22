package com.example.learnerapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.learnerapp.data.local.dao.ChecklistDao
import com.example.learnerapp.data.local.dao.ProgressDao
import com.example.learnerapp.data.local.dao.ScheduleDao
import com.example.learnerapp.data.local.dao.TaskDao
import com.example.learnerapp.data.local.dao.TaskStepDao
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.ScheduleItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity

/**
 * Main Room Database for the Learner Workstation application.
 */
@Database(
    entities = [
        TaskEntity::class,
        TaskStepEntity::class,
        ChecklistItemEntity::class,
        ScheduleItemEntity::class,
        TaskProgressEntity::class,
        ChecklistProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun taskStepDao(): TaskStepDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "learner_workstation.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
