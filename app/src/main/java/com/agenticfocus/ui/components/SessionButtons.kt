package com.agenticfocus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.agenticfocus.R
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed

@Composable
fun SessionButtons(
    isRunning: Boolean,
    onFocusClick: () -> Unit,
    onBreakClick: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Focus button
        ElevatedButton(
            onClick = onFocusClick,
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = GlassWhite,
                contentColor = TextWhite
            )
        ) {
            Text("Focus")
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Play / Pause FAB — Pause uses a custom vector drawable (not in material-icons-core)
        FloatingActionButton(
            onClick = onTogglePlay,
            shape = CircleShape,
            containerColor = TomatoRed,
            contentColor = TextWhite
        ) {
            if (isRunning) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pause),
                    contentDescription = "Pause"
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play"
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Break button
        ElevatedButton(
            onClick = onBreakClick,
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = GlassWhite,
                contentColor = TextWhite
            )
        ) {
            Text("Break")
        }
    }
}
