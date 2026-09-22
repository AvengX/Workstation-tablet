package com.example.learnerapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnerapp.LearnerApplication
import com.example.learnerapp.data.SampleData
import com.example.learnerapp.data.repository.LearnerRepository
import com.example.learnerapp.model.LearnerScreen
import com.example.learnerapp.model.LearnerUiState
import com.example.learnerapp.model.TaskScheduleItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * ViewModel for Learner Mode Phase 6.
 * Backed by Room via LearnerRepository, managing dynamic daily schedule consumption,
 * active task progress protection, dynamic checklist states, and automatic session restoration.
 */
class LearnerViewModel(
    private val repository: LearnerRepository? = null,
    private val scheduleDate: String = LocalDate.now(ZoneId.systemDefault()).toString()
) : ViewModel() {

    private val activeRepository: LearnerRepository? = repository ?: run {
        try {
            LearnerApplication.instance?.repository
        } catch (_: Throwable) {
            null
        }
    }

    private val _uiState = MutableStateFlow(
        LearnerUiState(
            currentScreen = LearnerScreen.TODAY,
            task = SampleData.sampleTask,
            schedule = SampleData.sampleSchedule,
            currentStepIndex = 0,
            checklistItems = SampleData.sampleChecklist,
            isContinueEnabled = false,
            isHelpDialogOpen = false,
            isStaffNoticeDialogOpen = false,
            isNextTaskPlaceholderDialogOpen = false,
            nextTaskTitle = "Sort supplies",
            isScheduleEmpty = false,
            isAllTasksCompleted = false
        )
    )
    val uiState: StateFlow<LearnerUiState> = _uiState.asStateFlow()

    private var hasRestoredSession = false
    private var currentActiveTaskId: String? = null

    private var taskJob: Job? = null
    private var checklistJob: Job? = null
    private var progressJob: Job? = null

    init {
        observeRepositoryData()
    }

    private fun observeRepositoryData() {
        val repo = activeRepository ?: return

        // Observe daily schedule and task progress dynamically
        viewModelScope.launch {
            combine(
                repo.getScheduledTasks(scheduleDate),
                repo.getAllTaskProgress()
            ) { scheduledTasks, allProgress ->
                Pair(scheduledTasks, allProgress)
            }.collect { (scheduledTasks, allProgress) ->
                val progressMap = allProgress.associateBy { it.taskId }

                if (scheduledTasks.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            schedule = emptyList(),
                            isScheduleEmpty = true,
                            isAllTasksCompleted = false,
                            nextTaskTitle = null
                        )
                    }
                    return@collect
                }

                // 1. Determine active task (NOW):
                // If any scheduled task has IN_PROGRESS or CHECKING, pick earliest scheduled in order ASC
                val inProgressItem = scheduledTasks.firstOrNull {
                    val status = progressMap[it.task.id]?.status
                    status == "IN_PROGRESS" || status == "CHECKING"
                }

                val activeItem = inProgressItem ?: scheduledTasks.firstOrNull {
                    progressMap[it.task.id]?.status != "COMPLETED"
                }

                if (activeItem == null) {
                    // All scheduled tasks are completed
                    _uiState.update {
                        it.copy(
                            schedule = emptyList(),
                            isScheduleEmpty = false,
                            isAllTasksCompleted = true,
                            nextTaskTitle = null
                        )
                    }
                } else {
                    currentActiveTaskId = activeItem.task.id
                    val otherUncompleted = scheduledTasks.filter {
                        it != activeItem && progressMap[it.task.id]?.status != "COMPLETED"
                    }
                    val nextItem = otherUncompleted.firstOrNull()

                    val derivedSchedule = mutableListOf<TaskScheduleItem>()
                    derivedSchedule.add(
                        TaskScheduleItem(
                            timeCategory = "NOW",
                            title = activeItem.task.title,
                            isCurrent = true,
                            isCompleted = false
                        )
                    )
                    if (nextItem != null) {
                        derivedSchedule.add(
                            TaskScheduleItem(
                                timeCategory = "NEXT",
                                title = nextItem.task.title,
                                isCurrent = false,
                                isCompleted = false
                            )
                        )
                    }
                    otherUncompleted.drop(1).forEach {
                        derivedSchedule.add(
                            TaskScheduleItem(
                                timeCategory = "LATER",
                                title = it.task.title,
                                isCurrent = false,
                                isCompleted = false
                            )
                        )
                    }

                    _uiState.update {
                        it.copy(
                            schedule = derivedSchedule,
                            isScheduleEmpty = false,
                            isAllTasksCompleted = false,
                            nextTaskTitle = nextItem?.task?.title
                        )
                    }

                    if (!hasRestoredSession) {
                        val activeProgress = progressMap[activeItem.task.id]
                        if (activeProgress != null) {
                            hasRestoredSession = true
                            when (activeProgress.status) {
                                "IN_PROGRESS" -> {
                                    val restoredStepIndex = (activeProgress.currentStep - 1).coerceAtLeast(0)
                                    _uiState.update {
                                        it.copy(
                                            currentScreen = LearnerScreen.HOW,
                                            currentStepIndex = restoredStepIndex
                                        )
                                    }
                                }
                                "CHECKING" -> {
                                    _uiState.update {
                                        it.copy(
                                            currentScreen = LearnerScreen.CHECK,
                                            currentStepIndex = (it.totalSteps - 1).coerceAtLeast(0)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    bindActiveTask(activeItem.task.id)
                }
            }
        }
    }

    private fun bindActiveTask(taskId: String) {
        if (currentActiveTaskId == taskId && taskJob?.isActive == true) return
        currentActiveTaskId = taskId
        val repo = activeRepository ?: return

        taskJob?.cancel()
        checklistJob?.cancel()
        progressJob?.cancel()

        taskJob = viewModelScope.launch {
            repo.getTask(taskId).collect { task ->
                if (task != null) {
                    _uiState.update { it.copy(task = task) }
                }
            }
        }

        checklistJob = viewModelScope.launch {
            repo.getChecklistWithProgress(taskId).collect { checklist ->
                val allChecked = checklist.isEmpty() || checklist.all { it.isChecked }
                _uiState.update {
                    it.copy(
                        checklistItems = checklist,
                        isContinueEnabled = allChecked
                    )
                }
            }
        }

        progressJob = viewModelScope.launch {
            repo.getTaskProgress(taskId).collect { progress ->
                if (progress != null && !hasRestoredSession) {
                    hasRestoredSession = true
                    when (progress.status) {
                        "IN_PROGRESS" -> {
                            val restoredStepIndex = (progress.currentStep - 1).coerceAtLeast(0)
                            _uiState.update {
                                it.copy(
                                    currentScreen = LearnerScreen.HOW,
                                    currentStepIndex = restoredStepIndex
                                )
                            }
                        }
                        "CHECKING" -> {
                            _uiState.update {
                                it.copy(
                                    currentScreen = LearnerScreen.CHECK,
                                    currentStepIndex = (it.totalSteps - 1).coerceAtLeast(0)
                                )
                            }
                        }
                        else -> {
                            // NOT_STARTED or COMPLETED: remain on TODAY
                        }
                    }
                }
            }
        }
    }

    /**
     * Direct navigation to a specific screen (e.g. from the navigation rail).
     */
    fun navigateTo(screen: LearnerScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    /**
     * Navigates to the NOW screen from TODAY.
     */
    fun navigateToNow() {
        _uiState.update { it.copy(currentScreen = LearnerScreen.NOW) }
    }

    /**
     * Starts the current task: resets steps and checklist, transitions to HOW Step 1.
     * Persists status = IN_PROGRESS and currentStep = 1.
     */
    fun startTask() {
        val taskId = currentActiveTaskId ?: _uiState.value.task.id
        val repo = activeRepository
        if (repo != null) {
            viewModelScope.launch {
                repo.startTask(taskId)
            }
        }
        _uiState.update { state ->
            val resetChecklist = state.checklistItems.map { it.copy(isChecked = false) }
            state.copy(
                currentScreen = LearnerScreen.HOW,
                currentStepIndex = 0,
                checklistItems = resetChecklist,
                isContinueEnabled = resetChecklist.isEmpty()
            )
        }
    }

    /**
     * Advances to the next HOW step, or moves to the CHECK screen if at the last step.
     * Persists updated step or status = CHECKING in Room.
     */
    fun nextStep() {
        val taskId = currentActiveTaskId ?: _uiState.value.task.id
        _uiState.update { state ->
            if (state.currentStepIndex < state.totalSteps - 1) {
                val nextIndex = state.currentStepIndex + 1
                activeRepository?.let { repo ->
                    viewModelScope.launch {
                        repo.updateCurrentStep(taskId, nextIndex + 1)
                    }
                }
                state.copy(currentStepIndex = nextIndex)
            } else {
                activeRepository?.let { repo ->
                    viewModelScope.launch {
                        repo.moveToChecking(taskId)
                    }
                }
                state.copy(currentScreen = LearnerScreen.CHECK)
            }
        }
    }

    /**
     * Moves to the previous HOW step (disabled if already on Step 1).
     * Persists updated step in Room.
     */
    fun previousStep() {
        val taskId = currentActiveTaskId ?: _uiState.value.task.id
        _uiState.update { state ->
            if (state.currentStepIndex > 0) {
                val prevIndex = state.currentStepIndex - 1
                activeRepository?.let { repo ->
                    viewModelScope.launch {
                        repo.updateCurrentStep(taskId, prevIndex + 1)
                    }
                }
                state.copy(currentStepIndex = prevIndex)
            } else {
                state
            }
        }
    }

    /**
     * Toggles a checklist item and updates whether CONTINUE is enabled.
     * Persists checklist selection in Room.
     */
    fun toggleChecklistItem(itemId: Int) {
        val taskId = currentActiveTaskId ?: _uiState.value.task.id
        activeRepository?.let { repo ->
            viewModelScope.launch {
                repo.toggleChecklistItem(taskId, itemId)
            }
        }
        _uiState.update { state ->
            val updatedList = state.checklistItems.map { item ->
                if (item.id == itemId) {
                    item.copy(isChecked = !item.isChecked)
                } else {
                    item
                }
            }
            val allChecked = updatedList.isEmpty() || updatedList.all { it.isChecked }
            state.copy(
                checklistItems = updatedList,
                isContinueEnabled = allChecked
            )
        }
    }

    /**
     * Moves from the CHECK screen to the DONE screen when validation passes.
     */
    fun continueFromCheck() {
        _uiState.update { state ->
            if (state.isContinueEnabled) {
                state.copy(currentScreen = LearnerScreen.DONE)
            } else {
                state
            }
        }
    }

    /**
     * Completes the current task and advances to the NEXT screen.
     * Persists status = COMPLETED and completedAt timestamp in Room.
     */
    fun completeTask() {
        val taskId = currentActiveTaskId ?: _uiState.value.task.id
        activeRepository?.let { repo ->
            viewModelScope.launch {
                repo.completeTask(taskId)
            }
        }
        _uiState.update { state ->
            state.copy(currentScreen = LearnerScreen.NEXT)
        }
    }

    /**
     * Advances from NEXT screen to either the next task's NOW screen or back to TODAY.
     */
    fun startNextTask() {
        if (_uiState.value.isAllTasksCompleted) {
            _uiState.update { it.copy(currentScreen = LearnerScreen.TODAY) }
        } else {
            _uiState.update { it.copy(currentScreen = LearnerScreen.NOW) }
        }
    }

    /**
     * Controls the placeholder notice for the NEXT task.
     */
    fun showNextTaskPlaceholder(show: Boolean) {
        _uiState.update { it.copy(isNextTaskPlaceholderDialogOpen = show) }
    }

    /**
     * Toggles the "I Need Help" dialog.
     */
    fun openHelpDialog(open: Boolean) {
        _uiState.update { it.copy(isHelpDialogOpen = open) }
    }

    /**
     * Toggles the Staff Notice dialog.
     */
    fun openStaffDialog(open: Boolean) {
        _uiState.update { it.copy(isStaffNoticeDialogOpen = open) }
    }

    /**
     * Resets the session restoration flag and reapplies the Phase 6 active task derivation
     * to evaluate current persisted progress (used when resuming from Staff Mode).
     */
    fun restoreSessionFromProgress() {
        hasRestoredSession = false
        val repo = activeRepository ?: return
        viewModelScope.launch {
            val scheduledTasks = repo.getScheduledTasks(scheduleDate).firstOrNull() ?: return@launch
            val allProgress = repo.getAllTaskProgress().firstOrNull() ?: return@launch
            val progressMap = allProgress.associateBy { it.taskId }
            if (scheduledTasks.isEmpty()) return@launch

            val inProgressItem = scheduledTasks.firstOrNull {
                val status = progressMap[it.task.id]?.status
                status == "IN_PROGRESS" || status == "CHECKING"
            }
            val activeItem = inProgressItem ?: scheduledTasks.firstOrNull {
                progressMap[it.task.id]?.status != "COMPLETED"
            }

            if (activeItem != null) {
                val activeProgress = progressMap[activeItem.task.id]
                if (activeProgress != null) {
                    hasRestoredSession = true
                    when (activeProgress.status) {
                        "IN_PROGRESS" -> {
                            val restoredStepIndex = (activeProgress.currentStep - 1).coerceAtLeast(0)
                            _uiState.update {
                                it.copy(
                                    currentScreen = LearnerScreen.HOW,
                                    currentStepIndex = restoredStepIndex
                                )
                            }
                        }
                        "CHECKING" -> {
                            _uiState.update {
                                it.copy(
                                    currentScreen = LearnerScreen.CHECK,
                                    currentStepIndex = (it.totalSteps - 1).coerceAtLeast(0)
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    currentScreen = LearnerScreen.TODAY,
                                    currentStepIndex = 0
                                )
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            currentScreen = LearnerScreen.TODAY,
                            currentStepIndex = 0
                        )
                    }
                }
                bindActiveTask(activeItem.task.id)
            }
        }
    }
}
