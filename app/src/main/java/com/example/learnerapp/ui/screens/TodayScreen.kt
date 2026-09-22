package com.example.learnerapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.model.TaskScheduleItem
import com.example.learnerapp.ui.components.LargePrimaryButton
import com.example.learnerapp.ui.theme.PrimaryTeal
import com.example.learnerapp.ui.theme.TextDark
import com.example.learnerapp.ui.theme.TextMuted
import com.example.learnerapp.ui.theme.WorkstationBackground
import com.example.learnerapp.ui.theme.WorkstationCardBorder

/**
 * Screen 1: TODAY
 * Displays the work planned for the day with the active task highlighted.
 */
@Composable
fun TodayScreen(
    schedule: List<TaskScheduleItem>,
    onSelectCurrentTask: () -> Unit,
    modifier: Modifier = Modifier,
    isAllTasksCompleted: Boolean = false,
    isScheduleEmpty: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkstationBackground)
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "TODAY'S WORK",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isAllTasksCompleted) "All scheduled tasks for today have been completed."
                else if (isScheduleEmpty || schedule.isEmpty()) "No workplace tasks have been scheduled for today."
                else "Select your current task to begin.",
                fontSize = 18.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isAllTasksCompleted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, PrimaryTeal),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircleOutline,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ALL TODAY'S TASKS COMPLETE",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Great job! All assigned workstation tasks for today have been completed. Please notify a member of staff.",
                            fontSize = 18.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (schedule.isEmpty() || isScheduleEmpty) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, WorkstationCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "NO TASKS ASSIGNED",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No workstation tasks are scheduled for today. Please check with your supervisor or staff member.",
                            fontSize = 18.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                schedule.forEach { item ->
                    ScheduleCard(
                        item = item,
                        onSelect = if (item.isCurrent) onSelectCurrentTask else null
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    item: TaskScheduleItem,
    onSelect: (() -> Unit)?
) {
    val isHighlighted = item.isCurrent
    val containerColor = if (isHighlighted) PrimaryTeal else Color.White
    val contentColor = if (isHighlighted) Color.White else TextDark
    val borderColor = if (isHighlighted) PrimaryTeal else WorkstationCardBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onSelect != null) {
                    Modifier.clickable(role = Role.Button, onClick = onSelect)
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isHighlighted) Color(0xFF004D54) else Color(0xFFE2E8F0),
                    modifier = Modifier.padding(end = 20.dp)
                ) {
                    Text(
                        text = item.timeCategory,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) Color.White else Color(0xFF334155),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Column {
                    Text(
                        text = item.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    if (isHighlighted) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (item.isCompleted) "Completed • Ready for next assignment" else "Assigned for now • Ready to start",
                            fontSize = 16.sp,
                            color = Color(0xFFCCFBF1)
                        )
                    }
                }
            }

            if (isHighlighted && onSelect != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircleOutline,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COMPLETED",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "START",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go to task",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
