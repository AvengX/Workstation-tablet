package com.example.learnerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.learnerapp.model.LearnerScreen
import com.example.learnerapp.ui.components.AppNavigationRail
import com.example.learnerapp.ui.components.HelpDialog
import com.example.learnerapp.ui.components.NextTaskPlaceholderDialog
import com.example.learnerapp.ui.components.StaffNoticeDialog
import com.example.learnerapp.ui.components.TopWorkstationBar
import com.example.learnerapp.ui.theme.WorkstationBackground
import com.example.learnerapp.viewmodel.LearnerViewModel

/**
 * Main workstation landscape scaffold for the learner experience.
 * Hosts the persistent left navigation rail, the top workstation header,
 * the active screen content, and modal dialogs.
 */
@Composable
fun LearnerMainScreen(
    viewModel: LearnerViewModel,
    modifier: Modifier = Modifier,
    onOpenStaffPin: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(WorkstationBackground)
    ) {
        // Persistent Left Navigation Rail
        AppNavigationRail(
            currentScreen = uiState.currentScreen,
            onNavigateTo = { screen -> viewModel.navigateTo(screen) }
        )

        // Main Workstation Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            // Workstation Top Bar with Task Title and Staff Lock
            TopWorkstationBar(
                taskTitle = uiState.task.title,
                onStaffClick = onOpenStaffPin
            )

            // Current Screen Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                when (uiState.currentScreen) {
                    LearnerScreen.TODAY -> {
                        TodayScreen(
                            schedule = uiState.schedule,
                            onSelectCurrentTask = { viewModel.navigateToNow() },
                            isAllTasksCompleted = uiState.isAllTasksCompleted,
                            isScheduleEmpty = uiState.isScheduleEmpty
                        )
                    }
                    LearnerScreen.NOW -> {
                        NowScreen(
                            task = uiState.task,
                            onStartTask = { viewModel.startTask() }
                        )
                    }
                    LearnerScreen.HOW -> {
                        uiState.currentStep?.let { step ->
                            HowScreen(
                                currentStep = step,
                                stepIndex = uiState.currentStepIndex,
                                totalSteps = uiState.totalSteps,
                                canGoBack = uiState.canGoBack,
                                isLastStep = uiState.isLastStep,
                                onPreviousStep = { viewModel.previousStep() },
                                onNextStep = { viewModel.nextStep() },
                                onHelpClick = { viewModel.openHelpDialog(true) }
                            )
                        }
                    }
                    LearnerScreen.CHECK -> {
                        CheckScreen(
                            checklistItems = uiState.checklistItems,
                            isContinueEnabled = uiState.isContinueEnabled,
                            onToggleItem = { itemId -> viewModel.toggleChecklistItem(itemId) },
                            onContinue = { viewModel.continueFromCheck() }
                        )
                    }
                    LearnerScreen.DONE -> {
                        DoneScreen(
                            completionPhrase = uiState.task.completionPhrase,
                            onComplete = { viewModel.completeTask() }
                        )
                    }
                    LearnerScreen.NEXT -> {
                        NextScreen(
                            nextTaskTitle = uiState.nextTaskTitle,
                            isAllTasksCompleted = uiState.isAllTasksCompleted,
                            onStartNextTask = { viewModel.startNextTask() }
                        )
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (uiState.isHelpDialogOpen) {
        HelpDialog(
            helpPhrase = uiState.task.helpPhrase,
            onDismiss = { viewModel.openHelpDialog(false) }
        )
    }

    if (uiState.isStaffNoticeDialogOpen) {
        StaffNoticeDialog(onDismiss = { viewModel.openStaffDialog(false) })
    }

    if (uiState.isNextTaskPlaceholderDialogOpen) {
        NextTaskPlaceholderDialog(onDismiss = { viewModel.showNextTaskPlaceholder(false) })
    }
}
