package com.example.learnerapp.staff.repository

import androidx.room.withTransaction
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository interface for workplace Task Template management (Staff CRUD),
 * including Step and Checklist management.
 */
interface TaskRepository {
    // Task templates
    fun observeTasks(): Flow<List<TaskEntity>>
    suspend fun getTask(taskId: String): TaskEntity?
    suspend fun createTask(task: TaskEntity): Result<Unit>
    suspend fun updateTask(task: TaskEntity): Result<Unit>
    suspend fun copyTask(originalTaskId: String): Result<TaskEntity>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun isTaskActiveInLearnerProgress(taskId: String): Boolean

    // Task Steps
    fun observeSteps(taskId: String): Flow<List<TaskStepEntity>>
    suspend fun getSteps(taskId: String): List<TaskStepEntity>
    suspend fun addStep(taskId: String, instruction: String, imagePath: String? = null): Result<TaskStepEntity>
    suspend fun updateStep(step: TaskStepEntity): Result<Unit>
    suspend fun deleteStep(taskId: String, stepId: String): Result<Unit>
    suspend fun moveStep(taskId: String, stepId: String, directionUp: Boolean): Result<Unit>

    // Checklist Items
    fun observeChecklist(taskId: String): Flow<List<ChecklistItemEntity>>
    suspend fun getChecklist(taskId: String): List<ChecklistItemEntity>
    suspend fun addChecklistItem(taskId: String, text: String): Result<ChecklistItemEntity>
    suspend fun updateChecklistItem(item: ChecklistItemEntity): Result<Unit>
    suspend fun deleteChecklistItem(taskId: String, itemId: Int): Result<Unit>
    suspend fun moveChecklistItem(taskId: String, itemId: Int, directionUp: Boolean): Result<Unit>
}

/**
 * Room-backed implementation of [TaskRepository].
 * Enforces transactional safety, cascade cleanup, active learner progress protection,
 * contiguous renumbering, and stable step identity on deletion.
 */
