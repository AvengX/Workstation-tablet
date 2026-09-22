package com.example.learnerapp.staff.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnerapp.LearnerApplication
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.staff.repository.ScheduleRepository
import com.example.learnerapp.staff.repository.ScheduledTaskItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UI State for Staff Daily Schedule Management.
 */
data class ScheduleUiState(
    val selectedDate: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    val scheduledTasks: List<ScheduledTaskItem> = emptyList(),
    val availableTasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAddTaskDialogOpen: Boolean = false,
    val selectedTaskIdToAdd: String? = null,
    val taskToRemove: ScheduledTaskItem? = null,
    val isDatePickerOpen: Boolean = false
) {
    val formattedDate: String
        get() = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))

    val isToday: Boolean
        get() = selectedDate == LocalDate.now(ZoneId.systemDefault())
}

/**
 * ViewModel for Staff Daily Schedule Management.
 * Controls date selection, reordering, adding and removing tasks.
 */
class ScheduleManagementViewModel(
    repository: ScheduleRepository? = null
) : ViewModel() {

    private val scheduleRepository: ScheduleRepository? = repository ?: run {
        try {
            LearnerApplication.instance?.scheduleRepository
        } catch (_: Throwable) {
            null
        }
    }

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var scheduleJob: Job? = null
    private var availableTasksJob: Job? = null

    init {
        loadScheduleForCurrentDate()
    }

    private fun loadScheduleForCurrentDate() {
        val repo = scheduleRepository ?: return
        val dateStr = _uiState.value.selectedDate.toString()

        scheduleJob?.cancel()
        availableTasksJob?.cancel()

        scheduleJob = viewModelScope.launch {
            repo.observeScheduleForDate(dateStr).collect { tasks ->
                _uiState.update { it.copy(scheduledTasks = tasks) }
            }
        }

        availableTasksJob = viewModelScope.launch {
            repo.observeAvailableTasksForDate(dateStr).collect { tasks ->
                _uiState.update { it.copy(availableTasks = tasks) }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        if (_uiState.value.selectedDate != date) {
            _uiState.update { it.copy(selectedDate = date, isDatePickerOpen = false) }
            loadScheduleForCurrentDate()
        } else {
            _uiState.update { it.copy(isDatePickerOpen = false) }
        }
    }

    fun previousDay() {
        selectDate(_uiState.value.selectedDate.minusDays(1))
    }

    fun nextDay() {
        selectDate(_uiState.value.selectedDate.plusDays(1))
    }

    fun goToToday() {
        selectDate(LocalDate.now(ZoneId.systemDefault()))
    }

    fun openDatePicker(open: Boolean) {
        _uiState.update { it.copy(isDatePickerOpen = open) }
    }

    fun openAddTaskDialog(open: Boolean) {
        _uiState.update {
            it.copy(
                isAddTaskDialogOpen = open,
                selectedTaskIdToAdd = if (open) it.availableTasks.firstOrNull()?.id else null
            )
        }
    }

    fun selectTaskToAdd(taskId: String) {
        _uiState.update { it.copy(selectedTaskIdToAdd = taskId) }
    }

    fun confirmAddTask() {
        val taskId = _uiState.value.selectedTaskIdToAdd ?: return
        val repo = scheduleRepository ?: return
        val dateStr = _uiState.value.selectedDate.toString()

        viewModelScope.launch {
            val result = repo.addTaskToSchedule(dateStr, taskId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isAddTaskDialogOpen = false,
                        selectedTaskIdToAdd = null,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "Unable to add this task."
                    )
                }
            }
        }
    }

    fun openRemoveDialog(task: ScheduledTaskItem?) {
        _uiState.update { it.copy(taskToRemove = task) }
    }

    fun confirmRemoveTask() {
        val task = _uiState.value.taskToRemove ?: return
        val repo = scheduleRepository ?: return

        viewModelScope.launch {
            val result = repo.removeTaskFromSchedule(task.scheduleItem.id)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(taskToRemove = null, errorMessage = null)
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "Unable to remove this task."
                    )
                }
            }
        }
    }

    fun moveTaskUp(scheduleItemId: String) {
        val repo = scheduleRepository ?: return
        val dateStr = _uiState.value.selectedDate.toString()

        viewModelScope.launch {
            val result = repo.moveTaskUp(dateStr, scheduleItemId)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = "Unable to update task order.") }
            }
        }
    }

    fun moveTaskDown(scheduleItemId: String) {
        val repo = scheduleRepository ?: return
        val dateStr = _uiState.value.selectedDate.toString()

        viewModelScope.launch {
            val result = repo.moveTaskDown(dateStr, scheduleItemId)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = "Unable to update task order.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
