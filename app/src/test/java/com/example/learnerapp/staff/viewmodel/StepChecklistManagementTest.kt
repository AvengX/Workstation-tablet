package com.example.learnerapp.staff.viewmodel

import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity
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
 * Unit tests for [TaskContentViewModel] covering Step & Checklist management,
 * input validation, and active learner step protection warnings.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StepChecklistManagementTest {

    private lateinit var repository: FakeTaskRepository
    private lateinit var viewModel: TaskContentViewModel

    private val testTask = TaskEntity(
        id = "task_packaging",
        title = "Pack one parcel",
        description = "Packaging tasks",
        helpPhrase = "Ask staff",
        completionPhrase = "Finished parcel"
    )

    @Before
    fun setUp() {
        repository = FakeTaskRepository()
        repository.tasksFlow.value = listOf(testTask)
        viewModel = TaskContentViewModel(repository)
        viewModel.loadTask("task_packaging")
    }

    @Test
    fun openAddStepDialog_initializesEmptyForm() {
        viewModel.openAddStepDialog()
        val state = viewModel.uiState.value

        assertTrue(state.isStepDialogOpen)
        assertNull(state.editingStep)
        assertEquals("", state.stepFormData.instruction)
        assertNull(state.stepFormData.imagePath)
        assertNull(state.stepFormData.instructionError)
    }

    @Test
    fun saveStep_withEmptyInstruction_setsValidationError() {
        viewModel.openAddStepDialog()
        viewModel.onStepInstructionChanged("   ")
        viewModel.saveStep()

        val state = viewModel.uiState.value
        assertEquals("Instruction cannot be empty", state.stepFormData.instructionError)
        assertEquals(0, repository.stepsFlow.value.size)
    }

    @Test
    fun saveStep_createsNewStepWithSequentialNumber() = runTest {
        viewModel.openAddStepDialog()
        viewModel.onStepInstructionChanged("Fold carton box")
        viewModel.onStepPhotoChanged("/data/photos/step1.jpg")

        var successCallbackCalled = false
        viewModel.saveStep(onSuccess = { successCallbackCalled = true })

        assertTrue(successCallbackCalled)
        val state = viewModel.uiState.value
        assertFalse(state.isStepDialogOpen)
        assertEquals(1, state.steps.size)
        assertEquals("Fold carton box", state.steps[0].instruction)
        assertEquals("/data/photos/step1.jpg", state.steps[0].imagePath)
        assertEquals(1, state.steps[0].stepNumber)
    }

    @Test
    fun editStep_updatesInstructionAndPhoto() = runTest {
        val existingStep = TaskStepEntity(
            id = "step_1",
            taskId = "task_packaging",
            stepNumber = 1,
            instruction = "Old instruction",
            imagePath = null
        )
        repository.stepsFlow.value = listOf(existingStep)
        viewModel.loadTask("task_packaging")

        viewModel.openEditStepDialog(existingStep)
        viewModel.onStepInstructionChanged("Updated instruction")
        viewModel.onStepPhotoChanged("/new/path.jpg")
        viewModel.saveStep()

        val updatedList = repository.stepsFlow.value
        assertEquals(1, updatedList.size)
        assertEquals("Updated instruction", updatedList[0].instruction)
        assertEquals("/new/path.jpg", updatedList[0].imagePath)
    }

    @Test
    fun deleteStep_removesStepFromRepository() = runTest {
        val step = TaskStepEntity(
            id = "step_1",
            taskId = "task_packaging",
            stepNumber = 1,
            instruction = "Delete me"
        )
        repository.stepsFlow.value = listOf(step)
        viewModel.loadTask("task_packaging")

        viewModel.requestDeleteStep(step)
        assertEquals(step, viewModel.uiState.value.stepPendingDelete)

        viewModel.confirmDeleteStep()
        assertNull(viewModel.uiState.value.stepPendingDelete)
        assertEquals(0, repository.stepsFlow.value.size)
    }

    @Test
    fun openAddChecklistDialog_initializesEmptyForm() {
        viewModel.openAddChecklistDialog()
        val state = viewModel.uiState.value

        assertTrue(state.isChecklistDialogOpen)
        assertNull(state.editingChecklist)
        assertEquals("", state.checklistFormData.text)
        assertNull(state.checklistFormData.textError)
    }

    @Test
    fun saveChecklistItem_withEmptyText_setsValidationError() {
        viewModel.openAddChecklistDialog()
        viewModel.onChecklistTextChanged("   ")
        viewModel.saveChecklistItem()

        val state = viewModel.uiState.value
        assertEquals("Item text cannot be empty", state.checklistFormData.textError)
        assertEquals(0, repository.checklistFlow.value.size)
    }

    @Test
    fun saveChecklistItem_addsNewItem() = runTest {
        viewModel.openAddChecklistDialog()
        viewModel.onChecklistTextChanged("Verify tape is sealed")

        var saved = false
        viewModel.saveChecklistItem(onSuccess = { saved = true })

        assertTrue(saved)
        val state = viewModel.uiState.value
        assertFalse(state.isChecklistDialogOpen)
        assertEquals(1, state.checklist.size)
        assertEquals("Verify tape is sealed", state.checklist[0].text)
        assertEquals(1, state.checklist[0].order)
    }

    @Test
    fun editChecklistItem_updatesItemText() = runTest {
        val existingItem = ChecklistItemEntity(
            id = 10,
            taskId = "task_packaging",
            order = 1,
            text = "Old checklist text"
        )
        repository.checklistFlow.value = listOf(existingItem)
        viewModel.loadTask("task_packaging")

        viewModel.openEditChecklistDialog(existingItem)
        viewModel.onChecklistTextChanged("Correct updated check")
        viewModel.saveChecklistItem()

        val items = repository.checklistFlow.value
        assertEquals(1, items.size)
        assertEquals("Correct updated check", items[0].text)
    }

    @Test
    fun deleteChecklistItem_removesItem() = runTest {
        val item = ChecklistItemEntity(
            id = 10,
            taskId = "task_packaging",
            order = 1,
            text = "Check items"
        )
        repository.checklistFlow.value = listOf(item)
        viewModel.loadTask("task_packaging")

        viewModel.requestDeleteChecklist(item)
        assertEquals(item, viewModel.uiState.value.checklistPendingDelete)

        viewModel.confirmDeleteChecklist()
        assertNull(viewModel.uiState.value.checklistPendingDelete)
        assertEquals(0, repository.checklistFlow.value.size)
    }
}
