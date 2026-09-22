package com.example.learnerapp.model

/**
 * Screen stages for the Learner experience.
 * Sequence: TODAY -> NOW -> HOW -> CHECK -> DONE -> NEXT
 */
enum class LearnerScreen(val title: String) {
    TODAY("Today"),
    NOW("Now"),
    HOW("How"),
    CHECK("Check"),
    DONE("Done"),
    NEXT("Next")
}

/**
 * Represents a single instruction step in the HOW flow.
 * [imageResId] allows providing a local drawable resource when real photographs become available.
 */
data class TaskStep(
    val stepNumber: Int,
    val instruction: String,
    val photoPlaceholderLabel: String,
    val imageResId: Int? = null,
    val imagePath: String? = null
)

/**
 * Represents a single verification check on the CHECK screen.
 */
data class ChecklistItem(
    val id: Int,
    val text: String,
    val isChecked: Boolean = false
)

/**
 * Represents an item on the TODAY's work schedule.
 */
data class TaskScheduleItem(
    val timeCategory: String, // "NOW", "NEXT", "LATER"
    val title: String,
    val isCurrent: Boolean = false,
    val isCompleted: Boolean = false
)

/**
 * Full definition of a workplace task.
 */
data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val steps: List<TaskStep>,
    val checklist: List<ChecklistItem>,
    val completionPhrase: String
)

/**
 * Immutable UI state for the learner workflow.
 */
data class LearnerUiState(
    val currentScreen: LearnerScreen = LearnerScreen.TODAY,
    val task: Task,
    val schedule: List<TaskScheduleItem>,
    val currentStepIndex: Int = 0,
    val checklistItems: List<ChecklistItem>,
    val isContinueEnabled: Boolean = false,
    val isHelpDialogOpen: Boolean = false,
    val isStaffNoticeDialogOpen: Boolean = false,
    val isNextTaskPlaceholderDialogOpen: Boolean = false,
    val nextTaskTitle: String? = null,
    val isScheduleEmpty: Boolean = false,
    val isAllTasksCompleted: Boolean = false
) {
    val totalSteps: Int get() = task.steps.size
    val currentStep: TaskStep? get() = task.steps.getOrNull(currentStepIndex)
    val canGoBack: Boolean get() = currentStepIndex > 0
    val isLastStep: Boolean get() = currentStepIndex >= totalSteps - 1
}
