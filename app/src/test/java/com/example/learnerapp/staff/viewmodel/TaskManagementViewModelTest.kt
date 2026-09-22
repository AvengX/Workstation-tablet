package com.example.learnerapp.staff.viewmodel

import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.staff.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fake in-memory implementation of [TaskRepository] for ViewModel unit testing.
 */
class FakeTaskRepository : TaskRepository {
    val tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())
    var activeTaskIds = mutableSetOf<String>()
    var shouldFailWrites = false

    override fun observeTasks(): Flow<List<TaskEntity>> = tasksFlow.asStateFlow()

    override suspend fun getTask(taskId: String): TaskEntity? {
        return tasksFlow.value.find { it.id == taskId }
    }

    override suspend fun createTask(task: TaskEntity): Result<Unit> {
        if (shouldFailWrites) return Result.failure(RuntimeException("DB error"))
        tasksFlow.value = tasksFlow.value + task
        return Result.success(Unit)
    }

    override suspend fun updateTask(task: TaskEntity): Result<Unit> {
        if (shouldFailWrites) return Result.failure(RuntimeException("DB error"))
        tasksFlow.value = tasksFlow.value.map { if (it.id == task.id) task else it }
        return Result.success(Unit)
    }

    override suspend fun copyTask(originalTaskId: String): Result<TaskEntity> {
        if (shouldFailWrites) return Result.failure(RuntimeException("DB error"))
        val original = tasksFlow.value.find { it.id == originalTaskId }
            ?: return Result.failure(NoSuchElementException("Task not found"))
        val copy = original.copy(
            id = original.id + "_copy",
            title = "${original.title} — Copy"
        )
        tasksFlow.value = tasksFlow.value + copy
        return Result.success(copy)
    }

    override suspend fun isTaskActiveInLearnerProgress(taskId: String): Boolean {
        return activeTaskIds.contains(taskId)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        if (shouldFailWrites) return Result.failure(RuntimeException("DB error"))
        if (isTaskActiveInLearnerProgress(taskId)) {
            return Result.failure(IllegalStateException("This task is currently in progress and cannot be deleted."))
        }
        tasksFlow.value = tasksFlow.value.filterNot { it.id == taskId }
        return Result.success(Unit)
    }

    // Step Stubs
    val stepsFlow = MutableStateFlow<List<com.example.learnerapp.data.local.entities.TaskStepEntity>>(emptyList())
    override fun observeSteps(taskId: String): Flow<List<com.example.learnerapp.data.local.entities.TaskStepEntity>> = stepsFlow.asStateFlow()
    override suspend fun getSteps(taskId: String): List<com.example.learnerapp.data.local.entities.TaskStepEntity> = stepsFlow.value.filter { it.taskId == taskId }
    override suspend fun addStep(taskId: String, instruction: String, imagePath: String?): Result<com.example.learnerapp.data.local.entities.TaskStepEntity> {
        val nextNum = (stepsFlow.value.filter { it.taskId == taskId }.maxOfOrNull { it.stepNumber } ?: 0) + 1
        val step = com.example.learnerapp.data.local.entities.TaskStepEntity("step_${java.util.UUID.randomUUID()}", taskId, nextNum, instruction, imagePath)
        stepsFlow.value = stepsFlow.value + step
        return Result.success(step)
    }
    override suspend fun updateStep(step: com.example.learnerapp.data.local.entities.TaskStepEntity): Result<Unit> {
        stepsFlow.value = stepsFlow.value.map { if (it.id == step.id) step else it }
        return Result.success(Unit)
    }
    override suspend fun deleteStep(taskId: String, stepId: String): Result<Unit> {
        stepsFlow.value = stepsFlow.value.filterNot { it.id == stepId }
        return Result.success(Unit)
    }
    override suspend fun moveStep(taskId: String, stepId: String, directionUp: Boolean): Result<Unit> = Result.success(Unit)

    // Checklist Stubs
    val checklistFlow = MutableStateFlow<List<com.example.learnerapp.data.local.entities.ChecklistItemEntity>>(emptyList())
    override fun observeChecklist(taskId: String): Flow<List<com.example.learnerapp.data.local.entities.ChecklistItemEntity>> = checklistFlow.asStateFlow()
    override suspend fun getChecklist(taskId: String): List<com.example.learnerapp.data.local.entities.ChecklistItemEntity> = checklistFlow.value.filter { it.taskId == taskId }
    override suspend fun addChecklistItem(taskId: String, text: String): Result<com.example.learnerapp.data.local.entities.ChecklistItemEntity> {
        val nextId = (checklistFlow.value.maxOfOrNull { it.id } ?: 0) + 1
        val nextOrder = (checklistFlow.value.filter { it.taskId == taskId }.maxOfOrNull { it.order } ?: 0) + 1
        val item = com.example.learnerapp.data.local.entities.ChecklistItemEntity(nextId, taskId, nextOrder, text)
        checklistFlow.value = checklistFlow.value + item
        return Result.success(item)
    }
    override suspend fun updateChecklistItem(item: com.example.learnerapp.data.local.entities.ChecklistItemEntity): Result<Unit> {
        checklistFlow.value = checklistFlow.value.map { if (it.id == item.id) item else it }
        return Result.success(Unit)
    }
    override suspend fun deleteChecklistItem(taskId: String, itemId: Int): Result<Unit> {
        checklistFlow.value = checklistFlow.value.filterNot { it.id == itemId }
        return Result.success(Unit)
    }
    override suspend fun moveChecklistItem(taskId: String, itemId: Int, directionUp: Boolean): Result<Unit> = Result.success(Unit)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskManagementViewModelTest {

    private lateinit var repository: FakeTaskRepository
    private lateinit var viewModel: TaskManagementViewModel

    private val sampleTask1 = TaskEntity(
        id = "task_1",
        title = "Pack one parcel",
        description = "Workstation packaging",
        helpPhrase = "Ask staff for help",
        completionPhrase = "Finished parcel"
    )

    private val sampleTask2 = TaskEntity(
        id = "task_2",
        title = "Sort supplies",
        description = "Sort inventory",
        helpPhrase = "Ask staff for help",
        completionPhrase = "Finished sorting"
    )

    @Before
    fun setUp() {
        repository = FakeTaskRepository()
        repository.tasksFlow.value = listOf(sampleTask1, sampleTask2)
        viewModel = TaskManagementViewModel(repository)
    }

    @Test
    fun loadTasks_populatesTasksFromRepository() {
        val state = viewModel.uiState.value
        assertEquals(2, state.tasks.size)
        assertEquals("Pack one parcel", state.tasks[0].title)
        assertEquals("Sort supplies", state.tasks[1].title)
    }

    @Test
    fun initCreateForm_setsDefaults_andClearsErrors() {
        viewModel.initCreateForm()
        val form = viewModel.uiState.value.formData
        assertEquals("", form.title)
        assertEquals("", form.description)
        assertEquals(TaskFormData.DEFAULT_HELP_PHRASE, form.helpPhrase)
        assertEquals(TaskFormData.DEFAULT_COMPLETION_PHRASE, form.completionPhrase)
        assertNull(form.titleError)
        assertFalse(form.isModified)
        assertNull(viewModel.uiState.value.selectedTask)
    }

    @Test
    fun initEditForm_prepopulatesTaskData() {
        viewModel.initEditForm(sampleTask1)
        val state = viewModel.uiState.value
        assertEquals(sampleTask1, state.selectedTask)
        assertEquals("Pack one parcel", state.formData.title)
        assertEquals("Workstation packaging", state.formData.description)
        assertEquals("Ask staff for help", state.formData.helpPhrase)
        assertEquals("Finished parcel", state.formData.completionPhrase)
        assertNull(state.formData.titleError)
        assertFalse(state.formData.isModified)
    }

    @Test
    fun validateForm_rejectsEmptyTitle() {
        viewModel.initCreateForm()
        viewModel.onTitleChanged("   ") // Whitespace only

        var onSuccessCalled = false
        viewModel.saveTask(onSuccess = { onSuccessCalled = true })

        assertFalse(onSuccessCalled)
        assertEquals("Task name is required.", viewModel.uiState.value.formData.titleError)
    }

    @Test
    fun createTask_savesNewTask_andTriggersSuccessCallback() = runTest {
        viewModel.initCreateForm()
        viewModel.onTitleChanged("Assemble Carton")
        viewModel.onDescriptionChanged("Folding and taping boxes")

        var onSuccessCalled = false
        viewModel.saveTask(onSuccess = { onSuccessCalled = true })

        assertTrue(onSuccessCalled)
        assertEquals("Task created.", viewModel.uiState.value.successMessage)
        assertEquals(3, repository.tasksFlow.value.size)

        val created = repository.tasksFlow.value.find { it.title == "Assemble Carton" }
        assertNotNull(created)
        assertTrue(created!!.id.startsWith("task_"))
        assertEquals("Folding and taping boxes", created.description)
    }

    @Test
    fun editTask_updatesTask_andPreservesExistingId() = runTest {
        viewModel.initEditForm(sampleTask1)
        viewModel.onTitleChanged("Pack one parcel (Express)")
        viewModel.onDescriptionChanged("Priority customer packing")

        var onSuccessCalled = false
        viewModel.saveTask(onSuccess = { onSuccessCalled = true })

        assertTrue(onSuccessCalled)
        assertEquals("Task updated.", viewModel.uiState.value.successMessage)

        val updated = repository.tasksFlow.value.find { it.id == "task_1" }
        assertNotNull(updated)
        assertEquals("task_1", updated!!.id) // ID MUST NOT CHANGE
        assertEquals("Pack one parcel (Express)", updated.title)
        assertEquals("Priority customer packing", updated.description)
    }

    @Test
    fun copyTask_createsIndependentDuplicate_withNewId() = runTest {
        viewModel.copyTask("task_1")

        assertEquals("Task copied.", viewModel.uiState.value.successMessage)
        assertEquals(3, repository.tasksFlow.value.size)

        val copy = repository.tasksFlow.value.find { it.title == "Pack one parcel — Copy" }
        assertNotNull(copy)
        assertEquals("task_1_copy", copy!!.id)
        assertEquals(sampleTask1.description, copy.description)

        // Original task remains completely unchanged
        val original = repository.tasksFlow.value.find { it.id == "task_1" }
        assertEquals("Pack one parcel", original?.title)
    }

    @Test
    fun requestDelete_forInactiveTask_opensDeleteConfirmation() {
        viewModel.requestDeleteTask(sampleTask2)

        val state = viewModel.uiState.value
        assertEquals(sampleTask2, state.taskPendingDelete)
        assertNull(state.activeTaskProtectionWarning)
    }

    @Test
    fun requestDelete_forActiveTask_blocksDeleteAndShowsWarning() {
        repository.activeTaskIds.add("task_1") // Mark task_1 as in progress

        viewModel.requestDeleteTask(sampleTask1)

        val state = viewModel.uiState.value
        assertNull(state.taskPendingDelete)
        assertEquals("This task is currently in progress and cannot be deleted.", state.activeTaskProtectionWarning)
    }

    @Test
    fun confirmDeleteTask_removesTask_andClearsPendingState() = runTest {
        viewModel.requestDeleteTask(sampleTask2)
        assertEquals(sampleTask2, viewModel.uiState.value.taskPendingDelete)

        viewModel.confirmDeleteTask()

        val state = viewModel.uiState.value
        assertNull(state.taskPendingDelete)
        assertEquals("Task deleted.", state.successMessage)
        assertEquals(1, repository.tasksFlow.value.size)
        assertNull(repository.tasksFlow.value.find { it.id == "task_2" })
    }

    @Test
    fun cancelDeleteTask_clearsPendingState_andPreservesTask() {
        viewModel.requestDeleteTask(sampleTask2)
        viewModel.cancelDeleteTask()

        assertNull(viewModel.uiState.value.taskPendingDelete)
        assertEquals(2, repository.tasksFlow.value.size)
    }

    @Test
    fun unsavedChanges_triggersDialogOnlyWhenModified() {
        viewModel.initCreateForm()
        var onDiscardCalled = false

        // Not modified yet -> direct navigation
        viewModel.handleBackNavigation(onNavigateBack = { onDiscardCalled = true })
        assertTrue(onDiscardCalled)
        assertFalse(viewModel.uiState.value.showUnsavedChangesDialog)

        // Modify field -> triggers unsaved changes dialog
        onDiscardCalled = false
        viewModel.onTitleChanged("Draft Title")
        viewModel.handleBackNavigation(onNavigateBack = { onDiscardCalled = true })
        assertFalse(onDiscardCalled)
        assertTrue(viewModel.uiState.value.showUnsavedChangesDialog)

        // Confirm discard -> invokes callback
        viewModel.confirmDiscardChanges(onDiscard = { onDiscardCalled = true })
        assertTrue(onDiscardCalled)
        assertFalse(viewModel.uiState.value.showUnsavedChangesDialog)
    }
}
