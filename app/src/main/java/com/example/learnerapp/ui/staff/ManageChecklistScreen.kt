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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
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
 * Staff screen for managing Checklist Items for a workplace task:
 * creating, editing, contiguous renumbering, and UP/DOWN reordering.
 */
@Composable
fun ManageChecklistScreen(
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
                            text = "MANAGE CHECKLIST",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = RailDarkNavy,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = task?.title ?: "Manage verification items",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = { viewModel.openAddChecklistDialog() },
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
                    Text(text = "ADD ITEM", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

        // Checklist List Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.checklist.isEmpty() && !uiState.isLoading) {
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
                                imageVector = Icons.Filled.Checklist,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "NO CHECKLIST ITEMS",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tasks without verification checks allow learners to continue directly to Done.",
                        fontSize = 15.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.openAddChecklistDialog() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = OnPrimaryTeal
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ADD FIRST ITEM", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.checklist, key = { _, item -> item.id }) { index, item ->
                        ChecklistItemCard(
                            item = item,
                            isFirst = index == 0,
                            isLast = index == uiState.checklist.size - 1,
                            onMoveUp = { viewModel.moveChecklistUp(item.id) },
                            onMoveDown = { viewModel.moveChecklistDown(item.id) },
                            onEdit = { viewModel.openEditChecklistDialog(item) },
                            onDelete = { viewModel.requestDeleteChecklist(item) }
                        )
                    }
                }
            }
        }
    }

    // Add/Edit Checklist Dialog
    if (uiState.isChecklistDialogOpen) {
        ChecklistFormDialog(
            isEdit = uiState.editingChecklist != null,
            orderNumber = uiState.editingChecklist?.order ?: (uiState.checklist.size + 1),
            text = uiState.checklistFormData.text,
            textError = uiState.checklistFormData.textError,
            onTextChanged = { viewModel.onChecklistTextChanged(it) },
            onSave = { viewModel.saveChecklistItem() },
            onDismiss = { viewModel.closeChecklistDialog() }
        )
    }

    // Delete Confirmation Dialog
    uiState.checklistPendingDelete?.let { item ->
        DeleteChecklistConfirmationDialog(
            orderNumber = item.order,
            text = item.text,
            onConfirm = { viewModel.confirmDeleteChecklist() },
            onDismiss = { viewModel.cancelDeleteChecklist() }
        )
    }
}

/**
 * Checklist Item Card in Staff Management list.
 */
@Composable
private fun ChecklistItemCard(
    item: ChecklistItemEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Left: Order badge + text
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text(
                        text = "#${item.order}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = RailDarkNavy,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Text(
                    text = item.text,
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
 * Dialog for creating or editing a checklist item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistFormDialog(
    isEdit: Boolean,
    orderNumber: Int,
    text: String,
    textError: String?,
    onTextChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, WorkstationCardBorder),
            modifier = Modifier
                .width(540.dp)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (isEdit) "EDIT CHECKLIST ITEM" else "ADD CHECKLIST ITEM",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = RailDarkNavy
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Verification Prompt *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = { Text("e.g. Correct items packed inside") },
                    isError = textError != null,
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                textError?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
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
                            text = if (isEdit) "SAVE CHANGES" else "ADD ITEM",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog for deleting a checklist item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteChecklistConfirmationDialog(
    orderNumber: Int,
    text: String,
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
                        text = "Delete Item #$orderNumber?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Are you sure you want to delete this checklist item? Remaining items will be renumbered contiguously.\n\n\"$text\"",
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
                        Text("DELETE ITEM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
