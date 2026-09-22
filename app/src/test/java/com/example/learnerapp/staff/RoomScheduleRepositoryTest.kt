package com.example.learnerapp.staff

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.DatabaseSeeder
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import com.example.learnerapp.data.repository.RoomLearnerRepository
import com.example.learnerapp.staff.repository.RoomScheduleRepository
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomScheduleRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var learnerRepository: RoomLearnerRepository
    private lateinit var scheduleRepository: RoomScheduleRepository
    private lateinit var taskRepository: RoomTaskRepository

    private val testDate1 = "2030-01-01"
    private val testDate2 = "2030-01-02"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        learnerRepository = RoomLearnerRepository(database)
        scheduleRepository = RoomScheduleRepository(database)
        taskRepository = RoomTaskRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun schedule_loadsByDate_andReturnsEmptyWhenNoTasksScheduled() = runTest {
        learnerRepository.seedIfEmpty()

        val emptySchedule = scheduleRepository.getScheduleForDate("2029-01-01")
        assertTrue(emptySchedule.isEmpty())

        val scheduledFlow = scheduleRepository.observeScheduleForDate("2029-01-01").first()
        assertTrue(scheduledFlow.isEmpty())
    }

    @Test
    fun addTaskToSchedule_appendsAtMaxOrderPlusOne() = runTest {
        learnerRepository.seedIfEmpty()

        // Schedule first task on testDate1
        val res1 = scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertTrue(res1.isSuccess)

        val scheduleAfter1 = scheduleRepository.getScheduleForDate(testDate1)
        assertEquals(1, scheduleAfter1.size)
        assertEquals(1, scheduleAfter1[0].scheduleItem.order)
        assertEquals(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, scheduleAfter1[0].task.id)

        // Schedule second task
        val res2 = scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_SORT_SUPPLIES)
        assertTrue(res2.isSuccess)

        val scheduleAfter2 = scheduleRepository.getScheduleForDate(testDate1)
        assertEquals(2, scheduleAfter2.size)
        assertEquals(1, scheduleAfter2[0].scheduleItem.order)
        assertEquals(2, scheduleAfter2[1].scheduleItem.order)
        assertEquals(DatabaseSeeder.TASK_ID_SORT_SUPPLIES, scheduleAfter2[1].task.id)
    }

    @Test
    fun addTaskToSchedule_duplicateTaskPreventedOnSameDay() = runTest {
        learnerRepository.seedIfEmpty()

        val res1 = scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertTrue(res1.isSuccess)

        // Adding the same task on testDate1 again must fail
        val res2 = scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertTrue(res2.isFailure)
        assertTrue(res2.exceptionOrNull()?.message?.contains("already scheduled") == true)

        val schedule = scheduleRepository.getScheduleForDate(testDate1)
        assertEquals(1, schedule.size)
    }

    @Test
    fun removeTaskFromSchedule_reindexesRemainingTasks_andLeavesTaskAndProgressIntact() = runTest {
        learnerRepository.seedIfEmpty()

        scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_SORT_SUPPLIES)
        scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA)

        // Add some progress to DatabaseSeeder.TASK_ID_SORT_SUPPLIES
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity(
                id = "progress_sort_supplies",
                taskId = DatabaseSeeder.TASK_ID_SORT_SUPPLIES,
                currentStep = 3,
                status = "IN_PROGRESS"
            )
        )
        database.progressDao().insertOrUpdateChecklistProgress(
            ChecklistProgressEntity(
                taskId = DatabaseSeeder.TASK_ID_SORT_SUPPLIES,
                checklistItemId = 101,
                isChecked = true
            )
        )

        val scheduleBefore = scheduleRepository.getScheduleForDate(testDate1)
        assertEquals(3, scheduleBefore.size)
        val middleItemId = scheduleBefore[1].scheduleItem.id

        // Remove the middle task (Sort supplies) from schedule
        val removeRes = scheduleRepository.removeTaskFromSchedule(middleItemId)
        assertTrue(removeRes.isSuccess)

        val scheduleAfter = scheduleRepository.getScheduleForDate(testDate1)
        assertEquals(2, scheduleAfter.size)
        assertEquals(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, scheduleAfter[0].task.id)
        assertEquals(1, scheduleAfter[0].scheduleItem.order)
        assertEquals(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA, scheduleAfter[1].task.id)
        assertEquals(2, scheduleAfter[1].scheduleItem.order) // Reindexed from 3 to 2

        // Verify task template STILL exists in Task table
        assertNotNull(database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_SORT_SUPPLIES))

        // Verify progress STILL exists in Room progress tables
        val progress = database.progressDao().getProgressForTask(DatabaseSeeder.TASK_ID_SORT_SUPPLIES)
        assertNotNull(progress)
        assertEquals(3, progress?.currentStep)
        assertEquals("IN_PROGRESS", progress?.status)

        val checklistProgress = database.progressDao().getChecklistProgress(DatabaseSeeder.TASK_ID_SORT_SUPPLIES)
        assertEquals(1, checklistProgress.size)
        assertTrue(checklistProgress[0].isChecked)
    }

    @Test
    fun moveTaskUp_andMoveTaskDown_atomicallyReorder_andMaintainContiguousOrder() = runTest {
        learnerRepository.seedIfEmpty()

        scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL) // order 1
        scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_SORT_SUPPLIES)    // order 2
        scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA) // order 3

        val initial = scheduleRepository.getScheduleForDate(testDate1)
        val secondItemId = initial[1].scheduleItem.id

        // Move 2nd item (Sort supplies) UP
        val moveUpRes = scheduleRepository.moveTaskUp(testDate1, secondItemId)
        assertTrue(moveUpRes.isSuccess)

        val afterMoveUp = scheduleRepository.getScheduleForDate(testDate1)
        assertEquals(3, afterMoveUp.size)
        assertEquals(DatabaseSeeder.TASK_ID_SORT_SUPPLIES, afterMoveUp[0].task.id)
        assertEquals(1, afterMoveUp[0].scheduleItem.order)
        assertEquals(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, afterMoveUp[1].task.id)
        assertEquals(2, afterMoveUp[1].scheduleItem.order)
        assertEquals(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA, afterMoveUp[2].task.id)
        assertEquals(3, afterMoveUp[2].scheduleItem.order)

        // Move it back DOWN
        val moveDownRes = scheduleRepository.moveTaskDown(testDate1, secondItemId)
        assertTrue(moveDownRes.isSuccess)

        val afterMoveDown = scheduleRepository.getScheduleForDate(testDate1)
        assertEquals(3, afterMoveDown.size)
        assertEquals(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, afterMoveDown[0].task.id)
        assertEquals(1, afterMoveDown[0].scheduleItem.order)
        assertEquals(DatabaseSeeder.TASK_ID_SORT_SUPPLIES, afterMoveDown[1].task.id)
        assertEquals(2, afterMoveDown[1].scheduleItem.order)
        assertEquals(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA, afterMoveDown[2].task.id)
        assertEquals(3, afterMoveDown[2].scheduleItem.order)
    }

    @Test
    fun dateIsolation_differentDatesMaintainIndependentSchedules() = runTest {
        learnerRepository.seedIfEmpty()

        scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        scheduleRepository.addTaskToSchedule(testDate2, DatabaseSeeder.TASK_ID_SORT_SUPPLIES)

        val schedule1 = scheduleRepository.getScheduleForDate(testDate1)
        val schedule2 = scheduleRepository.getScheduleForDate(testDate2)

        assertEquals(1, schedule1.size)
        assertEquals(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, schedule1[0].task.id)

        assertEquals(1, schedule2.size)
        assertEquals(DatabaseSeeder.TASK_ID_SORT_SUPPLIES, schedule2[0].task.id)
    }

    @Test
    fun taskDeletion_cleansUpScheduleEntries_preventingOrphanRecords() = runTest {
        learnerRepository.seedIfEmpty()

        scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA)
        val scheduleBefore = scheduleRepository.getScheduleForDate(testDate1)
        assertEquals(1, scheduleBefore.size)

        // Delete the task template from library
        val deleteRes = taskRepository.deleteTask(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA)
        assertTrue(deleteRes.isSuccess)

        // Verify task template is deleted
        assertNull(database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA))

        // Verify schedule entries referencing this task were deleted (no orphan entries)
        val scheduleAfter = database.scheduleDao().getAllScheduleItems().filter { it.taskId == DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA }
        assertTrue(scheduleAfter.isEmpty())
    }

    @Test
    fun availableTasks_excludesAlreadyScheduledTasksForDate() = runTest {
        learnerRepository.seedIfEmpty()

        val allTasksCount = database.taskDao().getTaskCount()
        val initialAvailable = scheduleRepository.observeAvailableTasksForDate(testDate1).first()
        assertEquals(allTasksCount, initialAvailable.size)

        // Schedule Pack one parcel
        scheduleRepository.addTaskToSchedule(testDate1, DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)

        val updatedAvailable = scheduleRepository.observeAvailableTasksForDate(testDate1).first()
        assertEquals(allTasksCount - 1, updatedAvailable.size)
        assertTrue(updatedAvailable.none { it.id == DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL })

        // On testDate2, Pack one parcel should still be available
        val availableDate2 = scheduleRepository.observeAvailableTasksForDate(testDate2).first()
        assertEquals(allTasksCount, availableDate2.size)
        assertTrue(availableDate2.any { it.id == DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL })
    }
}
