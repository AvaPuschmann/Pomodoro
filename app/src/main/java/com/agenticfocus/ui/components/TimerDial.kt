package com.agenticfocus.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.agenticfocus.R
import com.agenticfocus.ui.theme.ArcBackground
import com.agenticfocus.ui.theme.LedCore
import com.agenticfocus.ui.theme.LedGlow
import com.agenticfocus.ui.theme.LedHalo
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.viewmodel.Phase
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TimerDial(
    progress: Float,
    phase: Phase,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "arc_progress"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val dialRadius = size.minDimension * 0.40f
            val arcStrokeWidth = 8.dp.toPx()

            // Draw 60 tick marks (watch-style)
            for (i in 0 until 60) {
                val angle = Math.toRadians(i * 6.0)
                val isLong = i % 5 == 0
                val innerR = dialRadius + arcStrokeWidth / 2 + if (isLong) 16.dp.toPx() else 8.dp.toPx()
                val outerR = dialRadius + arcStrokeWidth / 2 + if (isLong) 28.dp.toPx() else 16.dp.toPx()
                val tickStart = Offset(
                    center.x + innerR * sin(angle).toFloat(),
                    center.y - innerR * cos(angle).toFloat()
                )
                val tickEnd = Offset(
                    center.x + outerR * sin(angle).toFloat(),
                    center.y - outerR * cos(angle).toFloat()
                )
                drawLine(
                    color = SubtleWhite,
                    start = tickStart,
                    end = tickEnd,
                    strokeWidth = if (isLong) 2.5.dp.toPx() else 1.5.dp.toPx()
                )
            }

            // Arc bounding box
            val arcTopLeft = Offset(center.x - dialRadius, center.y - dialRadius)
            val arcSize = Size(dialRadius * 2, dialRadius * 2)

            // Background arc (full circle)
            drawArc(
                color = ArcBackground,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = arcStrokeWidth)
            )

            // LED amber glow arc — 7 concentric layers simulating a blur/glow
            if (animatedProgress > 0f) {
                val sweepAngle = animatedProgress * 360f

                // Outermost soft halo
                drawArc(color = LedHalo.copy(alpha = 0.07f), startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = arcStrokeWidth * 10f, cap = StrokeCap.Round))
                drawArc(color = LedHalo.copy(alpha = 0.12f), startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = arcStrokeWidth * 7f,  cap = StrokeCap.Round))
                drawArc(color = LedHalo.copy(alpha = 0.20f), startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = arcStrokeWidth * 5f,  cap = StrokeCap.Round))
                drawArc(color = LedGlow.copy(alpha = 0.35f), startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = arcStrokeWidth * 3.5f, cap = StrokeCap.Round))
                drawArc(color = LedGlow.copy(alpha = 0.55f), startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = arcStrokeWidth * 2f,  cap = StrokeCap.Round))
                drawArc(color = LedGlow.copy(alpha = 0.80f), startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = arcStrokeWidth * 1.2f, cap = StrokeCap.Round))
                // Bright core
                drawArc(color = LedCore,                      startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = arcStrokeWidth * 0.6f, cap = StrokeCap.Round))
            }
        }

        // Real tomato photo centered in the dial
        Image(
            painter = painterResource(id = R.drawable.tomato),
            contentDescription = "Tomato",
            modifier = Modifier.size(128.dp)
        )
    }
}
