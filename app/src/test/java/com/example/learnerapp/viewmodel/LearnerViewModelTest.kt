package com.example.learnerapp.viewmodel

import com.example.learnerapp.model.LearnerScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LearnerViewModelTest {

    private lateinit var viewModel: LearnerViewModel

    @Before
    fun setUp() {
        viewModel = LearnerViewModel()
    }

    @Test
    fun initialState_isToday_withCorrectScheduleAndTask() {
        val state = viewModel.uiState.value
        assertEquals(LearnerScreen.TODAY, state.currentScreen)
        assertEquals("Pack one parcel", state.task.title)
        assertEquals(5, state.totalSteps)
        assertEquals(4, state.checklistItems.size)
        assertTrue(state.checklistItems.all { !it.isChecked })
        assertFalse(state.isContinueEnabled)
        assertEquals(3, state.schedule.size)
        assertEquals("NOW", state.schedule[0].timeCategory)
        assertTrue(state.schedule[0].isCurrent)
    }

    @Test
    fun navigateToNow_setsScreenToNow() {
        viewModel.navigateToNow()
        assertEquals(LearnerScreen.NOW, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun startTask_setsScreenToHow_andResetsToStep1() {
        viewModel.startTask()
        val state = viewModel.uiState.value
        assertEquals(LearnerScreen.HOW, state.currentScreen)
        assertEquals(0, state.currentStepIndex)
        assertEquals(1, state.currentStep?.stepNumber)
        assertEquals("Take the assigned item, box and packing materials.", state.currentStep?.instruction)
        assertFalse(state.canGoBack)
        assertFalse(state.isLastStep)
    }

    @Test
    fun howStep1_backIsDisabled_andDoesNotGoBelowZero() {
        viewModel.startTask()
        assertFalse(viewModel.uiState.value.canGoBack)
        viewModel.previousStep()
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun howSteps_advanceSequentiallyFrom1To5() {
        viewModel.startTask()

        // Step 1
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertEquals("Take the assigned item, box and packing materials.", viewModel.uiState.value.currentStep?.instruction)

        // Step 2
        viewModel.nextStep()
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        assertTrue(viewModel.uiState.value.canGoBack)
        assertEquals("Check the item and quantity against the packing instruction.", viewModel.uiState.value.currentStep?.instruction)

        // Step 3
        viewModel.nextStep()
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        assertEquals("Place the item in the box. Add the required protective material.", viewModel.uiState.value.currentStep?.instruction)

        // Step 4
        viewModel.nextStep()
        assertEquals(3, viewModel.uiState.value.currentStepIndex)
        assertEquals("Close and seal the box.", viewModel.uiState.value.currentStep?.instruction)

        // Step 5
        viewModel.nextStep()
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertTrue(viewModel.uiState.value.isLastStep)
        assertEquals("Attach the supplied parcel label in the correct position.", viewModel.uiState.value.currentStep?.instruction)
    }

    @Test
    fun howStep_backReturnsToPreviousStep() {
        viewModel.startTask()
        viewModel.nextStep() // To Step 2 (index 1)
        viewModel.nextStep() // To Step 3 (index 2)
        assertEquals(2, viewModel.uiState.value.currentStepIndex)

        viewModel.previousStep() // Back to Step 2 (index 1)
        assertEquals(1, viewModel.uiState.value.currentStepIndex)

        viewModel.previousStep() // Back to Step 1 (index 0)
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertFalse(viewModel.uiState.value.canGoBack)
    }

    @Test
    fun howStep5_nextStepTransitionsToCheckScreen() {
        viewModel.startTask()
        // Advance to step 5
        repeat(4) { viewModel.nextStep() }
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertTrue(viewModel.uiState.value.isLastStep)

        // On last step, nextStep transitions to CHECK
        viewModel.nextStep()
        assertEquals(LearnerScreen.CHECK, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun checkScreen_continueRequiresAllFourItemsChecked() {
        viewModel.startTask()
        repeat(5) { viewModel.nextStep() }
        assertEquals(LearnerScreen.CHECK, viewModel.uiState.value.currentScreen)
        assertFalse(viewModel.uiState.value.isContinueEnabled)

        // Check first 3 items
        viewModel.toggleChecklistItem(1)
        viewModel.toggleChecklistItem(2)
        viewModel.toggleChecklistItem(3)
        assertFalse(viewModel.uiState.value.isContinueEnabled)

        // Check 4th item -> now enabled!
        viewModel.toggleChecklistItem(4)
        assertTrue(viewModel.uiState.value.isContinueEnabled)

        // Uncheck 2nd item -> becomes disabled again
        viewModel.toggleChecklistItem(2)
        assertFalse(viewModel.uiState.value.isContinueEnabled)

        // Re-check 2nd item -> enabled again
        viewModel.toggleChecklistItem(2)
        assertTrue(viewModel.uiState.value.isContinueEnabled)
    }

    @Test
    fun continueFromCheck_transitionsToDone_onlyWhenValidationPasses() {
        viewModel.startTask()
        repeat(5) { viewModel.nextStep() }

        // Attempt when not enabled
        viewModel.continueFromCheck()
        assertEquals(LearnerScreen.CHECK, viewModel.uiState.value.currentScreen)

        // Check all 4 items
        (1..4).forEach { viewModel.toggleChecklistItem(it) }
        assertTrue(viewModel.uiState.value.isContinueEnabled)

        viewModel.continueFromCheck()
        assertEquals(LearnerScreen.DONE, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun completeTask_fromDone_transitionsToNext() {
        viewModel.navigateTo(LearnerScreen.DONE)
        viewModel.completeTask()
        assertEquals(LearnerScreen.NEXT, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun nextTaskPlaceholderDialog_controlsVisibility() {
        assertFalse(viewModel.uiState.value.isNextTaskPlaceholderDialogOpen)
        viewModel.showNextTaskPlaceholder(true)
        assertTrue(viewModel.uiState.value.isNextTaskPlaceholderDialogOpen)
        viewModel.showNextTaskPlaceholder(false)
        assertFalse(viewModel.uiState.value.isNextTaskPlaceholderDialogOpen)
    }

    @Test
    fun helpAndStaffDialogs_toggleCorrectly() {
        // Help dialog
        assertFalse(viewModel.uiState.value.isHelpDialogOpen)
        viewModel.openHelpDialog(true)
        assertTrue(viewModel.uiState.value.isHelpDialogOpen)
        viewModel.openHelpDialog(false)
        assertFalse(viewModel.uiState.value.isHelpDialogOpen)

        // Staff dialog
        assertFalse(viewModel.uiState.value.isStaffNoticeDialogOpen)
        viewModel.openStaffDialog(true)
        assertTrue(viewModel.uiState.value.isStaffNoticeDialogOpen)
        viewModel.openStaffDialog(false)
        assertFalse(viewModel.uiState.value.isStaffNoticeDialogOpen)
    }
}
