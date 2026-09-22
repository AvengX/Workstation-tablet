package com.example.learnerapp.staff.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnerapp.LearnerApplication
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity
import com.example.learnerapp.staff.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StepFormData(
    val instruction: String = "",
    val imagePath: String? = null,
    val instructionError: String? = null
)

data class ChecklistFormData(
    val text: String = "",
    val textError: String? = null
)

data class TaskContentUiState(
    val task: TaskEntity? = null,
    val steps: List<TaskStepEntity> = emptyList(),
    val checklist: List<ChecklistItemEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Step Dialog & Delete States
    val isStepDialogOpen: Boolean = false,
    val editingStep: TaskStepEntity? = null,
    val stepFormData: StepFormData = StepFormData(),
    val stepPendingDelete: TaskStepEntity? = null,
    // Checklist Dialog & Delete States
    val isChecklistDialogOpen: Boolean = false,
    val editingChecklist: ChecklistItemEntity? = null,
    val checklistFormData: ChecklistFormData = ChecklistFormData(),
    val checklistPendingDelete: ChecklistItemEntity? = null,
    // Active Step Protection Warning Dialog
    val activeStepProtectionWarning: String? = null
)

/**
 * ViewModel managing Task Steps, Local Photos, and Checklist Items for a workplace task template.
 */
