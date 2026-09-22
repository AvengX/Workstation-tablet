package com.example.learnerapp.ui.staff

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.staff.backup.BackupSummary
import com.example.learnerapp.staff.viewmodel.BackupRestoreUiState
import com.example.learnerapp.staff.viewmodel.BackupRestoreViewModel
import com.example.learnerapp.staff.viewmodel.BackupUiStatus
import com.example.learnerapp.ui.theme.HelpAmber
import com.example.learnerapp.ui.theme.PrimaryTeal
import com.example.learnerapp.ui.theme.RailDarkNavy
import com.example.learnerapp.ui.theme.SuccessGreen
import com.example.learnerapp.ui.theme.TextDark
import com.example.learnerapp.ui.theme.TextMuted
import com.example.learnerapp.ui.theme.WorkstationBackground
import com.example.learnerapp.ui.theme.WorkstationCardBorder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen for Staff to export portable workstation backups and restore from backup files.
 * Uses Storage Access Framework (SAF) document creation and picker launchers.
 */
@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onBack: () -> Unit,
    onExitStaff: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // SAF Document Creator launcher for exporting backup
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackupToUri(uri, context.contentResolver)
        }
    }

    // SAF Document Picker launcher for restoring backup
    val restorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.prepareRestoreFromUri(uri, context.contentResolver)
        }
    }

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
                            contentDescription = "Back to Staff Dashboard",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "BACKUP & RESTORE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Export workstation configuration and operational data or restore a previous state.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Notification banner if in success or error state
        when (val status = uiState.status) {
            is BackupUiStatus.BackupSuccess -> {
                StatusBanner(
                    isSuccess = true,
                    title = "Backup Exported Successfully",
                    message = "Created portable archive containing ${status.summary.taskCount} tasks, ${status.summary.stepCount} steps, ${status.summary.photoCount} photos, and ${status.summary.scheduleCount} schedules.",
                    onDismiss = { viewModel.clearStatus() }
                )
            }
            is BackupUiStatus.RestoreSuccess -> {
                StatusBanner(
                    isSuccess = true,
                    title = "Workstation Successfully Restored",
                    message = "Restored ${status.summary.taskCount} tasks, ${status.summary.stepCount} steps, ${status.summary.photoCount} photos, and ${status.summary.scheduleCount} schedule items with active learner progress intact.",
                    onDismiss = { viewModel.clearStatus() }
                )
            }
            is BackupUiStatus.Error -> {
                StatusBanner(
                    isSuccess = false,
                    title = "Operation Failed",
                    message = status.message,
                    onDismiss = { viewModel.clearStatus() }
                )
            }
            else -> {}
        }

        // Main content: Two action cards (Landscape Tablet Layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Card: Export Backup
            ExportBackupCard(
                isLoading = uiState.status is BackupUiStatus.BackingUp,
                onExportClick = {
                    val defaultName = "dss_workstation_backup_${DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").format(LocalDateTime.now())}.dssbackup"
                    exportLauncher.launch(defaultName)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            // Right Card: Restore Backup
            RestoreBackupCard(
                isLoading = uiState.status is BackupUiStatus.Validating || uiState.status is BackupUiStatus.Restoring,
                onRestoreClick = {
                    restorePickerLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }

    // Restore Confirmation Dialog
    if (uiState.isConfirmDialogOpen && uiState.confirmationSummary != null) {
        RestoreConfirmationDialog(
            summary = uiState.confirmationSummary!!,
            onConfirm = { viewModel.confirmRestore() },
            onDismiss = { viewModel.cancelRestore() }
        )
    }

    // In-Progress Loading Dialog
    when (uiState.status) {
        is BackupUiStatus.BackingUp -> {
            LoadingOverlay(title = "Creating Backup Archive", message = "Exporting Room database records and compressing task photos...")
        }
        is BackupUiStatus.Validating -> {
            LoadingOverlay(title = "Validating Backup Archive", message = "Checking schema compatibility, relational foreign keys, and photo integrity...")
        }
        is BackupUiStatus.Restoring -> {
            LoadingOverlay(title = "Restoring Workstation State", message = "Applying atomic Room transaction and restoring task photos...")
        }
        else -> {}
    }
}

/**
 * Export backup action card.
 */
