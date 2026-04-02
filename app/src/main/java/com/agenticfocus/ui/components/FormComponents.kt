package com.agenticfocus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agenticfocus.ui.theme.SubtleWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ToggleChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) selectedColor else Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, if (selected) Color.Transparent else Color.White.copy(alpha = 0.20f))
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else SubtleWhite,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

fun formatDueDate(dueDate: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(Date(dueDate))
