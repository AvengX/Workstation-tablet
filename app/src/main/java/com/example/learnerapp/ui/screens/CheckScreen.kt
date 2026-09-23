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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.model.ChecklistItem
import com.example.learnerapp.ui.components.ChecklistCard
import com.example.learnerapp.ui.components.LargePrimaryButton
import com.example.learnerapp.ui.theme.TextDark
import com.example.learnerapp.ui.theme.TextMuted
import com.example.learnerapp.ui.theme.WorkstationBackground

/**
 * Screen 4: CHECK
 * Interactive checklist screen for physically inspecting the completed parcel.
 * The CONTINUE button remains disabled until every item is checked.
 */
@Composable
fun CheckScreen(
    checklistItems: List<ChecklistItem>,
    isContinueEnabled: Boolean,
    onToggleItem: (Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkstationBackground)
            .padding(horizontal = 40.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header and items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "CHECK YOUR WORK",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Tap each item after physically checking the parcel.",
                    fontSize = 18.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (checklistItems.isEmpty()) {
                    Text(
                        text = "No inspection items required for this task. Tap CONTINUE to complete.",
                        fontSize = 18.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    checklistItems.forEach { item ->
                        ChecklistCard(
                            item = item,
                            onToggle = { onToggleItem(item.id) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            // Bottom action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LargePrimaryButton(
                    text = "CONTINUE",
                    enabled = isContinueEnabled,
                    onClick = onContinue
                )
            }
        }
    }
}
