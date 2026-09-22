package com.example.learnerapp.staff.backup

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.ScheduleItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomBackupRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var backupRepository: RoomBackupRepository
    private lateinit var activePhotosDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupRepository = RoomBackupRepository(database, context)
        activePhotosDir = File(context.filesDir, "task_photos").apply {
            if (!exists()) mkdirs()
        }
    }

    @After
    fun tearDown() {
        database.close()
        activePhotosDir.deleteRecursively()
        File(context.cacheDir, "").listFiles()?.forEach {
            if (it.name.startsWith("backup_staging_") || it.name.startsWith("photos_rollback_")) {
                it.deleteRecursively()
            }
        }
    }

    private fun createPhotoFile(name: String): File {
        val file = File(activePhotosDir, name)
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }

    private suspend fun seedInitialData() {
        database.taskDao().insertTask(
            TaskEntity("task_alpha", "Task Alpha", "Alpha Desc", "Alpha Help", "Alpha Done")
        )
        val photoFile = createPhotoFile("alpha_step_1.jpg")
        database.taskStepDao().insertSteps(
            listOf(
                TaskStepEntity("step_a1", "task_alpha", 1, "First Alpha Step", photoFile.absolutePath),
                TaskStepEntity("step_a2", "task_alpha", 2, "Second Alpha Step", null)
            )
        )
        database.checklistDao().insertItems(
            listOf(
                ChecklistItemEntity(10, "task_alpha", 1, "Alpha Checklist 1"),
                ChecklistItemEntity(20, "task_alpha", 2, "Alpha Checklist 2")
            )
        )
        database.scheduleDao().insertScheduleItem(
            ScheduleItemEntity("sched_a1", "2026-09-22", "task_alpha", 1, "NOW")
        )
        database.progressDao().insertOrUpdateProgress(
            TaskProgressEntity("prog_a1", "task_alpha", "sched_a1", 1, "IN_PROGRESS", 1000L, null)
        )
        database.progressDao().insertAllChecklistProgress(
            listOf(
                ChecklistProgressEntity("task_alpha", 10, true),
                ChecklistProgressEntity("task_alpha", 20, false)
            )
        )
    }

    @Test
    fun exportBackup_createsValidZipWithAllEntitiesAndPhotos() = runTest {
        seedInitialData()

        val outputStream = ByteArrayOutputStream()
        val result = backupRepository.exportBackup(outputStream)

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(1, summary.taskCount)
        assertEquals(2, summary.stepCount)
        assertEquals(1, summary.photoCount)
        assertEquals(2, summary.checklistCount)
        assertEquals(1, summary.scheduleCount)

        // Inspect ZIP structure
        val zipBytes = outputStream.toByteArray()
        val entryNames = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        assertTrue(entryNames.contains("manifest.json"))
        assertTrue(entryNames.contains("payload.json"))
        assertTrue(entryNames.contains("photos/alpha_step_1.jpg"))
    }

    @Test
    fun restoreBackup_restoresAllEntitiesAndReanchorsPhotos() = runTest {
        seedInitialData()

        // 1. Export backup
        val outputStream = ByteArrayOutputStream()
        val exportResult = backupRepository.exportBackup(outputStream)
        assertTrue(exportResult.isSuccess)
        val zipBytes = outputStream.toByteArray()

        // 2. Modify active database to a completely different state
        database.taskDao().deleteAllTasks()
        database.taskStepDao().deleteAllSteps()
        database.checklistDao().deleteAllItems()
        database.scheduleDao().deleteAllScheduleItems()
        database.progressDao().deleteAllTaskProgress()
        database.progressDao().deleteAllChecklistProgress()
        activePhotosDir.listFiles()?.forEach { it.delete() }

        // Insert new dummy data
        database.taskDao().insertTask(
            TaskEntity("task_dummy", "Dummy Task", "Desc", "Help", "Done")
        )

        assertEquals(1, database.taskDao().getAllTasks().size)
        assertEquals("task_dummy", database.taskDao().getAllTasks()[0].id)
        assertEquals(0, activePhotosDir.listFiles()?.size ?: 0)

        // 3. Stage and Restore the backup
        val inputStream = ByteArrayInputStream(zipBytes)
        val stageResult = backupRepository.stageAndValidateBackup(inputStream)
        assertTrue(stageResult.isSuccess)
        val stagedBackup = stageResult.getOrThrow()

        val restoreResult = backupRepository.restoreStagedBackup(stagedBackup)
        assertTrue(restoreResult.isSuccess)

        // 4. Verify all entities restored
        val restoredTasks = database.taskDao().getAllTasks()
        assertEquals(1, restoredTasks.size)
        assertEquals("task_alpha", restoredTasks[0].id)
        assertEquals("Task Alpha", restoredTasks[0].title)

        val restoredSteps = database.taskStepDao().getAllSteps()
        assertEquals(2, restoredSteps.size)
        assertEquals("step_a1", restoredSteps[0].id)
        assertNotNull(restoredSteps[0].imagePath)
        val expectedPhotoFile = File(activePhotosDir, "alpha_step_1.jpg")
        assertEquals(expectedPhotoFile.absolutePath, restoredSteps[0].imagePath)
        assertTrue(expectedPhotoFile.exists())
        assertTrue(expectedPhotoFile.length() > 0)

        val restoredChecklist = database.checklistDao().getAllItems()
        assertEquals(2, restoredChecklist.size)
        assertEquals(10, restoredChecklist[0].id)
        assertEquals("Alpha Checklist 1", restoredChecklist[0].text)

        val restoredSchedule = database.scheduleDao().getAllScheduleItems()
        assertEquals(1, restoredSchedule.size)
        assertEquals("sched_a1", restoredSchedule[0].id)
        assertEquals("NOW", restoredSchedule[0].category)

        val restoredProgress = database.progressDao().getAllProgress()
        assertEquals(1, restoredProgress.size)
        assertEquals("IN_PROGRESS", restoredProgress[0].status)
        assertEquals(1, restoredProgress[0].currentStep)
        assertEquals(1000L, restoredProgress[0].startedAt)

        val restoredChkProgress = database.progressDao().getAllChecklistProgress()
        assertEquals(2, restoredChkProgress.size)
        assertTrue(restoredChkProgress.find { it.checklistItemId == 10 }?.isChecked == true)
        assertEquals(false, restoredChkProgress.find { it.checklistItemId == 20 }?.isChecked)
    }

    @Test
    fun restoreFailure_leavesOriginalDatabaseAndPhotosUntouched() = runTest {
        seedInitialData()

        val initialTasks = database.taskDao().getAllTasks()
        val initialSteps = database.taskStepDao().getAllSteps()
        val initialPhotos = activePhotosDir.listFiles()?.map { it.name } ?: emptyList()

        // Attempt staging of an invalid backup (empty stream)
        val invalidInputStream = ByteArrayInputStream("GARBAGE DATA".toByteArray(Charsets.UTF_8))
        val stageResult = backupRepository.stageAndValidateBackup(invalidInputStream)
        assertTrue(stageResult.isFailure)

        // Database and photos must remain 100% intact
        val currentTasks = database.taskDao().getAllTasks()
        val currentSteps = database.taskStepDao().getAllSteps()
        val currentPhotos = activePhotosDir.listFiles()?.map { it.name } ?: emptyList()

        assertEquals(initialTasks.size, currentTasks.size)
        assertEquals(initialTasks[0].id, currentTasks[0].id)
        assertEquals(initialSteps.size, currentSteps.size)
        assertEquals(initialPhotos, currentPhotos)
    }
}
