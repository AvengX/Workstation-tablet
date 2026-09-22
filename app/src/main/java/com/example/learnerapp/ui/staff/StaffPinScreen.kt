package com.example.learnerapp.ui.staff

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.staff.viewmodel.StaffViewModel
import com.example.learnerapp.ui.theme.PrimaryTeal
import com.example.learnerapp.ui.theme.RailDarkNavy
import com.example.learnerapp.ui.theme.SecondaryButtonBorder
import com.example.learnerapp.ui.theme.SecondaryButtonContent
import com.example.learnerapp.ui.theme.TextDark
import com.example.learnerapp.ui.theme.TextMuted
import com.example.learnerapp.ui.theme.WorkstationBackground
import com.example.learnerapp.ui.theme.WorkstationCardBorder

/**
 * Tablet-friendly PIN entry screen for accessing Staff Mode.
 */
@Composable
fun StaffPinScreen(
    viewModel: StaffViewModel,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onAuthenticated: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthenticated()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkstationBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, WorkstationCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.width(480.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = RailDarkNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STAFF MODE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = RailDarkNavy,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Text(
                    text = "Enter staff PIN",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Masked PIN indicator (dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (i in 0 until uiState.pinLength) {
                        val isFilled = i < uiState.enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    color = if (isFilled) PrimaryTeal else Color.Transparent,
                                    shape = CircleShape
                                )
                                .then(
                                    if (!isFilled) Modifier.background(
                                        color = Color.White,
                                        shape = CircleShape
                                    ) else Modifier
                                )
                                .then(
                                    BorderStroke(
                                        2.dp,
                                        if (isFilled) PrimaryTeal else Color(0xFF94A3B8)
                                    ).let {
                                        Modifier.background(Color.Transparent, CircleShape)
                                    }
                                )
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isFilled) PrimaryTeal else Color(0xFFE2E8F0),
                                border = BorderStroke(2.dp, if (isFilled) PrimaryTeal else Color(0xFF94A3B8)),
                                modifier = Modifier.fillMaxSize()
                            ) {}
                        }
                    }
                }

                // Error message feedback
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3x4 Numeric Keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Row 1: 1, 2, 3
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PinKeyButton("1") { viewModel.onDigitEntered('1') }
                        PinKeyButton("2") { viewModel.onDigitEntered('2') }
                        PinKeyButton("3") { viewModel.onDigitEntered('3') }
                    }

                    // Row 2: 4, 5, 6
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PinKeyButton("4") { viewModel.onDigitEntered('4') }
                        PinKeyButton("5") { viewModel.onDigitEntered('5') }
                        PinKeyButton("6") { viewModel.onDigitEntered('6') }
                    }

                    // Row 3: 7, 8, 9
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PinKeyButton("7") { viewModel.onDigitEntered('7') }
                        PinKeyButton("8") { viewModel.onDigitEntered('8') }
                        PinKeyButton("9") { viewModel.onDigitEntered('9') }
                    }

                    // Row 4: Backspace, 0, Submit
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Backspace
                        OutlinedButton(
                            onClick = { viewModel.onBackspace() },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, SecondaryButtonBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = SecondaryButtonContent
                            ),
                            modifier = Modifier
                                .width(88.dp)
                                .height(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 0
                        PinKeyButton("0") { viewModel.onDigitEntered('0') }

                        // Submit
                        Button(
                            onClick = { viewModel.onSubmitPin() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryTeal,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .width(88.dp)
                                .height(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Submit PIN",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cancel button returning to Learner Mode
                OutlinedButton(
                    onClick = {
                        viewModel.onCancel()
                        onCancel()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, SecondaryButtonBorder),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryButtonContent
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFFF8FAFC),
            contentColor = TextDark
        ),
        modifier = Modifier
            .width(88.dp)
            .height(64.dp)
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
    }
}
