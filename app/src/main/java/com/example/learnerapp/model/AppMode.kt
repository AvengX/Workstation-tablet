package com.example.learnerapp.model

/**
 * Top-level application mode states for the tablet workstation.
 *
 * - [LEARNER]: Normal learner workstation interface (TODAY -> NOW -> HOW -> CHECK -> DONE -> NEXT).
 * - [STAFF_PIN]: Access gate PIN keypad prompt to enter Staff Mode.
 * - [STAFF_DASHBOARD]: Administrative staff dashboard session.
 */
enum class AppMode {
    LEARNER,
    STAFF_PIN,
    STAFF_DASHBOARD
}
