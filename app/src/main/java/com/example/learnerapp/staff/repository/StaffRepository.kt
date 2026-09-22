package com.example.learnerapp.staff.repository

import com.example.learnerapp.staff.model.StaffAuthConfig
import com.example.learnerapp.staff.model.StaffSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository interface for Staff Mode authentication and session state.
 */
interface StaffRepository {
    val session: StateFlow<StaffSession>
    fun validatePin(pin: String): Boolean
    fun authenticate(): Boolean
    fun logout()
}

/**
 * In-memory prototype implementation of StaffRepository.
 * Keeps session strictly ephemeral — resets whenever the application restarts.
 */
class InMemoryStaffRepository(
    private val expectedPin: String = StaffAuthConfig.DEFAULT_STAFF_PIN
) : StaffRepository {

    private val _session = MutableStateFlow(StaffSession(isAuthenticated = false))
    override val session: StateFlow<StaffSession> = _session.asStateFlow()

    override fun validatePin(pin: String): Boolean {
        return pin == expectedPin
    }

    override fun authenticate(): Boolean {
        _session.value = StaffSession(
            isAuthenticated = true,
            authenticatedAt = System.currentTimeMillis()
        )
        return true
    }

    override fun logout() {
        _session.value = StaffSession(isAuthenticated = false, authenticatedAt = null)
    }
}
