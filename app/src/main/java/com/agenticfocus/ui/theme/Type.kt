package com.agenticfocus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        color = TextWhite
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 4.sp,
        color = TextWhite
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        color = TextWhite
    )
)
