package com.example.learnerapp.staff.backup

import android.graphics.BitmapFactory
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.ScheduleItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Manifest header describing backup metadata.
 */
data class BackupManifest(
    val formatVersion: Int = 1,
    val appVersion: String = "1.0",
    val createdAt: Long,
    val taskCount: Int,
    val photoCount: Int,
    val scheduleCount: Int
)

/**
 * DTO representing a task step in the backup payload.
 * [photoFileName] stores relative photo name within the archive (e.g., "photo_abc.jpg").
 */
data class BackupStepDto(
    val id: String,
    val taskId: String,
    val stepNumber: Int,
    val instruction: String,
    val photoFileName: String? = null
)

/**
 * Complete operational data payload across all 6 entities.
 */
data class BackupPayload(
    val tasks: List<TaskEntity>,
    val steps: List<BackupStepDto>,
    val checklistItems: List<ChecklistItemEntity>,
    val scheduleItems: List<ScheduleItemEntity>,
    val taskProgress: List<TaskProgressEntity>,
    val checklistProgress: List<ChecklistProgressEntity>
)

/**
 * Staged backup in cache before database restore.
 */
data class StagedBackup(
    val manifest: BackupManifest,
    val payload: BackupPayload,
    val stagedPhotosDir: File,
    val stagingDir: File
)

/**
 * Human-readable summary of backup contents for UI display.
 */
data class BackupSummary(
    val createdAt: Long,
    val taskCount: Int,
    val stepCount: Int,
    val photoCount: Int,
    val checklistCount: Int,
    val scheduleCount: Int
)

/**
 * Specific validation exception thrown when a backup archive violates structural,
 * schema, relational, or integrity constraints.
 */
class BackupValidationException(message: String) : Exception(message)

/**
 * JSON serialization and deserialization utilities for backup manifest and payload.
 * Implemented with [JSONObject] and [JSONArray] for deterministic behavior and zero external dependencies.
 */
object BackupJsonSerializer {

    private const val KEY_FORMAT_VERSION = "formatVersion"
    private const val KEY_APP_VERSION = "appVersion"
    private const val KEY_CREATED_AT = "createdAt"
    private const val KEY_TASK_COUNT = "taskCount"
    private const val KEY_PHOTO_COUNT = "photoCount"
    private const val KEY_SCHEDULE_COUNT = "scheduleCount"

    private const val KEY_TASKS = "tasks"
    private const val KEY_STEPS = "steps"
    private const val KEY_CHECKLIST_ITEMS = "checklistItems"
    private const val KEY_SCHEDULE_ITEMS = "scheduleItems"
    private const val KEY_TASK_PROGRESS = "taskProgress"
    private const val KEY_CHECKLIST_PROGRESS = "checklistProgress"

    fun serializeManifest(manifest: BackupManifest): String {
        val json = JSONObject().apply {
            put(KEY_FORMAT_VERSION, manifest.formatVersion)
            put(KEY_APP_VERSION, manifest.appVersion)
            put(KEY_CREATED_AT, manifest.createdAt)
            put(KEY_TASK_COUNT, manifest.taskCount)
            put(KEY_PHOTO_COUNT, manifest.photoCount)
            put(KEY_SCHEDULE_COUNT, manifest.scheduleCount)
        }
        return json.toString(2)
    }

    fun parseManifest(jsonString: String): BackupManifest {
        try {
            val json = JSONObject(jsonString)
            if (!json.has(KEY_FORMAT_VERSION)) {
                throw BackupValidationException("Manifest missing required field: $KEY_FORMAT_VERSION")
            }
            return BackupManifest(
                formatVersion = json.getInt(KEY_FORMAT_VERSION),
                appVersion = json.optString(KEY_APP_VERSION, "1.0"),
                createdAt = json.optLong(KEY_CREATED_AT, 0L),
                taskCount = json.optInt(KEY_TASK_COUNT, 0),
                photoCount = json.optInt(KEY_PHOTO_COUNT, 0),
                scheduleCount = json.optInt(KEY_SCHEDULE_COUNT, 0)
            )
        } catch (e: Exception) {
            if (e is BackupValidationException) throw e
            throw BackupValidationException("Failed to parse backup manifest: ${e.message}")
        }
    }

