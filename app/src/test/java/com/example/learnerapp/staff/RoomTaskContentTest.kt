package com.example.learnerapp.staff

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.DatabaseSeeder
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import com.example.learnerapp.data.repository.RoomLearnerRepository
import com.example.learnerapp.staff.repository.RoomTaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
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
 * Room integration tests verifying Phase 5 capabilities:
 * - Step CRUD, contiguous renumbering, and reordering
 * - Checklist CRUD, contiguous renumbering, and reordering
 * - Deep Task Copy duplicating steps and checklist items with independent IDs
 * - Stable Step Identity & Active Step Deletion Protection (All 3 Cases)
 * - Active learner progress stability during staff edits
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomTaskContentTest {

    private lateinit var database: AppDatabase
    private lateinit var taskRepository: RoomTaskRepository
    private lateinit var learnerRepository: RoomLearnerRepository

    private val taskId = "task_pack_parcel_test"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskRepository = RoomTaskRepository(database)
        learnerRepository = RoomLearnerRepository(database)

        // Seed base task
        runTest {
            taskRepository.createTask(
                TaskEntity(
                    id = taskId,
                    title = "Pack one parcel",
                    description = "Workstation packing",
                    helpPhrase = "Ask staff",
                    completionPhrase = "Parcel packed"
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Step Operations ====================

    @Test
    fun addStep_persistsStepsWithContiguousNumbering() = runTest {
        val step1 = taskRepository.addStep(taskId, "Get empty box", "/photos/box.jpg").getOrThrow()
        val step2 = taskRepository.addStep(taskId, "Place items inside", null).getOrThrow()
        val step3 = taskRepository.addStep(taskId, "Seal with tape", null).getOrThrow()

        assertEquals(1, step1.stepNumber)
        assertEquals(2, step2.stepNumber)
        assertEquals(3, step3.stepNumber)

        val stepsInDb = taskRepository.getSteps(taskId)
        assertEquals(3, stepsInDb.size)
        assertEquals("Get empty box", stepsInDb[0].instruction)
        assertEquals("/photos/box.jpg", stepsInDb[0].imagePath)
        assertEquals("Place items inside", stepsInDb[1].instruction)
        assertEquals("Seal with tape", stepsInDb[2].instruction)
    }

    @Test
    fun moveStep_swapsStepNumbersAtomically() = runTest {
        val s1 = taskRepository.addStep(taskId, "Step 1", null).getOrThrow()
        val s2 = taskRepository.addStep(taskId, "Step 2", null).getOrThrow()
        val s3 = taskRepository.addStep(taskId, "Step 3", null).getOrThrow()

        // Move Step 2 UP -> should swap with Step 1
        taskRepository.moveStep(taskId, s2.id, directionUp = true)

        val steps = taskRepository.getSteps(taskId)
        assertEquals(s2.id, steps[0].id)
        assertEquals(1, steps[0].stepNumber)
        assertEquals(s1.id, steps[1].id)
        assertEquals(2, steps[1].stepNumber)
        assertEquals(s3.id, steps[2].id)
        assertEquals(3, steps[2].stepNumber)
    }

    @Test
    fun deleteStep_renumbersSubsequentStepsContiguously() = runTest {
        val s1 = taskRepository.addStep(taskId, "Step 1", null).getOrThrow()
        val s2 = taskRepository.addStep(taskId, "Step 2", null).getOrThrow()
        val s3 = taskRepository.addStep(taskId, "Step 3", null).getOrThrow()
        val s4 = taskRepository.addStep(taskId, "Step 4", null).getOrThrow()

        // Delete Step 2
        val deleteResult = taskRepository.deleteStep(taskId, s2.id)
        assertTrue(deleteResult.isSuccess)

        val remaining = taskRepository.getSteps(taskId)
        assertEquals(3, remaining.size)
        assertEquals(s1.id, remaining[0].id)
        assertEquals(1, remaining[0].stepNumber)
        assertEquals(s3.id, remaining[1].id)
        assertEquals(2, remaining[1].stepNumber) // was 3, renumbered to 2
        assertEquals(s4.id, remaining[2].id)
        assertEquals(3, remaining[2].stepNumber) // was 4, renumbered to 3
    }

    // ==================== Active Step Deletion Protection (All 3 Cases) ====================

    @Test
    fun activeStepDeletion_case1_learnerOnDeletedStep_preventsDeletion() = runTest {
        val s1 = taskRepository.addStep(taskId, "Step 1", null).getOrThrow()
        val s2 = taskRepository.addStep(taskId, "Step 2", null).getOrThrow()
        val s3 = taskRepository.addStep(taskId, "Step 3", null).getOrThrow()

        // Learner is actively working on Step 2
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "progress_$taskId",
                taskId = taskId,
                scheduleItemId = "sched_1",
                currentStep = 2,
                status = "IN_PROGRESS",
                startedAt = 1000L,
                completedAt = null
            )
        )

        // Attempting to delete Step 2 MUST FAIL
        val result = taskRepository.deleteStep(taskId, s2.id)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()?.message?.contains("currently on this step") == true)

        // Verify Step 2 is STILL in database
        val steps = taskRepository.getSteps(taskId)
        assertEquals(3, steps.size)
        assertNotNull(steps.find { it.id == s2.id })

        // Verify learner progress is UNTOUCHED
        val progress = database.progressDao().getProgressForTask(taskId)
        assertEquals(2, progress?.currentStep)
        assertEquals("IN_PROGRESS", progress?.status)
    }

    @Test
    fun activeStepDeletion_case2_deletedStepBeforeLearner_decrementsCurrentStep() = runTest {
        val s1 = taskRepository.addStep(taskId, "Step 1", null).getOrThrow()
        val s2 = taskRepository.addStep(taskId, "Step 2", null).getOrThrow()
        val s3 = taskRepository.addStep(taskId, "Step 3", null).getOrThrow()
        val s4 = taskRepository.addStep(taskId, "Step 4", null).getOrThrow()

        // Learner is actively working on Step 3 (which is instruction "Step 3")
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "progress_$taskId",
                taskId = taskId,
                scheduleItemId = "sched_1",
                currentStep = 3,
                status = "IN_PROGRESS",
                startedAt = 1000L,
                completedAt = null
            )
        )

        // Delete Step 1 (preceding the learner's current step)
        val result = taskRepository.deleteStep(taskId, s1.id)
        assertTrue(result.isSuccess)

        // Verify steps renumbered: old step 2 is now step 1, old step 3 is now step 2
        val remaining = taskRepository.getSteps(taskId)
        assertEquals(3, remaining.size)
        assertEquals(s2.id, remaining[0].id)
        assertEquals(1, remaining[0].stepNumber)
        assertEquals(s3.id, remaining[1].id)
        assertEquals(2, remaining[1].stepNumber)

        // Verify learner's currentStep decremented from 3 to 2, pointing to identical physical step (s3)
        val progress = database.progressDao().getProgressForTask(taskId)
        assertEquals(2, progress?.currentStep)
        assertEquals("IN_PROGRESS", progress?.status)
    }

    @Test
    fun activeStepDeletion_case3_deletedStepAfterLearner_leavesCurrentStepUnchanged() = runTest {
        val s1 = taskRepository.addStep(taskId, "Step 1", null).getOrThrow()
        val s2 = taskRepository.addStep(taskId, "Step 2", null).getOrThrow()
        val s3 = taskRepository.addStep(taskId, "Step 3", null).getOrThrow()
        val s4 = taskRepository.addStep(taskId, "Step 4", null).getOrThrow()

        // Learner is actively working on Step 2
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "progress_$taskId",
                taskId = taskId,
                scheduleItemId = "sched_1",
                currentStep = 2,
                status = "IN_PROGRESS",
                startedAt = 1000L,
                completedAt = null
            )
        )

        // Delete Step 4 (after the learner's current step)
        val result = taskRepository.deleteStep(taskId, s4.id)
        assertTrue(result.isSuccess)

        // Verify remaining steps
        val remaining = taskRepository.getSteps(taskId)
        assertEquals(3, remaining.size)

        // Verify learner's currentStep remains 2 (UNCHANGED)
        val progress = database.progressDao().getProgressForTask(taskId)
        assertEquals(2, progress?.currentStep)
        assertEquals("IN_PROGRESS", progress?.status)
    }

    // ==================== Checklist Operations ====================

    @Test
    fun checklistOperations_addDeleteAndReorderContiguously() = runTest {
        val item1 = taskRepository.addChecklistItem(taskId, "Items counted").getOrThrow()
        val item2 = taskRepository.addChecklistItem(taskId, "Correct address").getOrThrow()
        val item3 = taskRepository.addChecklistItem(taskId, "Tape secure").getOrThrow()

        assertEquals(1, item1.order)
        assertEquals(2, item2.order)
        assertEquals(3, item3.order)

        // Move Item 2 UP
        taskRepository.moveChecklistItem(taskId, item2.id, directionUp = true)
        val reordered = taskRepository.getChecklist(taskId)
        assertEquals(item2.id, reordered[0].id)
        assertEquals(1, reordered[0].order)
        assertEquals(item1.id, reordered[1].id)
        assertEquals(2, reordered[1].order)

        // Delete item 1 (currently at order 2)
        taskRepository.deleteChecklistItem(taskId, item1.id)
        val remaining = taskRepository.getChecklist(taskId)
        assertEquals(2, remaining.size)
        assertEquals(1, remaining[0].order)
        assertEquals(2, remaining[1].order)
    }

    // ==================== Deep Task Copying ====================

    @Test
    fun copyTask_duplicatesChildStepsAndChecklistItemsWithIndependentIds() = runTest {
        // Create 2 steps and 2 checklist items on original task
        val s1 = taskRepository.addStep(taskId, "Original Step 1", "/photos/1.jpg").getOrThrow()
        val s2 = taskRepository.addStep(taskId, "Original Step 2", null).getOrThrow()
        val c1 = taskRepository.addChecklistItem(taskId, "Original Check 1").getOrThrow()
        val c2 = taskRepository.addChecklistItem(taskId, "Original Check 2").getOrThrow()

        // Deep copy the task
        val copyResult = taskRepository.copyTask(taskId)
        assertTrue(copyResult.isSuccess)
        val copiedTask = copyResult.getOrThrow()

        assertEquals("Pack one parcel — Copy", copiedTask.title)
        assertFalse(copiedTask.id == taskId)

        // Verify copied steps
        val copiedSteps = taskRepository.getSteps(copiedTask.id)
        assertEquals(2, copiedSteps.size)
        assertEquals("Original Step 1", copiedSteps[0].instruction)
        assertEquals("/photos/1.jpg", copiedSteps[0].imagePath)
        assertEquals(1, copiedSteps[0].stepNumber)
        assertFalse(copiedSteps[0].id == s1.id) // Independent ID

        assertEquals("Original Step 2", copiedSteps[1].instruction)
        assertEquals(2, copiedSteps[1].stepNumber)
        assertFalse(copiedSteps[1].id == s2.id)

        // Verify copied checklist items
        val copiedChecklist = taskRepository.getChecklist(copiedTask.id)
        assertEquals(2, copiedChecklist.size)
        assertEquals("Original Check 1", copiedChecklist[0].text)
        assertEquals(1, copiedChecklist[0].order)
        assertFalse(copiedChecklist[0].id == c1.id) // Independent integer ID

        assertEquals("Original Check 2", copiedChecklist[1].text)
        assertEquals(2, copiedChecklist[1].order)
        assertFalse(copiedChecklist[1].id == c2.id)

        // Verify original steps and checklist remain intact
        val origSteps = taskRepository.getSteps(taskId)
        val origChecklist = taskRepository.getChecklist(taskId)
        assertEquals(2, origSteps.size)
        assertEquals(2, origChecklist.size)
    }

    // ==================== Active Learner Progress Stability ====================

    @Test
    fun editingStepInstructionOrChecklistText_doesNotResetLearnerProgress() = runTest {
        val s1 = taskRepository.addStep(taskId, "Original Instruction", null).getOrThrow()
        val c1 = taskRepository.addChecklistItem(taskId, "Original Check").getOrThrow()

        // Establish active learner progress
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "progress_$taskId",
                taskId = taskId,
                scheduleItemId = "sched_1",
                currentStep = 1,
                status = "IN_PROGRESS",
                startedAt = 5000L,
                completedAt = null
            )
        )
        database.progressDao().insertOrUpdateChecklistProgress(
            ChecklistProgressEntity(
                taskId = taskId,
                checklistItemId = c1.id,
                isChecked = true
            )
        )

        // Staff updates step instruction and checklist text
        taskRepository.updateStep(s1.copy(instruction = "Staff modified instruction"))
        taskRepository.updateChecklistItem(c1.copy(text = "Staff modified check text"))

        // Verify definitions updated
        val updatedStep = taskRepository.getSteps(taskId)[0]
        val updatedCheck = taskRepository.getChecklist(taskId)[0]
        assertEquals("Staff modified instruction", updatedStep.instruction)
        assertEquals("Staff modified check text", updatedCheck.text)

        // Verify learner progress is COMPLETELY UNTOUCHED
        val progress = database.progressDao().getProgressForTask(taskId)
        assertEquals("IN_PROGRESS", progress?.status)
        assertEquals(1, progress?.currentStep)
        assertEquals(5000L, progress?.startedAt)

        val checklistProgress = database.progressDao().getChecklistProgress(taskId)
        assertEquals(1, checklistProgress.size)
        assertTrue(checklistProgress[0].isChecked)
    }
}
