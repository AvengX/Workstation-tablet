package com.example.learnerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.ScheduleItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks ORDER BY title ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY title ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface TaskStepDao {
    @Query("SELECT * FROM task_steps WHERE taskId = :taskId ORDER BY stepNumber ASC")
    suspend fun getStepsForTask(taskId: String): List<TaskStepEntity>

    @Query("SELECT * FROM task_steps WHERE taskId = :taskId ORDER BY stepNumber ASC")
    fun observeStepsForTask(taskId: String): Flow<List<TaskStepEntity>>

    @Query("SELECT * FROM task_steps WHERE id = :stepId")
    suspend fun getStepById(stepId: String): TaskStepEntity?

    @Query("SELECT MAX(stepNumber) FROM task_steps WHERE taskId = :taskId")
    suspend fun getMaxStepNumber(taskId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: TaskStepEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<TaskStepEntity>)

    @Update
    suspend fun updateStep(step: TaskStepEntity)

    @Query("DELETE FROM task_steps WHERE id = :stepId")
    suspend fun deleteStepById(stepId: String)

    @Query("DELETE FROM task_steps WHERE taskId = :taskId")
    suspend fun deleteStepsForTask(taskId: String)

    @Query("SELECT * FROM task_steps ORDER BY taskId, stepNumber ASC")
    suspend fun getAllSteps(): List<TaskStepEntity>

    @Query("DELETE FROM task_steps")
    suspend fun deleteAllSteps()
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY `order` ASC")
    suspend fun getItemsForTask(taskId: String): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY `order` ASC")
    fun observeItemsForTask(taskId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE id = :itemId")
    suspend fun getItemById(itemId: Int): ChecklistItemEntity?

    @Query("SELECT MAX(id) FROM checklist_items")
    suspend fun getMaxId(): Int?

    @Query("SELECT MAX(`order`) FROM checklist_items WHERE taskId = :taskId")
    suspend fun getMaxOrder(taskId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Int)

    @Query("DELETE FROM checklist_items WHERE taskId = :taskId")
    suspend fun deleteItemsForTask(taskId: String)

    @Query("SELECT * FROM checklist_items ORDER BY taskId, `order` ASC")
    suspend fun getAllItems(): List<ChecklistItemEntity>

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAllItems()
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_items ORDER BY `order` ASC")
    suspend fun getAllScheduleItems(): List<ScheduleItemEntity>

    @Query("SELECT * FROM schedule_items ORDER BY `order` ASC")
    fun observeAllScheduleItems(): Flow<List<ScheduleItemEntity>>

    @Query("SELECT * FROM schedule_items WHERE date = :date OR (date = 'today' AND :date = :todayDate) ORDER BY `order` ASC")
    fun observeScheduleForDate(date: String, todayDate: String): Flow<List<ScheduleItemEntity>>

    @Query("SELECT * FROM schedule_items WHERE date = :date OR (date = 'today' AND :date = :todayDate) ORDER BY `order` ASC")
    suspend fun getScheduleForDate(date: String, todayDate: String): List<ScheduleItemEntity>

    @Query("SELECT * FROM schedule_items WHERE id = :id")
    suspend fun getScheduleItemById(id: String): ScheduleItemEntity?

    @Query("SELECT MAX(`order`) FROM schedule_items WHERE date = :date OR (date = 'today' AND :date = :todayDate)")
    suspend fun getMaxOrderForDate(date: String, todayDate: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItem(item: ScheduleItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItems(items: List<ScheduleItemEntity>)

    @Update
    suspend fun updateScheduleItem(item: ScheduleItemEntity)

    @Update
    suspend fun updateScheduleItems(items: List<ScheduleItemEntity>)

    @Query("DELETE FROM schedule_items WHERE id = :id")
    suspend fun deleteScheduleItemById(id: String)

    @Query("DELETE FROM schedule_items WHERE taskId = :taskId")
    suspend fun deleteScheduleItemsForTask(taskId: String)

    @Query("DELETE FROM schedule_items")
    suspend fun deleteAllScheduleItems()
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM task_progress WHERE taskId = :taskId")
    suspend fun getProgressForTask(taskId: String): TaskProgressEntity?

    @Query("SELECT * FROM task_progress WHERE taskId = :taskId")
    fun observeProgressForTask(taskId: String): Flow<TaskProgressEntity?>

    @Query("SELECT * FROM task_progress")
    fun observeAllProgress(): Flow<List<TaskProgressEntity>>

    @Query("SELECT * FROM task_progress")
    suspend fun getAllProgress(): List<TaskProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: TaskProgressEntity)

    @Query("UPDATE task_progress SET currentStep = :stepNumber WHERE taskId = :taskId")
    suspend fun updateCurrentStep(taskId: String, stepNumber: Int)

    @Query("UPDATE task_progress SET status = :status WHERE taskId = :taskId")
    suspend fun updateStatus(taskId: String, status: String)

    @Query("UPDATE task_progress SET status = :status, completedAt = :completedAt WHERE taskId = :taskId")
    suspend fun markCompleted(taskId: String, status: String = "COMPLETED", completedAt: Long)

    @Query("SELECT * FROM checklist_progress WHERE taskId = :taskId")
    suspend fun getChecklistProgress(taskId: String): List<ChecklistProgressEntity>

    @Query("SELECT * FROM checklist_progress WHERE taskId = :taskId")
    fun observeChecklistProgress(taskId: String): Flow<List<ChecklistProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChecklistProgress(progress: ChecklistProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChecklistProgress(progressList: List<ChecklistProgressEntity>)

    @Query("UPDATE checklist_progress SET isChecked = :isChecked WHERE taskId = :taskId AND checklistItemId = :itemId")
    suspend fun updateChecklistItem(taskId: String, itemId: Int, isChecked: Boolean)

    @Query("UPDATE checklist_progress SET isChecked = 0 WHERE taskId = :taskId")
    suspend fun resetChecklistProgress(taskId: String)

    @Query("DELETE FROM checklist_progress WHERE taskId = :taskId")
    suspend fun deleteChecklistProgressForTask(taskId: String)

    @Query("DELETE FROM checklist_progress WHERE checklistItemId = :itemId")
    suspend fun deleteProgressForChecklistItem(itemId: Int)

    @Query("DELETE FROM task_progress WHERE taskId = :taskId")
    suspend fun deleteProgressForTask(taskId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProgress(progressList: List<TaskProgressEntity>)

    @Query("SELECT * FROM checklist_progress")
    suspend fun getAllChecklistProgress(): List<ChecklistProgressEntity>

    @Query("DELETE FROM task_progress")
    suspend fun deleteAllTaskProgress()

    @Query("DELETE FROM checklist_progress")
    suspend fun deleteAllChecklistProgress()
}
