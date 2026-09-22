package com.example.learnerapp.staff.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.staff.backup.BackupManifest
import com.example.learnerapp.staff.backup.BackupPayload
import com.example.learnerapp.staff.backup.BackupRepository
import com.example.learnerapp.staff.backup.BackupSummary
import com.example.learnerapp.staff.backup.StagedBackup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream
import java.io.OutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreViewModelTest {

    private lateinit var application: Application
    private lateinit var fakeRepository: FakeBackupRepository
    private lateinit var viewModel: BackupRestoreViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        fakeRepository = FakeBackupRepository()
        viewModel = BackupRestoreViewModel(application, fakeRepository)
    }

    @Test
    fun initialState_isIdle() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.status is BackupUiStatus.Idle)
        assertFalse(state.isConfirmDialogOpen)
        assertNull(state.pendingStagedBackup)
    }

    @Test
    fun prepareRestore_success_opensConfirmationDialog() = runTest {
        val dummyUri = Uri.parse("content://dummy/backup.zip")
        val contentResolver = application.contentResolver

        fakeRepository.stageResult = Result.success(createFakeStagedBackup())

        viewModel.prepareRestoreFromUri(dummyUri, contentResolver)

        val state = viewModel.uiState.value
        assertTrue(state.status is BackupUiStatus.ConfirmationRequired)
        assertTrue(state.isConfirmDialogOpen)
        assertEquals(2, state.confirmationSummary?.taskCount)
        assertEquals(3, state.confirmationSummary?.scheduleCount)
    }

    @Test
    fun confirmRestore_executesRestoreAndSetsSuccess() = runTest {
        val dummyUri = Uri.parse("content://dummy/backup.zip")
        val contentResolver = application.contentResolver

        fakeRepository.stageResult = Result.success(createFakeStagedBackup())
        fakeRepository.restoreResult = Result.success(
            BackupSummary(1000L, 2, 4, 1, 3, 3)
        )

        viewModel.prepareRestoreFromUri(dummyUri, contentResolver)
        viewModel.confirmRestore()

        val state = viewModel.uiState.value
        assertTrue(state.status is BackupUiStatus.RestoreSuccess)
        assertFalse(state.isConfirmDialogOpen)
        assertNull(state.pendingStagedBackup)
    }

    @Test
    fun cancelRestore_cleansStagingAndReturnsToIdle() = runTest {
        val dummyUri = Uri.parse("content://dummy/backup.zip")
        val contentResolver = application.contentResolver

        fakeRepository.stageResult = Result.success(createFakeStagedBackup())

        viewModel.prepareRestoreFromUri(dummyUri, contentResolver)
        assertTrue(viewModel.uiState.value.isConfirmDialogOpen)

        viewModel.cancelRestore()

        val state = viewModel.uiState.value
        assertTrue(state.status is BackupUiStatus.Idle)
        assertFalse(state.isConfirmDialogOpen)
        assertNull(state.pendingStagedBackup)
        assertTrue(fakeRepository.cleanStagingCalled)
    }

    @Test
    fun prepareRestore_failure_setsErrorStatus() = runTest {
        val dummyUri = Uri.parse("content://dummy/backup.zip")
        val contentResolver = application.contentResolver

        fakeRepository.stageResult = Result.failure(Exception("Corrupt backup archive"))

        viewModel.prepareRestoreFromUri(dummyUri, contentResolver)

        val state = viewModel.uiState.value
        assertTrue(state.status is BackupUiStatus.Error)
        assertEquals("Corrupt backup archive", (state.status as BackupUiStatus.Error).message)
        assertFalse(state.isConfirmDialogOpen)
    }

    private fun createFakeStagedBackup() = StagedBackup(
        manifest = BackupManifest(1, "1.0", 1000L, 2, 1, 3),
        payload = BackupPayload(
            tasks = listOf(
                com.example.learnerapp.data.local.entities.TaskEntity("t1", "Task 1", "", "", ""),
                com.example.learnerapp.data.local.entities.TaskEntity("t2", "Task 2", "", "", "")
            ),
            steps = emptyList(),
            checklistItems = emptyList(),
            scheduleItems = listOf(
                com.example.learnerapp.data.local.entities.ScheduleItemEntity("s1", "2026-09-22", "t1", 1, "NOW"),
                com.example.learnerapp.data.local.entities.ScheduleItemEntity("s2", "2026-09-22", "t2", 2, "NEXT"),
                com.example.learnerapp.data.local.entities.ScheduleItemEntity("s3", "2026-09-22", "t2", 3, "LATER")
            ),
            taskProgress = emptyList(),
            checklistProgress = emptyList()
        ),
        stagedPhotosDir = File(application.cacheDir, "test_photos"),
        stagingDir = File(application.cacheDir, "test_staging")
    )

    private class FakeBackupRepository : BackupRepository {
        var stageResult: Result<StagedBackup> = Result.failure(Exception("Not set"))
        var restoreResult: Result<BackupSummary> = Result.failure(Exception("Not set"))
        var exportResult: Result<BackupSummary> = Result.failure(Exception("Not set"))
        var cleanStagingCalled: Boolean = false

        override suspend fun exportBackup(outputStream: OutputStream): Result<BackupSummary> = exportResult

        override suspend fun stageAndValidateBackup(inputStream: InputStream): Result<StagedBackup> = stageResult

        override suspend fun restoreStagedBackup(stagedBackup: StagedBackup): Result<BackupSummary> = restoreResult

        override suspend fun cleanStaging(stagedBackup: StagedBackup) {
            cleanStagingCalled = true
        }
    }
}
