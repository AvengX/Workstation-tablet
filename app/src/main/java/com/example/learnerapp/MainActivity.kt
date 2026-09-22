package com.example.learnerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.learnerapp.model.AppMode
import com.example.learnerapp.staff.viewmodel.ScheduleManagementViewModel
import com.example.learnerapp.staff.viewmodel.StaffViewModel
import com.example.learnerapp.staff.viewmodel.TaskContentViewModel
import com.example.learnerapp.staff.viewmodel.TaskManagementViewModel
import com.example.learnerapp.ui.screens.LearnerMainScreen
import com.example.learnerapp.ui.staff.ExitStaffDialog
import com.example.learnerapp.ui.staff.ManageChecklistScreen
import com.example.learnerapp.ui.staff.ManageStepsScreen
import com.example.learnerapp.ui.staff.ScheduleManagementScreen
import com.example.learnerapp.ui.staff.StaffDashboardScreen
import com.example.learnerapp.ui.staff.StaffPinScreen
import com.example.learnerapp.ui.staff.TaskFormScreen
import com.example.learnerapp.ui.staff.TaskTemplatesScreen
import com.example.learnerapp.ui.theme.LearnerAppTheme
import com.example.learnerapp.ui.theme.WorkstationBackground
import com.example.learnerapp.viewmodel.LearnerViewModel

/**
 * Sub-screens within Staff Mode.
 */
enum class StaffSubScreen {
    DASHBOARD,
    TASK_TEMPLATES,
    CREATE_TASK,
    EDIT_TASK,
    MANAGE_STEPS,
    MANAGE_CHECKLIST,
    SCHEDULE_MANAGEMENT
}

/**
 * Main Activity for the Workstation tablet experience.
 * Manages mode switching between Learner Mode, Staff PIN authentication,
 * and Staff Management Dashboard / Task Template / Step & Checklist screens.
 */
class MainActivity : ComponentActivity() {

    private val learnerViewModel: LearnerViewModel by viewModels()
    private val staffViewModel: StaffViewModel by viewModels()
    private val taskViewModel: TaskManagementViewModel by viewModels()
    private val taskContentViewModel: TaskContentViewModel by viewModels()
    private val scheduleViewModel: ScheduleManagementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearnerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WorkstationBackground
                ) {
                    WorkstationApp(
                        learnerViewModel = learnerViewModel,
                        staffViewModel = staffViewModel,
                        taskViewModel = taskViewModel,
                        taskContentViewModel = taskContentViewModel,
                        scheduleViewModel = scheduleViewModel
                    )
                }
            }
        }
    }
}

/**
 * Top-level Workstation composable coordinating Learner Mode and Staff Mode.
 */
