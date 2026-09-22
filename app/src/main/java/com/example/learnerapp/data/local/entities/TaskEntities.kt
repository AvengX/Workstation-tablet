package com.example.learnerapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Task definition stored in Room.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val helpPhrase: String,
    val completionPhrase: String
)

/**
 * Step definition for a specific task.
 */
@Entity(tableName = "task_steps")
data class TaskStepEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val stepNumber: Int,
    val instruction: String,
    val imagePath: String? = null
)

/**
 * Checklist item definition for physical task verification.
 */
@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey
    val id: Int,
    val taskId: String,
    val order: Int,
    val text: String
)

/**
 * Daily schedule item (NOW, NEXT, LATER).
 */
@Entity(tableName = "schedule_items")
data class ScheduleItemEntity(
    @PrimaryKey
    val id: String,
    val date: String,
    val taskId: String,
    val order: Int,
    val category: String // "NOW", "NEXT", "LATER"
)

/**
 * Persistent progress state for a task.
 * Statuses: NOT_STARTED, IN_PROGRESS, CHECKING, COMPLETED.
 */
@Entity(tableName = "task_progress")
data class TaskProgressEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val scheduleItemId: String? = null,
    val currentStep: Int = 1,
    val status: String = "NOT_STARTED",
    val startedAt: Long? = null,
    val completedAt: Long? = null
)

/**
 * Persistent selection state for each checklist item.
 */
@Entity(
    tableName = "checklist_progress",
    primaryKeys = ["taskId", "checklistItemId"]
)
data class ChecklistProgressEntity(
    val taskId: String,
    val checklistItemId: Int,
    val isChecked: Boolean = false
)
