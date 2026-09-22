package com.example.learnerapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnerapp.ui.theme.HelpAmber
import com.example.learnerapp.ui.theme.OnHelpAmber
import com.example.learnerapp.ui.theme.OnPrimaryTeal
import com.example.learnerapp.ui.theme.PrimaryTeal
import com.example.learnerapp.ui.theme.SecondaryButtonBg
import com.example.learnerapp.ui.theme.SecondaryButtonBorder
import com.example.learnerapp.ui.theme.SecondaryButtonContent

/**
 * High-contrast primary action button with large touch target.
 */
@Composable
fun LargePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    containerColor: Color = PrimaryTeal,
    contentColor: Color = OnPrimaryTeal
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFFCBD5E1),
            disabledContentColor = Color(0xFF64748B)
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
        modifier = modifier.height(60.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Secondary action button (e.g. BACK).
 */
@Composable
fun LargeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, if (enabled) SecondaryButtonBorder else Color(0xFFE2E8F0)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (enabled) SecondaryButtonBg else Color(0xFFF1F5F9),
            contentColor = if (enabled) SecondaryButtonContent else Color(0xFF94A3B8)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier.height(60.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * High-visibility Help Button with amber accent.
 */
@Composable
fun HelpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HelpAmber,
            contentColor = OnHelpAmber
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
        modifier = modifier.height(60.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = "Help Icon",
            tint = OnHelpAmber,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "I NEED HELP",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnHelpAmber,
            letterSpacing = 0.5.sp
        )
    }
}
