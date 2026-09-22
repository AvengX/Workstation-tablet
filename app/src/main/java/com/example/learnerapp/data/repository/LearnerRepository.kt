package com.example.learnerapp.data.repository

import androidx.room.withTransaction
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.DatabaseSeeder
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import com.example.learnerapp.model.ChecklistItem
import com.example.learnerapp.model.Task
import com.example.learnerapp.model.TaskScheduleItem
import com.example.learnerapp.model.TaskStep
import com.example.learnerapp.staff.repository.ScheduledTaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repository interface for Learner Mode data operations.
 */
interface LearnerRepository {
    fun getSchedule(date: String = LocalDate.now().toString()): Flow<List<TaskScheduleItem>>
    fun getScheduledTasks(date: String = LocalDate.now().toString()): Flow<List<ScheduledTaskItem>>
    fun getTask(taskId: String): Flow<Task?>
    fun getTaskProgress(taskId: String): Flow<TaskProgressEntity?>
    fun getAllTaskProgress(): Flow<List<TaskProgressEntity>>
    fun getChecklistWithProgress(taskId: String): Flow<List<ChecklistItem>>

    suspend fun startTask(taskId: String)
    suspend fun updateCurrentStep(taskId: String, stepNumber: Int)
    suspend fun moveToChecking(taskId: String)
    suspend fun toggleChecklistItem(taskId: String, itemId: Int)
    suspend fun completeTask(taskId: String)
    suspend fun seedIfEmpty()
}

/**
 * Room-backed implementation of LearnerRepository.
 */
