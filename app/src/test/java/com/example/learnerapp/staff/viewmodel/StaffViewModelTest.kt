package com.example.learnerapp.staff.viewmodel

import com.example.learnerapp.staff.model.StaffAuthConfig
import com.example.learnerapp.staff.repository.InMemoryStaffRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StaffViewModelTest {

    private lateinit var repository: InMemoryStaffRepository
    private lateinit var viewModel: StaffViewModel

    @Before
    fun setUp() {
        repository = InMemoryStaffRepository()
        viewModel = StaffViewModel(repository)
    }

    @Test
    fun initialState_isUnauthenticated_withEmptyPinAndNoErrors() {
        val state = viewModel.uiState.value
        assertEquals("", state.enteredPin)
        assertEquals(StaffAuthConfig.PIN_LENGTH, state.pinLength)
        assertNull(state.errorMessage)
        assertFalse(state.isAuthenticated)
        assertFalse(state.isExitDialogOpen)
    }

    @Test
    fun onDigitEntered_appendsDigitsUntilMaxLength() {
        viewModel.onDigitEntered('1')
        assertEquals("1", viewModel.uiState.value.enteredPin)

        viewModel.onDigitEntered('2')
        assertEquals("12", viewModel.uiState.value.enteredPin)

        viewModel.onDigitEntered('x') // Non-digit ignored
        assertEquals("12", viewModel.uiState.value.enteredPin)

        viewModel.onDigitEntered('3')
        assertEquals("123", viewModel.uiState.value.enteredPin)
    }

    @Test
    fun onBackspace_removesLastDigit() {
        viewModel.onDigitEntered('1')
        viewModel.onDigitEntered('2')
        assertEquals("12", viewModel.uiState.value.enteredPin)

        viewModel.onBackspace()
        assertEquals("1", viewModel.uiState.value.enteredPin)

        viewModel.onBackspace()
        assertEquals("", viewModel.uiState.value.enteredPin)

        // Backspace on empty string does not fail
        viewModel.onBackspace()
        assertEquals("", viewModel.uiState.value.enteredPin)
    }

    @Test
    fun onClearPin_clearsEnteredDigitsAndError() {
        viewModel.onDigitEntered('9')
        viewModel.onDigitEntered('9')
        viewModel.onDigitEntered('9')
        viewModel.onDigitEntered('9') // Incorrect PIN triggers error

        assertEquals("Incorrect PIN. Please try again.", viewModel.uiState.value.errorMessage)

        viewModel.onClearPin()
        assertEquals("", viewModel.uiState.value.enteredPin)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun enterValidPin_authenticatesSuccessfully() {
        viewModel.onDigitEntered('1')
        viewModel.onDigitEntered('2')
        viewModel.onDigitEntered('3')
        viewModel.onDigitEntered('4') // Triggers auto-validation for "1234"

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertEquals("", state.enteredPin)
        assertNull(state.errorMessage)
        assertTrue(repository.session.value.isAuthenticated)
    }

    @Test
    fun enterInvalidPin_failsAuthentication_andShowsErrorMessage() {
        viewModel.onDigitEntered('9')
        viewModel.onDigitEntered('9')
        viewModel.onDigitEntered('9')
        viewModel.onDigitEntered('9') // Triggers auto-validation for "9999"

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertEquals("", state.enteredPin)
        assertEquals("Incorrect PIN. Please try again.", state.errorMessage)
        assertFalse(repository.session.value.isAuthenticated)
    }

    @Test
    fun enteringNewDigit_clearsPreviousErrorMessage() {
        viewModel.onDigitEntered('0')
        viewModel.onDigitEntered('0')
        viewModel.onDigitEntered('0')
        viewModel.onDigitEntered('0') // Wrong PIN

        assertEquals("Incorrect PIN. Please try again.", viewModel.uiState.value.errorMessage)

        viewModel.onDigitEntered('1')
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("1", viewModel.uiState.value.enteredPin)
    }

    @Test
    fun onCancel_resetsPinAndError() {
        viewModel.onDigitEntered('1')
        viewModel.onDigitEntered('2')
        viewModel.onCancel()

        val state = viewModel.uiState.value
        assertEquals("", state.enteredPin)
        assertNull(state.errorMessage)
    }

    @Test
    fun openExitDialog_controlsDialogVisibility() {
        assertFalse(viewModel.uiState.value.isExitDialogOpen)

        viewModel.openExitDialog(true)
        assertTrue(viewModel.uiState.value.isExitDialogOpen)

        viewModel.openExitDialog(false)
        assertFalse(viewModel.uiState.value.isExitDialogOpen)
    }

    @Test
    fun logout_clearsAuthenticationAndClosesDialog() {
        // First authenticate
        viewModel.onDigitEntered('1')
        viewModel.onDigitEntered('2')
        viewModel.onDigitEntered('3')
        viewModel.onDigitEntered('4')
        assertTrue(viewModel.uiState.value.isAuthenticated)

        viewModel.openExitDialog(true)
        assertTrue(viewModel.uiState.value.isExitDialogOpen)

        viewModel.logout()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertFalse(state.isExitDialogOpen)
        assertFalse(repository.session.value.isAuthenticated)
    }
}
