package com.example.learnerapp.staff

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.DatabaseSeeder
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity
import com.example.learnerapp.data.repository.RoomLearnerRepository
import com.example.learnerapp.model.LearnerScreen
import com.example.learnerapp.staff.repository.RoomScheduleRepository
import com.example.learnerapp.staff.repository.RoomTaskRepository
import com.example.learnerapp.staff.viewmodel.FakeTaskRepository
import com.example.learnerapp.staff.viewmodel.TaskManagementViewModel
import com.example.learnerapp.viewmodel.LearnerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Phase 8 Unit tests for Staff Reset and Resume controls.
 * Validates progress isolation, template protection, and dynamic learner workstation resumption.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskResetResumeTest {

    private lateinit var database: AppDatabase
    private lateinit var taskRepository: RoomTaskRepository
    private lateinit var learnerRepository: RoomLearnerRepository
    private lateinit var scheduleRepository: RoomScheduleRepository
    private val todayStr = LocalDate.now().toString()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskRepository = RoomTaskRepository(database)
        learnerRepository = RoomLearnerRepository(database)
        scheduleRepository = RoomScheduleRepository(database)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun resetTaskProgress_deletesOnlySelectedTaskProgress_leavingTemplatesAndOthersIntact() = runTest {
        learnerRepository.seedIfEmpty()

        val task1Id = DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL
        val task2Id = DatabaseSeeder.TASK_ID_SORT_SUPPLIES

        // Seed active progress for Task 1: Step 3, checking 2 checklist items
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "prog_1",
                taskId = task1Id,
                scheduleItemId = "sched_1",
                currentStep = 3,
                status = "IN_PROGRESS",
                startedAt = 1000L,
                completedAt = null
            )
        )
        database.progressDao().insertAllChecklistProgress(
            listOf(
                ChecklistProgressEntity(taskId = task1Id, checklistItemId = 1, isChecked = true),
                ChecklistProgressEntity(taskId = task1Id, checklistItemId = 2, isChecked = true)
            )
        )

        // Seed active progress for Task 2: Step 2, checking 1 checklist item
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "prog_2",
                taskId = task2Id,
                scheduleItemId = "sched_2",
                currentStep = 2,
                status = "IN_PROGRESS",
                startedAt = 2000L,
                completedAt = null
            )
        )
        database.progressDao().insertAllChecklistProgress(
            listOf(
                ChecklistProgressEntity(taskId = task2Id, checklistItemId = 10, isChecked = true)
            )
        )

        // Execute RESET for Task 1 ONLY
        val resetResult = taskRepository.resetTaskProgress(task1Id)
        assertTrue(resetResult.isSuccess)

        // Verify Task 1 progress is completely wiped
        assertNull(database.progressDao().getProgressForTask(task1Id))
        assertTrue(database.progressDao().getChecklistProgress(task1Id).isEmpty())

        // Verify Task 1 definition, steps, and checklist items remain 100% INTACT
        assertNotNull(database.taskDao().getTaskById(task1Id))
        val steps = database.taskStepDao().getStepsForTask(task1Id)
        assertEquals(5, steps.size)
        val checklist = database.checklistDao().getItemsForTask(task1Id)
        assertEquals(4, checklist.size)

        // Verify Task 2 progress remains 100% INTACT and unaffected
        val task2Progress = database.progressDao().getProgressForTask(task2Id)
        assertNotNull(task2Progress)
        assertEquals("IN_PROGRESS", task2Progress?.status)
        assertEquals(2, task2Progress?.currentStep)
        val task2ChecklistProgress = database.progressDao().getChecklistProgress(task2Id)
        assertEquals(1, task2ChecklistProgress.size)
        assertTrue(task2ChecklistProgress[0].isChecked)
    }

    @Test
    fun taskManagementViewModel_requestAndConfirmReset_executesSuccessfully() = runTest {
        val fakeRepo = FakeTaskRepository()
        val task = TaskEntity(
            id = "task_box",
            title = "Fold Carton",
            description = "",
            helpPhrase = "",
            completionPhrase = ""
        )
        fakeRepo.tasksFlow.value = listOf(task)
        fakeRepo.progressFlow.value = listOf(
            TaskProgressEntity(
                id = "p1",
                taskId = "task_box",
                scheduleItemId = "s1",
                currentStep = 2,
                status = "IN_PROGRESS",
                startedAt = 100L,
                completedAt = null
            )
        )

        val viewModel = TaskManagementViewModel(fakeRepo)

        // Request reset
        viewModel.requestResetTask(task)
        assertEquals(task, viewModel.uiState.value.taskPendingReset)

        // Cancel reset
        viewModel.cancelResetTask()
        assertNull(viewModel.uiState.value.taskPendingReset)

        // Request reset again and confirm
        viewModel.requestResetTask(task)
        var resetSuccess = false
        viewModel.confirmResetTask(onSuccess = { resetSuccess = true })

        assertTrue(resetSuccess)
        assertNull(viewModel.uiState.value.taskPendingReset)
        assertEquals("task_box", fakeRepo.resetTaskCalledWith)
        assertNotNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun taskManagementViewModel_observesTaskProgressMap() = runTest {
        val fakeRepo = FakeTaskRepository()
        val task = TaskEntity(id = "task_1", title = "Task 1", description = "", helpPhrase = "", completionPhrase = "")
        fakeRepo.tasksFlow.value = listOf(task)
        fakeRepo.progressFlow.value = listOf(
            TaskProgressEntity(
                id = "p1",
                taskId = "task_1",
                scheduleItemId = "s1",
                currentStep = 4,
                status = "IN_PROGRESS",
                startedAt = 100L,
                completedAt = null
            )
        )

        val viewModel = TaskManagementViewModel(fakeRepo)
        viewModel.loadTasks()

        val progressInfo = viewModel.uiState.value.taskProgressMap["task_1"]
        assertNotNull(progressInfo)
        assertEquals("IN_PROGRESS", progressInfo?.status)
        assertEquals(4, progressInfo?.currentStep)
    }

    @Test
    fun restoreSessionFromProgress_resumesActiveTaskAtCurrentStep() = runTest {
        learnerRepository.seedIfEmpty()

        // Set Pack parcel to IN_PROGRESS at step 4
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "p_resume",
                taskId = DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL,
                scheduleItemId = "schedule_now",
                currentStep = 4,
                status = "IN_PROGRESS",
                startedAt = 500L,
                completedAt = null
            )
        )

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        database.invalidationTracker.refreshVersionsSync()

        // Trigger session restoration
        viewModel.restoreSessionFromProgress()

        viewModel.uiState.first { it.currentScreen == LearnerScreen.HOW && it.currentStepIndex == 3 }
        assertEquals(LearnerScreen.HOW, viewModel.uiState.value.currentScreen)
        assertEquals(3, viewModel.uiState.value.currentStepIndex) // Step 4 is 0-indexed as 3
        assertEquals("Close and seal the box.", viewModel.uiState.value.currentStep?.instruction)
    }
}
