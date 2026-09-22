package com.example.learnerapp.staff.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnerapp.LearnerApplication
import com.example.learnerapp.staff.backup.BackupRepository
import com.example.learnerapp.staff.backup.BackupSummary
import com.example.learnerapp.staff.backup.StagedBackup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lifecycle status for backup export and restore operations.
 */
sealed interface BackupUiStatus {
    data object Idle : BackupUiStatus
    data object BackingUp : BackupUiStatus
    data class BackupSuccess(val summary: BackupSummary) : BackupUiStatus
    data object Validating : BackupUiStatus
    data class ConfirmationRequired(val summary: BackupSummary, val stagedBackup: StagedBackup) : BackupUiStatus
    data object Restoring : BackupUiStatus
    data class RestoreSuccess(val summary: BackupSummary) : BackupUiStatus
    data class Error(val message: String) : BackupUiStatus
}

data class BackupRestoreUiState(
    val status: BackupUiStatus = BackupUiStatus.Idle,
    val pendingStagedBackup: StagedBackup? = null,
    val isConfirmDialogOpen: Boolean = false,
    val confirmationSummary: BackupSummary? = null
)

/**
 * ViewModel managing Staff Backup and Restore interactions.
 */
class BackupRestoreViewModel(
    application: Application,
    repository: BackupRepository? = null
) : AndroidViewModel(application) {

    private val backupRepository: BackupRepository =
        repository ?: (application as? LearnerApplication)?.backupRepository
        ?: throw IllegalStateException("LearnerApplication or BackupRepository must be available")

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    /**
     * Creates and streams a backup ZIP archive to the selected SAF URI.
     */
    fun exportBackupToUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupUiStatus.BackingUp) }
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val result = backupRepository.exportBackup(outputStream)
                    result.fold(
                        onSuccess = { summary ->
                            _uiState.update { it.copy(status = BackupUiStatus.BackupSuccess(summary)) }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(status = BackupUiStatus.Error(error.message ?: "Failed to export backup"))
                            }
                        }
                    )
                } ?: run {
                    _uiState.update {
                        it.copy(status = BackupUiStatus.Error("Cannot open destination file for writing"))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(status = BackupUiStatus.Error(e.message ?: "Unexpected export error"))
                }
            }
        }
    }

    /**
     * Reads and stages an incoming backup archive from SAF, validates all integrity constraints,
     * and prompts staff for confirmation before executing the database transaction.
     */
    fun prepareRestoreFromUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupUiStatus.Validating) }
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val result = backupRepository.stageAndValidateBackup(inputStream)
                    result.fold(
                        onSuccess = { stagedBackup ->
                            val summary = BackupSummary(
                                createdAt = stagedBackup.manifest.createdAt,
                                taskCount = stagedBackup.payload.tasks.size,
                                stepCount = stagedBackup.payload.steps.size,
                                photoCount = stagedBackup.stagedPhotosDir.listFiles()?.size ?: 0,
                                checklistCount = stagedBackup.payload.checklistItems.size,
                                scheduleCount = stagedBackup.payload.scheduleItems.size
                            )
                            _uiState.update {
                                it.copy(
                                    status = BackupUiStatus.ConfirmationRequired(summary, stagedBackup),
                                    pendingStagedBackup = stagedBackup,
                                    confirmationSummary = summary,
                                    isConfirmDialogOpen = true
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    status = BackupUiStatus.Error(error.message ?: "Backup validation failed"),
                                    pendingStagedBackup = null,
                                    isConfirmDialogOpen = false
                                )
                            }
                        }
                    )
                } ?: run {
                    _uiState.update {
                        it.copy(status = BackupUiStatus.Error("Cannot open selected backup file for reading"))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(status = BackupUiStatus.Error(e.message ?: "Unexpected validation error"))
                }
            }
        }
    }

    /**
     * Executes the atomic restore of the staged backup into Room and replaces active photos.
     */
    fun confirmRestore() {
        val stagedBackup = _uiState.value.pendingStagedBackup ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConfirmDialogOpen = false,
                    status = BackupUiStatus.Restoring
                )
            }
            val result = backupRepository.restoreStagedBackup(stagedBackup)
            result.fold(
                onSuccess = { summary ->
                    _uiState.update {
                        it.copy(
                            status = BackupUiStatus.RestoreSuccess(summary),
                            pendingStagedBackup = null,
                            confirmationSummary = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            status = BackupUiStatus.Error(error.message ?: "Restore failed"),
                            pendingStagedBackup = null,
                            confirmationSummary = null
                        )
                    }
                }
            )
        }
    }

    /**
     * Cancels pending restore and discards cache staging.
     */
    fun cancelRestore() {
        val stagedBackup = _uiState.value.pendingStagedBackup
        if (stagedBackup != null) {
            viewModelScope.launch {
                backupRepository.cleanStaging(stagedBackup)
            }
        }
        _uiState.update {
            it.copy(
                status = BackupUiStatus.Idle,
                pendingStagedBackup = null,
                confirmationSummary = null,
                isConfirmDialogOpen = false
            )
        }
    }

    /**
     * Clears banner status back to Idle.
     */
    fun clearStatus() {
        _uiState.update { it.copy(status = BackupUiStatus.Idle) }
    }
}
