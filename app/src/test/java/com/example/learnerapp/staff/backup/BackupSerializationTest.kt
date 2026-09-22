package com.example.learnerapp.staff.backup

import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.ScheduleItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupSerializationTest {

    @Test
    fun manifest_serializationAndParsing_preservesAllFields() {
        val manifest = BackupManifest(
            formatVersion = 1,
            appVersion = "1.0",
            createdAt = 1774358400000L,
            taskCount = 3,
            photoCount = 2,
            scheduleCount = 4
        )

        val jsonString = BackupJsonSerializer.serializeManifest(manifest)
        assertTrue(jsonString.contains("\"formatVersion\": 1"))
        assertTrue(jsonString.contains("\"appVersion\": \"1.0\""))

        val parsed = BackupJsonSerializer.parseManifest(jsonString)
        assertEquals(1, parsed.formatVersion)
        assertEquals("1.0", parsed.appVersion)
        assertEquals(1774358400000L, parsed.createdAt)
        assertEquals(3, parsed.taskCount)
        assertEquals(2, parsed.photoCount)
        assertEquals(4, parsed.scheduleCount)
    }

    @Test
    fun payload_serializationAndParsing_roundTripFidelity() {
        val tasks = listOf(
            TaskEntity("task_1", "Title 1", "Desc 1", "Help 1", "Done 1"),
            TaskEntity("task_2", "Title 2", "Desc 2", "Help 2", "Done 2")
        )
        val steps = listOf(
            BackupStepDto("step_1", "task_1", 1, "Instruction 1", "photo_1.jpg"),
            BackupStepDto("step_2", "task_1", 2, "Instruction 2", null)
        )
        val checklistItems = listOf(
            ChecklistItemEntity(1, "task_1", 1, "Item 1"),
            ChecklistItemEntity(2, "task_1", 2, "Item 2")
        )
        val scheduleItems = listOf(
            ScheduleItemEntity("sched_1", "2026-09-22", "task_1", 1, "NOW"),
            ScheduleItemEntity("sched_2", "2026-09-22", "task_2", 2, "NEXT")
        )
        val taskProgress = listOf(
            TaskProgressEntity("prog_1", "task_1", "sched_1", 2, "IN_PROGRESS", 1000L, null)
        )
        val checklistProgress = listOf(
            ChecklistProgressEntity("task_1", 1, true),
            ChecklistProgressEntity("task_1", 2, false)
        )

        val payload = BackupPayload(
            tasks = tasks,
            steps = steps,
            checklistItems = checklistItems,
            scheduleItems = scheduleItems,
            taskProgress = taskProgress,
            checklistProgress = checklistProgress
        )

        val jsonString = BackupJsonSerializer.serializePayload(payload)
        val parsed = BackupJsonSerializer.parsePayload(jsonString)

        assertEquals(2, parsed.tasks.size)
        assertEquals("task_1", parsed.tasks[0].id)
        assertEquals("Title 1", parsed.tasks[0].title)

        assertEquals(2, parsed.steps.size)
        assertEquals("photo_1.jpg", parsed.steps[0].photoFileName)
        assertNull(parsed.steps[1].photoFileName)

        assertEquals(2, parsed.checklistItems.size)
        assertEquals(1, parsed.checklistItems[0].id)
        assertEquals("Item 1", parsed.checklistItems[0].text)

        assertEquals(2, parsed.scheduleItems.size)
        assertEquals("NOW", parsed.scheduleItems[0].category)
        assertEquals("NEXT", parsed.scheduleItems[1].category)

        assertEquals(1, parsed.taskProgress.size)
        assertEquals(2, parsed.taskProgress[0].currentStep)
        assertEquals("IN_PROGRESS", parsed.taskProgress[0].status)
        assertEquals(1000L, parsed.taskProgress[0].startedAt)
        assertNull(parsed.taskProgress[0].completedAt)

        assertEquals(2, parsed.checklistProgress.size)
        assertTrue(parsed.checklistProgress[0].isChecked)
        assertEquals(false, parsed.checklistProgress[1].isChecked)
    }

    @Test(expected = BackupValidationException::class)
    fun parseManifest_invalidJson_throwsBackupValidationException() {
        BackupJsonSerializer.parseManifest("not valid json")
    }

    @Test(expected = BackupValidationException::class)
    fun parsePayload_missingTasksSection_throwsBackupValidationException() {
        BackupJsonSerializer.parsePayload("{\"steps\": []}")
    }
}
