package com.example.learnerapp.staff.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnerapp.LearnerApplication
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.staff.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Encapsulates editable fields for creating or editing a workplace task template.
 */
data class TaskFormData(
    val title: String = "",
    val description: String = "",
    val helpPhrase: String = DEFAULT_HELP_PHRASE,
    val completionPhrase: String = DEFAULT_COMPLETION_PHRASE,
    val titleError: String? = null,
    val isModified: Boolean = false
) {
    companion object {
        const val DEFAULT_HELP_PHRASE = "I need help with this step. Please ask a member of staff."
        const val DEFAULT_COMPLETION_PHRASE = "I have finished this task. Please check."
    }
}

/**
 * Encapsulates derived learner workstation progress for a task template.
 */
data class TaskProgressInfo(
    val status: String,
    val currentStep: Int
)

/**
 * UI State for the Task Template Management feature.
 */
data class TaskManagementUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val taskProgressMap: Map<String, TaskProgressInfo> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedTask: TaskEntity? = null,
    val formData: TaskFormData = TaskFormData(),
    val taskPendingDelete: TaskEntity? = null,
    val taskPendingReset: TaskEntity? = null,
    val activeTaskProtectionWarning: String? = null,
    val showUnsavedChangesDialog: Boolean = false
)

/**
 * ViewModel managing workplace task template operations (Staff CRUD),
 * validation, and safety invariants.
 */
