package com.example.learnerapp.ui.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.staff.viewmodel.StaffViewModel
import com.example.learnerapp.ui.theme.PrimaryTeal
import com.example.learnerapp.ui.theme.RailDarkNavy
import com.example.learnerapp.ui.theme.TextDark
import com.example.learnerapp.ui.theme.TextMuted
import com.example.learnerapp.ui.theme.WorkstationBackground
import com.example.learnerapp.ui.theme.WorkstationCardBorder

/**
 * Descriptor for a Staff Dashboard feature card.
 */
private data class StaffFeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val actionLabel: String,
    val statusBadge: String = "Feature coming soon",
    val isEnabled: Boolean = false,
    val onClick: () -> Unit = {}
)

/**
 * Staff Dashboard screen for workstation configuration and administration.
 * Guarded by [StaffViewModel.uiState.isAuthenticated].
 */
@Composable
fun StaffDashboardScreen(
    viewModel: StaffViewModel,
    onExitStaff: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToTaskTemplates: () -> Unit = {},
    onNavigateToScheduleManagement: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onNavigateToWorkstationPreview: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Security guard: redirect back to Learner Mode if session is unauthenticated
    LaunchedEffect(uiState.isAuthenticated) {
        if (!uiState.isAuthenticated) {
            onExitStaff()
        }
    }

    val featureItems = listOf(
        StaffFeatureItem(
            title = "Task Templates",
            description = "Manage workplace task library, step-by-step instructions, and checklist templates.",
            icon = Icons.AutoMirrored.Filled.Assignment,
            actionLabel = "Configure Tasks",
            statusBadge = "Active",
            isEnabled = true,
            onClick = onNavigateToTaskTemplates
        ),
        StaffFeatureItem(
            title = "Today's Schedule",
            description = "Review and configure scheduled tasks, order, and workstation timeline.",
            icon = Icons.Filled.CalendarToday,
            actionLabel = "Edit Schedule",
            statusBadge = "Active",
            isEnabled = true,
            onClick = onNavigateToScheduleManagement
        ),
        StaffFeatureItem(
            title = "Workstation Preview",
            description = "Inspect the learner workstation interface and verify instructions as presented to learners.",
            icon = Icons.Filled.Visibility,
            actionLabel = "Preview Workstation",
            statusBadge = "Active",
            isEnabled = true,
            onClick = onNavigateToWorkstationPreview
        ),
        StaffFeatureItem(
            title = "Learner Progress",
            description = "View workstation task completion metrics, session history, and step checklist logs.",
            icon = Icons.Filled.BarChart,
            actionLabel = "View Progress"
        ),
        StaffFeatureItem(
            title = "Backup & Restore",
            description = "Export or restore workstation templates, configuration, and local database records.",
            icon = Icons.Filled.Storage,
            actionLabel = "Manage Backup",
            statusBadge = "Active",
            isEnabled = true,
            onClick = onNavigateToBackupRestore
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkstationBackground)
    ) {
        // Staff Top Bar with Exit button
        StaffTopBar(
            onExitStaff = { viewModel.openExitDialog(true) }
        )

        // Subheader banner
        Surface(
            color = Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Workstation Management Dashboard",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Select an administrative configuration area below. Learner workstation progress remains preserved in Room.",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE6FFFA),
                    border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PrimaryTeal, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACTIVE STAFF SESSION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Feature cards grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(featureItems) { feature ->
                StaffFeatureCard(feature = feature)
            }
        }
    }

    // Exit Staff Mode Confirmation Dialog
    if (uiState.isExitDialogOpen) {
        ExitStaffDialog(
            onConfirmExit = {
                viewModel.logout()
                onExitStaff()
            },
            onDismiss = {
                viewModel.openExitDialog(false)
            }
        )
    }
}

/**
 * Material 3 Card displaying a workstation feature with disabled placeholder state for future phases.
 */
@Composable
private fun StaffFeatureCard(
    feature: StaffFeatureItem,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, WorkstationCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = null,
                            tint = RailDarkNavy,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = feature.statusBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = feature.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = feature.description,
                fontSize = 13.sp,
                color = TextMuted,
                lineHeight = 18.sp,
                modifier = Modifier.height(54.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = feature.onClick,
                enabled = feature.isEnabled,
                shape = RoundedCornerShape(10.dp),
                colors = if (feature.isEnabled) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = PrimaryTeal,
                        contentColor = Color.White
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors(
                        disabledContainerColor = Color(0xFFF8FAFC),
                        disabledContentColor = Color(0xFF94A3B8)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Text(
                    text = feature.actionLabel,
                    fontSize = 14.sp,
                    fontWeight = if (feature.isEnabled) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
