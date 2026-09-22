package com.example.learnerapp.staff.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.DatabaseSeeder
import com.example.learnerapp.data.repository.RoomLearnerRepository
import com.example.learnerapp.staff.repository.RoomScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScheduleManagementViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var learnerRepository: RoomLearnerRepository
    private lateinit var scheduleRepository: RoomScheduleRepository
    private lateinit var viewModel: ScheduleManagementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        learnerRepository = RoomLearnerRepository(database)
        scheduleRepository = RoomScheduleRepository(database)
        runBlocking {
            learnerRepository.seedIfEmpty()
        }
        database.invalidationTracker.refreshVersionsSync()
        viewModel = ScheduleManagementViewModel(scheduleRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun initialState_defaultsToToday_andAllowsDateNavigation() = runTest {
        val today = LocalDate.now(ZoneId.systemDefault())
        assertEquals(today, viewModel.uiState.value.selectedDate)
        assertTrue(viewModel.uiState.value.isToday)

        // Previous Day
        viewModel.previousDay()
        assertEquals(today.minusDays(1), viewModel.uiState.value.selectedDate)
        assertFalse(viewModel.uiState.value.isToday)

        // Next Day
        viewModel.nextDay()
        assertEquals(today, viewModel.uiState.value.selectedDate)
        assertTrue(viewModel.uiState.value.isToday)

        // Jump to specific date
        val futureDate = today.plusDays(7)
        viewModel.selectDate(futureDate)
        assertEquals(futureDate, viewModel.uiState.value.selectedDate)
        assertFalse(viewModel.uiState.value.isToday)

        // Go to Today
        viewModel.goToToday()
        assertEquals(today, viewModel.uiState.value.selectedDate)
        assertTrue(viewModel.uiState.value.isToday)
    }

    @Test
    fun datePicker_opensAndCloses() = runTest {
        assertFalse(viewModel.uiState.value.isDatePickerOpen)
        viewModel.openDatePicker(true)
        assertTrue(viewModel.uiState.value.isDatePickerOpen)
        viewModel.openDatePicker(false)
        assertFalse(viewModel.uiState.value.isDatePickerOpen)
    }

    @Test
    fun addTaskFlow_opensDialog_selectsTask_andAddsSuccessfully() = runTest {
        val tomorrow = LocalDate.now(ZoneId.systemDefault()).plusDays(1)
        viewModel.selectDate(tomorrow)
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.selectedDate == tomorrow && it.availableTasks.isNotEmpty() }

        val availableBefore = viewModel.uiState.value.availableTasks
        assertTrue(availableBefore.isNotEmpty())

        // Open add dialog
        viewModel.openAddTaskDialog(true)
        assertTrue(viewModel.uiState.value.isAddTaskDialogOpen)
        assertNotNull(viewModel.uiState.value.selectedTaskIdToAdd)

        // Select specific task
        viewModel.selectTaskToAdd(DatabaseSeeder.TASK_ID_SORT_SUPPLIES)
        assertEquals(DatabaseSeeder.TASK_ID_SORT_SUPPLIES, viewModel.uiState.value.selectedTaskIdToAdd)

        // Confirm add
        viewModel.confirmAddTask()
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { !it.isAddTaskDialogOpen && it.scheduledTasks.isNotEmpty() }

        assertFalse(viewModel.uiState.value.isAddTaskDialogOpen)
        assertNull(viewModel.uiState.value.errorMessage)

        val scheduled = viewModel.uiState.value.scheduledTasks
        assertTrue(scheduled.any { it.task.id == DatabaseSeeder.TASK_ID_SORT_SUPPLIES })
    }

    @Test
    fun removeTaskFlow_opensDialog_andRemovesOnConfirmation() = runTest {
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.scheduledTasks.size == 3 }

        val scheduled = viewModel.uiState.value.scheduledTasks
        val taskToRemove = scheduled.first()

        // Open remove dialog
        viewModel.openRemoveDialog(taskToRemove)
        assertEquals(taskToRemove, viewModel.uiState.value.taskToRemove)

        // Confirm remove
        viewModel.confirmRemoveTask()
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.taskToRemove == null && it.scheduledTasks.size == 2 }

        assertNull(viewModel.uiState.value.taskToRemove)
        assertNull(viewModel.uiState.value.errorMessage)
        val scheduledAfter = viewModel.uiState.value.scheduledTasks
        assertTrue(scheduledAfter.none { it.scheduleItem.id == taskToRemove.scheduleItem.id })
    }

    @Test
    fun reorder_movesTaskUpAndDown() = runTest {
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.scheduledTasks.size >= 2 }

        val scheduled = viewModel.uiState.value.scheduledTasks
        val secondItem = scheduled[1]
        val firstItem = scheduled[0]

        // Move 2nd item UP
        viewModel.moveTaskUp(secondItem.scheduleItem.id)
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.scheduledTasks[0].task.id == secondItem.task.id }

        val afterUp = viewModel.uiState.value.scheduledTasks
        assertEquals(secondItem.task.id, afterUp[0].task.id)
        assertEquals(firstItem.task.id, afterUp[1].task.id)

        // Move it back DOWN
        viewModel.moveTaskDown(secondItem.scheduleItem.id)
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.scheduledTasks[0].task.id == firstItem.task.id }

        val afterDown = viewModel.uiState.value.scheduledTasks
        assertEquals(firstItem.task.id, afterDown[0].task.id)
        assertEquals(secondItem.task.id, afterDown[1].task.id)
    }

    @Test
    fun errorHandling_displaysAndClearsErrors() = runTest {
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.scheduledTasks.isNotEmpty() }

        // Attempting to add an already scheduled task
        val scheduled = viewModel.uiState.value.scheduledTasks
        val existingTaskId = scheduled.first().task.id

        viewModel.selectTaskToAdd(existingTaskId)
        viewModel.confirmAddTask()
        database.invalidationTracker.refreshVersionsSync()
        viewModel.uiState.first { it.errorMessage != null }

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("already scheduled") == true)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