    fun serializePayload(payload: BackupPayload): String {
        val root = JSONObject()

        // Tasks
        val tasksArray = JSONArray()
        for (task in payload.tasks) {
            tasksArray.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("helpPhrase", task.helpPhrase)
                put("completionPhrase", task.completionPhrase)
            })
        }
        root.put(KEY_TASKS, tasksArray)

        // Steps
        val stepsArray = JSONArray()
        for (step in payload.steps) {
            stepsArray.put(JSONObject().apply {
                put("id", step.id)
                put("taskId", step.taskId)
                put("stepNumber", step.stepNumber)
                put("instruction", step.instruction)
                if (step.photoFileName != null) {
                    put("photoFileName", step.photoFileName)
                } else {
                    put("photoFileName", JSONObject.NULL)
                }
            })
        }
        root.put(KEY_STEPS, stepsArray)

        // Checklist Items
        val checklistArray = JSONArray()
        for (item in payload.checklistItems) {
            checklistArray.put(JSONObject().apply {
                put("id", item.id)
                put("taskId", item.taskId)
                put("order", item.order)
                put("text", item.text)
            })
        }
        root.put(KEY_CHECKLIST_ITEMS, checklistArray)

        // Schedule Items
        val scheduleArray = JSONArray()
        for (item in payload.scheduleItems) {
            scheduleArray.put(JSONObject().apply {
                put("id", item.id)
                put("date", item.date)
                put("taskId", item.taskId)
                put("order", item.order)
                put("category", item.category)
            })
        }
        root.put(KEY_SCHEDULE_ITEMS, scheduleArray)

        // Task Progress
        val progressArray = JSONArray()
        for (prog in payload.taskProgress) {
            progressArray.put(JSONObject().apply {
                put("id", prog.id)
                put("taskId", prog.taskId)
                if (prog.scheduleItemId != null) {
                    put("scheduleItemId", prog.scheduleItemId)
                } else {
                    put("scheduleItemId", JSONObject.NULL)
                }
                put("currentStep", prog.currentStep)
                put("status", prog.status)
                if (prog.startedAt != null) {
                    put("startedAt", prog.startedAt)
                } else {
                    put("startedAt", JSONObject.NULL)
                }
                if (prog.completedAt != null) {
                    put("completedAt", prog.completedAt)
                } else {
                    put("completedAt", JSONObject.NULL)
                }
            })
        }
        root.put(KEY_TASK_PROGRESS, progressArray)

        // Checklist Progress
        val chkProgressArray = JSONArray()
        for (chkProg in payload.checklistProgress) {
            chkProgressArray.put(JSONObject().apply {
                put("taskId", chkProg.taskId)
                put("checklistItemId", chkProg.checklistItemId)
                put("isChecked", chkProg.isChecked)
            })
        }
        root.put(KEY_CHECKLIST_PROGRESS, chkProgressArray)

        return root.toString(2)
    }

    fun parsePayload(jsonString: String): BackupPayload {
        try {
            val root = JSONObject(jsonString)

            if (!root.has(KEY_TASKS)) {
                throw BackupValidationException("Payload missing required section: $KEY_TASKS")
            }

            // Parse Tasks
            val tasks = mutableListOf<TaskEntity>()
            val tasksArray = root.getJSONArray(KEY_TASKS)
            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                tasks.add(
                    TaskEntity(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        helpPhrase = obj.optString("helpPhrase", ""),
                        completionPhrase = obj.optString("completionPhrase", "")
                    )
                )
            }

            // Parse Steps
            val steps = mutableListOf<BackupStepDto>()
            val stepsArray = root.optJSONArray(KEY_STEPS) ?: JSONArray()
            for (i in 0 until stepsArray.length()) {
                val obj = stepsArray.getJSONObject(i)
                val photoFileName = if (!obj.isNull("photoFileName")) obj.optString("photoFileName") else null
                steps.add(
                    BackupStepDto(
                        id = obj.getString("id"),
                        taskId = obj.getString("taskId"),
                        stepNumber = obj.getInt("stepNumber"),
                        instruction = obj.getString("instruction"),
                        photoFileName = if (photoFileName.isNullOrBlank()) null else photoFileName
                    )
                )
            }

            // Parse Checklist Items
            val checklistItems = mutableListOf<ChecklistItemEntity>()
            val checklistArray = root.optJSONArray(KEY_CHECKLIST_ITEMS) ?: JSONArray()
            for (i in 0 until checklistArray.length()) {
                val obj = checklistArray.getJSONObject(i)
                checklistItems.add(
                    ChecklistItemEntity(
                        id = obj.getInt("id"),
                        taskId = obj.getString("taskId"),
                        order = obj.getInt("order"),
                        text = obj.getString("text")
                    )
                )
            }

            // Parse Schedule Items
            val scheduleItems = mutableListOf<ScheduleItemEntity>()
            val scheduleArray = root.optJSONArray(KEY_SCHEDULE_ITEMS) ?: JSONArray()
            for (i in 0 until scheduleArray.length()) {
                val obj = scheduleArray.getJSONObject(i)
                scheduleItems.add(
                    ScheduleItemEntity(
                        id = obj.getString("id"),
                        date = obj.getString("date"),
                        taskId = obj.getString("taskId"),
                        order = obj.getInt("order"),
                        category = obj.getString("category")
                    )
                )
            }

            // Parse Task Progress
            val taskProgress = mutableListOf<TaskProgressEntity>()
            val progressArray = root.optJSONArray(KEY_TASK_PROGRESS) ?: JSONArray()
            for (i in 0 until progressArray.length()) {
                val obj = progressArray.getJSONObject(i)
                val scheduleItemId = if (!obj.isNull("scheduleItemId")) obj.optString("scheduleItemId") else null
                val startedAt = if (!obj.isNull("startedAt")) obj.optLong("startedAt") else null
                val completedAt = if (!obj.isNull("completedAt")) obj.optLong("completedAt") else null
                taskProgress.add(
                    TaskProgressEntity(
                        id = obj.getString("id"),
                        taskId = obj.getString("taskId"),
                        scheduleItemId = scheduleItemId,
                        currentStep = obj.optInt("currentStep", 1),
                        status = obj.optString("status", "NOT_STARTED"),
                        startedAt = startedAt,
                        completedAt = completedAt
                    )
                )
            }

            // Parse Checklist Progress
            val checklistProgress = mutableListOf<ChecklistProgressEntity>()
            val chkProgressArray = root.optJSONArray(KEY_CHECKLIST_PROGRESS) ?: JSONArray()
            for (i in 0 until chkProgressArray.length()) {
                val obj = chkProgressArray.getJSONObject(i)
                checklistProgress.add(
                    ChecklistProgressEntity(
                        taskId = obj.getString("taskId"),
                        checklistItemId = obj.getInt("checklistItemId"),
                        isChecked = obj.optBoolean("isChecked", false)
                    )
                )
            }

            return BackupPayload(
                tasks = tasks,
                steps = steps,
                checklistItems = checklistItems,
                scheduleItems = scheduleItems,
                taskProgress = taskProgress,
                checklistProgress = checklistProgress
            )
        } catch (e: Exception) {
            if (e is BackupValidationException) throw e
            throw BackupValidationException("Failed to parse backup payload: ${e.message}")
        }
    }
}

