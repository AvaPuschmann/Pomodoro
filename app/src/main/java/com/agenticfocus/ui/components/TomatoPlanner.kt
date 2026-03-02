package com.agenticfocus.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agenticfocus.viewmodel.Phase

private val TomatoGreen  = Color(0xFF4CAF50)
private val TomatoOrange = Color(0xFFFF9800)
private val TomatoRed    = Color(0xFFF44336)
private val LeafGreen    = Color(0xFF388E3C)

/** Flat Canvas-drawn tomato: colored circle + small green leaf on top. */
@Composable
fun TomatoIcon(color: Color, size: Dp = 28.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val r = this.size.minDimension
        // Body — filled circle
        drawCircle(color = color, radius = r * 0.42f, center = Offset(r / 2f, r * 0.54f))
        // Leaf — small oval at the top center
        drawOval(
            color = LeafGreen,
            topLeft = Offset(r * 0.38f, r * 0.08f),
            size = Size(r * 0.24f, r * 0.28f)
        )
    }
}

@Composable
fun TomatoPlanner(
    plannedPomodoros: Int,
    completedPomodoros: Int,
    phase: Phase,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSlots = minOf(
        6,
        maxOf(plannedPomodoros, completedPomodoros + if (phase == Phase.FOCUS) 1 else 0)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tomato icon row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSlots) { i ->
                val color = when {
                    i < completedPomodoros                          -> TomatoRed
                    i == completedPomodoros && phase == Phase.FOCUS -> TomatoOrange
                    else                                            -> TomatoGreen
                }
                TomatoIcon(color = color, size = 28.dp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // +/- controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDecrease) {
                Text("−", fontSize = 22.sp, color = Color.White)
            }
            Text(
                text = "$plannedPomodoros",
                color = Color.White,
                fontSize = 16.sp
            )
            IconButton(onClick = onIncrease) {
                Text("+", fontSize = 22.sp, color = Color.White)
            }
        }
    }
}