@Composable
fun WorkstationApp(
    learnerViewModel: LearnerViewModel,
    staffViewModel: StaffViewModel,
    taskViewModel: TaskManagementViewModel,
    taskContentViewModel: TaskContentViewModel? = null,
    scheduleViewModel: ScheduleManagementViewModel? = null,
    modifier: Modifier = Modifier
) {
    var currentMode by rememberSaveable { mutableStateOf(AppMode.LEARNER) }
    var staffSubScreen by rememberSaveable { mutableStateOf(StaffSubScreen.DASHBOARD) }
    val staffUiState by staffViewModel.uiState.collectAsState()

    // Enforce security invariant: cannot view Staff areas without active authentication
    LaunchedEffect(staffUiState.isAuthenticated, currentMode) {
        if (currentMode == AppMode.STAFF_DASHBOARD && !staffUiState.isAuthenticated) {
            currentMode = AppMode.LEARNER
            staffSubScreen = StaffSubScreen.DASHBOARD
        }
    }

    // Hardware/gesture back press management based on active workstation mode & subscreen
    when (currentMode) {
        AppMode.STAFF_PIN -> {
            BackHandler {
                staffViewModel.onCancel()
                currentMode = AppMode.LEARNER
            }
        }
        AppMode.STAFF_DASHBOARD -> {
            when (staffSubScreen) {
                StaffSubScreen.DASHBOARD -> {
                    BackHandler {
                        staffViewModel.openExitDialog(true)
                    }
                }
                StaffSubScreen.TASK_TEMPLATES -> {
                    BackHandler {
                        staffSubScreen = StaffSubScreen.DASHBOARD
                    }
                }
                StaffSubScreen.CREATE_TASK, StaffSubScreen.EDIT_TASK -> {
                    // Handled inside TaskFormScreen with unsaved changes check
                }
                StaffSubScreen.MANAGE_STEPS, StaffSubScreen.MANAGE_CHECKLIST -> {
                    BackHandler {
                        staffSubScreen = StaffSubScreen.TASK_TEMPLATES
                    }
                }
                StaffSubScreen.SCHEDULE_MANAGEMENT -> {
                    BackHandler {
                        staffSubScreen = StaffSubScreen.DASHBOARD
                    }
                }
            }
        }
        AppMode.LEARNER -> {
            // Default system back behavior
        }
    }

    // Active mode screen rendering
    when (currentMode) {
        AppMode.LEARNER -> {
            LearnerMainScreen(
                viewModel = learnerViewModel,
                onOpenStaffPin = {
                    staffViewModel.onCancel()
                    currentMode = AppMode.STAFF_PIN
                },
                modifier = modifier
            )
        }
        AppMode.STAFF_PIN -> {
            StaffPinScreen(
                viewModel = staffViewModel,
                onCancel = {
                    currentMode = AppMode.LEARNER
                },
                onAuthenticated = {
                    currentMode = AppMode.STAFF_DASHBOARD
                    staffSubScreen = StaffSubScreen.DASHBOARD
                },
                modifier = modifier
            )
        }
        AppMode.STAFF_DASHBOARD -> {
            when (staffSubScreen) {
                StaffSubScreen.DASHBOARD -> {
                    StaffDashboardScreen(
                        viewModel = staffViewModel,
                        onExitStaff = {
                            currentMode = AppMode.LEARNER
                            staffSubScreen = StaffSubScreen.DASHBOARD
                        },
                        onNavigateToTaskTemplates = {
                            staffSubScreen = StaffSubScreen.TASK_TEMPLATES
                        },
                        onNavigateToScheduleManagement = {
                            staffSubScreen = StaffSubScreen.SCHEDULE_MANAGEMENT
                        },
                        modifier = modifier
                    )
                }
                StaffSubScreen.TASK_TEMPLATES -> {
                    TaskTemplatesScreen(
                        viewModel = taskViewModel,
                        onBack = {
                            staffSubScreen = StaffSubScreen.DASHBOARD
                        },
                        onCreateTask = {
                            taskViewModel.initCreateForm()
                            staffSubScreen = StaffSubScreen.CREATE_TASK
                        },
                        onEditTask = { task ->
                            taskViewModel.initEditForm(task)
                            staffSubScreen = StaffSubScreen.EDIT_TASK
                        },
                        onManageSteps = { task ->
                            taskContentViewModel?.loadTask(task.id)
                            staffSubScreen = StaffSubScreen.MANAGE_STEPS
                        },
                        onManageChecklist = { task ->
                            taskContentViewModel?.loadTask(task.id)
                            staffSubScreen = StaffSubScreen.MANAGE_CHECKLIST
                        },
                        onExitStaff = {
                            staffViewModel.openExitDialog(true)
                        },
                        modifier = modifier
                    )
                }
                StaffSubScreen.CREATE_TASK -> {
                    TaskFormScreen(
                        viewModel = taskViewModel,
                        isEdit = false,
                        onBack = {
                            staffSubScreen = StaffSubScreen.TASK_TEMPLATES
                        },
                        onTaskSaved = {
                            staffSubScreen = StaffSubScreen.TASK_TEMPLATES
                        },
                        modifier = modifier
                    )
                }
                StaffSubScreen.EDIT_TASK -> {
                    TaskFormScreen(
                        viewModel = taskViewModel,
                        isEdit = true,
                        onBack = {
                            staffSubScreen = StaffSubScreen.TASK_TEMPLATES
                        },
                        onTaskSaved = {
                            staffSubScreen = StaffSubScreen.TASK_TEMPLATES
                        },
                        modifier = modifier
                    )
                }
                StaffSubScreen.MANAGE_STEPS -> {
                    taskContentViewModel?.let { vm ->
                        ManageStepsScreen(
                            viewModel = vm,
                            onBack = {
                                staffSubScreen = StaffSubScreen.TASK_TEMPLATES
                            },
                            modifier = modifier
                        )
                    }
                }
                StaffSubScreen.MANAGE_CHECKLIST -> {
                    taskContentViewModel?.let { vm ->
                        ManageChecklistScreen(
                            viewModel = vm,
                            onBack = {
                                staffSubScreen = StaffSubScreen.TASK_TEMPLATES
                            },
                            modifier = modifier
                        )
                    }
                }
                StaffSubScreen.SCHEDULE_MANAGEMENT -> {
                    scheduleViewModel?.let { vm ->
                        ScheduleManagementScreen(
                            viewModel = vm,
                            onBack = {
                                staffSubScreen = StaffSubScreen.DASHBOARD
                            },
                            onExitStaff = {
                                staffViewModel.openExitDialog(true)
                            },
                            modifier = modifier
                        )
                    }
                }
            }

            // Global Exit Staff Dialog when triggered from inner Staff screens
            if (staffUiState.isExitDialogOpen && staffSubScreen != StaffSubScreen.DASHBOARD) {
                ExitStaffDialog(
                    onConfirmExit = {
                        staffViewModel.logout()
                        currentMode = AppMode.LEARNER
                        staffSubScreen = StaffSubScreen.DASHBOARD
                    },
                    onDismiss = {
                        staffViewModel.openExitDialog(false)
                    }
                )
            }
        }
    }
}
