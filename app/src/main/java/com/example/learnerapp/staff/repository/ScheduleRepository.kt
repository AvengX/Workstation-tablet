package com.example.learnerapp.staff.repository

import androidx.room.withTransaction
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.entities.ScheduleItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.util.UUID

/**
 * Pairs a daily schedule entry with its corresponding task template.
 */
data class ScheduledTaskItem(
    val scheduleItem: ScheduleItemEntity,
    val task: TaskEntity
)

/**
 * Repository interface for Staff Daily Schedule operations.
 */
interface ScheduleRepository {
    fun observeScheduleForDate(date: String): Flow<List<ScheduledTaskItem>>
    suspend fun getScheduleForDate(date: String): List<ScheduledTaskItem>
    fun observeAvailableTasksForDate(date: String): Flow<List<TaskEntity>>
    suspend fun addTaskToSchedule(date: String, taskId: String): Result<Unit>
    suspend fun removeTaskFromSchedule(scheduleItemId: String): Result<Unit>
    suspend fun moveTaskUp(date: String, scheduleItemId: String): Result<Unit>
    suspend fun moveTaskDown(date: String, scheduleItemId: String): Result<Unit>
}

/**
 * Room-backed implementation of ScheduleRepository.
 * Manages schedule persistence, atomic reordering, contiguous order enforcement (1..N),
 * and duplicate scheduling prevention.
 */
class RoomScheduleRepository(
    private val database: AppDatabase
) : ScheduleRepository {

    private val scheduleDao = database.scheduleDao()
    private val taskDao = database.taskDao()
    private val progressDao = database.progressDao()

    override fun observeScheduleForDate(date: String): Flow<List<ScheduledTaskItem>> {
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

    override suspend fun getScheduleForDate(date: String): List<ScheduledTaskItem> {
        val todayStr = LocalDate.now().toString()
        val scheduleEntities = scheduleDao.getScheduleForDate(date, todayStr).sortedBy { it.order }
        val taskMap = taskDao.getAllTasks().associateBy { it.id }
        return scheduleEntities.mapNotNull { scheduleEntity ->
            val task = taskMap[scheduleEntity.taskId]
            if (task != null) {
                ScheduledTaskItem(scheduleItem = scheduleEntity, task = task)
            } else null
        }
    }

    override fun observeAvailableTasksForDate(date: String): Flow<List<TaskEntity>> {
        val todayStr = LocalDate.now().toString()
        return combine(
            taskDao.observeAllTasks(),
            scheduleDao.observeScheduleForDate(date, todayStr)
        ) { allTasks, scheduledEntities ->
            val scheduledTaskIds = scheduledEntities.map { it.taskId }.toSet()
            allTasks.filter { it.id !in scheduledTaskIds }
        }
    }

    override suspend fun addTaskToSchedule(date: String, taskId: String): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val todayStr = LocalDate.now().toString()
                val task = taskDao.getTaskById(taskId)
                    ?: throw IllegalArgumentException("Task not found.")

                // Verify task is not already scheduled on that date
                val existing = scheduleDao.getScheduleForDate(date, todayStr)
                if (existing.any { it.taskId == taskId }) {
                    throw IllegalStateException("${task.title} is already scheduled for this day.")
                }

                val maxOrder = scheduleDao.getMaxOrderForDate(date, todayStr) ?: 0
                val newOrder = maxOrder + 1
                val category = when (newOrder) {
                    1 -> "NOW"
                    2 -> "NEXT"
                    else -> "LATER"
                }

                val newId = "sched_" + UUID.randomUUID().toString().replace("-", "").take(12)
                val newScheduleItem = ScheduleItemEntity(
                    id = newId,
                    date = date,
                    taskId = taskId,
                    order = newOrder,
                    category = category
                )
                scheduleDao.insertScheduleItem(newScheduleItem)

                // Ensure a baseline progress record exists if this task was never started
                val existingProgress = progressDao.getProgressForTask(taskId)
                if (existingProgress == null) {
                    progressDao.insertOrUpdateProgress(
                        TaskProgressEntity(
                            id = "progress_" + taskId,
                            taskId = taskId,
                            scheduleItemId = newId,
                            currentStep = 1,
                            status = "NOT_STARTED",
                            startedAt = null,
                            completedAt = null
                        )
                    )
                }
            }
        }
    }

    override suspend fun removeTaskFromSchedule(scheduleItemId: String): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val todayStr = LocalDate.now().toString()
                val item = scheduleDao.getScheduleItemById(scheduleItemId)
                    ?: throw IllegalArgumentException("Schedule item not found.")

                val date = item.date
                scheduleDao.deleteScheduleItemById(scheduleItemId)

                // Re-index remaining schedule entries to maintain contiguous 1..N order
                val remaining = scheduleDao.getScheduleForDate(date, todayStr)
                    .filter { it.id != scheduleItemId }
                    .sortedBy { it.order }

                val updated = remaining.mapIndexed { index, entity ->
                    val newOrder = index + 1
                    val newCategory = when (newOrder) {
                        1 -> "NOW"
                        2 -> "NEXT"
                        else -> "LATER"
                    }
                    entity.copy(order = newOrder, category = newCategory)
                }
                if (updated.isNotEmpty()) {
                    scheduleDao.updateScheduleItems(updated)
                }
            }
        }
    }

    override suspend fun moveTaskUp(date: String, scheduleItemId: String): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val todayStr = LocalDate.now().toString()
                val items = scheduleDao.getScheduleForDate(date, todayStr)
                    .sortedBy { it.order }
                    .toMutableList()

                val index = items.indexOfFirst { it.id == scheduleItemId }
                if (index > 0) {
                    val itemToMove = items.removeAt(index)
                    items.add(index - 1, itemToMove)

                    // Reassign contiguous order 1..N
                    val updated = items.mapIndexed { i, entity ->
                        val newOrder = i + 1
                        val newCategory = when (newOrder) {
                            1 -> "NOW"
                            2 -> "NEXT"
                            else -> "LATER"
                        }
                        entity.copy(order = newOrder, category = newCategory)
                    }
                    scheduleDao.updateScheduleItems(updated)
                }
            }
        }
    }

    override suspend fun moveTaskDown(date: String, scheduleItemId: String): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val todayStr = LocalDate.now().toString()
                val items = scheduleDao.getScheduleForDate(date, todayStr)
                    .sortedBy { it.order }
                    .toMutableList()

                val index = items.indexOfFirst { it.id == scheduleItemId }
                if (index >= 0 && index < items.size - 1) {
                    val itemToMove = items.removeAt(index)
                    items.add(index + 1, itemToMove)

                    // Reassign contiguous order 1..N
                    val updated = items.mapIndexed { i, entity ->
                        val newOrder = i + 1
                        val newCategory = when (newOrder) {
                            1 -> "NOW"
                            2 -> "NEXT"
                            else -> "LATER"
                        }
                        entity.copy(order = newOrder, category = newCategory)
                    }
                    scheduleDao.updateScheduleItems(updated)
                }
            }
        }
    }
}
