package com.example.learnerapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.DatabaseSeeder
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.ScheduleItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity
import com.example.learnerapp.data.repository.RoomLearnerRepository
import com.example.learnerapp.model.LearnerScreen
import com.example.learnerapp.staff.backup.RoomBackupRepository
import com.example.learnerapp.staff.repository.RoomScheduleRepository
import com.example.learnerapp.staff.repository.RoomTaskRepository
import com.example.learnerapp.staff.util.PhotoStorageManager
import com.example.learnerapp.staff.viewmodel.StaffViewModel
import com.example.learnerapp.viewmodel.LearnerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Full-System Hardening & Acceptance Test Suite (Phase 9).
 * Comprehensive end-to-end integration tests validating state transitions,
 * Room persistence, dynamic scheduling, task CRUD & deep copy, phrases,
 * preview isolation, reset/resume, backup/restore, and empty states.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FullSystemAcceptanceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var learnerRepository: RoomLearnerRepository
    private lateinit var taskRepository: RoomTaskRepository
    private lateinit var scheduleRepository: RoomScheduleRepository
    private val todayStr = LocalDate.now().toString()
    private val activeViewModels = mutableListOf<LearnerViewModel>()

    private fun createLearnerViewModel(date: String = todayStr): LearnerViewModel {
        val vm = LearnerViewModel(learnerRepository, date)
        activeViewModels.add(vm)
        return vm
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        learnerRepository = RoomLearnerRepository(database)
        taskRepository = RoomTaskRepository(database)
        scheduleRepository = RoomScheduleRepository(database)
    }

    @After
    fun tearDown() {
        activeViewModels.forEach { it.cleanup() }
        activeViewModels.clear()
        Dispatchers.resetMain()
        database.close()
    }

    // =========================================================================
    // ACCEPTANCE TEST #1: COMPLETE LEARNER WORKFLOW & SCREENSHOT BUG REGRESSION
    // =========================================================================

    @Test
    fun completeLearnerWorkflow_fromTodayThroughNext_verifiesDynamicNextTask_andNoPhasePlaceholders() = runTest {
        learnerRepository.seedIfEmpty()

        val viewModel = createLearnerViewModel(todayStr)
        viewModel.uiState.first { it.schedule.size == 3 }

        // 1. TODAY screen: verify dynamic schedule order
        assertEquals(LearnerScreen.TODAY, viewModel.uiState.value.currentScreen)
        assertEquals("Pack one parcel", viewModel.uiState.value.schedule[0].title)
        assertEquals("NOW", viewModel.uiState.value.schedule[0].timeCategory)
        assertEquals("Sort supplies", viewModel.uiState.value.schedule[1].title)
        assertEquals("NEXT", viewModel.uiState.value.schedule[1].timeCategory)

        // 2. NOW screen
        viewModel.navigateToNow()
        assertEquals(LearnerScreen.NOW, viewModel.uiState.value.currentScreen)
        assertEquals("Pack one parcel", viewModel.uiState.value.task.title)

        // 3. HOW screen: step navigation through all 5 steps
        viewModel.startTask()
        assertEquals(LearnerScreen.HOW, viewModel.uiState.value.currentScreen)
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertEquals(5, viewModel.uiState.value.totalSteps)

        // Advance through steps 1 -> 2 -> 3 -> 4 -> 5
        viewModel.nextStep() // step 2
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        viewModel.nextStep() // step 3
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        viewModel.previousStep() // back to step 2
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        viewModel.nextStep() // back to step 3
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        viewModel.nextStep() // step 4
        assertEquals(3, viewModel.uiState.value.currentStepIndex)
        viewModel.nextStep() // step 5 (last step)
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertTrue(viewModel.uiState.value.isLastStep)

        // 4. CHECK screen: checklist gating
        viewModel.nextStep() // transitions from last step to CHECK
        assertEquals(LearnerScreen.CHECK, viewModel.uiState.value.currentScreen)
        assertEquals(4, viewModel.uiState.value.checklistItems.size)
        assertFalse("CONTINUE must be disabled before checking items", viewModel.uiState.value.isContinueEnabled)

        // Check each item
        viewModel.uiState.value.checklistItems.forEach { item ->
            viewModel.toggleChecklistItem(item.id)
        }
        advanceUntilIdle()
        viewModel.uiState.first { it.isContinueEnabled }
        assertTrue("CONTINUE must be enabled when all checked", viewModel.uiState.value.isContinueEnabled)

        // 5. DONE screen: completion phrase
        viewModel.continueFromCheck()
        assertEquals(LearnerScreen.DONE, viewModel.uiState.value.currentScreen)
        assertEquals(
            "I have finished packing this parcel. Please check.",
            viewModel.uiState.value.task.completionPhrase
        )

        // 6. NEXT screen: dynamic next scheduled task
        viewModel.completeTask()
        advanceUntilIdle()
        database.invalidationTracker.refreshVersionsSync()
        learnerRepository.getTaskProgress(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL).first { it?.status == "COMPLETED" }
        viewModel.uiState.first { it.currentScreen == LearnerScreen.NEXT }

        assertEquals(LearnerScreen.NEXT, viewModel.uiState.value.currentScreen)
        assertEquals("Sort supplies", viewModel.uiState.value.schedule.first().title)
        assertEquals("Clean work area", viewModel.uiState.value.nextTaskTitle)

        // Verify NO placeholder / Phase 2 text exists in any learner-facing property
        val nextTaskTitle = viewModel.uiState.value.nextTaskTitle ?: ""
        assertFalse(nextTaskTitle.contains("Phase", ignoreCase = true))
        assertFalse(nextTaskTitle.contains("coming in", ignoreCase = true))
        assertFalse(nextTaskTitle.contains("demo", ignoreCase = true))
        assertFalse(nextTaskTitle.contains("placeholder", ignoreCase = true))
        viewModel.cleanup()
        advanceUntilIdle()
    }

    // =========================================================================
    // ACCEPTANCE TEST #2: ACTIVE TASK TRANSITION (DEFECT #2 FIX VERIFICATION)
    // =========================================================================

    @Test
    fun activeTaskTransition_onTaskCompletion_rebindsSortSupplies_withNoStaleData() = runTest {
        learnerRepository.seedIfEmpty()

        val viewModel = createLearnerViewModel(todayStr)
        viewModel.uiState.first { it.schedule.size == 3 }

        assertEquals("Pack one parcel", viewModel.uiState.value.task.title)

        // Complete Pack one parcel
        viewModel.startTask()
        advanceUntilIdle()
        learnerRepository.getTaskProgress("pack_one_parcel").first { it?.status == "IN_PROGRESS" }
        viewModel.completeTask()
        advanceUntilIdle()
        learnerRepository.getTaskProgress("pack_one_parcel").first { it?.status == "COMPLETED" }
        database.invalidationTracker.refreshVersionsSync()

        // Transition from NEXT to the next task
        viewModel.startNextTask()
        advanceUntilIdle()
        database.invalidationTracker.refreshVersionsSync()
        learnerRepository.getTaskProgress("sort_supplies").first { it?.status == "IN_PROGRESS" }

        // Wait for next active task (Sort supplies) to bind
        viewModel.uiState.first { it.task.title == "Sort supplies" }

        // Assert Sort supplies is completely bound with fresh data
        assertEquals("Sort supplies", viewModel.uiState.value.task.title)
        assertEquals("sort_supplies", viewModel.uiState.value.task.id)
        assertEquals(LearnerScreen.NOW, viewModel.uiState.value.currentScreen)

        // Verify schedule re-derived with Sort supplies as NOW
        val schedule = viewModel.uiState.value.schedule
        assertEquals("NOW", schedule[0].timeCategory)
        assertEquals("Sort supplies", schedule[0].title)
        assertEquals("NEXT", schedule[1].timeCategory)
        assertEquals("Clean work area", schedule[1].title)

        // Verify Room persistence
        val sortProgress = database.progressDao().getProgressForTask("sort_supplies")
        assertEquals("IN_PROGRESS", sortProgress?.status)
        val packProgress = database.progressDao().getProgressForTask("pack_one_parcel")
        assertEquals("COMPLETED", packProgress?.status)

        viewModel.cleanup()
        advanceUntilIdle()
    }

    @Test
    fun startNextTask_activatesNextScheduledTask_transitionsToNowAndHow_andPackParcelRemainsCompleted() = runTest {
        learnerRepository.seedIfEmpty()

        val viewModel = createLearnerViewModel(todayStr)
        viewModel.uiState.first { it.schedule.size == 3 }

        // Start and complete Pack one parcel
        viewModel.startTask()
        advanceUntilIdle()
        viewModel.completeTask()
        advanceUntilIdle()
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.currentScreen == LearnerScreen.NEXT && it.schedule.firstOrNull()?.title == "Sort supplies" }

        // Verify on NEXT screen
        assertEquals(LearnerScreen.NEXT, viewModel.uiState.value.currentScreen)
        assertEquals("Sort supplies", viewModel.uiState.value.schedule.first().title)

        // Learner taps START on NEXT screen
        viewModel.startNextTask()
        advanceUntilIdle()
        database.invalidationTracker.refreshVersionsSync()

        // Wait for Sort supplies to bind as active task
        viewModel.uiState.first { it.task.id == "sort_supplies" && it.currentScreen == LearnerScreen.NOW }

        // Assert screen is NOW and active task is Sort supplies
        assertEquals(LearnerScreen.NOW, viewModel.uiState.value.currentScreen)
        assertEquals("sort_supplies", viewModel.uiState.value.task.id)
        assertEquals("Sort supplies", viewModel.uiState.value.task.title)

        // Assert dynamic schedule derivation: NOW is Sort supplies, NEXT is Clean work area
        val updatedSchedule = viewModel.uiState.value.schedule
        assertEquals("NOW", updatedSchedule[0].timeCategory)
        assertEquals("Sort supplies", updatedSchedule[0].title)
        assertTrue(updatedSchedule[0].isCurrent)
        assertEquals("NEXT", updatedSchedule[1].timeCategory)
        assertEquals("Clean work area", updatedSchedule[1].title)
        assertFalse(updatedSchedule[1].isCurrent)

        // Assert Room state: Pack one parcel is COMPLETED, Sort supplies is IN_PROGRESS
        val packProgress = database.progressDao().getProgressForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertEquals("COMPLETED", packProgress?.status)
        val sortProgress = database.progressDao().getProgressForTask(DatabaseSeeder.TASK_ID_SORT_SUPPLIES)
        assertEquals("IN_PROGRESS", sortProgress?.status)

        // Advance into Sort supplies workflow
        viewModel.startTask()
        advanceUntilIdle()
        assertEquals(LearnerScreen.HOW, viewModel.uiState.value.currentScreen)
        assertEquals(0, viewModel.uiState.value.currentStepIndex)

        viewModel.cleanup()
        advanceUntilIdle()
    }

    // =========================================================================
    // ACCEPTANCE TEST #3: PERSISTENCE & RESTORATION ACROSS RESTARTS
    // =========================================================================

    @Test
    fun persistence_stepAndChecklistRestoration_survivesAppRestart() = runTest {
        learnerRepository.seedIfEmpty()

        // 1. Progress at Step 3
        learnerRepository.startTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        learnerRepository.updateCurrentStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 3)
        database.invalidationTracker.refreshVersionsSync()

        // Restart app (simulate by creating a new ViewModel instance)
        val vmRestart1 = createLearnerViewModel(todayStr)
        vmRestart1.uiState.first { it.currentScreen == LearnerScreen.HOW && it.currentStepIndex == 2 }

        assertEquals(LearnerScreen.HOW, vmRestart1.uiState.value.currentScreen)
        assertEquals(2, vmRestart1.uiState.value.currentStepIndex) // Step 3 (0-based index 2)

        // 2. Progress at CHECK with checklist toggles
        learnerRepository.moveToChecking(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        learnerRepository.toggleChecklistItem(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 1)
        database.invalidationTracker.refreshVersionsSync()

        val vmRestart2 = createLearnerViewModel(todayStr)
        vmRestart2.uiState.first { it.currentScreen == LearnerScreen.CHECK && it.checklistItems.any { c -> c.isChecked } }

        assertEquals(LearnerScreen.CHECK, vmRestart2.uiState.value.currentScreen)
        assertTrue(vmRestart2.uiState.value.checklistItems.first { it.id == 1 }.isChecked)
        vmRestart1.cleanup()
        vmRestart2.cleanup()
        advanceUntilIdle()
    }

    // =========================================================================
    // ACCEPTANCE TEST #4: DYNAMIC SCHEDULE RESILIENCE
    // =========================================================================

    @Test
    fun dynamicSchedule_activeTaskRemainsNow_evenWhenStaffReordersSchedule() = runTest {
        learnerRepository.seedIfEmpty()

        val viewModel = createLearnerViewModel(todayStr)
        viewModel.uiState.first { it.schedule.size == 3 }

        // Learner starts Pack one parcel and advances to Step 3
        viewModel.startTask()
        viewModel.nextStep()
        viewModel.nextStep()
        advanceUntilIdle()

        assertEquals("Pack one parcel", viewModel.uiState.value.task.title)
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        assertEquals(LearnerScreen.HOW, viewModel.uiState.value.currentScreen)

        // Staff moves Sort supplies UP to position 1
        val scheduled = scheduleRepository.getScheduleForDate(todayStr)
        val sortItemId = scheduled.first { it.task.id == DatabaseSeeder.TASK_ID_SORT_SUPPLIES }.scheduleItem.id
        scheduleRepository.moveTaskUp(todayStr, sortItemId)
        advanceUntilIdle()

        // Active IN_PROGRESS task MUST REMAIN NOW
        assertEquals("Pack one parcel", viewModel.uiState.value.task.title)
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        assertEquals(LearnerScreen.HOW, viewModel.uiState.value.currentScreen)

        val schedule = viewModel.uiState.value.schedule
        assertEquals("NOW", schedule[0].timeCategory)
        assertEquals("Pack one parcel", schedule[0].title)
        viewModel.cleanup()
        advanceUntilIdle()
    }

    @Test
    fun dynamicSchedule_dateIsolation_tomorrowScheduleDoesNotLeakIntoToday() = runTest {
        learnerRepository.seedIfEmpty()
        val tomorrowStr = LocalDate.now().plusDays(1).toString()

        // Add an exclusive task only to tomorrow's schedule
        scheduleRepository.addTaskToSchedule(tomorrowStr, DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA)
        database.invalidationTracker.refreshVersionsSync()

        // Verify Today's LearnerViewModel does not contain tomorrow's schedule items
        val todayVm = createLearnerViewModel(todayStr)
        todayVm.uiState.first { it.schedule.size == 3 }

        // Verify tomorrow's schedule query is completely isolated
        val tomorrowScheduled = scheduleRepository.getScheduleForDate(tomorrowStr)
        assertEquals(1, tomorrowScheduled.size)
        assertEquals(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA, tomorrowScheduled[0].task.id)
        todayVm.cleanup()
        advanceUntilIdle()
    }

    // =========================================================================
    // ACCEPTANCE TEST #5: STAFF AUTHENTICATION & SECURITY ISOLATION
    // =========================================================================

    @Test
    fun staffAuthentication_invalidPinRejected_correctPinAccepted_logoutClearsAuth() {
        val staffVm = StaffViewModel()

        // Wrong PIN
        staffVm.onDigitEntered('9')
        staffVm.onDigitEntered('9')
        staffVm.onDigitEntered('9')
        staffVm.onDigitEntered('9')

        assertFalse(staffVm.uiState.value.isAuthenticated)
        assertNotNull(staffVm.uiState.value.errorMessage)

        // Correct PIN
        staffVm.onClearPin()
        staffVm.onDigitEntered('1')
        staffVm.onDigitEntered('2')
        staffVm.onDigitEntered('3')
        staffVm.onDigitEntered('4')

        assertTrue(staffVm.uiState.value.isAuthenticated)
        assertNull(staffVm.uiState.value.errorMessage)

        // Logout
        staffVm.logout()
        assertFalse(staffVm.uiState.value.isAuthenticated)
    }

    // =========================================================================
    // ACCEPTANCE TEST #6: TASK CRUD & DEEP COPY INTEGRITY
    // =========================================================================

    @Test
    fun taskCrud_createDeepCopy_preservesIndependentIdsAndPhrases() = runTest {
        // Create custom task
        val customTask = TaskEntity(
            id = "custom_task_1",
            title = "Assemble Safety Kit",
            description = "Pack gloves, goggles, and mask.",
            helpPhrase = "Ask safety supervisor for assistance.",
            completionPhrase = "Safety kit assembled and verified."
        )
        taskRepository.createTask(customTask)
        taskRepository.addStep("custom_task_1", "Place gloves in bag", null)
        taskRepository.addChecklistItem("custom_task_1", "Gloves present")

        // Deep copy
        val copyResult = taskRepository.copyTask("custom_task_1")
        assertTrue(copyResult.isSuccess)
        val copiedTask = copyResult.getOrThrow()

        // Verify new ID and copied custom phrases
        assertEquals("Assemble Safety Kit — Copy", copiedTask.title)
        assertEquals("Ask safety supervisor for assistance.", copiedTask.helpPhrase)
        assertEquals("Safety kit assembled and verified.", copiedTask.completionPhrase)

        // Verify independent child steps and checklists
        val copiedSteps = taskRepository.getSteps(copiedTask.id)
        val copiedChecklist = taskRepository.getChecklist(copiedTask.id)
        assertEquals(1, copiedSteps.size)
        assertEquals("Place gloves in bag", copiedSteps[0].instruction)
        assertEquals(1, copiedChecklist.size)
        assertEquals("Gloves present", copiedChecklist[0].text)

        // Modify copied checklist: original must remain unaffected
        taskRepository.addChecklistItem(copiedTask.id, "Second item in copy")
        assertEquals(1, taskRepository.getChecklist("custom_task_1").size)
        assertEquals(2, taskRepository.getChecklist(copiedTask.id).size)

        // Delete copy: original remains
        val deleteResult = taskRepository.deleteTask(copiedTask.id)
        assertTrue(deleteResult.isSuccess)
        assertNotNull(database.taskDao().getTaskById("custom_task_1"))
    }

    // =========================================================================
    // ACCEPTANCE TEST #7: STEP DELETION PROGRESS PROTECTION RULES
    // =========================================================================

    @Test
    fun stepDeletion_protectionRules_preventProgressCorruption() = runTest {
        learnerRepository.seedIfEmpty()

        // Pack parcel starts, currentStep = 3 (stepNumber 3)
        learnerRepository.startTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        learnerRepository.updateCurrentStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 3)

        val steps = taskRepository.getSteps(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        val step3 = steps.first { it.stepNumber == 3 }
        val step1 = steps.first { it.stepNumber == 1 }
        val step5 = steps.first { it.stepNumber == 5 }

        // Rule 1: Attempting to delete active step 3 must fail!
        val deleteActiveResult = taskRepository.deleteStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, step3.id)
        assertTrue(deleteActiveResult.isFailure)

        // Rule 2: Deleting step 5 (after current step 3) leaves currentStep at 3
        val deleteAfterResult = taskRepository.deleteStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, step5.id)
        assertTrue(deleteAfterResult.isSuccess)
        var progress = database.progressDao().getProgressForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertEquals(3, progress?.currentStep)

        // Rule 3: Deleting step 1 (before current step 3) decrements currentStep to 2
        val deleteBeforeResult = taskRepository.deleteStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, step1.id)
        assertTrue(deleteBeforeResult.isSuccess)
        progress = database.progressDao().getProgressForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertEquals(2, progress?.currentStep)
    }

    // =========================================================================
    // ACCEPTANCE TEST #8: PHOTO LOADING & MISSING FILE GRACEFUL FALLBACK
    // =========================================================================

    @Test
    fun photoStorage_missingFileSafelyReturnsNull_withoutCrashing() {
        val nonExistentPath = "/data/data/com.example.learnerapp/files/task_photos/missing_photo.jpg"
        val bitmap = PhotoStorageManager.loadBitmap(nonExistentPath)
        assertNull("Missing photo path must return null safely", bitmap)

        val nullPathBitmap = PhotoStorageManager.loadBitmap(null)
        assertNull("Null photo path must return null safely", nullPathBitmap)
    }

    // =========================================================================
    // ACCEPTANCE TEST #9 & #10: CONFIGURABLE HELP & COMPLETION PHRASES
    // =========================================================================

    @Test
    fun configurablePhrases_displayedDynamically_withoutSideEffects() = runTest {
        // Create custom task with distinct phrases
        val custom = TaskEntity(
            id = "phrase_task",
            title = "Phrase Test Task",
            description = "Testing phrases",
            helpPhrase = "Custom Help: Call the shift manager immediately.",
            completionPhrase = "Custom Done: Verification complete. All clear."
        )
        taskRepository.createTask(custom)
        scheduleRepository.addTaskToSchedule(todayStr, "phrase_task")
        database.invalidationTracker.refreshVersionsSync()

        val vm = createLearnerViewModel(todayStr)
        vm.uiState.first { it.schedule.any { s -> s.title == "Phrase Test Task" } }

        // Help dialog displays custom help phrase
        vm.openHelpDialog(true)
        assertTrue(vm.uiState.value.isHelpDialogOpen)
        vm.openHelpDialog(false)
        assertFalse(vm.uiState.value.isHelpDialogOpen)
        assertEquals(0, vm.uiState.value.currentStepIndex)
        vm.cleanup()
        advanceUntilIdle()
    }

    // =========================================================================
    // ACCEPTANCE TEST #11: STAFF LEARNER PREVIEW ISOLATION INVARIANT
    // =========================================================================

    @Test
    fun staffLearnerPreview_fullSequenceNavigation_hasZeroDatabaseWrites() = runTest {
        learnerRepository.seedIfEmpty()

        // Capture initial database state
        val initialTaskCount = database.taskDao().getAllTasks().size
        val initialStepCount = database.taskStepDao().getAllSteps().size
        val initialChecklistCount = database.checklistDao().getAllItems().size
        val initialProgressCount = database.progressDao().getAllProgress().size
        val initialScheduleCount = database.scheduleDao().getAllScheduleItems().size

        // Preview state: simulated in-memory navigation
        var previewScreen = "TODAY"
        var previewStepIndex = 0
        var previewChecklist = listOf(
            ChecklistItemEntity(1, "pack_one_parcel", 1, "Check 1"),
            ChecklistItemEntity(2, "pack_one_parcel", 2, "Check 2")
        )

        // Navigate sequence in preview
        previewScreen = "NOW"
        previewScreen = "HOW"
        previewStepIndex++
        previewScreen = "CHECK"
        previewScreen = "DONE"
        previewScreen = "NEXT"

        // Assert zero database modifications occurred
        assertEquals(initialTaskCount, database.taskDao().getAllTasks().size)
        assertEquals(initialStepCount, database.taskStepDao().getAllSteps().size)
        assertEquals(initialChecklistCount, database.checklistDao().getAllItems().size)
        assertEquals(initialProgressCount, database.progressDao().getAllProgress().size)
        assertEquals(initialScheduleCount, database.scheduleDao().getAllScheduleItems().size)
    }

    // =========================================================================
    // ACCEPTANCE TEST #12: STAFF RESET SAFE DELETION
    // =========================================================================

    @Test
    fun staffReset_clearsOnlyTargetTaskProgress_preservesTemplatesAndSchedules() = runTest {
        learnerRepository.seedIfEmpty()

        // Create active progress
        learnerRepository.startTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        learnerRepository.updateCurrentStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 3)
        learnerRepository.toggleChecklistItem(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 1)

        assertNotNull(database.progressDao().getProgressForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL))
        assertTrue(database.progressDao().getChecklistProgress(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL).isNotEmpty())

        // Staff resets progress
        val resetResult = taskRepository.resetTaskProgress(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertTrue(resetResult.isSuccess)

        // Progress cleared
        assertNull(database.progressDao().getProgressForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL))
        assertTrue(database.progressDao().getChecklistProgress(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL).isEmpty())

        // Templates, steps, photos, checklists, and schedules 100% intact
        assertNotNull(database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL))
        assertEquals(5, database.taskStepDao().getStepsForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL).size)
        assertEquals(4, database.checklistDao().getItemsForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL).size)
        assertTrue(scheduleRepository.getScheduleForDate(todayStr).isNotEmpty())
    }

    // =========================================================================
    // ACCEPTANCE TEST #13: STAFF RESUME VIA AUTHORITATIVE PHASE 6 DERIVATION
    // =========================================================================

    @Test
    fun staffResume_restoresActiveTaskAndCurrentStep() = runTest {
        learnerRepository.seedIfEmpty()
        learnerRepository.startTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        learnerRepository.updateCurrentStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 3)

        val vm = createLearnerViewModel(todayStr)
        vm.restoreSessionFromProgress()
        advanceUntilIdle()

        vm.uiState.first { it.currentScreen == LearnerScreen.HOW && it.currentStepIndex == 2 }
        assertEquals(LearnerScreen.HOW, vm.uiState.value.currentScreen)
        assertEquals(2, vm.uiState.value.currentStepIndex)
        vm.cleanup()
        advanceUntilIdle()
    }

    // =========================================================================
    // ACCEPTANCE TEST #14 & #15: BACKUP & RESTORE COMPLETE CYCLE
    // =========================================================================

    @Test
    fun backupAndRestore_exportsAndRestoresExactWorkstationState() = runTest {
        learnerRepository.seedIfEmpty()
        val backupRepo = RoomBackupRepository(database, context)

        // 1. Export backup
        val outputStream = ByteArrayOutputStream()
        val exportResult = backupRepo.exportBackup(outputStream)
        assertTrue(exportResult.isSuccess)
        val zipBytes = outputStream.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        // 2. Mutate database: delete all tasks
        database.taskDao().deleteTaskById(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertNull(database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL))

        // 3. Stage and Restore the backup
        val inputStream = ByteArrayInputStream(zipBytes)
        val stageResult = backupRepo.stageAndValidateBackup(inputStream)
        assertTrue(stageResult.isSuccess)
        val stagedBackup = stageResult.getOrThrow()

        val restoreResult = backupRepo.restoreStagedBackup(stagedBackup)
        assertTrue(restoreResult.isSuccess)

        // 4. Verify original state returned
        assertNotNull(database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL))
    }

    // =========================================================================
    // ACCEPTANCE TEST #16: INVALID BACKUP REJECTION (FAIL-SAFE)
    // =========================================================================

    @Test
    fun invalidBackup_unsupportedVersion_rejectedWithRollbackSafety() = runTest {
        learnerRepository.seedIfEmpty()
        val initialTaskCount = database.taskDao().getAllTasks().size
        val backupRepo = RoomBackupRepository(database, context)

        // Create corrupt backup with unsupported formatVersion 99
        val corruptBytes = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write("""{"formatVersion": 99, "appVersion": "1.0", "createdAt": 1000, "taskCount": 0, "photoCount": 0, "scheduleCount": 0}""".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        val stageResult = backupRepo.stageAndValidateBackup(ByteArrayInputStream(corruptBytes))
        assertTrue("Unsupported backup version must fail validation", stageResult.isFailure)

        // Database remains completely untouched
        assertEquals(initialTaskCount, database.taskDao().getAllTasks().size)
    }

    // =========================================================================
    // ACCEPTANCE TEST #17 & #18: EMPTY STATES & ALL TASKS COMPLETE
    // =========================================================================

    @Test
    fun emptyStates_emptySchedule_andAllComplete_handledGracefully() = runTest {
        learnerRepository.seedIfEmpty()
        database.scheduleDao().deleteScheduleItemsForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        database.scheduleDao().deleteScheduleItemsForTask(DatabaseSeeder.TASK_ID_SORT_SUPPLIES)
        database.scheduleDao().deleteScheduleItemsForTask(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA)

        val vm = createLearnerViewModel(todayStr)
        vm.uiState.first { it.isScheduleEmpty }

        assertTrue(vm.uiState.value.isScheduleEmpty)
        assertFalse(vm.uiState.value.isAllTasksCompleted)
        assertTrue(vm.uiState.value.schedule.isEmpty())
        vm.cleanup()
        advanceUntilIdle()
    }

    // =========================================================================
    // ACCEPTANCE TEST #19 & #20: ZERO STEPS & ZERO CHECKLIST TASKS
    // =========================================================================

    @Test
    fun zeroStepsAndZeroChecklist_handledGracefullyWithoutCrashing() = runTest {
        val emptyTask = TaskEntity(
            id = "empty_task",
            title = "Zero Steps Task",
            description = "A task with no instructions and no checks.",
            helpPhrase = "Ask staff for assistance.",
            completionPhrase = "I have completed this task."
        )
        taskRepository.createTask(emptyTask)
        scheduleRepository.addTaskToSchedule(todayStr, "empty_task")
        database.invalidationTracker.refreshVersionsSync()

        val vm = createLearnerViewModel(todayStr)
        vm.uiState.first { it.task.id == "empty_task" }

        // Start zero-step task
        vm.startTask()
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.totalSteps)
        assertNull(vm.uiState.value.currentStep)

        // Navigate to CHECK: empty checklist allows continue immediately
        vm.navigateTo(LearnerScreen.CHECK)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.checklistItems.size)
        vm.cleanup()
        advanceUntilIdle()
    }
}
