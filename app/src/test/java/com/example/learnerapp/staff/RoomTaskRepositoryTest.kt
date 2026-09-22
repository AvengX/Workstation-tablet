package com.example.learnerapp.staff

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.DatabaseSeeder
import com.example.learnerapp.data.local.entities.TaskEntity
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
 * Room database integration tests for [RoomTaskRepository].
 * Tests Room persistence, alphabetical sorting, copy independence,
 * cascading deletion, and active learner progress safety.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomTaskRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var taskRepository: RoomTaskRepository
    private lateinit var learnerRepository: RoomLearnerRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskRepository = RoomTaskRepository(database)
        learnerRepository = RoomLearnerRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeTasks_loadsSeededTasksInAlphabeticalOrder() = runTest {
        learnerRepository.seedIfEmpty()

        val tasks = taskRepository.observeTasks().first()
        assertEquals(3, tasks.size)

        // Alphabetical: Clean work area, Pack one parcel, Sort supplies
        assertEquals("Clean work area", tasks[0].title)
        assertEquals("Pack one parcel", tasks[1].title)
        assertEquals("Sort supplies", tasks[2].title)
    }

    @Test
    fun createTask_insertsNewTaskIntoRoom() = runTest {
        learnerRepository.seedIfEmpty()

        val newTask = TaskEntity(
            id = "task_assemble_carton",
            title = "Assemble Carton",
            description = "Fold carton box",
            helpPhrase = "Ask staff for help",
            completionPhrase = "Finished carton"
        )

        val result = taskRepository.createTask(newTask)
        assertTrue(result.isSuccess)

        val retrieved = database.taskDao().getTaskById("task_assemble_carton")
        assertNotNull(retrieved)
        assertEquals("Assemble Carton", retrieved?.title)
        assertEquals("Fold carton box", retrieved?.description)

        val allTasks = taskRepository.observeTasks().first()
        assertEquals(4, allTasks.size)
    }

    @Test
    fun updateTask_updatesExistingTaskWithoutChangingId() = runTest {
        learnerRepository.seedIfEmpty()

        val original = database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertNotNull(original)

        val updated = original!!.copy(
            title = "Pack one parcel (Express)",
            description = "High-priority customer delivery"
        )

        val result = taskRepository.updateTask(updated)
        assertTrue(result.isSuccess)

        val retrieved = database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertNotNull(retrieved)
        assertEquals(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, retrieved?.id)
        assertEquals("Pack one parcel (Express)", retrieved?.title)
        assertEquals("High-priority customer delivery", retrieved?.description)
    }

    @Test
    fun copyTask_createsIndependentTaskWithNewIdAndSuffix() = runTest {
        learnerRepository.seedIfEmpty()

        val result = taskRepository.copyTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertTrue(result.isSuccess)

        val copy = result.getOrNull()
        assertNotNull(copy)
        assertTrue(copy!!.id.startsWith("task_"))
        assertEquals("Pack one parcel — Copy", copy.title)

        // Verify original remains untouched in Room
        val original = database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertEquals("Pack one parcel", original?.title)

        // Verify independent editing
        val modifiedCopy = copy.copy(title = "Pack Fragile Parcel")
        taskRepository.updateTask(modifiedCopy)

        val retrievedCopy = database.taskDao().getTaskById(copy.id)
        val retrievedOriginal = database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)

        assertEquals("Pack Fragile Parcel", retrievedCopy?.title)
        assertEquals("Pack one parcel", retrievedOriginal?.title)
    }

    @Test
    fun deleteTask_removesTaskAndCascadesCleanup() = runTest {
        learnerRepository.seedIfEmpty()

        // Clean work area is not in active progress
        val taskId = DatabaseSeeder.TASK_ID_CLEAN_WORK_AREA
        assertNotNull(database.taskDao().getTaskById(taskId))

        val result = taskRepository.deleteTask(taskId)
        assertTrue(result.isSuccess)

        assertNull(database.taskDao().getTaskById(taskId))
        // Verify schedule items were cleaned up
        val scheduleItems = database.scheduleDao().getAllScheduleItems().filter { it.taskId == taskId }
        assertTrue(scheduleItems.isEmpty())
    }

    @Test
    fun deleteTask_onActiveTask_throwsExceptionAndBlocksDeletion() = runTest {
        learnerRepository.seedIfEmpty()

        // Learner starts task and advances to step 2
        learnerRepository.startTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        learnerRepository.updateCurrentStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 2)

        assertTrue(taskRepository.isTaskActiveInLearnerProgress(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL))

        // Attempting to delete active task MUST fail
        val result = taskRepository.deleteTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertTrue(result.isFailure)
        assertEquals(
            "This task is currently in progress and cannot be deleted.",
            result.exceptionOrNull()?.message
        )

        // Verify task still exists in Room
        assertNotNull(database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL))

        // Verify learner progress is 100% intact
        val progress = database.progressDao().getProgressForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertNotNull(progress)
        assertEquals(2, progress?.currentStep)
        assertEquals("IN_PROGRESS", progress?.status)
    }

    @Test
    fun updateTask_doesNotResetActiveLearnerProgress() = runTest {
        learnerRepository.seedIfEmpty()
        learnerRepository.startTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        learnerRepository.updateCurrentStep(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL, 3)

        // Staff modifies task title and description
        val original = database.taskDao().getTaskById(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)!!
        val updated = original.copy(title = "Pack one parcel (Standard)")
        taskRepository.updateTask(updated)

        // Learner progress must remain at Step 3, IN_PROGRESS
        val progress = database.progressDao().getProgressForTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL)
        assertNotNull(progress)
        assertEquals(3, progress?.currentStep)
        assertEquals("IN_PROGRESS", progress?.status)
    }
}
