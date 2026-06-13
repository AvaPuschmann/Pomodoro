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

/**
 * Format a periodKey (YYYY-MM-DD) into a French human-friendly date string.
 * Story 24-2 Sprint 22 Epic 24 — used as hero title on DailyReflectionScreen.
 *
 * Examples :
 * - "2026-05-21" → "Jeudi 21 mai 2026"
 * - "2026-12-31" → "Jeudi 31 décembre 2026"
 *
 * @return Capitalized French formatted date, or the raw periodKey if parsing fails (defensive).
 */
fun formatFrenchDate(periodKey: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val date = parser.parse(periodKey) ?: return periodKey
        val formatter = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
        val formatted = formatter.format(date)
        // Capitalize first letter (French locale starts day name lowercase: "jeudi 21 mai 2026")
        formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() }
    } catch (_: Exception) {
        periodKey
    }
}
