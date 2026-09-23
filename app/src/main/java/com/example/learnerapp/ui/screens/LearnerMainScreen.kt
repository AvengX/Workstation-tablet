package com.example.learnerapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.model.LearnerScreen
import com.example.learnerapp.ui.components.AppNavigationRail
import com.example.learnerapp.ui.components.HelpDialog
import com.example.learnerapp.ui.components.LargePrimaryButton
import com.example.learnerapp.ui.components.NextTaskPlaceholderDialog
import com.example.learnerapp.ui.components.StaffNoticeDialog
import com.example.learnerapp.ui.components.TopWorkstationBar
import com.example.learnerapp.ui.theme.TextDark
import com.example.learnerapp.ui.theme.TextMuted
import com.example.learnerapp.ui.theme.WorkstationBackground
import com.example.learnerapp.ui.theme.WorkstationCardBorder
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
                        val step = uiState.currentStep
                        if (step != null) {
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
                        } else {
                            ZeroStepsFallbackCard(
                                onProceedToCheck = { viewModel.navigateTo(LearnerScreen.CHECK) }
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
                        val upcomingTitle = if (uiState.schedule.isNotEmpty()) {
                            uiState.schedule.first().title
                        } else {
                            uiState.nextTaskTitle ?: uiState.task.title
                        }
                        NextScreen(
                            nextTaskTitle = upcomingTitle,
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
        NextTaskPlaceholderDialog(
            taskTitle = uiState.nextTaskTitle ?: "Sort supplies",
            onDismiss = { viewModel.showNextTaskPlaceholder(false) }
        )
    }
}

/**
 * Fallback card rendered on HOW screen when a task has zero steps.
 */
@Composable
private fun ZeroStepsFallbackCard(
    onProceedToCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, WorkstationCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NO INSTRUCTIONS REQUIRED",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This task has no step-by-step instructions. Tap proceed to check your work.",
                    fontSize = 18.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
                LargePrimaryButton(
                    text = "PROCEED TO CHECK",
                    onClick = onProceedToCheck
                )
            }
        }
    }
}
