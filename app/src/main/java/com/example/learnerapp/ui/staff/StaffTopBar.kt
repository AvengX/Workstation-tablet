package com.example.learnerapp.ui.staff

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
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.ui.theme.PrimaryTeal
import com.example.learnerapp.ui.theme.RailDarkNavy
import com.example.learnerapp.ui.theme.SecondaryButtonBorder
import com.example.learnerapp.ui.theme.TextDark

/**
 * Top bar for Staff Mode clearly indicating the administrative context.
 */
@Composable
fun StaffTopBar(
    onExitStaff: () -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "STAFF MODE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = RailDarkNavy,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE0F2FE)
            ) {
                Text(
                    text = "WORKSTATION ADMINISTRATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        OutlinedButton(
            onClick = onExitStaff,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, SecondaryButtonBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFF8FAFC),
                contentColor = TextDark
            ),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LockOpen,
                contentDescription = "Exit Staff",
                modifier = Modifier.size(18.dp),
                tint = TextDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "EXIT STAFF MODE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                letterSpacing = 0.5.sp
            )
        }
    }
}
