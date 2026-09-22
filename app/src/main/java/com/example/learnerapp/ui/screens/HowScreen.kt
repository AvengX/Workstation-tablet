package com.example.learnerapp.ui.screens

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.model.TaskStep
import com.example.learnerapp.ui.components.HelpButton
import com.example.learnerapp.ui.components.LargePrimaryButton
import com.example.learnerapp.ui.components.LargeSecondaryButton
import com.example.learnerapp.ui.components.StepPhotoCard
import com.example.learnerapp.ui.theme.PrimaryTeal
import com.example.learnerapp.ui.theme.TextDark
import com.example.learnerapp.ui.theme.WorkstationBackground

/**
 * Screen 3: HOW
 * The core step-by-step guidance screen presenting one instruction at a time.
 */
@Composable
fun HowScreen(
    currentStep: TaskStep,
    stepIndex: Int,
    totalSteps: Int,
    canGoBack: Boolean,
    isLastStep: Boolean,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkstationBackground)
            .padding(horizontal = 32.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main content area: Photo + Step counter + Instruction
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large photograph / placeholder card
                StepPhotoCard(
                    stepNumber = currentStep.stepNumber,
                    placeholderLabel = currentStep.photoPlaceholderLabel,
                    imageResId = currentStep.imageResId,
                    imagePath = currentStep.imagePath,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(260.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Step counter
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Step ${stepIndex + 1} of $totalSteps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                // Short, clear instruction text
                Text(
                    text = currentStep.instruction,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 8.dp)
                )
            }

            // Bottom action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // BACK button (disabled on Step 1)
                LargeSecondaryButton(
                    text = "BACK",
                    enabled = canGoBack,
                    onClick = onPreviousStep
                )

                // NEXT STEP button (advances step or moves to CHECK)
                LargePrimaryButton(
                    text = if (isLastStep) "NEXT STEP" else "NEXT STEP",
                    onClick = onNextStep
                )

                // I NEED HELP button
                HelpButton(
                    onClick = onHelpClick
                )
            }
        }
    }
}
