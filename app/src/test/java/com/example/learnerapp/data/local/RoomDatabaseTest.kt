package com.example.learnerapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.repository.RoomLearnerRepository
import com.example.learnerapp.model.LearnerScreen
import com.example.learnerapp.viewmodel.LearnerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomLearnerRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomLearnerRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseInitializes_andSeedsDataOnce() = runTest {
        assertEquals(0, database.taskDao().getTaskCount())

        // First seed
        repository.seedIfEmpty()
        assertEquals(3, database.taskDao().getTaskCount())
        assertEquals(5, database.taskStepDao().getStepsForTask("pack_one_parcel").size)
        assertEquals(4, database.checklistDao().getItemsForTask("pack_one_parcel").size)
        assertEquals(3, database.scheduleDao().getAllScheduleItems().size)

        // Second seed call must not duplicate data
        repository.seedIfEmpty()
        assertEquals(3, database.taskDao().getTaskCount())
        assertEquals(5, database.taskStepDao().getStepsForTask("pack_one_parcel").size)
        assertEquals(4, database.checklistDao().getItemsForTask("pack_one_parcel").size)
        assertEquals(3, database.scheduleDao().getAllScheduleItems().size)
    }

    @Test
    fun taskAndSteps_loadInCorrectOrder() = runTest {
        repository.seedIfEmpty()

        val task = database.taskDao().getTaskById("pack_one_parcel")
        assertNotNull(task)
        assertEquals("Pack one parcel", task?.title)
        assertEquals("I have finished packing this parcel. Please check.", task?.completionPhrase)

        val steps = database.taskStepDao().getStepsForTask("pack_one_parcel")
        assertEquals(5, steps.size)
        for (i in 1..5) {
            assertEquals(i, steps[i - 1].stepNumber)
        }
        assertEquals("Take the assigned item, box and packing materials.", steps[0].instruction)
        assertEquals("Check the item and quantity against the packing instruction.", steps[1].instruction)
        assertEquals("Place the item in the box. Add the required protective material.", steps[2].instruction)
        assertEquals("Close and seal the box.", steps[3].instruction)
        assertEquals("Attach the supplied parcel label in the correct position.", steps[4].instruction)
    }

    @Test
    fun checklist_loadsInOrder() = runTest {
        repository.seedIfEmpty()

        val items = database.checklistDao().getItemsForTask("pack_one_parcel")
        assertEquals(4, items.size)
        assertEquals("Correct item and quantity", items[0].text)
        assertEquals("Item is protected", items[1].text)
        assertEquals("Box is sealed", items[2].text)
        assertEquals("Correct label is attached", items[3].text)
    }

    @Test
    fun schedule_loadsCategoriesCorrectly() = runTest {
        repository.seedIfEmpty()

        val schedule = database.scheduleDao().getAllScheduleItems()
        assertEquals(3, schedule.size)
        assertEquals("NOW", schedule[0].category)
        assertEquals("pack_one_parcel", schedule[0].taskId)
        assertEquals("NEXT", schedule[1].category)
        assertEquals("sort_supplies", schedule[1].taskId)
        assertEquals("LATER", schedule[2].category)
        assertEquals("clean_work_area", schedule[2].taskId)
    }

    @Test
    fun startTask_persistsInProgressStatus_andSetsStep1() = runTest {
        repository.seedIfEmpty()

        repository.startTask("pack_one_parcel")

        val progress = database.progressDao().getProgressForTask("pack_one_parcel")
        assertNotNull(progress)
        assertEquals("IN_PROGRESS", progress?.status)
        assertEquals(1, progress?.currentStep)
        assertNotNull(progress?.startedAt)
    }

    @Test
    fun currentStep_persistsCorrectly() = runTest {
        repository.seedIfEmpty()
        repository.startTask("pack_one_parcel")

        repository.updateCurrentStep("pack_one_parcel", 3)

        val progress = database.progressDao().getProgressForTask("pack_one_parcel")
        assertEquals(3, progress?.currentStep)
        assertEquals("IN_PROGRESS", progress?.status)
    }

    @Test
    fun checklistToggles_persistIndividually() = runTest {
        repository.seedIfEmpty()
        repository.startTask("pack_one_parcel")

        repository.toggleChecklistItem("pack_one_parcel", 1)
        repository.toggleChecklistItem("pack_one_parcel", 3)

        val checklistProgress = database.progressDao().getChecklistProgress("pack_one_parcel")
        val item1 = checklistProgress.find { it.checklistItemId == 1 }
        val item2 = checklistProgress.find { it.checklistItemId == 2 }
        val item3 = checklistProgress.find { it.checklistItemId == 3 }

        assertTrue(item1?.isChecked == true)
        assertFalse(item2?.isChecked == true)
        assertTrue(item3?.isChecked == true)

        // Toggle item 1 off
        repository.toggleChecklistItem("pack_one_parcel", 1)
        val updated = database.progressDao().getChecklistProgress("pack_one_parcel")
        assertFalse(updated.find { it.checklistItemId == 1 }?.isChecked == true)
    }

    @Test
    fun checkingStatus_persistsWhenEnteringCheck() = runTest {
        repository.seedIfEmpty()
        repository.startTask("pack_one_parcel")

        repository.moveToChecking("pack_one_parcel")

        val progress = database.progressDao().getProgressForTask("pack_one_parcel")
        assertEquals("CHECKING", progress?.status)
    }

    @Test
    fun completedStatus_andTimestampPersist() = runTest {
        repository.seedIfEmpty()
        repository.startTask("pack_one_parcel")
        repository.moveToChecking("pack_one_parcel")

        repository.completeTask("pack_one_parcel")

        val progress = database.progressDao().getProgressForTask("pack_one_parcel")
        assertEquals("COMPLETED", progress?.status)
        assertNotNull(progress?.completedAt)
        assertTrue((progress?.completedAt ?: 0) > 0)
    }

    @Test
    fun appRestart_restoresUnfinishedTaskAtCurrentStep() = runTest {
        repository.seedIfEmpty()
        repository.startTask("pack_one_parcel")
        repository.updateCurrentStep("pack_one_parcel", 4) // Learner was on Step 4

        // Simulate app restart by creating a new ViewModel observing the same repository
        val viewModel = LearnerViewModel(repository)

        // Let the flow collect
        val progress = repository.getTaskProgress("pack_one_parcel").first()
        assertEquals(4, progress?.currentStep)
        assertEquals("IN_PROGRESS", progress?.status)
    }

    @Test
    fun appRestart_restoresChecklistSelection() = runTest {
        repository.seedIfEmpty()
        repository.startTask("pack_one_parcel")
        repository.moveToChecking("pack_one_parcel")
        repository.toggleChecklistItem("pack_one_parcel", 1)
        repository.toggleChecklistItem("pack_one_parcel", 2)

        val checklistItems = repository.getChecklistWithProgress("pack_one_parcel").first()
        assertEquals(4, checklistItems.size)
        assertTrue(checklistItems.find { it.id == 1 }?.isChecked == true)
        assertTrue(checklistItems.find { it.id == 2 }?.isChecked == true)
        assertFalse(checklistItems.find { it.id == 3 }?.isChecked == true)
        assertFalse(checklistItems.find { it.id == 4 }?.isChecked == true)
    }
}