class TaskManagementViewModel(
    repository: TaskRepository? = null
) : ViewModel() {

    private val activeRepository: TaskRepository? = repository ?: run {
        try {
            LearnerApplication.instance?.taskRepository
        } catch (_: Throwable) {
            null
        }
    }

    private val _uiState = MutableStateFlow(TaskManagementUiState())
    val uiState: StateFlow<TaskManagementUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    /**
     * Observes task templates and learner progress from Room database.
     */
    fun loadTasks() {
        val repo = activeRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repo.observeTasks().collect { taskList ->
                _uiState.update { it.copy(tasks = taskList, isLoading = false) }
            }
        }
        viewModelScope.launch {
            repo.observeAllProgress().collect { progressList ->
                val map = progressList.associate {
                    it.taskId to TaskProgressInfo(status = it.status, currentStep = it.currentStep)
                }
                _uiState.update { it.copy(taskProgressMap = map) }
            }
        }
    }

    /**
     * Initializes the form for creating a new task template.
     */
    fun initCreateForm() {
        _uiState.update {
            it.copy(
                selectedTask = null,
                formData = TaskFormData(
                    title = "",
                    description = "",
                    helpPhrase = TaskFormData.DEFAULT_HELP_PHRASE,
                    completionPhrase = TaskFormData.DEFAULT_COMPLETION_PHRASE,
                    titleError = null,
                    isModified = false
                ),
                showUnsavedChangesDialog = false
            )
        }
    }

    /**
     * Initializes the form for editing an existing task template.
     */
    fun initEditForm(task: TaskEntity) {
        _uiState.update {
            it.copy(
                selectedTask = task,
                formData = TaskFormData(
                    title = task.title,
                    description = task.description,
                    helpPhrase = task.helpPhrase,
                    completionPhrase = task.completionPhrase,
                    titleError = null,
                    isModified = false
                ),
                showUnsavedChangesDialog = false
            )
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    title = newTitle,
                    titleError = null,
                    isModified = true
                )
            )
        }
    }

    fun onDescriptionChanged(newDescription: String) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    description = newDescription,
                    isModified = true
                )
            )
        }
    }

    fun onHelpPhraseChanged(newHelpPhrase: String) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    helpPhrase = newHelpPhrase,
                    isModified = true
                )
            )
        }
    }

    fun onCompletionPhraseChanged(newCompletionPhrase: String) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    completionPhrase = newCompletionPhrase,
                    isModified = true
                )
            )
        }
    }

    /**
     * Validates the form fields before saving.
     */
    private fun validateForm(): Boolean {
        val currentTitle = _uiState.value.formData.title.trim()
        if (currentTitle.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    formData = state.formData.copy(titleError = "Task name is required.")
                )
            }
            return false
        }
        return true
    }

    /**
     * Saves the task template (Create or Edit) through the repository.
     */
    fun saveTask(onSuccess: () -> Unit) {
        if (!validateForm()) return
        val repo = activeRepository ?: return

        val form = _uiState.value.formData
        val selected = _uiState.value.selectedTask

        viewModelScope.launch {
            if (selected == null) {
                // CREATE new task
                val uniqueId = "task_" + UUID.randomUUID().toString().replace("-", "").take(12)
                val newTask = TaskEntity(
                    id = uniqueId,
                    title = form.title.trim(),
                    description = form.description.trim(),
                    helpPhrase = form.helpPhrase.trim(),
                    completionPhrase = form.completionPhrase.trim()
                )
                val result = repo.createTask(newTask)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            successMessage = "Task created.",
                            errorMessage = null
                        )
                    }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "Unable to save this task. Please try again.")
                    }
                }
            } else {
                // EDIT existing task (preserve existing task ID)
                val updatedTask = selected.copy(
                    title = form.title.trim(),
                    description = form.description.trim(),
                    helpPhrase = form.helpPhrase.trim(),
                    completionPhrase = form.completionPhrase.trim()
                )
                val result = repo.updateTask(updatedTask)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            successMessage = "Task updated.",
                            errorMessage = null
                        )
                    }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "Unable to save this task. Please try again.")
                    }
                }
            }
        }
    }

    /**
     * Creates an independent duplicate of the specified task template.
     */
    fun copyTask(taskId: String) {
        val repo = activeRepository ?: return
        viewModelScope.launch {
            val result = repo.copyTask(taskId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = "Task copied.",
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Unable to copy task. Please try again.")
                }
            }
        }
    }

    /**
     * Evaluates task deletion safety: blocks delete if active in learner progress,
     * otherwise prompts confirmation dialog.
     */
    fun requestDeleteTask(task: TaskEntity) {
        val repo = activeRepository ?: return
        viewModelScope.launch {
            val isActive = repo.isTaskActiveInLearnerProgress(task.id)
            if (isActive) {
                _uiState.update {
                    it.copy(
                        activeTaskProtectionWarning = "This task is currently in progress and cannot be deleted.",
                        taskPendingDelete = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        taskPendingDelete = task,
                        activeTaskProtectionWarning = null
                    )
                }
            }
        }
    }

    /**
     * Executes task deletion once confirmed.
     */
    fun confirmDeleteTask() {
        val taskToDelete = _uiState.value.taskPendingDelete ?: return
        val repo = activeRepository ?: return

        viewModelScope.launch {
            val result = repo.deleteTask(taskToDelete.id)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        taskPendingDelete = null,
                        successMessage = "Task deleted.",
                        errorMessage = null
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unable to delete task. Please try again."
                _uiState.update {
                    it.copy(
                        taskPendingDelete = null,
                        errorMessage = error
                    )
                }
            }
        }
    }

    fun cancelDeleteTask() {
        _uiState.update { it.copy(taskPendingDelete = null) }
    }

    /**
     * Prepares task progress reset and prompts staff confirmation dialog.
     */
    fun requestResetTask(task: TaskEntity) {
        _uiState.update { it.copy(taskPendingReset = task) }
    }

    fun cancelResetTask() {
        _uiState.update { it.copy(taskPendingReset = null) }
    }

    /**
     * Confirms and executes task progress reset (clearing TaskProgress and ChecklistProgress).
     * Template definitions, steps, checklists, and schedule items remain untouched.
     */
    fun confirmResetTask(onSuccess: () -> Unit = {}) {
        val task = _uiState.value.taskPendingReset ?: return
        val repo = activeRepository ?: return
        viewModelScope.launch {
            val result = repo.resetTaskProgress(task.id)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        taskPendingReset = null,
                        successMessage = "Task progress reset for \"${task.title}\"."
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        taskPendingReset = null,
                        errorMessage = "Unable to reset progress. Please try again."
                    )
                }
            }
        }
    }

    fun dismissActiveTaskWarning() {
        _uiState.update { it.copy(activeTaskProtectionWarning = null) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /**
     * Checks whether the user has uncommitted form changes before navigating away.
     */
    fun handleBackNavigation(onNavigateBack: () -> Unit) {
        if (_uiState.value.formData.isModified) {
            _uiState.update { it.copy(showUnsavedChangesDialog = true) }
        } else {
            onNavigateBack()
        }
    }

    fun confirmDiscardChanges(onDiscard: () -> Unit) {
        _uiState.update { it.copy(showUnsavedChangesDialog = false) }
        onDiscard()
    }

    fun dismissUnsavedChangesDialog() {
        _uiState.update { it.copy(showUnsavedChangesDialog = false) }
    }
}