@Composable
private fun ExportBackupCard(
    isLoading: Boolean,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, WorkstationCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE6FFFA),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Export Workstation Backup",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Generate a self-contained portable backup archive",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "This operation compiles the tablet workstation's complete operational state into a single .dssbackup archive. You can save it to local tablet storage, Google Drive, or an external USB drive.",
                    fontSize = 14.sp,
                    color = TextMuted,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "WHAT IS INCLUDED IN THE BACKUP:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RailDarkNavy,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                IncludedItemRow(text = "Workplace Task Templates & step instructions")
                IncludedItemRow(text = "Task Step photos and visual guides (raw binaries)")
                IncludedItemRow(text = "Physical verification checklist items & order")
                IncludedItemRow(text = "Daily workstation schedules for all configured dates")
                IncludedItemRow(text = "Active learner progress & completed checklist states")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onExportClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "EXPORTING BACKUP...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "CREATE & EXPORT BACKUP", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Restore backup action card.
 */
@Composable
private fun RestoreBackupCard(
    isLoading: Boolean,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, WorkstationCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SettingsBackupRestore,
                                contentDescription = null,
                                tint = HelpAmber,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Restore From Backup File",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Import and restore workstation data from a file",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Load an existing .dssbackup archive to restore tasks, photos, schedules, and learner state. All data is validated before anything is modified.",
                    fontSize = 14.sp,
                    color = TextMuted,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Caution Warning Box
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFC2410C),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "DATA REPLACEMENT WARNING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Restoring will replace all current tasks, step photos, checklists, schedules, and learner progress with the contents of the backup archive.",
                                fontSize = 13.sp,
                                color = TextDark,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SAFETY GUARANTEES:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RailDarkNavy,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                IncludedItemRow(text = "Pre-restore validation rejects corrupt or invalid files")
                IncludedItemRow(text = "Atomic Room transaction: fails cleanly on error")
                IncludedItemRow(text = "Active photos protected with automatic rollback")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRestoreClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RailDarkNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "PREPARING RESTORE...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "SELECT BACKUP FILE & RESTORE", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Bullet item inside cards.
 */
@Composable
private fun IncludedItemRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(PrimaryTeal, CircleShape)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextDark
        )
    }
}

/**
 * Material 3 Confirmation Dialog presented prior to executing restore.
 */
@Composable
private fun RestoreConfirmationDialog(
    summary: BackupSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedDate = try {
        val instant = Instant.ofEpochMilli(summary.createdAt)
        val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy 'at' HH:mm").format(ldt)
    } catch (e: Exception) {
        "Unknown date"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Restore this backup?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This will replace the current tasks, schedules, checklists, photos, and learner progress with the selected backup.",
                    fontSize = 14.sp,
                    color = TextDark,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This action cannot be undone.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "BACKUP METADATA PREVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RailDarkNavy,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MetadataRow(label = "Created:", value = formattedDate)
                        MetadataRow(label = "Task Templates:", value = "${summary.taskCount} templates")
                        MetadataRow(label = "Step Instructions:", value = "${summary.stepCount} steps")
                        MetadataRow(label = "Photo Binaries:", value = "${summary.photoCount} photos")
                        MetadataRow(label = "Checklist Items:", value = "${summary.checklistCount} items")
                        MetadataRow(label = "Schedule Entries:", value = "${summary.scheduleCount} entries")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "RESTORE AND REPLACE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "CANCEL", color = TextDark)
            }
        }
    )
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextMuted)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }
}

/**
 * Top notification feedback banner.
 */
@Composable
private fun StatusBanner(
    isSuccess: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    val bgColor = if (isSuccess) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
    val borderColor = if (isSuccess) Color(0xFFBBF7D0) else Color(0xFFFECACA)
    val iconColor = if (isSuccess) SuccessGreen else Color(0xFFDC2626)

    Surface(
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }
            TextButton(onClick = onDismiss) {
                Text(text = "DISMISS", fontWeight = FontWeight.Bold, color = TextDark)
            }
        }
    }
}

/**
 * Fullscreen loading overlay.
 */
@Composable
private fun LoadingOverlay(title: String, message: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                CircularProgressIndicator(
                    color = PrimaryTeal,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Text(text = message, fontSize = 14.sp, color = TextDark)
            }
        }
    )
}
