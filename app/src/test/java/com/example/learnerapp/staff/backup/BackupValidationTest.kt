package com.example.learnerapp.staff.backup

import android.graphics.Bitmap
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.ScheduleItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupValidationTest {

    private lateinit var tempDir: File
    private lateinit var photosDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("val_test", "").apply {
            delete()
            mkdirs()
        }
        photosDir = File(tempDir, "photos").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createValidPhoto(fileName: String) {
        val file = File(photosDir, fileName)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }

    private fun createCorruptPhoto(fileName: String) {
        val file = File(photosDir, fileName)
        file.writeBytes("THIS IS NOT A VALID JPEG IMAGE DATA".toByteArray(Charsets.UTF_8))
    }

    private fun createBasePayload(
        tasks: List<TaskEntity> = listOf(
            TaskEntity("t1", "Task 1", "Desc 1", "Help 1", "Done 1")
        ),
        steps: List<BackupStepDto> = listOf(
            BackupStepDto("s1", "t1", 1, "Step 1", null),
            BackupStepDto("s2", "t1", 2, "Step 2", null)
        ),
        checklistItems: List<ChecklistItemEntity> = listOf(
            ChecklistItemEntity(1, "t1", 1, "Item 1"),
            ChecklistItemEntity(2, "t1", 2, "Item 2")
        ),
        scheduleItems: List<ScheduleItemEntity> = listOf(
            ScheduleItemEntity("sc1", "2026-09-22", "t1", 1, "NOW")
        ),
        taskProgress: List<TaskProgressEntity> = listOf(
            TaskProgressEntity("p1", "t1", "sc1", 1, "NOT_STARTED", null, null)
        ),
        checklistProgress: List<ChecklistProgressEntity> = listOf(
            ChecklistProgressEntity("t1", 1, false)
        )
    ) = BackupPayload(tasks, steps, checklistItems, scheduleItems, taskProgress, checklistProgress)

    @Test
    fun validBackup_validationPassesWithoutException() {
        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 1, photoCount = 0, scheduleCount = 1)
        val payload = createBasePayload()

        BackupValidator.validate(manifest, payload, photosDir)
    }

    @Test
    fun unsupportedFormatVersion_throwsException() {
        val manifest = BackupManifest(formatVersion = 2, createdAt = 1000L, taskCount = 1, photoCount = 0, scheduleCount = 1)
        val payload = createBasePayload()

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun emptyTasks_throwsException() {
        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 0, photoCount = 0, scheduleCount = 0)
        val payload = createBasePayload(tasks = emptyList())

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun duplicateTaskIds_throwsException() {
        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 2, photoCount = 0, scheduleCount = 0)
        val payload = createBasePayload(
            tasks = listOf(
                TaskEntity("t1", "Task 1", "Desc", "Help", "Done"),
                TaskEntity("t1", "Task 1 Duplicate", "Desc", "Help", "Done")
            )
        )

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun orphanedStep_referencesMissingTask_throwsException() {
        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 1, photoCount = 0, scheduleCount = 0)
        val payload = createBasePayload(
            steps = listOf(
                BackupStepDto("s1", "missing_task_id", 1, "Instruction", null)
            )
        )

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun nonContiguousStepNumbers_throwsException() {
        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 1, photoCount = 0, scheduleCount = 0)
        val payload = createBasePayload(
            steps = listOf(
                BackupStepDto("s1", "t1", 1, "Instruction 1", null),
                BackupStepDto("s2", "t1", 3, "Instruction 3 - skipped 2", null)
            )
        )

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun duplicateChecklistItemIds_throwsException() {
        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 1, photoCount = 0, scheduleCount = 0)
        val payload = createBasePayload(
            checklistItems = listOf(
                ChecklistItemEntity(1, "t1", 1, "Item 1"),
                ChecklistItemEntity(1, "t1", 2, "Item 1 Duplicate ID")
            )
        )

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun nonContiguousChecklistOrder_throwsException() {
        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 1, photoCount = 0, scheduleCount = 0)
        val payload = createBasePayload(
            checklistItems = listOf(
                ChecklistItemEntity(1, "t1", 1, "Item 1"),
                ChecklistItemEntity(2, "t1", 3, "Item 2 - skipped order 2")
            )
        )

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun invalidScheduleCategory_throwsException() {
        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 1, photoCount = 0, scheduleCount = 1)
        val payload = createBasePayload(
            scheduleItems = listOf(
                ScheduleItemEntity("sc1", "2026-09-22", "t1", 1, "INVALID_CATEGORY")
            )
        )

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun duplicateTaskProgress_throwsException() {
        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 1, photoCount = 0, scheduleCount = 0)
        val payload = createBasePayload(
            taskProgress = listOf(
                TaskProgressEntity("p1", "t1", "sc1", 1, "NOT_STARTED", null, null),
                TaskProgressEntity("p2", "t1", "sc1", 1, "IN_PROGRESS", null, null)
            )
        )

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun missingPhotoBinary_throwsException() {
        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 1, photoCount = 1, scheduleCount = 0)
        val payload = createBasePayload(
            steps = listOf(
                BackupStepDto("s1", "t1", 1, "Step 1", "non_existent_photo.jpg")
            )
        )

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun corruptPhotoBinary_throwsException() {
        val photoName = "corrupt.jpg"
        createCorruptPhoto(photoName)

        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 1, photoCount = 1, scheduleCount = 0)
        val payload = createBasePayload(
            steps = listOf(
                BackupStepDto("s1", "t1", 1, "Step 1", photoName)
            )
        )

        assertThrows(BackupValidationException::class.java) {
            BackupValidator.validate(manifest, payload, photosDir)
        }
    }

    @Test
    fun validPhotoBinary_validationPasses() {
        val photoName = "valid_step_photo.jpg"
        createValidPhoto(photoName)

        val manifest = BackupManifest(formatVersion = 1, createdAt = 1000L, taskCount = 1, photoCount = 1, scheduleCount = 0)
        val payload = createBasePayload(
            steps = listOf(
                BackupStepDto("s1", "t1", 1, "Step 1", photoName)
            )
        )

        BackupValidator.validate(manifest, payload, photosDir)
    }
}
