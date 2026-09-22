package com.example.learnerapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.learnerapp.model.ChecklistItem
import com.example.learnerapp.ui.theme.PrimaryTeal
import com.example.learnerapp.ui.theme.SuccessGreen
import com.example.learnerapp.ui.theme.TextDark
import com.example.learnerapp.ui.theme.WorkstationCardBorder

/**
 * Large touch target item for the CHECK screen.
 * Tapping anywhere on the card toggles the checked state.
 */
@Composable
fun ChecklistCard(
    item: ChecklistItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (item.isChecked) SuccessGreen else WorkstationCardBorder
    val bgColor = if (item.isChecked) Color(0xFFF0FDF4) else Color.White

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                role = Role.Checkbox,
                onClick = onToggle
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isChecked) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = if (item.isChecked) "Checked" else "Unchecked",
                tint = if (item.isChecked) SuccessGreen else Color(0xFF64748B),
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(18.dp))

            Text(
                text = item.text,
                fontSize = 22.sp,
                fontWeight = if (item.isChecked) FontWeight.Bold else FontWeight.Medium,
                color = TextDark,
                lineHeight = 28.sp
            )
        }
    }
}
