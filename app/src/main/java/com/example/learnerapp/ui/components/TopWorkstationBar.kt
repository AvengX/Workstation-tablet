package com.example.learnerapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.ui.theme.SecondaryButtonBorder
import com.example.learnerapp.ui.theme.TextDark

/**
 * Workstation top bar displaying current task name and the locked STAFF button.
 */
@Composable
fun TopWorkstationBar(
    taskTitle: String,
    onStaffClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.White)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = taskTitle,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        OutlinedButton(
            onClick = onStaffClick,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, SecondaryButtonBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFF1F5F9),
                contentColor = TextDark
            ),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Staff Lock",
                modifier = Modifier.size(18.dp),
                tint = TextDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "STAFF",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                letterSpacing = 0.5.sp
            )
        }
    }
}