class RoomLearnerRepository(
    private val database: AppDatabase
) : LearnerRepository {

    private val taskDao = database.taskDao()
    private val taskStepDao = database.taskStepDao()
    private val checklistDao = database.checklistDao()
    private val scheduleDao = database.scheduleDao()
    private val progressDao = database.progressDao()

    override fun getAllTaskProgress(): Flow<List<TaskProgressEntity>> {
        return progressDao.observeAllProgress()
    }

    override fun getScheduledTasks(date: String): Flow<List<ScheduledTaskItem>> {
        val todayStr = LocalDate.now().toString()
        return combine(
            scheduleDao.observeScheduleForDate(date, todayStr),
            taskDao.observeAllTasks()
        ) { scheduleEntities, allTasks ->
            val taskMap = allTasks.associateBy { it.id }
            scheduleEntities
                .sortedBy { it.order }
                .mapNotNull { scheduleEntity ->
                    val task = taskMap[scheduleEntity.taskId]
                    if (task != null) {
                        ScheduledTaskItem(scheduleItem = scheduleEntity, task = task)
                    } else null
                }
        }
    }

    override fun getSchedule(date: String): Flow<List<TaskScheduleItem>> {
        val todayStr = LocalDate.now().toString()
        return combine(
            scheduleDao.observeScheduleForDate(date, todayStr),
            taskDao.observeAllTasks(),
            progressDao.observeAllProgress()
        ) { scheduleEntities, allTasks, allProgress ->
            val taskMap = allTasks.associateBy { it.id }
            val progressMap = allProgress.associateBy { it.taskId }

            val scheduledTasks = scheduleEntities
                .sortedBy { it.order }
                .mapNotNull { scheduleEntity ->
                    val task = taskMap[scheduleEntity.taskId]
                    if (task != null) {
                        Triple(scheduleEntity, task, progressMap[scheduleEntity.taskId])
                    } else null
                }

            if (scheduledTasks.isEmpty()) {
                return@combine emptyList<TaskScheduleItem>()
            }

            // Derive active task:
            // 1. If any scheduled task has IN_PROGRESS or CHECKING, that earliest scheduled task is NOW
            val activeTriple = scheduledTasks.firstOrNull {
                val status = it.third?.status
                status == "IN_PROGRESS" || status == "CHECKING"
            }

            val uncompleted = if (activeTriple != null) {
                listOf(activeTriple) + scheduledTasks.filter { it != activeTriple && it.third?.status != "COMPLETED" }
            } else {
                scheduledTasks.filter { it.third?.status != "COMPLETED" }
            }

            uncompleted.mapIndexed { index, triple ->
                val category = when (index) {
                    0 -> "NOW"
                    1 -> "NEXT"
                    else -> "LATER"
                }
                TaskScheduleItem(
                    timeCategory = category,
                    title = triple.second.title,
                    isCurrent = index == 0,
                    isCompleted = false
                )
            }
        }
    }

    override fun getTask(taskId: String): Flow<Task?> {
        return combine(
            taskDao.observeTaskById(taskId),
            taskStepDao.observeStepsForTask(taskId),
            getChecklistWithProgress(taskId)
        ) { taskEntity, stepEntities, checklistItems ->
            if (taskEntity == null) return@combine null

            val domainSteps = stepEntities.map { step ->
                TaskStep(
                    stepNumber = step.stepNumber,
                    instruction = step.instruction,
                    photoPlaceholderLabel = "Step ${step.stepNumber} Photo Placeholder:\n${step.instruction}",
                    imageResId = null,
                    imagePath = step.imagePath
                )
            }

            Task(
                id = taskEntity.id,
                title = taskEntity.title,
                description = taskEntity.description,
                steps = domainSteps,
                checklist = checklistItems,
                completionPhrase = taskEntity.completionPhrase,
                helpPhrase = taskEntity.helpPhrase
            )
        }
    }

    override fun getTaskProgress(taskId: String): Flow<TaskProgressEntity?> {
        return progressDao.observeProgressForTask(taskId)
    }

    override fun getChecklistWithProgress(taskId: String): Flow<List<ChecklistItem>> {
        return combine(
            checklistDao.observeItemsForTask(taskId),
            progressDao.observeChecklistProgress(taskId)
        ) { items, progressList ->
            val checkedMap = progressList.associate { it.checklistItemId to it.isChecked }
            items.map { item ->
                ChecklistItem(
                    id = item.id,
                    text = item.text,
                    isChecked = checkedMap[item.id] ?: false
                )
            }
        }
    }

    override suspend fun startTask(taskId: String) {
        val existingProgress = progressDao.getProgressForTask(taskId)
        val now = System.currentTimeMillis()
        val newProgress = TaskProgressEntity(
            id = "progress_$taskId",
            taskId = taskId,
            scheduleItemId = "schedule_now",
            currentStep = 1,
            status = "IN_PROGRESS",
            startedAt = existingProgress?.startedAt ?: now,
            completedAt = null
        )
        progressDao.insertOrUpdateProgress(newProgress)
        progressDao.resetChecklistProgress(taskId)
    }

    override suspend fun updateCurrentStep(taskId: String, stepNumber: Int) {
        progressDao.updateCurrentStep(taskId, stepNumber)
    }

    override suspend fun moveToChecking(taskId: String) {
        progressDao.updateStatus(taskId, "CHECKING")
    }

    override suspend fun toggleChecklistItem(taskId: String, itemId: Int) {
        val currentProgress = progressDao.getChecklistProgress(taskId)
        val itemProgress = currentProgress.find { it.checklistItemId == itemId }
        val newChecked = !(itemProgress?.isChecked ?: false)
        progressDao.insertOrUpdateChecklistProgress(
            ChecklistProgressEntity(
                taskId = taskId,
                checklistItemId = itemId,
                isChecked = newChecked
            )
        )
    }

    override suspend fun completeTask(taskId: String) {
        val now = System.currentTimeMillis()
        val existing = progressDao.getProgressForTask(taskId)
        if (existing == null) {
            progressDao.insertOrUpdateProgress(
                TaskProgressEntity(
                    id = "progress_$taskId",
                    taskId = taskId,
                    scheduleItemId = "schedule_now",
                    currentStep = 1,
                    status = "COMPLETED",
                    startedAt = now,
                    completedAt = now
                )
            )
        } else {
            progressDao.markCompleted(taskId = taskId, status = "COMPLETED", completedAt = now)
        }
    }

    override suspend fun seedIfEmpty() {
        DatabaseSeeder.seedIfEmpty(database)
    }
}
