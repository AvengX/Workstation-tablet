package com.example.learnerapp.staff.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.local.entities.TaskStepEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Repository interface for creating, validating, staging, and restoring workstation backups.
 */
interface BackupRepository {
    suspend fun exportBackup(outputStream: OutputStream): Result<BackupSummary>
    suspend fun stageAndValidateBackup(inputStream: InputStream): Result<StagedBackup>
    suspend fun restoreStagedBackup(stagedBackup: StagedBackup): Result<BackupSummary>
    suspend fun cleanStaging(stagedBackup: StagedBackup)
}

/**
 * Room-backed implementation of [BackupRepository].
 * Ensures atomic transactions and filesystem rollback protection.
 */
class RoomBackupRepository(
    private val database: AppDatabase,
    private val context: Context
) : BackupRepository {

    private val photosDir: File
        get() = File(context.filesDir, "task_photos").apply { if (!exists()) mkdirs() }

    override suspend fun exportBackup(outputStream: OutputStream): Result<BackupSummary> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Query all entities from Room
                val tasks = database.taskDao().getAllTasks()
                val steps = database.taskStepDao().getAllSteps()
                val checklistItems = database.checklistDao().getAllItems()
                val scheduleItems = database.scheduleDao().getAllScheduleItems()
                val taskProgress = database.progressDao().getAllProgress()
                val checklistProgress = database.progressDao().getAllChecklistProgress()

                // 2. Identify referenced photos and construct Step DTOs
                val photoFilesToExport = mutableMapOf<String, File>()
                val stepDtos = steps.map { step ->
                    var photoFileName: String? = null
                    if (!step.imagePath.isNullOrBlank()) {
                        val photoFile = File(step.imagePath)
                        if (photoFile.exists() && photoFile.isFile && photoFile.canRead()) {
                            photoFileName = photoFile.name
                            photoFilesToExport[photoFileName] = photoFile
                        }
                    }
                    BackupStepDto(
                        id = step.id,
                        taskId = step.taskId,
                        stepNumber = step.stepNumber,
                        instruction = step.instruction,
                        photoFileName = photoFileName
                    )
                }

                val manifest = BackupManifest(
                    formatVersion = 1,
                    appVersion = "1.0",
                    createdAt = System.currentTimeMillis(),
                    taskCount = tasks.size,
                    photoCount = photoFilesToExport.size,
                    scheduleCount = scheduleItems.size
                )

                val payload = BackupPayload(
                    tasks = tasks,
                    steps = stepDtos,
                    checklistItems = checklistItems,
                    scheduleItems = scheduleItems,
                    taskProgress = taskProgress,
                    checklistProgress = checklistProgress
                )

                // 3. Serialize manifest and payload
                val manifestJson = BackupJsonSerializer.serializeManifest(manifest)
                val payloadJson = BackupJsonSerializer.serializePayload(payload)

                // 4. Stream into ZIP archive
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                    // Entry: manifest.json
                    zipOut.putNextEntry(ZipEntry("manifest.json"))
                    zipOut.write(manifestJson.toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()

                    // Entry: payload.json
                    zipOut.putNextEntry(ZipEntry("payload.json"))
                    zipOut.write(payloadJson.toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()

                    // Entries: photos/*
                    for ((fileName, file) in photoFilesToExport) {
                        zipOut.putNextEntry(ZipEntry("photos/$fileName"))
                        FileInputStream(file).use { input ->
                            input.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                    zipOut.finish()
                }

                Result.success(
                    BackupSummary(
                        createdAt = manifest.createdAt,
                        taskCount = tasks.size,
                        stepCount = stepDtos.size,
                        photoCount = photoFilesToExport.size,
                        checklistCount = checklistItems.size,
                        scheduleCount = scheduleItems.size
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    override suspend fun stageAndValidateBackup(inputStream: InputStream): Result<StagedBackup> =
        withContext(Dispatchers.IO) {
            val stagingDir = File(
                context.cacheDir,
                "backup_staging_${UUID.randomUUID().toString().replace("-", "").take(8)}"
            )
            val stagedPhotosDir = File(stagingDir, "photos")

            try {
                if (!stagingDir.mkdirs()) {
                    throw BackupValidationException("Unable to create cache staging directory: ${stagingDir.absolutePath}")
                }
                stagedPhotosDir.mkdirs()

                // Extract ZIP into staging directory
                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        // Prevent path traversal attacks
                        val targetFile = File(stagingDir, entry.name)
                        val canonicalDest = targetFile.canonicalPath
                        if (!canonicalDest.startsWith(stagingDir.canonicalPath)) {
                            throw BackupValidationException("Invalid entry path in archive: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            targetFile.mkdirs()
                        } else {
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { out ->
                                zipIn.copyTo(out)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }

                // Verify mandatory archive contents
                val manifestFile = File(stagingDir, "manifest.json")
                if (!manifestFile.exists() || !manifestFile.isFile) {
                    throw BackupValidationException("Archive is missing required 'manifest.json'")
                }

                val payloadFile = File(stagingDir, "payload.json")
                if (!payloadFile.exists() || !payloadFile.isFile) {
                    throw BackupValidationException("Archive is missing required 'payload.json'")
                }

                // Parse and validate
                val manifest = BackupJsonSerializer.parseManifest(manifestFile.readText(Charsets.UTF_8))
                val payload = BackupJsonSerializer.parsePayload(payloadFile.readText(Charsets.UTF_8))

                BackupValidator.validate(manifest, payload, stagedPhotosDir)

                Result.success(
                    StagedBackup(
                        manifest = manifest,
                        payload = payload,
                        stagedPhotosDir = stagedPhotosDir,
                        stagingDir = stagingDir
                    )
                )
            } catch (e: Exception) {
                // Ensure failed staging leaves no residual files
                stagingDir.deleteRecursively()
                Result.failure(e)
            }
        }

    override suspend fun restoreStagedBackup(stagedBackup: StagedBackup): Result<BackupSummary> =
        withContext(Dispatchers.IO) {
            val rollbackPhotosDir = File(
                context.cacheDir,
                "photos_rollback_${UUID.randomUUID().toString().replace("-", "").take(8)}"
            )

            try {
                // 1. Stage existing active photos into rollback directory for atomic safety
                rollbackPhotosDir.mkdirs()
                val currentPhotoFiles = photosDir.listFiles() ?: emptyArray()
                for (file in currentPhotoFiles) {
                    if (file.isFile) {
                        file.copyTo(File(rollbackPhotosDir, file.name), overwrite = true)
                    }
                }

                // 2. Perform database restore inside a Room transaction
                database.withTransaction {
                    // Child -> Parent deletion
                    database.progressDao().deleteAllChecklistProgress()
                    database.progressDao().deleteAllTaskProgress()
                    database.scheduleDao().deleteAllScheduleItems()
                    database.checklistDao().deleteAllItems()
                    database.taskStepDao().deleteAllSteps()
                    database.taskDao().deleteAllTasks()

                    // Parent -> Child insertion
                    database.taskDao().insertTasks(stagedBackup.payload.tasks)

                    // Re-anchor photo paths to app-private storage on current device
                    val finalSteps = stagedBackup.payload.steps.map { dto ->
                        val finalImagePath = if (!dto.photoFileName.isNullOrBlank()) {
                            File(photosDir, dto.photoFileName).absolutePath
                        } else {
                            null
                        }
                        TaskStepEntity(
                            id = dto.id,
                            taskId = dto.taskId,
                            stepNumber = dto.stepNumber,
                            instruction = dto.instruction,
                            imagePath = finalImagePath
                        )
                    }
                    database.taskStepDao().insertSteps(finalSteps)
                    database.checklistDao().insertItems(stagedBackup.payload.checklistItems)
                    database.scheduleDao().insertScheduleItems(stagedBackup.payload.scheduleItems)
                    database.progressDao().insertAllProgress(stagedBackup.payload.taskProgress)
                    database.progressDao().insertAllChecklistProgress(stagedBackup.payload.checklistProgress)
                }

                // 3. Atomically replace active photos with staged photos
                photosDir.listFiles()?.forEach { it.delete() }
                stagedBackup.stagedPhotosDir.listFiles()?.forEach { photo ->
                    if (photo.isFile) {
                        photo.copyTo(File(photosDir, photo.name), overwrite = true)
                    }
                }

                // 4. Clean up rollback and staging
                rollbackPhotosDir.deleteRecursively()
                stagedBackup.stagingDir.deleteRecursively()

                Result.success(
                    BackupSummary(
                        createdAt = stagedBackup.manifest.createdAt,
                        taskCount = stagedBackup.payload.tasks.size,
                        stepCount = stagedBackup.payload.steps.size,
                        photoCount = stagedBackup.stagedPhotosDir.listFiles()?.size ?: 0,
                        checklistCount = stagedBackup.payload.checklistItems.size,
                        scheduleCount = stagedBackup.payload.scheduleItems.size
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // Rollback photos
                try {
                    photosDir.listFiles()?.forEach { it.delete() }
                    rollbackPhotosDir.listFiles()?.forEach { photo ->
                        if (photo.isFile) {
                            photo.copyTo(File(photosDir, photo.name), overwrite = true)
                        }
                    }
                } catch (rollbackEx: Exception) {
                    rollbackEx.printStackTrace()
                } finally {
                    rollbackPhotosDir.deleteRecursively()
                    stagedBackup.stagingDir.deleteRecursively()
                }
                Result.failure(e)
            }
        }

    override suspend fun cleanStaging(stagedBackup: StagedBackup) {
        withContext(Dispatchers.IO) {
            try {
                stagedBackup.stagingDir.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
