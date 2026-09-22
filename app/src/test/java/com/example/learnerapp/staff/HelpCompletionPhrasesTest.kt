package com.example.learnerapp.staff

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.DatabaseSeeder
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.repository.RoomLearnerRepository
import com.example.learnerapp.model.Task
import com.example.learnerapp.staff.repository.RoomTaskRepository
import com.example.learnerapp.staff.viewmodel.FakeTaskRepository
import com.example.learnerapp.staff.viewmodel.TaskManagementViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 8 Unit & Integration tests for Staff-configurable Help Phrase and Completion Phrase.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HelpCompletionPhrasesTest {

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
    fun taskEntity_storesAndRetrievesCustomHelpAndCompletionPhrases() = runTest {
        val task = TaskEntity(
            id = "task_custom_phrases",
            title = "Sanitize Workstation",
            description = "Clean tablet screen and table surface.",
            helpPhrase = "Ask staff for extra sanitizing wipes if empty.",
            completionPhrase = "I have finished sanitizing my workstation. Ready for inspection."
        )

        val result = taskRepository.createTask(task)
        assertTrue(result.isSuccess)

        val retrieved = taskRepository.getTask("task_custom_phrases")
        assertNotNull(retrieved)
        assertEquals("Ask staff for extra sanitizing wipes if empty.", retrieved?.helpPhrase)
        assertEquals("I have finished sanitizing my workstation. Ready for inspection.", retrieved?.completionPhrase)
    }

    @Test
    fun deepCopyTask_preservesHelpAndCompletionPhrases() = runTest {
        val original = TaskEntity(
            id = "task_source",
            title = "Sort Mail",
            description = "Sort incoming letters into trays.",
            helpPhrase = "Raise your hand if a letter has no tray number.",
            completionPhrase = "All mail has been sorted."
        )
        taskRepository.createTask(original)

        val copyResult = taskRepository.copyTask("task_source")
        assertTrue(copyResult.isSuccess)

        val copiedTask = copyResult.getOrNull()
        assertNotNull(copiedTask)
        assertTrue(copiedTask!!.id != "task_source")
        assertEquals("Sort Mail — Copy", copiedTask.title)
        assertEquals("Raise your hand if a letter has no tray number.", copiedTask.helpPhrase)
        assertEquals("All mail has been sorted.", copiedTask.completionPhrase)
    }

    @Test
    fun learnerRepository_mapsHelpPhraseAndCompletionPhraseToDomainTask() = runTest {
        learnerRepository.seedIfEmpty()

        val seeded = learnerRepository.getTask(DatabaseSeeder.TASK_ID_PACK_ONE_PARCEL).first()
        assertNotNull(seeded)
        assertEquals("Pack one parcel", seeded?.title)
        assertEquals(DatabaseSeeder.initialTask.helpPhrase, seeded?.helpPhrase)
        assertEquals(DatabaseSeeder.initialTask.completionPhrase, seeded?.completionPhrase)
    }

    @Test
    fun taskManagementViewModel_createsTaskWithCustomPhrases() = runTest {
        val fakeRepo = FakeTaskRepository()
        val viewModel = TaskManagementViewModel(fakeRepo)

        viewModel.initCreateForm()
        viewModel.onTitleChanged("Weigh Packages")
        viewModel.onDescriptionChanged("Weigh each package on scale")
        viewModel.onHelpPhraseChanged("Call supervisor if scale shows error.")
        viewModel.onCompletionPhraseChanged("Packages weighed and recorded.")

        var saved = false
        viewModel.saveTask(onSuccess = { saved = true })
        assertTrue(saved)

        val createdTask = fakeRepo.tasksFlow.value.first()
        assertEquals("Weigh Packages", createdTask.title)
        assertEquals("Call supervisor if scale shows error.", createdTask.helpPhrase)
        assertEquals("Packages weighed and recorded.", createdTask.completionPhrase)
    }

    @Test
    fun taskManagementViewModel_editsTaskPhrases() = runTest {
        val fakeRepo = FakeTaskRepository()
        val existingTask = TaskEntity(
            id = "task_orig",
            title = "Check Inventory",
            description = "Count shelf units",
            helpPhrase = "Original help phrase",
            completionPhrase = "Original done phrase"
        )
        fakeRepo.tasksFlow.value = listOf(existingTask)

        val viewModel = TaskManagementViewModel(fakeRepo)
        viewModel.initEditForm(existingTask)
        viewModel.onHelpPhraseChanged("Updated help: check item barcodes.")
        viewModel.onCompletionPhraseChanged("Updated done: counts match list.")

        var saved = false
        viewModel.saveTask(onSuccess = { saved = true })
        assertTrue(saved)

        val updated = fakeRepo.tasksFlow.value.first { it.id == "task_orig" }
        assertEquals("Updated help: check item barcodes.", updated.helpPhrase)
        assertEquals("Updated done: counts match list.", updated.completionPhrase)
    }

    @Test
    fun domainTask_defaultHelpPhrase_isBackwardsCompatible() {
        val task = Task(
            id = "t1",
            title = "Test Task",
            steps = emptyList(),
            checklist = emptyList(),
            completionPhrase = "Done phrase"
        )
        assertEquals("Please ask a member of staff for help with this step.", task.helpPhrase)
    }
}
