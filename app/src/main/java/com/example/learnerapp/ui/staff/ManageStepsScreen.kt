package com.example.learnerapp.ui.staff

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.data.local.entities.TaskStepEntity
import com.example.learnerapp.staff.util.PhotoStorageManager
import com.example.learnerapp.staff.viewmodel.TaskContentViewModel
import com.example.learnerapp.ui.theme.OnPrimaryTeal
import com.example.learnerapp.ui.theme.PrimaryTeal
import com.example.learnerapp.ui.theme.RailDarkNavy
import com.example.learnerapp.ui.theme.SecondaryButtonBorder
import com.example.learnerapp.ui.theme.SecondaryButtonContent
import com.example.learnerapp.ui.theme.TextDark
import com.example.learnerapp.ui.theme.TextMuted
import com.example.learnerapp.ui.theme.WorkstationBackground
import com.example.learnerapp.ui.theme.WorkstationCardBorder

/**
 * Staff screen for managing Task Steps: creating, editing, contiguous renumbering,
 * UP/DOWN reordering, and local photo selection.
 */
@Composable
fun ManageStepsScreen(
    viewModel: TaskContentViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val task = uiState.task

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkstationBackground)
    ) {
        // Top Bar
        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Templates",
                            tint = RailDarkNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "TASK STEPS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = RailDarkNavy,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = task?.title ?: "Manage instruction steps",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = { viewModel.openAddStepDialog() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryTeal,
                        contentColor = OnPrimaryTeal
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "ADD STEP", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Feedback banners
        uiState.successMessage?.let { msg ->
            Surface(
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = msg, color = Color(0xFF065F46), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedButton(
                        onClick = { viewModel.dismissFeedback() },
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text("DISMISS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        uiState.errorMessage?.let { err ->
            Surface(
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = err, color = Color(0xFF991B1B), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedButton(
                        onClick = { viewModel.dismissFeedback() },
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text("DISMISS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Steps List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.steps.isEmpty() && !uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "NO STEPS DEFINED",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Add the first step to guide the learner through this task.",
                        fontSize = 15.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.openAddStepDialog() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = OnPrimaryTeal
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ADD FIRST STEP", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.steps, key = { _, s -> s.id }) { index, step ->
                        StepItemCard(
                            step = step,
                            isFirst = index == 0,
                            isLast = index == uiState.steps.size - 1,
                            onMoveUp = { viewModel.moveStepUp(step.id) },
                            onMoveDown = { viewModel.moveStepDown(step.id) },
                            onEdit = { viewModel.openEditStepDialog(step) },
                            onDelete = { viewModel.requestDeleteStep(step) }
                        )
                    }
                }
            }
        }
    }

    // Step Add/Edit Form Dialog
    if (uiState.isStepDialogOpen) {
        StepFormDialog(
            isEdit = uiState.editingStep != null,
            stepNumber = uiState.editingStep?.stepNumber ?: (uiState.steps.size + 1),
            instruction = uiState.stepFormData.instruction,
            imagePath = uiState.stepFormData.imagePath,
            instructionError = uiState.stepFormData.instructionError,
            onInstructionChanged = { viewModel.onStepInstructionChanged(it) },
            onPhotoChanged = { viewModel.onStepPhotoChanged(it) },
            onSave = { viewModel.saveStep() },
            onDismiss = { viewModel.closeStepDialog() }
        )
    }

    // Delete Confirmation Dialog
    uiState.stepPendingDelete?.let { step ->
        DeleteStepConfirmationDialog(
            stepNumber = step.stepNumber,
            instruction = step.instruction,
            onConfirm = { viewModel.confirmDeleteStep() },
            onDismiss = { viewModel.cancelDeleteStep() }
        )
    }

    // Active Step Deletion Protection Dialog
    uiState.activeStepProtectionWarning?.let { warning ->
        ActiveStepProtectionDialog(
            message = warning,
            onDismiss = { viewModel.dismissActiveStepProtectionWarning() }
        )
    }
}

/**
 * Step Card in the Staff Step Management list.
 */
@Composable
private fun StepItemCard(
    step: TaskStepEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(step.imagePath) {
        PhotoStorageManager.loadBitmap(step.imagePath, targetWidth = 200, targetHeight = 150)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, WorkstationCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Step Number Badge + Photo Thumbnail + Instruction
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Number Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text(
                        text = "STEP ${step.stepNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = RailDarkNavy,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Thumbnail preview or placeholder
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Step ${step.stepNumber} photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "No photo",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Instruction text
                Text(
                    text = step.instruction,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right: UP / DOWN / EDIT / DELETE Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Move UP
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Move Up",
                        tint = if (!isFirst) RailDarkNavy else Color(0xFFCBD5E1)
                    )
                }

                // Move DOWN
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Move Down",
                        tint = if (!isLast) RailDarkNavy else Color(0xFFCBD5E1)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Edit Button
                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EDIT", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Delete Button
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFECACA)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DELETE", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Dialog for adding or editing a Task Step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepFormDialog(
    isEdit: Boolean,
    stepNumber: Int,
    instruction: String,
    imagePath: String?,
    instructionError: String?,
    onInstructionChanged: (String) -> Unit,
    onPhotoChanged: (String?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = PhotoStorageManager.savePhotoFromUri(context, uri)
            if (savedPath != null) {
                onPhotoChanged(savedPath)
            }
        }
    }

    val previewBitmap = remember(imagePath) {
        PhotoStorageManager.loadBitmap(imagePath, targetWidth = 400, targetHeight = 300)
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, WorkstationCardBorder),
            modifier = Modifier
                .width(620.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = if (isEdit) "EDIT STEP $stepNumber" else "ADD STEP $stepNumber",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = RailDarkNavy
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Instruction Input
                Text(
                    text = "Instruction *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = instruction,
                    onValueChange = onInstructionChanged,
                    placeholder = { Text("e.g. Place items securely inside the parcel.") },
                    isError = instructionError != null,
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                instructionError?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Photo Selection Section
                Text(
                    text = "Step Photograph (Optional)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Photo Preview Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .size(130.dp, 100.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Step photo preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("No Photo", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    }

                    // Photo Action Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE2E8F0),
                                contentColor = RailDarkNavy
                            ),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (imagePath == null) "CHOOSE PHOTO" else "CHANGE PHOTO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (imagePath != null) {
                            OutlinedButton(
                                onClick = { onPhotoChanged(null) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("REMOVE PHOTO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, SecondaryButtonBorder),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("CANCEL", color = SecondaryButtonContent, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onSave,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = OnPrimaryTeal
                        ),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = if (isEdit) "SAVE CHANGES" else "ADD STEP",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog for deleting a step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteStepConfirmationDialog(
    stepNumber: Int,
    instruction: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, Color(0xFFFECACA)),
            modifier = Modifier
                .width(480.dp)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Delete Step $stepNumber?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Are you sure you want to delete this step? Subsequent steps will be renumbered contiguously.\n\n\"${instruction.take(60)}...\"",
                    fontSize = 14.sp,
                    color = TextMuted,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, SecondaryButtonBorder),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("CANCEL", color = SecondaryButtonContent, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("DELETE STEP", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Alert dialog shown when deletion is blocked because a learner is currently working on that step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveStepProtectionDialog(
    message: String,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, Color(0xFFFDE68A)),
            modifier = Modifier
                .width(480.dp)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Cannot Delete Active Step",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = TextMuted,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = OnPrimaryTeal
                        ),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