class RoomTaskRepository(
    private val database: AppDatabase
) : TaskRepository {

    // --- Task Template Operations ---

    override fun observeTasks(): Flow<List<TaskEntity>> {
        return database.taskDao().observeAllTasks()
    }

    override suspend fun getTask(taskId: String): TaskEntity? {
        return database.taskDao().getTaskById(taskId)
    }

    override suspend fun createTask(task: TaskEntity): Result<Unit> {
        return runCatching {
            database.taskDao().insertTask(task)
        }
    }

    override suspend fun updateTask(task: TaskEntity): Result<Unit> {
        return runCatching {
            database.taskDao().updateTask(task)
        }
    }

    /**
     * Deep-copy: Duplicates the task template along with all its child steps
     * and checklist items, allocating new unique IDs.
     */
    override suspend fun copyTask(originalTaskId: String): Result<TaskEntity> {
        return runCatching {
            database.withTransaction {
                val original = database.taskDao().getTaskById(originalTaskId)
                    ?: throw NoSuchElementException("Task with ID '$originalTaskId' not found.")

                val newTaskId = "task_" + UUID.randomUUID().toString().replace("-", "").take(12)
                val copiedTask = original.copy(
                    id = newTaskId,
                    title = "${original.title} — Copy"
                )
                database.taskDao().insertTask(copiedTask)

                // Duplicate steps
                val originalSteps = database.taskStepDao().getStepsForTask(originalTaskId)
                val copiedSteps = originalSteps.map { step ->
                    TaskStepEntity(
                        id = "step_" + UUID.randomUUID().toString().replace("-", "").take(12),
                        taskId = newTaskId,
                        stepNumber = step.stepNumber,
                        instruction = step.instruction,
                        imagePath = step.imagePath
                    )
                }
                if (copiedSteps.isNotEmpty()) {
                    database.taskStepDao().insertSteps(copiedSteps)
                }

                // Duplicate checklist items
                val originalItems = database.checklistDao().getItemsForTask(originalTaskId)
                var currentMaxId = database.checklistDao().getMaxId() ?: 0
                val copiedItems = originalItems.map { item ->
                    currentMaxId += 1
                    ChecklistItemEntity(
                        id = currentMaxId,
                        taskId = newTaskId,
                        order = item.order,
                        text = item.text
                    )
                }
                if (copiedItems.isNotEmpty()) {
                    database.checklistDao().insertItems(copiedItems)
                }

                copiedTask
            }
        }
    }

    override suspend fun isTaskActiveInLearnerProgress(taskId: String): Boolean {
        val progress = database.progressDao().getProgressForTask(taskId)
        return progress != null && (progress.status == "IN_PROGRESS" || progress.status == "CHECKING")
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return runCatching {
            if (isTaskActiveInLearnerProgress(taskId)) {
                throw IllegalStateException("This task is currently in progress and cannot be deleted.")
            }

            database.withTransaction {
                database.taskStepDao().deleteStepsForTask(taskId)
                database.checklistDao().deleteItemsForTask(taskId)
                database.scheduleDao().deleteScheduleItemsForTask(taskId)
                database.progressDao().deleteChecklistProgressForTask(taskId)
                database.progressDao().deleteProgressForTask(taskId)
                database.taskDao().deleteTaskById(taskId)
            }
        }
    }

    // --- Task Step Operations ---

    override fun observeSteps(taskId: String): Flow<List<TaskStepEntity>> {
        return database.taskStepDao().observeStepsForTask(taskId)
    }

    override suspend fun getSteps(taskId: String): List<TaskStepEntity> {
        return database.taskStepDao().getStepsForTask(taskId)
    }

    override suspend fun addStep(taskId: String, instruction: String, imagePath: String?): Result<TaskStepEntity> {
        return runCatching {
            database.withTransaction {
                val maxStep = database.taskStepDao().getMaxStepNumber(taskId) ?: 0
                val newStep = TaskStepEntity(
                    id = "step_" + UUID.randomUUID().toString().replace("-", "").take(12),
                    taskId = taskId,
                    stepNumber = maxStep + 1,
                    instruction = instruction.trim(),
                    imagePath = imagePath
                )
                database.taskStepDao().insertStep(newStep)
                newStep
            }
        }
    }

    override suspend fun updateStep(step: TaskStepEntity): Result<Unit> {
        return runCatching {
            database.taskStepDao().updateStep(step.copy(instruction = step.instruction.trim()))
        }
    }

    /**
     * Deletes a step with active learner protection and contiguous renumbering.
     *
     * Protection rules:
     * 1. If learner is currently on this step (stepNumber == currentStep): throw IllegalStateException.
     * 2. If deleted stepNumber < currentStep: decrement currentStep by 1.
     * 3. If deleted stepNumber > currentStep: leave currentStep unchanged.
     * 4. Ensure currentStep stays within valid bounds [1, remainingSteps.size].
     */
    override suspend fun deleteStep(taskId: String, stepId: String): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val stepToDelete = database.taskStepDao().getStepById(stepId)
                    ?: throw NoSuchElementException("Step with ID '$stepId' not found.")

                val progress = database.progressDao().getProgressForTask(taskId)
                if (progress != null && progress.status == "IN_PROGRESS") {
                    when {
                        stepToDelete.stepNumber == progress.currentStep -> {
                            throw IllegalStateException(
                                "Cannot delete step ${stepToDelete.stepNumber} because a learner is currently on this step."
                            )
                        }
                        stepToDelete.stepNumber < progress.currentStep -> {
                            // Step before active step deleted: decrement currentStep so learner stays on same instruction
                            val adjustedStep = (progress.currentStep - 1).coerceAtLeast(1)
                            database.progressDao().updateCurrentStep(taskId, adjustedStep)
                        }
                        stepToDelete.stepNumber > progress.currentStep -> {
                            // Step after active step deleted: leave currentStep unchanged
                        }
                    }
                }

                // Delete the step
                database.taskStepDao().deleteStepById(stepId)

                // Renumber remaining steps contiguously (1..N)
                val remainingSteps = database.taskStepDao().getStepsForTask(taskId)
                remainingSteps.forEachIndexed { index, step ->
                    val expectedNumber = index + 1
                    if (step.stepNumber != expectedNumber) {
                        database.taskStepDao().updateStep(step.copy(stepNumber = expectedNumber))
                    }
                }

                // Verify currentStep remains within valid bounds
                if (progress != null && progress.status == "IN_PROGRESS" && remainingSteps.isNotEmpty()) {
                    val updatedProgress = database.progressDao().getProgressForTask(taskId)
                    if (updatedProgress != null) {
                        val boundedStep = updatedProgress.currentStep.coerceIn(1, remainingSteps.size)
                        if (boundedStep != updatedProgress.currentStep) {
                            database.progressDao().updateCurrentStep(taskId, boundedStep)
                        }
                    }
                }
            }
        }
    }

    override suspend fun moveStep(taskId: String, stepId: String, directionUp: Boolean): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val steps = database.taskStepDao().getStepsForTask(taskId)
                val index = steps.indexOfFirst { it.id == stepId }
                if (index == -1) throw NoSuchElementException("Step with ID '$stepId' not found.")

                val targetIndex = if (directionUp) index - 1 else index + 1
                if (targetIndex !in steps.indices) {
                    // Already at top or bottom boundary
                    return@withTransaction
                }

                val current = steps[index]
                val target = steps[targetIndex]

                val currentNum = current.stepNumber
                val targetNum = target.stepNumber

                database.taskStepDao().updateStep(current.copy(stepNumber = targetNum))
                database.taskStepDao().updateStep(target.copy(stepNumber = currentNum))
            }
        }
    }

    // --- Checklist Operations ---

    override fun observeChecklist(taskId: String): Flow<List<ChecklistItemEntity>> {
        return database.checklistDao().observeItemsForTask(taskId)
    }

    override suspend fun getChecklist(taskId: String): List<ChecklistItemEntity> {
        return database.checklistDao().getItemsForTask(taskId)
    }

    override suspend fun addChecklistItem(taskId: String, text: String): Result<ChecklistItemEntity> {
        return runCatching {
            database.withTransaction {
                val nextId = (database.checklistDao().getMaxId() ?: 0) + 1
                val maxOrder = database.checklistDao().getMaxOrder(taskId) ?: 0
                val newItem = ChecklistItemEntity(
                    id = nextId,
                    taskId = taskId,
                    order = maxOrder + 1,
                    text = text.trim()
                )
                database.checklistDao().insertItem(newItem)
                newItem
            }
        }
    }

    override suspend fun updateChecklistItem(item: ChecklistItemEntity): Result<Unit> {
        return runCatching {
            database.checklistDao().updateItem(item.copy(text = item.text.trim()))
        }
    }

    override suspend fun deleteChecklistItem(taskId: String, itemId: Int): Result<Unit> {
        return runCatching {
            database.withTransaction {
                database.progressDao().deleteProgressForChecklistItem(itemId)
                database.checklistDao().deleteItemById(itemId)

                // Renumber remaining items contiguously (1..M)
                val remaining = database.checklistDao().getItemsForTask(taskId)
                remaining.forEachIndexed { index, item ->
                    val expectedOrder = index + 1
                    if (item.order != expectedOrder) {
                        database.checklistDao().updateItem(item.copy(order = expectedOrder))
                    }
                }
            }
        }
    }

    override suspend fun moveChecklistItem(taskId: String, itemId: Int, directionUp: Boolean): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val items = database.checklistDao().getItemsForTask(taskId)
                val index = items.indexOfFirst { it.id == itemId }
                if (index == -1) throw NoSuchElementException("Checklist item with ID '$itemId' not found.")

                val targetIndex = if (directionUp) index - 1 else index + 1
                if (targetIndex !in items.indices) {
                    return@withTransaction
                }

                val current = items[index]
                val target = items[targetIndex]

                val currentOrder = current.order
                val targetOrder = target.order

                database.checklistDao().updateItem(current.copy(order = targetOrder))
                database.checklistDao().updateItem(target.copy(order = currentOrder))
            }
        }
    }
}
