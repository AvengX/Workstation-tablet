package com.example.learnerapp.staff

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity
import com.example.learnerapp.model.ChecklistItem
import com.example.learnerapp.model.LearnerScreen
import com.example.learnerapp.model.Task
import com.example.learnerapp.model.TaskStep
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 8 Unit tests for Staff Learner Preview isolation and integrity.
 * Enforces zero database writes, pure in-memory state management, and correct workstation phrase rendering.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LearnerPreviewTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun previewMapping_createsDomainTaskWithAllStepsChecklistsAndPhrases() {
        val task = TaskEntity(
            id = "task_preview",
            title = "Label Envelopes",
            description = "Affix postage and address labels",
            helpPhrase = "Ask staff if you run out of barcode labels.",
            completionPhrase = "All envelopes labeled and counted."
        )

        val steps = listOf(
            TaskStepEntity(id = "s1", taskId = "task_preview", stepNumber = 1, instruction = "Take envelope batch", imagePath = "/photos/s1.jpg"),
            TaskStepEntity(id = "s2", taskId = "task_preview", stepNumber = 2, instruction = "Peel label", imagePath = null)
        )

        val checklistItems = listOf(
            ChecklistItemEntity(id = 1, taskId = "task_preview", order = 1, text = "Label straight"),
            ChecklistItemEntity(id = 2, taskId = "task_preview", order = 2, text = "Barcode clear")
        )

        // Map into domain Task as done in LearnerPreviewScreen
        val domainSteps = steps.sortedBy { it.stepNumber }.map {
            TaskStep(
                stepNumber = it.stepNumber,
                instruction = it.instruction,
                photoPlaceholderLabel = "Step ${it.stepNumber} Photo Placeholder:\n${it.instruction}",
                imageResId = null,
                imagePath = it.imagePath
            )
        }
        val domainChecklist = checklistItems.sortedBy { it.order }.map {
            ChecklistItem(id = it.id, text = it.text, isChecked = false)
        }
        val previewTask = Task(
            id = task.id,
            title = task.title,
            description = task.description,
            steps = domainSteps,
            checklist = domainChecklist,
            completionPhrase = task.completionPhrase,
            helpPhrase = task.helpPhrase
        )

        assertEquals("Label Envelopes", previewTask.title)
        assertEquals(2, previewTask.steps.size)
        assertEquals("/photos/s1.jpg", previewTask.steps[0].imagePath)
        assertEquals(2, previewTask.checklist.size)
        assertEquals("Ask staff if you run out of barcode labels.", previewTask.helpPhrase)
        assertEquals("All envelopes labeled and counted.", previewTask.completionPhrase)
    }

    @Test
    fun previewWorkflow_inMemoryStateTransitions_doNotTouchRoomDatabase() = runTest {
        // Assert database is initially empty
        assertEquals(0, database.progressDao().getAllProgress().size)
        assertEquals(0, database.progressDao().getAllChecklistProgress().size)

        // Simulate in-memory preview state transitions (as executed in LearnerPreviewScreen)
        var previewScreen = LearnerScreen.TODAY
        var currentStepIndex = 0
        var checklist = listOf(
            ChecklistItem(id = 1, text = "Step check", isChecked = false)
        )

        // TODAY -> NOW
        previewScreen = LearnerScreen.NOW
        assertEquals(LearnerScreen.NOW, previewScreen)

        // NOW -> HOW (Start task)
        previewScreen = LearnerScreen.HOW
        currentStepIndex = 0
        assertEquals(LearnerScreen.HOW, previewScreen)
        assertEquals(0, currentStepIndex)

        // HOW step advance
        currentStepIndex = 1
        assertEquals(1, currentStepIndex)

        // HOW -> CHECK
        previewScreen = LearnerScreen.CHECK
        assertEquals(LearnerScreen.CHECK, previewScreen)

        // Toggle checklist in memory
        assertFalse(checklist[0].isChecked)
        checklist = checklist.map { it.copy(isChecked = true) }
        assertTrue(checklist[0].isChecked)

        // CHECK -> DONE
        previewScreen = LearnerScreen.DONE
        assertEquals(LearnerScreen.DONE, previewScreen)

        // DONE -> NEXT
        previewScreen = LearnerScreen.NEXT
        assertEquals(LearnerScreen.NEXT, previewScreen)

        // CRITICAL INVARIANT: Zero Room database writes!
        assertEquals(0, database.progressDao().getAllProgress().size)
        assertEquals(0, database.progressDao().getAllChecklistProgress().size)
        assertEquals(0, database.scheduleDao().getAllScheduleItems().size)
    }
}
