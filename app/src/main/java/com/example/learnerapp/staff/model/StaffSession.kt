package com.example.learnerapp.staff.model

/**
 * Ephemeral, in-memory session representation for Staff Mode.
 * This session is NOT persisted across application restarts.
 */
data class StaffSession(
    val isAuthenticated: Boolean = false,
    val authenticatedAt: Long? = null
)
