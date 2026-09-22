package com.example.learnerapp.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.DatabaseSeeder
import com.example.learnerapp.data.local.entities.ScheduleItemEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import com.example.learnerapp.data.repository.RoomLearnerRepository
import com.example.learnerapp.model.LearnerScreen
import com.example.learnerapp.staff.repository.RoomScheduleRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DynamicScheduleLearnerFlowTest {

    private lateinit var database: AppDatabase
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
        learnerRepository = RoomLearnerRepository(database)
        scheduleRepository = RoomScheduleRepository(database)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun dynamicSchedule_firstIncompleteIsNow_secondIsNext_remainingAreLater() = runTest {
        learnerRepository.seedIfEmpty()

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        viewModel.uiState.first { it.schedule.size == 3 }

        val schedule = viewModel.uiState.value.schedule
        assertEquals(3, schedule.size)

        // 1. First incomplete task is NOW
        assertEquals("NOW", schedule[0].timeCategory)
        assertEquals("Pack one parcel", schedule[0].title)
        assertTrue(schedule[0].isCurrent)

        // 2. Second is NEXT
        assertEquals("NEXT", schedule[1].timeCategory)
        assertEquals("Sort supplies", schedule[1].title)
        assertFalse(schedule[1].isCurrent)

        // 3. Remaining are LATER
        assertEquals("LATER", schedule[2].timeCategory)
        assertEquals("Clean work area", schedule[2].title)
        assertFalse(schedule[2].isCurrent)
    }

    @Test
    fun oneTaskSchedule_showsNowOnly() = runTest {
        learnerRepository.seedIfEmpty()
        // Clear default schedule and add only 1 task on todayStr
        database.scheduleDao().deleteScheduleItemsForTask(DatabaseSeeder.TASK_ID_SORT_SUPPLIES)
        database.scheduleDao().deleteScheduleItemsForTask(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA)

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        viewModel.uiState.first { it.schedule.size == 1 }

        val schedule = viewModel.uiState.value.schedule
        assertEquals(1, schedule.size)
        assertEquals("NOW", schedule[0].timeCategory)
        assertEquals("Pack one parcel", schedule[0].title)
        assertFalse(viewModel.uiState.value.isScheduleEmpty)
        assertFalse(viewModel.uiState.value.isAllTasksCompleted)
    }

    @Test
    fun twoTaskSchedule_showsNowAndNextOnly() = runTest {
        learnerRepository.seedIfEmpty()
        database.scheduleDao().deleteScheduleItemsForTask(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA)

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        viewModel.uiState.first { it.schedule.size == 2 }

        val schedule = viewModel.uiState.value.schedule
        assertEquals(2, schedule.size)
        assertEquals("NOW", schedule[0].timeCategory)
        assertEquals("Pack one parcel", schedule[0].title)
        assertEquals("NEXT", schedule[1].timeCategory)
        assertEquals("Sort supplies", schedule[1].title)
    }

    @Test
    fun emptySchedule_showsNoTasksAssignedState() = runTest {
        learnerRepository.seedIfEmpty()
        database.scheduleDao().deleteScheduleItemsForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        database.scheduleDao().deleteScheduleItemsForTask(DatabaseSeeder.TASK_ID_SORT_SUPPLIES)
        database.scheduleDao().deleteScheduleItemsForTask(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA)

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        viewModel.uiState.first { it.isScheduleEmpty }

        assertTrue(viewModel.uiState.value.schedule.isEmpty())
        assertTrue(viewModel.uiState.value.isScheduleEmpty)
        assertFalse(viewModel.uiState.value.isAllTasksCompleted)
    }

    @Test
    fun activeInProgressTask_remainsNow_evenIfStaffReordersSchedule() = runTest {
        learnerRepository.seedIfEmpty()

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        advanceUntilIdle()

        // Learner starts Pack one parcel and advances to Step 3
        viewModel.startTask()
        viewModel.nextStep() // Step 2
        viewModel.nextStep() // Step 3
        advanceUntilIdle()

        assertEquals("Pack one parcel", viewModel.uiState.value.task.title)
        assertEquals(2, viewModel.uiState.value.currentStepIndex) // Step 3
        assertEquals(LearnerScreen.HOW, viewModel.uiState.value.currentScreen)

        // Staff moves Sort supplies UP to position 1, pushing Pack one parcel to position 2
        val scheduled = scheduleRepository.getScheduleForDate(todayStr)
        val sortSuppliesItemId = scheduled.first { it.task.id == DatabaseSeeder.TASK_ID_SORT_SUPPLIES }.scheduleItem.id
        scheduleRepository.moveTaskUp(todayStr, sortSuppliesItemId)
        advanceUntilIdle()

        // Active task MUST STILL be Pack one parcel because it is IN_PROGRESS!
        assertEquals("Pack one parcel", viewModel.uiState.value.task.title)
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        assertEquals(LearnerScreen.HOW, viewModel.uiState.value.currentScreen)

        val schedule = viewModel.uiState.value.schedule
        assertEquals("NOW", schedule[0].timeCategory)
        assertEquals("Pack one parcel", schedule[0].title)
    }

    @Test
    fun activeCheckingTask_remainsNow() = runTest {
        learnerRepository.seedIfEmpty()

        // Set Sort supplies to CHECKING
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "progress_sort_supplies",
                taskId = DatabaseSeeder.TASK_ID_SORT_SUPPLIES,
                currentStep = 5,
                status = "CHECKING"
            )
        )

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        viewModel.uiState.first { it.schedule.firstOrNull()?.title == "Sort supplies" }

        // Sort supplies must be NOW because it is CHECKING
        val schedule = viewModel.uiState.value.schedule
        assertEquals("NOW", schedule[0].timeCategory)
        assertEquals("Sort supplies", schedule[0].title)
    }

    @Test
    fun multipleActiveTasks_chooseEarliestScheduledActiveTask() = runTest {
        learnerRepository.seedIfEmpty()

        // Task 1 (Pack parcel) is IN_PROGRESS
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "progress_pack_parcel",
                taskId = DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL,
                currentStep = 2,
                status = "IN_PROGRESS"
            )
        )
        // Task 2 (Sort supplies) is ALSO CHECKING (corrupted/legacy state)
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "progress_sort_supplies",
                taskId = DatabaseSeeder.TASK_ID_SORT_SUPPLIES,
                currentStep = 4,
                status = "CHECKING"
            )
        )

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        viewModel.uiState.first { it.schedule.isNotEmpty() && it.schedule[0].title == "Pack one parcel" }

        // Earliest scheduled active task in order ASC (Pack parcel at order 1) is chosen as NOW
        assertEquals("NOW", viewModel.uiState.value.schedule[0].timeCategory)
        assertEquals("Pack one parcel", viewModel.uiState.value.schedule[0].title)
    }

    @Test
    fun currentStep_andChecklistProgress_surviveScheduleReordering() = runTest {
        learnerRepository.seedIfEmpty()
        learnerRepository.startTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        learnerRepository.updateCurrentStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 2)
        learnerRepository.toggleChecklistItem(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 1)
        database.invalidationTracker.refreshVersionsSync()

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.currentStepIndex == 1 && it.checklistItems.firstOrNull { c -> c.id == 1 }?.isChecked == true }

        // Verify state before reordering
        assertEquals("Pack one parcel", viewModel.uiState.value.task.title)
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        assertTrue(viewModel.uiState.value.checklistItems.first { it.id == 1 }.isChecked)

        // Staff moves Pack parcel down
        val scheduled = scheduleRepository.getScheduleForDate(todayStr)
        val packParcelItemId = scheduled.first { it.task.id == DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL }.scheduleItem.id
        scheduleRepository.moveTaskDown(todayStr, packParcelItemId)
        database.invalidationTracker.refreshVersionsSync()
        advanceUntilIdle()

        // Progress intact! Active in-progress task remains Pack one parcel, step intact, checklist intact!
        assertEquals("Pack one parcel", viewModel.uiState.value.task.title)
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        assertTrue(viewModel.uiState.value.checklistItems.first { it.id == 1 }.isChecked)
        assertEquals("NOW", viewModel.uiState.value.schedule[0].timeCategory)
        assertEquals("Pack one parcel", viewModel.uiState.value.schedule[0].title)
    }

    @Test
    fun appRestart_restoresActiveTask_andCurrentStep() = runTest {
        learnerRepository.seedIfEmpty()
        learnerRepository.startTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        learnerRepository.updateCurrentStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 4)
        database.invalidationTracker.refreshVersionsSync()

        // Simulate fresh app launch with new ViewModel instance
        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.currentStepIndex == 3 }

        assertEquals("Pack one parcel", viewModel.uiState.value.task.title)
        assertEquals(3, viewModel.uiState.value.currentStepIndex) // Step 4 (index 3) restored
        assertEquals(LearnerScreen.HOW, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun taskCompletion_advancesToNextScheduledTask() = runTest {
        learnerRepository.seedIfEmpty()

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        viewModel.uiState.first { it.schedule.size == 3 }

        assertEquals("Pack one parcel", viewModel.uiState.value.schedule[0].title)

        // Complete Pack one parcel
        viewModel.completeTask()
        advanceUntilIdle()
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.schedule.firstOrNull()?.title == "Sort supplies" }

        assertEquals(LearnerScreen.NEXT, viewModel.uiState.value.currentScreen)
        assertEquals("Clean work area", viewModel.uiState.value.nextTaskTitle)

        // Proceed to next task
        viewModel.startNextTask()
        assertEquals(LearnerScreen.NOW, viewModel.uiState.value.currentScreen)

        // NOW is now Sort supplies!
        val updatedSchedule = viewModel.uiState.value.schedule
        assertEquals("NOW", updatedSchedule[0].timeCategory)
        assertEquals("Sort supplies", updatedSchedule[0].title)
        assertEquals("NEXT", updatedSchedule[1].timeCategory)
        assertEquals("Clean work area", updatedSchedule[1].title)

        // Pack one parcel is COMPLETED in Room
        val packProgress = database.progressDao().getProgressForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertEquals("COMPLETED", packProgress?.status)
    }

    @Test
    fun allTasksCompleted_showsAllCompleteState() = runTest {
        learnerRepository.seedIfEmpty()

        val viewModel = LearnerViewModel(learnerRepository, todayStr)
        viewModel.uiState.first { it.schedule.size == 3 }

        // Complete all 3 tasks
        learnerRepository.completeTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        learnerRepository.completeTask(DatabaseSeeder.TASK_ID_SORT_SUPPLIES)
        learnerRepository.completeTask(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA)
        database.invalidationTracker.refreshVersionsSync()

        viewModel.uiState.first { it.isAllTasksCompleted }

        assertTrue(viewModel.uiState.value.isAllTasksCompleted)
        assertTrue(viewModel.uiState.value.schedule.isEmpty())
        assertFalse(viewModel.uiState.value.isScheduleEmpty)
    }
}
