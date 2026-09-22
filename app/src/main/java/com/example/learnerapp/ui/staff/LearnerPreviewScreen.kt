package com.example.learnerapp.ui.staff

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity
import com.example.learnerapp.model.ChecklistItem
import com.example.learnerapp.model.LearnerScreen
import com.example.learnerapp.model.Task
import com.example.learnerapp.model.TaskScheduleItem
import com.example.learnerapp.model.TaskStep
import com.example.learnerapp.ui.components.AppNavigationRail
import com.example.learnerapp.ui.components.HelpDialog
import com.example.learnerapp.ui.components.TopWorkstationBar
import com.example.learnerapp.ui.screens.CheckScreen
import com.example.learnerapp.ui.screens.DoneScreen
import com.example.learnerapp.ui.screens.HowScreen
import com.example.learnerapp.ui.screens.NextScreen
import com.example.learnerapp.ui.screens.NowScreen
import com.example.learnerapp.ui.screens.TodayScreen
import com.example.learnerapp.ui.theme.HelpAmber
import com.example.learnerapp.ui.theme.OnHelpAmber
import com.example.learnerapp.ui.theme.RailDarkNavy
import com.example.learnerapp.ui.theme.WorkstationBackground

/**
 * Isolated Staff Learner Preview Screen.
 *
 * Allows staff to preview and walk through the entire learner workstation experience
 * (TODAY -> NOW -> HOW -> CHECK -> DONE -> NEXT) using the actual task definition,
 * steps, step photos, checklist items, help phrase, and completion phrase.
 *
 * CRITICAL SAFETY INVARIANT:
 * This preview operates entirely on in-memory local state.
 * It NEVER writes to Room database (task_progress, checklist_progress, schedule_items, tasks).
 * Production learner workstation progress is 100% untouched.
 */
@Composable
fun LearnerPreviewScreen(
    task: TaskEntity,
    steps: List<TaskStepEntity>,
    checklistItems: List<ChecklistItemEntity>,
    onExitPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onExitPreview()
    }

    // Convert entity definitions into domain models for preview
    val domainSteps = remember(steps) {
        steps.sortedBy { it.stepNumber }.map { step ->
            TaskStep(
                stepNumber = step.stepNumber,
                instruction = step.instruction,
                photoPlaceholderLabel = "Step ${step.stepNumber} Photo Placeholder:\n${step.instruction}",
                imageResId = null,
                imagePath = step.imagePath
            )
        }
    }

    val initialChecklist = remember(checklistItems) {
        checklistItems.sortedBy { it.order }.map { item ->
            ChecklistItem(
                id = item.id,
                text = item.text,
                isChecked = false
            )
        }
    }

    val previewTask = remember(task, domainSteps, initialChecklist) {
        Task(
            id = task.id,
            title = task.title,
            description = task.description,
            steps = domainSteps,
            checklist = initialChecklist,
            completionPhrase = task.completionPhrase,
            helpPhrase = task.helpPhrase
        )
    }

    // In-memory preview state (completely isolated from Room database)
    var currentScreen by remember { mutableStateOf(LearnerScreen.TODAY) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var previewChecklist by remember(initialChecklist) { mutableStateOf(initialChecklist) }
    var isHelpDialogOpen by remember { mutableStateOf(false) }

    val totalSteps = domainSteps.size
    val currentStep = domainSteps.getOrNull(currentStepIndex)
    val canGoBack = currentStepIndex > 0
    val isLastStep = currentStepIndex >= totalSteps - 1
    val isContinueEnabled = previewChecklist.isEmpty() || previewChecklist.all { it.isChecked }

    val previewSchedule = remember(task.title) {
        listOf(
            TaskScheduleItem(timeCategory = "NOW", title = task.title, isCurrent = true, isCompleted = false),
            TaskScheduleItem(timeCategory = "NEXT", title = "Upcoming task (preview)", isCurrent = false, isCompleted = false)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkstationBackground)
    ) {
        // Prominent Staff Preview Mode Banner
        Surface(
            color = Color(0xFFFEF3C7), // Warm Amber Banner
            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "STAFF PREVIEW MODE — NO CHANGES ARE SAVED",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Previewing: ${task.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF78350F),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = onExitPreview,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RailDarkNavy,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXIT PREVIEW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Preview Main Body: Navigation Rail + Workstation Screen
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Left Navigation Rail
            AppNavigationRail(
                currentScreen = currentScreen,
                onNavigateTo = { screen -> currentScreen = screen }
            )

            // Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                // Top Workstation Bar
                TopWorkstationBar(
                    taskTitle = previewTask.title,
                    onStaffClick = onExitPreview
                )

                // Active Screen in Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    when (currentScreen) {
                        LearnerScreen.TODAY -> {
                            TodayScreen(
                                schedule = previewSchedule,
                                onSelectCurrentTask = { currentScreen = LearnerScreen.NOW },
                                isAllTasksCompleted = false,
                                isScheduleEmpty = false
                            )
                        }
                        LearnerScreen.NOW -> {
                            NowScreen(
                                task = previewTask,
                                onStartTask = {
                                    currentStepIndex = 0
                                    previewChecklist = initialChecklist
                                    currentScreen = LearnerScreen.HOW
                                }
                            )
                        }
                        LearnerScreen.HOW -> {
                            if (currentStep != null) {
                                HowScreen(
                                    currentStep = currentStep,
                                    stepIndex = currentStepIndex,
                                    totalSteps = totalSteps,
                                    canGoBack = canGoBack,
                                    isLastStep = isLastStep,
                                    onPreviousStep = {
                                        if (currentStepIndex > 0) currentStepIndex--
                                    },
                                    onNextStep = {
                                        if (currentStepIndex < totalSteps - 1) {
                                            currentStepIndex++
                                        } else {
                                            currentScreen = LearnerScreen.CHECK
                                        }
                                    },
                                    onHelpClick = { isHelpDialogOpen = true }
                                )
                            } else {
                                // Fallback if task has no steps defined yet
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "No steps defined for this task template.",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(onClick = { currentScreen = LearnerScreen.CHECK }) {
                                            Text("GO TO CHECKLIST")
                                        }
                                    }
                                }
                            }
                        }
                        LearnerScreen.CHECK -> {
                            CheckScreen(
                                checklistItems = previewChecklist,
                                isContinueEnabled = isContinueEnabled,
                                onToggleItem = { itemId ->
                                    previewChecklist = previewChecklist.map { item ->
                                        if (item.id == itemId) item.copy(isChecked = !item.isChecked)
                                        else item
                                    }
                                },
                                onContinue = {
                                    if (isContinueEnabled) {
                                        currentScreen = LearnerScreen.DONE
                                    }
                                }
                            )
                        }
                        LearnerScreen.DONE -> {
                            DoneScreen(
                                completionPhrase = previewTask.completionPhrase,
                                onComplete = {
                                    currentScreen = LearnerScreen.NEXT
                                }
                            )
                        }
                        LearnerScreen.NEXT -> {
                            NextScreen(
                                nextTaskTitle = "Upcoming task (preview)",
                                isAllTasksCompleted = false,
                                onStartNextTask = {
                                    currentScreen = LearnerScreen.TODAY
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Help Dialog within preview (shows configured helpPhrase)
    if (isHelpDialogOpen) {
        HelpDialog(
            helpPhrase = previewTask.helpPhrase,
            onDismiss = { isHelpDialogOpen = false }
        )
    }
}