/**
 * Strict validator for backup manifest, operational payload, relational constraints,
 * and photo integrity.
 */
object BackupValidator {

    private const val SUPPORTED_FORMAT_VERSION = 1
    private val VALID_STATUSES = setOf("NOT_STARTED", "IN_PROGRESS", "CHECKING", "COMPLETED")
    private val VALID_CATEGORIES = setOf("NOW", "NEXT", "LATER")

    /**
     * Validates that the parsed manifest, payload, and staged photos satisfy all integrity rules.
     * Throws [BackupValidationException] on the first rule violation.
     */
    fun validate(manifest: BackupManifest, payload: BackupPayload, stagedPhotosDir: File) {
        // 1. Version check
        if (manifest.formatVersion != SUPPORTED_FORMAT_VERSION) {
            throw BackupValidationException(
                "Unsupported backup format version: ${manifest.formatVersion}. Expected: $SUPPORTED_FORMAT_VERSION"
            )
        }

        if (manifest.createdAt <= 0L) {
            throw BackupValidationException("Invalid backup timestamp in manifest: ${manifest.createdAt}")
        }

        // 2. Tasks validation
        if (payload.tasks.isEmpty()) {
            throw BackupValidationException("Backup contains no task templates. At least one task template is required.")
        }

        val taskIds = mutableSetOf<String>()
        for (task in payload.tasks) {
            if (task.id.isBlank()) {
                throw BackupValidationException("Task template has blank or empty ID.")
            }
            if (task.title.isBlank()) {
                throw BackupValidationException("Task template '${task.id}' has blank title.")
            }
            if (!taskIds.add(task.id)) {
                throw BackupValidationException("Duplicate task template ID found: '${task.id}'")
            }
        }

        // 3. Steps validation
        val stepIds = mutableSetOf<String>()
        val stepsByTask = payload.steps.groupBy { it.taskId }

        for (step in payload.steps) {
            if (step.id.isBlank()) {
                throw BackupValidationException("Task step has blank or empty ID.")
            }
            if (!stepIds.add(step.id)) {
                throw BackupValidationException("Duplicate task step ID found: '${step.id}'")
            }
            if (!taskIds.contains(step.taskId)) {
                throw BackupValidationException(
                    "Step '${step.id}' references non-existent task template '${step.taskId}'"
                )
            }
            if (step.stepNumber < 1) {
                throw BackupValidationException(
                    "Step '${step.id}' has invalid stepNumber ${step.stepNumber} (must be >= 1)"
                )
            }
        }

        // Enforce contiguous step numbers 1..N for each task with steps
        for ((taskId, steps) in stepsByTask) {
            val sortedSteps = steps.sortedBy { it.stepNumber }
            for (index in sortedSteps.indices) {
                val expectedNumber = index + 1
                if (sortedSteps[index].stepNumber != expectedNumber) {
                    throw BackupValidationException(
                        "Task '$taskId' has non-contiguous step numbers: expected $expectedNumber but found ${sortedSteps[index].stepNumber}"
                    )
                }
            }
        }

        // 4. Checklist items validation
        val checklistIds = mutableSetOf<Int>()
        val itemsByTask = payload.checklistItems.groupBy { it.taskId }

        for (item in payload.checklistItems) {
            if (item.id <= 0) {
                throw BackupValidationException("Checklist item has invalid non-positive ID: ${item.id}")
            }
            if (!checklistIds.add(item.id)) {
                throw BackupValidationException("Duplicate checklist item ID found: ${item.id}")
            }
            if (!taskIds.contains(item.taskId)) {
                throw BackupValidationException(
                    "Checklist item ${item.id} references non-existent task template '${item.taskId}'"
                )
            }
            if (item.order < 1) {
                throw BackupValidationException(
                    "Checklist item ${item.id} has invalid order ${item.order} (must be >= 1)"
                )
            }
            if (item.text.isBlank()) {
                throw BackupValidationException("Checklist item ${item.id} has blank text.")
            }
        }

        // Enforce contiguous checklist order 1..N for each task with checklist items
        for ((taskId, items) in itemsByTask) {
            val sortedItems = items.sortedBy { it.order }
            for (index in sortedItems.indices) {
                val expectedOrder = index + 1
                if (sortedItems[index].order != expectedOrder) {
                    throw BackupValidationException(
                        "Task '$taskId' has non-contiguous checklist order: expected $expectedOrder but found ${sortedItems[index].order}"
                    )
                }
            }
        }

        // 5. Schedule items validation
        val scheduleIds = mutableSetOf<String>()
        for (item in payload.scheduleItems) {
            if (item.id.isBlank()) {
                throw BackupValidationException("Schedule item has blank or empty ID.")
            }
            if (!scheduleIds.add(item.id)) {
                throw BackupValidationException("Duplicate schedule item ID found: '${item.id}'")
            }
            if (!taskIds.contains(item.taskId)) {
                throw BackupValidationException(
                    "Schedule item '${item.id}' references non-existent task template '${item.taskId}'"
                )
            }
            if (item.order < 1) {
                throw BackupValidationException(
                    "Schedule item '${item.id}' has invalid order ${item.order} (must be >= 1)"
                )
            }
            if (item.category !in VALID_CATEGORIES) {
                throw BackupValidationException(
                    "Schedule item '${item.id}' has invalid category '${item.category}'. Allowed: $VALID_CATEGORIES"
                )
            }
            if (item.date.isBlank()) {
                throw BackupValidationException("Schedule item '${item.id}' has blank date.")
            }
        }

        // 6. Task progress validation
        val progressIds = mutableSetOf<String>()
        val progressTaskIds = mutableSetOf<String>()
        for (prog in payload.taskProgress) {
            if (prog.id.isBlank()) {
                throw BackupValidationException("Task progress has blank or empty ID.")
            }
            if (!progressIds.add(prog.id)) {
                throw BackupValidationException("Duplicate task progress ID found: '${prog.id}'")
            }
            if (!taskIds.contains(prog.taskId)) {
                throw BackupValidationException(
                    "Task progress '${prog.id}' references non-existent task template '${prog.taskId}'"
                )
            }
            if (!progressTaskIds.add(prog.taskId)) {
                throw BackupValidationException(
                    "Duplicate progress record found for task '${prog.taskId}'"
                )
            }
            if (prog.status !in VALID_STATUSES) {
                throw BackupValidationException(
                    "Task progress '${prog.id}' has invalid status '${prog.status}'. Allowed: $VALID_STATUSES"
                )
            }
            if (prog.currentStep < 1) {
                throw BackupValidationException(
                    "Task progress '${prog.id}' has invalid currentStep ${prog.currentStep} (must be >= 1)"
                )
            }
            if (prog.scheduleItemId != null && !scheduleIds.contains(prog.scheduleItemId)) {
                throw BackupValidationException(
                    "Task progress '${prog.id}' references non-existent schedule item '${prog.scheduleItemId}'"
                )
            }
        }

        // 7. Checklist progress validation
        val checklistProgressPairs = mutableSetOf<Pair<String, Int>>()
        for (chkProg in payload.checklistProgress) {
            if (!taskIds.contains(chkProg.taskId)) {
                throw BackupValidationException(
                    "Checklist progress references non-existent task template '${chkProg.taskId}'"
                )
            }
            if (!checklistIds.contains(chkProg.checklistItemId)) {
                throw BackupValidationException(
                    "Checklist progress references non-existent checklist item ID ${chkProg.checklistItemId}"
                )
            }
            val pair = Pair(chkProg.taskId, chkProg.checklistItemId)
            if (!checklistProgressPairs.add(pair)) {
                throw BackupValidationException(
                    "Duplicate checklist progress record for task '${chkProg.taskId}' and item ${chkProg.checklistItemId}"
                )
            }
        }

        // 8. Photo validation
        for (step in payload.steps) {
            val photoName = step.photoFileName
            if (!photoName.isNullOrBlank()) {
                val photoFile = File(stagedPhotosDir, photoName)
                if (!photoFile.exists() || !photoFile.isFile) {
                    throw BackupValidationException(
                        "Missing photo file '$photoName' required by step '${step.id}' in task '${step.taskId}'"
                    )
                }
                if (photoFile.length() <= 0L) {
                    throw BackupValidationException(
                        "Photo file '$photoName' is empty (0 bytes) for step '${step.id}'"
                    )
                }

                // Verify image header format and decodability
                if (!isValidImageFile(photoFile)) {
                    throw BackupValidationException(
                        "Photo file '$photoName' is corrupted or not a decodable image format for step '${step.id}'"
                    )
                }
            }
        }
    }

    private fun isValidImageFile(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() < 4) return false
        val header = ByteArray(12)
        val bytesRead = try {
            file.inputStream().use { it.read(header) }
        } catch (e: Exception) {
            return false
        }
        if (bytesRead < 4) return false

        // JPEG signature: FF D8 FF
        val isJpeg = (header[0].toInt() and 0xFF == 0xFF) &&
                (header[1].toInt() and 0xFF == 0xD8) &&
                (header[2].toInt() and 0xFF == 0xFF)

        // PNG signature: 89 50 4E 47
        val isPng = (header[0].toInt() and 0xFF == 0x89) &&
                (header[1].toInt() and 0xFF == 0x50) &&
                (header[2].toInt() and 0xFF == 0x4E) &&
                (header[3].toInt() and 0xFF == 0x47)

        // WebP signature: RIFF ... WEBP
        val isWebp = bytesRead >= 12 &&
                header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
                header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
                header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() &&
                header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte()

        if (!isJpeg && !isPng && !isWebp) return false

        // Also attempt BitmapFactory bounds decode
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }
}
