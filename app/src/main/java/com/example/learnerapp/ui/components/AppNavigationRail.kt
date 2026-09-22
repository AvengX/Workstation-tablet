package com.example.learnerapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.model.LearnerScreen
import com.example.learnerapp.ui.theme.RailActiveTeal
import com.example.learnerapp.ui.theme.RailDarkNavy
import com.example.learnerapp.ui.theme.RailItemActive
import com.example.learnerapp.ui.theme.RailItemUnselected

/**
 * Navigation item specification for the left navigation rail.
 */
data class RailItem(
    val screen: LearnerScreen,
    val icon: ImageVector,
    val label: String
)

private val railItems = listOf(
    RailItem(LearnerScreen.TODAY, Icons.Filled.CalendarMonth, "TODAY"),
    RailItem(LearnerScreen.NOW, Icons.Filled.PlayArrow, "NOW"),
    RailItem(LearnerScreen.HOW, Icons.AutoMirrored.Filled.MenuBook, "HOW"),
    RailItem(LearnerScreen.CHECK, Icons.AutoMirrored.Filled.FactCheck, "CHECK"),
    RailItem(LearnerScreen.DONE, Icons.Filled.CheckCircle, "DONE"),
    RailItem(LearnerScreen.NEXT, Icons.AutoMirrored.Filled.ArrowForward, "NEXT")
)

/**
 * Persistent left navigation rail for tablet landscape mode.
 * Shows all 6 stages with clear accessibility indicators and touch targets.
 */
@Composable
fun AppNavigationRail(
    currentScreen: LearnerScreen,
    onNavigateTo: (LearnerScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(116.dp)
            .fillMaxHeight()
            .background(RailDarkNavy)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        railItems.forEach { item ->
            val isSelected = currentScreen == item.screen
            RailButton(
                item = item,
                isSelected = isSelected,
                onClick = { onNavigateTo(item.screen) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun RailButton(
    item: RailItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) RailActiveTeal else RailDarkNavy
    val contentColor = if (isSelected) RailItemActive else RailItemUnselected
    val textWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.label,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = textWeight,
                letterSpacing = 0.5.sp
            )
        }
    }
}
