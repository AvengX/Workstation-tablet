package com.example.learnerapp.ui.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.staff.repository.ScheduledTaskItem
import com.example.learnerapp.staff.viewmodel.ScheduleManagementViewModel
import com.example.learnerapp.ui.theme.PrimaryTeal
import com.example.learnerapp.ui.theme.RailDarkNavy
import com.example.learnerapp.ui.theme.TextDark
import com.example.learnerapp.ui.theme.TextMuted
import com.example.learnerapp.ui.theme.WorkstationBackground
import com.example.learnerapp.ui.theme.WorkstationCardBorder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Screen for Staff to manage daily workstation task schedules.
 * Supports date navigation, date picker, adding task templates, atomic contiguous reordering (1..N),
 * and removing scheduled tasks with confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleManagementScreen(
    viewModel: ScheduleManagementViewModel,
    onBack: () -> Unit,
    onExitStaff: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkstationBackground)
    ) {
        // Staff Top Bar
        StaffTopBar(onExitStaff = onExitStaff)

        // Subheader banner with Back button
        Surface(
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextDark)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "DAILY WORK SCHEDULE",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Assign and order workplace tasks for learners at this workstation.",
                            fontSize = 15.sp,
                            color = TextMuted
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { viewModel.openAddTaskDialog(true) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PrimaryTeal,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ ADD TASK",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Date Navigation Control Bar
        DateNavigationBar(
            formattedDate = uiState.formattedDate,
            isToday = uiState.isToday,
            onPreviousDay = { viewModel.previousDay() },
            onNextDay = { viewModel.nextDay() },
            onGoToToday = { viewModel.goToToday() },
            onOpenDatePicker = { viewModel.openDatePicker(true) }
        )

        // Error message banner
        uiState.errorMessage?.let { errorMsg ->
            Surface(
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMsg,
                            fontSize = 14.sp,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(text = "Dismiss", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Scheduled Tasks List or Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            if (uiState.scheduledTasks.isEmpty()) {
                EmptyScheduleView(onAddTask = { viewModel.openAddTaskDialog(true) })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(
                        items = uiState.scheduledTasks,
                        key = { _, item -> item.scheduleItem.id }
                    ) { index, item ->
                        ScheduledTaskCard(
                            item = item,
                            index = index,
                            totalCount = uiState.scheduledTasks.size,
                            onMoveUp = { viewModel.moveTaskUp(item.scheduleItem.id) },
                            onMoveDown = { viewModel.moveTaskDown(item.scheduleItem.id) },
                            onRemove = { viewModel.openRemoveDialog(item) }
                        )
                    }
                }
            }
        }
    }

    // Remove Confirmation Dialog
    uiState.taskToRemove?.let { taskItem ->
        AlertDialog(
            onDismissRequest = { viewModel.openRemoveDialog(null) },
            title = {
                Text(
                    text = "Remove from Schedule?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
            },
            text = {
                Text(
                    text = "Remove \"${taskItem.task.title}\" from the schedule for ${uiState.formattedDate}?\n\nThe task template itself will not be deleted.",
                    fontSize = 16.sp,
                    color = TextDark,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRemoveTask() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "REMOVE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.openRemoveDialog(null) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "CANCEL", color = TextDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Add Task to Schedule Dialog
    if (uiState.isAddTaskDialogOpen) {
        AddTaskDialog(
            availableTasks = uiState.availableTasks,
            selectedTaskId = uiState.selectedTaskIdToAdd,
            formattedDate = uiState.formattedDate,
            onSelectTask = { viewModel.selectTaskToAdd(it) },
            onConfirm = { viewModel.confirmAddTask() },
            onDismiss = { viewModel.openAddTaskDialog(false) }
        )
    }

    // Material 3 Date Picker Dialog
    if (uiState.isDatePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.openDatePicker(false) },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            viewModel.selectDate(selectedLocalDate)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "SELECT DATE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.openDatePicker(false) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "CANCEL", color = TextDark)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Top date navigation bar allowing Staff to switch days or pick from a calendar.
 */
@Composable
private fun DateNavigationBar(
    formattedDate: String,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onGoToToday: () -> Unit,
    onOpenDatePicker: () -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPreviousDay,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = TextDark)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Day",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.clickable(onClick = onOpenDatePicker)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "Select Date",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onNextDay,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = TextDark)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (!isToday) {
                OutlinedButton(
                    onClick = onGoToToday,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PrimaryTeal),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Event,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "TODAY", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * Card representing a scheduled task in order 1..N.
 */
@Composable
private fun ScheduledTaskCard(
    item: ScheduledTaskItem,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val orderNumber = index + 1
    val isFirst = index == 0
    val isLast = index == totalCount - 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, WorkstationCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Order number badge
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F766E),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = orderNumber.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = item.task.title.uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.task.description.ifEmpty { "Assigned workplace task" },
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            }

            // Action Buttons: UP, DOWN, REMOVE
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Move UP button
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = TextDark,
                        disabledContentColor = Color(0xFFCBD5E1)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Move Up",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Move DOWN button
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = TextDark,
                        disabledContentColor = Color(0xFFCBD5E1)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Move Down",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Remove Button
                OutlinedButton(
                    onClick = onRemove,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFEF2F2),
                        contentColor = Color(0xFFDC2626)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REMOVE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Empty schedule placeholder.
 */
@Composable
private fun EmptyScheduleView(
    onAddTask: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, WorkstationCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No tasks scheduled for this day",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add task templates to configure the workstation daily work pipeline.",
                fontSize = 16.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddTask,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "+ ADD TASK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

/**
 * Dialog to select an available task template to add to the daily schedule.
 */
@Composable
private fun AddTaskDialog(
    availableTasks: List<com.example.learnerapp.data.local.entities.TaskEntity>,
    selectedTaskId: String?,
    formattedDate: String,
    onSelectTask: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Add Task to Schedule",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a task template for $formattedDate:",
                    fontSize = 14.sp,
                    color = TextMuted
                )
            }
        },
        text = {
            if (availableTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No task templates available",
                        fontSize = 16.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    availableTasks.forEach { task ->
                        val isSelected = task.id == selectedTaskId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFF0FDFA) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, PrimaryTeal) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .selectable(
                                    selected = isSelected,
                                    onClick = { onSelectTask(task.id) },
                                    role = Role.RadioButton
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = task.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    if (task.description.isNotEmpty()) {
                                        Text(
                                            text = task.description,
                                            fontSize = 13.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = availableTasks.isNotEmpty() && selectedTaskId != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "ADD", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "CANCEL", color = TextDark)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
