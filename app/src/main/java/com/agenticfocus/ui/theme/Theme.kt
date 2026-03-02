package com.agenticfocus.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AgenticFocusColorScheme = darkColorScheme(
    primary = TomatoRed,
    background = DarkBackground,
    surface = DarkBackground,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onPrimary = TextWhite
)

@Composable
fun AgenticFocusTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge is handled in MainActivity via enableEdgeToEdge()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = AgenticFocusColorScheme,
        typography = Typography,
        content = content
    )
}
