package com.example.learnerapp.staff.model

/**
 * Prototype credential configuration for development access gate.
 * NOTE: This is for local prototype development only, NOT production-grade security.
 * The authentication mechanism is designed to be fully replaced in a future security phase.
 */
object StaffAuthConfig {
    const val DEFAULT_STAFF_PIN = "1234"
    const val PIN_LENGTH = 4
}