class TaskContentViewModel(
    repository: TaskRepository? = null
) : ViewModel() {

    private val activeRepository: TaskRepository? = repository ?: run {
        try {
            LearnerApplication.instance?.taskRepository
        } catch (_: Throwable) {
            null
        }
    }

    private val _uiState = MutableStateFlow(TaskContentUiState())
    val uiState: StateFlow<TaskContentUiState> = _uiState.asStateFlow()

    private var currentTaskId: String? = null

    /**
     * Loads the specified task and begins observing its steps and checklist items from Room.
     */
    fun loadTask(taskId: String) {
        currentTaskId = taskId
        val repo = activeRepository ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val task = repo.getTask(taskId)
            _uiState.update { it.copy(task = task) }
        }

        // Observe Steps
        viewModelScope.launch {
            repo.observeSteps(taskId).collect { stepList ->
                _uiState.update { it.copy(steps = stepList, isLoading = false) }
            }
        }

        // Observe Checklist Items
        viewModelScope.launch {
            repo.observeChecklist(taskId).collect { checklistList ->
                _uiState.update { it.copy(checklist = checklistList, isLoading = false) }
            }
        }
    }

    // ==================== Step Management ====================

    fun openAddStepDialog() {
        _uiState.update {
            it.copy(
                isStepDialogOpen = true,
                editingStep = null,
                stepFormData = StepFormData()
            )
        }
    }

    fun openEditStepDialog(step: TaskStepEntity) {
        _uiState.update {
            it.copy(
                isStepDialogOpen = true,
                editingStep = step,
                stepFormData = StepFormData(
                    instruction = step.instruction,
                    imagePath = step.imagePath
                )
            )
        }
    }

    fun onStepInstructionChanged(text: String) {
        _uiState.update { state ->
            state.copy(
                stepFormData = state.stepFormData.copy(
                    instruction = text,
                    instructionError = if (text.trim().isEmpty()) "Instruction cannot be empty" else null
                )
            )
        }
    }

    fun onStepPhotoChanged(path: String?) {
        _uiState.update { state ->
            state.copy(stepFormData = state.stepFormData.copy(imagePath = path))
        }
    }

    fun closeStepDialog() {
        _uiState.update { it.copy(isStepDialogOpen = false, editingStep = null) }
    }

    fun saveStep(onSuccess: (() -> Unit)? = null) {
        val taskId = currentTaskId ?: return
        val repo = activeRepository ?: return
        val form = _uiState.value.stepFormData

        if (form.instruction.trim().isEmpty()) {
            _uiState.update {
                it.copy(stepFormData = form.copy(instructionError = "Instruction cannot be empty"))
            }
            return
        }

        val editing = _uiState.value.editingStep
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (editing == null) {
                // Add new step
                repo.addStep(taskId, form.instruction.trim(), form.imagePath).map { }
            } else {
                // Update existing step
                val updated = editing.copy(
                    instruction = form.instruction.trim(),
                    imagePath = form.imagePath
                )
                repo.updateStep(updated)
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isStepDialogOpen = false,
                            editingStep = null,
                            isLoading = false,
                            successMessage = if (editing == null) "Step added successfully." else "Step updated successfully."
                        )
                    }
                    onSuccess?.invoke()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Failed to save step."
                        )
                    }
                }
            )
        }
    }

    fun requestDeleteStep(step: TaskStepEntity) {
        _uiState.update { it.copy(stepPendingDelete = step) }
    }

    fun cancelDeleteStep() {
        _uiState.update { it.copy(stepPendingDelete = null) }
    }

    fun confirmDeleteStep() {
        val step = _uiState.value.stepPendingDelete ?: return
        val taskId = currentTaskId ?: return
        val repo = activeRepository ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(stepPendingDelete = null, isLoading = true) }
            val result = repo.deleteStep(taskId, step.id)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Step ${step.stepNumber} deleted successfully."
                        )
                    }
                },
                onFailure = { err ->
                    if (err is IllegalStateException && err.message?.contains("currently on this step") == true) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                activeStepProtectionWarning = err.message
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = err.message ?: "Failed to delete step."
                            )
                        }
                    }
                }
            )
        }
    }

    fun moveStepUp(stepId: String) {
        val taskId = currentTaskId ?: return
        val repo = activeRepository ?: return
        viewModelScope.launch {
            repo.moveStep(taskId, stepId, directionUp = true)
        }
    }

    fun moveStepDown(stepId: String) {
        val taskId = currentTaskId ?: return
        val repo = activeRepository ?: return
        viewModelScope.launch {
            repo.moveStep(taskId, stepId, directionUp = false)
        }
    }

    fun dismissActiveStepProtectionWarning() {
        _uiState.update { it.copy(activeStepProtectionWarning = null) }
    }

    // ==================== Checklist Management ====================

    fun openAddChecklistDialog() {
        _uiState.update {
            it.copy(
                isChecklistDialogOpen = true,
                editingChecklist = null,
                checklistFormData = ChecklistFormData()
            )
        }
    }

    fun openEditChecklistDialog(item: ChecklistItemEntity) {
        _uiState.update {
            it.copy(
                isChecklistDialogOpen = true,
                editingChecklist = item,
                checklistFormData = ChecklistFormData(text = item.text)
            )
        }
    }

    fun onChecklistTextChanged(text: String) {
        _uiState.update { state ->
            state.copy(
                checklistFormData = state.checklistFormData.copy(
                    text = text,
                    textError = if (text.trim().isEmpty()) "Item text cannot be empty" else null
                )
            )
        }
    }

    fun closeChecklistDialog() {
        _uiState.update { it.copy(isChecklistDialogOpen = false, editingChecklist = null) }
    }

    fun saveChecklistItem(onSuccess: (() -> Unit)? = null) {
        val taskId = currentTaskId ?: return
        val repo = activeRepository ?: return
        val form = _uiState.value.checklistFormData

        if (form.text.trim().isEmpty()) {
            _uiState.update {
                it.copy(checklistFormData = form.copy(textError = "Item text cannot be empty"))
            }
            return
        }

        val editing = _uiState.value.editingChecklist
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (editing == null) {
                // Add new checklist item
                repo.addChecklistItem(taskId, form.text.trim()).map { }
            } else {
                // Update existing item
                val updated = editing.copy(text = form.text.trim())
                repo.updateChecklistItem(updated)
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isChecklistDialogOpen = false,
                            editingChecklist = null,
                            isLoading = false,
                            successMessage = if (editing == null) "Checklist item added." else "Checklist item updated."
                        )
                    }
                    onSuccess?.invoke()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Failed to save checklist item."
                        )
                    }
                }
            )
        }
    }

    fun requestDeleteChecklist(item: ChecklistItemEntity) {
        _uiState.update { it.copy(checklistPendingDelete = item) }
    }

    fun cancelDeleteChecklist() {
        _uiState.update { it.copy(checklistPendingDelete = null) }
    }

    fun confirmDeleteChecklist() {
        val item = _uiState.value.checklistPendingDelete ?: return
        val taskId = currentTaskId ?: return
        val repo = activeRepository ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(checklistPendingDelete = null, isLoading = true) }
            val result = repo.deleteChecklistItem(taskId, item.id)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Checklist item deleted."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Failed to delete checklist item."
                        )
                    }
                }
            )
        }
    }

    fun moveChecklistUp(itemId: Int) {
        val taskId = currentTaskId ?: return
        val repo = activeRepository ?: return
        viewModelScope.launch {
            repo.moveChecklistItem(taskId, itemId, directionUp = true)
        }
    }

    fun moveChecklistDown(itemId: Int) {
        val taskId = currentTaskId ?: return
        val repo = activeRepository ?: return
        viewModelScope.launch {
            repo.moveChecklistItem(taskId, itemId, directionUp = false)
        }
    }

    // ==================== Common ====================

    fun dismissFeedback() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
