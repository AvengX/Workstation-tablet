package com.example.learnerapp.staff.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnerapp.staff.model.StaffAuthConfig
import com.example.learnerapp.staff.repository.InMemoryStaffRepository
import com.example.learnerapp.staff.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for Staff Mode authentication and dashboard session.
 */
data class StaffUiState(
    val enteredPin: String = "",
    val pinLength: Int = StaffAuthConfig.PIN_LENGTH,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val isExitDialogOpen: Boolean = false
)

/**
 * ViewModel managing Staff authentication, PIN validation, and session exit.
 */
class StaffViewModel(
    private val repository: StaffRepository = InMemoryStaffRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StaffUiState(
            enteredPin = "",
            pinLength = StaffAuthConfig.PIN_LENGTH,
            errorMessage = null,
            isAuthenticated = repository.session.value.isAuthenticated,
            isExitDialogOpen = false
        )
    )
    val uiState: StateFlow<StaffUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.session.collect { session ->
                _uiState.update { it.copy(isAuthenticated = session.isAuthenticated) }
            }
        }
    }

    /**
     * Handles entering a numeric digit on the tablet keypad.
     */
    fun onDigitEntered(digit: Char) {
        if (!digit.isDigit()) return
        val currentPin = _uiState.value.enteredPin
        if (currentPin.length < _uiState.value.pinLength) {
            val newPin = currentPin + digit
            _uiState.update { it.copy(enteredPin = newPin, errorMessage = null) }
            if (newPin.length == _uiState.value.pinLength) {
                validatePinInternal(newPin)
            }
        }
    }

    /**
     * Removes the last entered PIN digit.
     */
    fun onBackspace() {
        val currentPin = _uiState.value.enteredPin
        if (currentPin.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    enteredPin = currentPin.dropLast(1),
                    errorMessage = null
                )
            }
        }
    }

    /**
     * Clears all entered digits and error message.
     */
    fun onClearPin() {
        _uiState.update { it.copy(enteredPin = "", errorMessage = null) }
    }

    /**
     * Submits the currently entered PIN.
     */
    fun onSubmitPin() {
        val currentPin = _uiState.value.enteredPin
        validatePinInternal(currentPin)
    }

    private fun validatePinInternal(pin: String) {
        if (pin.isEmpty()) return
        if (repository.validatePin(pin)) {
            repository.authenticate()
            _uiState.update {
                it.copy(
                    enteredPin = "",
                    errorMessage = null,
                    isAuthenticated = true
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    enteredPin = "",
                    errorMessage = "Incorrect PIN. Please try again."
                )
            }
        }
    }

    /**
     * Resets input when canceling back to Learner Mode.
     */
    fun onCancel() {
        _uiState.update { it.copy(enteredPin = "", errorMessage = null) }
    }

    /**
     * Toggles the Exit Staff Mode confirmation dialog.
     */
    fun openExitDialog(open: Boolean) {
        _uiState.update { it.copy(isExitDialogOpen = open) }
    }

    /**
     * Terminates the active Staff session and clears authentication.
     */
    fun logout() {
        repository.logout()
        _uiState.update {
            it.copy(
                enteredPin = "",
                errorMessage = null,
                isAuthenticated = false,
                isExitDialogOpen = false
            )
        }
    }
}
