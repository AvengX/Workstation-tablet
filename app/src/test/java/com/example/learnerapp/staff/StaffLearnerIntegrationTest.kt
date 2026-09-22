package com.example.learnerapp.staff

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learnerapp.data.local.AppDatabase
import com.example.learnerapp.data.repository.RoomLearnerRepository
import com.example.learnerapp.staff.repository.InMemoryStaffRepository
import com.example.learnerapp.staff.viewmodel.StaffViewModel
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

/**
 * Integration tests ensuring complete isolation between Staff authentication / dashboard
 * and Room learner task persistence across session transitions and app restarts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StaffLearnerIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var learnerRepository: RoomLearnerRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        learnerRepository = RoomLearnerRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun learnerProgress_isPreserved_whenEnteringAndExitingStaffMode() = runTest {
        // 1. Initialize learner workflow and make progress in Room
        learnerRepository.seedIfEmpty()
        learnerRepository.startTask("pack_one_parcel")
        learnerRepository.updateCurrentStep("pack_one_parcel", 3)

        // Verify progress before staff authentication
        val savedProgressBeforeStaff = database.progressDao().getProgressForTask("pack_one_parcel")
        assertNotNull(savedProgressBeforeStaff)
        assertEquals(3, savedProgressBeforeStaff?.currentStep)
        assertEquals("IN_PROGRESS", savedProgressBeforeStaff?.status)

        // 2. Staff enters PIN and accesses Staff Dashboard
        val staffRepo = InMemoryStaffRepository()
        val staffVm = StaffViewModel(staffRepo)

        staffVm.onDigitEntered('1')
        staffVm.onDigitEntered('2')
        staffVm.onDigitEntered('3')
        staffVm.onDigitEntered('4')

        assertTrue(staffVm.uiState.value.isAuthenticated)

        // 3. Staff exits Staff Mode
        staffVm.openExitDialog(true)
        assertTrue(staffVm.uiState.value.isExitDialogOpen)
        staffVm.logout()

        assertFalse(staffVm.uiState.value.isAuthenticated)
        assertFalse(staffVm.uiState.value.isExitDialogOpen)

        // 4. Verify learner progress in Room is completely untouched
        val savedProgressAfterStaff = database.progressDao().getProgressForTask("pack_one_parcel")
        assertNotNull(savedProgressAfterStaff)
        assertEquals(3, savedProgressAfterStaff?.currentStep)
        assertEquals("IN_PROGRESS", savedProgressAfterStaff?.status)
    }

    @Test
    fun appRestart_clearsStaffSession_andRestoresLearnerRoomProgress() = runTest {
        // 1. Initial run: learner completes checklist items in Room
        learnerRepository.seedIfEmpty()
        learnerRepository.startTask("pack_one_parcel")
        learnerRepository.moveToChecking("pack_one_parcel")
        learnerRepository.toggleChecklistItem("pack_one_parcel", 1)
        learnerRepository.toggleChecklistItem("pack_one_parcel", 2)

        // Staff authenticates during this run
        val staffRepo1 = InMemoryStaffRepository()
        val staffVm1 = StaffViewModel(staffRepo1)
        staffVm1.onDigitEntered('1')
        staffVm1.onDigitEntered('2')
        staffVm1.onDigitEntered('3')
        staffVm1.onDigitEntered('4')
        assertTrue(staffVm1.uiState.value.isAuthenticated)

        // 2. Simulate App Process Restart
        // Fresh StaffViewModel created, Room database survives
        val staffRepo2 = InMemoryStaffRepository()
        val staffVm2 = StaffViewModel(staffRepo2)

        // Staff session MUST NOT persist across app restart (starts unauthenticated)
        assertFalse(staffVm2.uiState.value.isAuthenticated)
        assertEquals("", staffVm2.uiState.value.enteredPin)

        // Learner session MUST restore persistent progress from Room
        val progress = learnerRepository.getTaskProgress("pack_one_parcel").first()
        assertNotNull(progress)
        assertEquals("CHECKING", progress?.status)

        val checklistItems = learnerRepository.getChecklistWithProgress("pack_one_parcel").first()
        assertEquals(4, checklistItems.size)
        assertTrue(checklistItems.find { it.id == 1 }?.isChecked == true)
        assertTrue(checklistItems.find { it.id == 2 }?.isChecked == true)
        assertFalse(checklistItems.find { it.id == 3 }?.isChecked == true)
        assertFalse(checklistItems.find { it.id == 4 }?.isChecked == true)
    }

    @Test
    fun failedPinAttempts_doNotAlterLearnerDatabase() = runTest {
        learnerRepository.seedIfEmpty()
        learnerRepository.startTask("pack_one_parcel")
        learnerRepository.updateCurrentStep("pack_one_parcel", 2)

        val staffVm = StaffViewModel(InMemoryStaffRepository())

        // Multiple invalid PIN attempts
        repeat(3) {
            staffVm.onDigitEntered('9')
            staffVm.onDigitEntered('9')
            staffVm.onDigitEntered('9')
            staffVm.onDigitEntered('9')
            assertFalse(staffVm.uiState.value.isAuthenticated)
        }

        val progress = database.progressDao().getProgressForTask("pack_one_parcel")
        assertNotNull(progress)
        assertEquals(2, progress?.currentStep)
    }

    @Test
    fun cancelingPinScreen_leavesLearnerProgressIntact() = runTest {
        learnerRepository.seedIfEmpty()
        learnerRepository.startTask("pack_one_parcel")

        val staffVm = StaffViewModel(InMemoryStaffRepository())
        staffVm.onDigitEntered('1')
        staffVm.onDigitEntered('2')
        staffVm.onCancel()

        assertEquals("", staffVm.uiState.value.enteredPin)
        assertFalse(staffVm.uiState.value.isAuthenticated)

        val progress = database.progressDao().getProgressForTask("pack_one_parcel")
        assertNotNull(progress)
        assertEquals(1, progress?.currentStep)
    }
}
