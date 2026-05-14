package com.agenticfocus.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.R
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.StatsPeriod
import com.agenticfocus.viewmodel.StatsMetric
import com.agenticfocus.viewmodel.StatsViewModel
import androidx.compose.foundation.Canvas

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.nature_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Fix 2026-05-13 : top padding aussi (TopAppBar globale Sprint 18-1bis)
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding()
                )
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Statistiques", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }

            // Period toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = state.period == period,
                        onClick = { viewModel.setPeriod(period) },
                        label = { Text(period.label, color = TextWhite) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TomatoRed,
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                StatsMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = state.metric == metric,
                        onClick = { viewModel.setMetric(metric) },
                        label = { Text(metric.label, color = TextWhite, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3F51B5),
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Chart card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (state.metric) {
                            StatsMetric.POMODOROS -> "🍅 Pomodoros / jour"
                            StatsMetric.POINTS    -> "⭐ Points de valeur / jour"
                        },
                        color = SubtleWhite,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    val labels = remember(state.dataPoints) {
                        state.dataPoints.map { it.shortLabel }
                    }

                    if (state.period == StatsPeriod.WEEK) {
                        BarChart(
                            values = state.values,
                            labels = labels,
                            barColor = TomatoRed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        LineChart(
                            values = state.values,
                            labels = labels,
                            lineColor = TomatoRed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Summary cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    label = "Total",
                    value = state.total.toString(),
                    unit = when (state.metric) {
                        StatsMetric.POMODOROS -> "🍅"
                        StatsMetric.POINTS    -> "pts"
                    },
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label = "Record",
                    value = state.bestValue.toString(),
                    unit = when (state.metric) {
                        StatsMetric.POMODOROS -> "🍅/j"
                        StatsMetric.POINTS    -> "pts/j"
                    },
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label = "Moyenne",
                    value = "%.1f".format(state.average),
                    unit = when (state.metric) {
                        StatsMetric.POMODOROS -> "🍅/j"
                        StatsMetric.POINTS    -> "pts/j"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = SubtleWhite, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(unit, color = SubtleWhite, fontSize = 11.sp)
        }
    }
}

// ─── Bar Chart ───────────────────────────────────────────────────────────────

@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
    val gridStyle  = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val maxVal = values.max().coerceAtLeast(1f)
        val padLeft   = 36.dp.toPx()
        val padBottom = 28.dp.toPx()
        val padTop    = 8.dp.toPx()
        val chartW = size.width - padLeft
        val chartH = size.height - padBottom - padTop

        drawGrid(maxVal, padLeft, padTop, chartW, chartH, gridStyle, textMeasurer)

        val barSpacing = chartW / values.size
        val barWidth   = barSpacing * 0.55f

        values.forEachIndexed { i, v ->
            val barH = (v / maxVal * chartH).coerceAtLeast(if (v > 0f) 4.dp.toPx() else 0f)
            val x = padLeft + i * barSpacing + (barSpacing - barWidth) / 2
            val y = padTop + chartH - barH

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            val lbl = labels.getOrElse(i) { "" }
            if (lbl.isNotEmpty()) {
                val measured = textMeasurer.measure(lbl, labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x + barWidth / 2 - measured.size.width / 2,
                        size.height - padBottom + 4.dp.toPx()
                    )
                )
            }
        }
    }
}

// ─── Line Chart ──────────────────────────────────────────────────────────────

@Composable
fun LineChart(
    values: List<Float>,
    labels: List<String>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
    val gridStyle  = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)

    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        val maxVal = values.max().coerceAtLeast(1f)
        val padLeft   = 36.dp.toPx()
        val padBottom = 28.dp.toPx()
        val padTop    = 8.dp.toPx()
        val chartW = size.width - padLeft
        val chartH = size.height - padBottom - padTop

        drawGrid(maxVal, padLeft, padTop, chartW, chartH, gridStyle, textMeasurer)

        val step = chartW / (values.size - 1)

        fun xFor(i: Int) = padLeft + i * step
        fun yFor(v: Float) = padTop + chartH - (v / maxVal * chartH)

        // Fill path
        val fillPath = Path().apply {
            moveTo(xFor(0), yFor(values[0]))
            for (i in 1 until values.size) {
                val cpX = (xFor(i - 1) + xFor(i)) / 2
                cubicTo(cpX, yFor(values[i - 1]), cpX, yFor(values[i]), xFor(i), yFor(values[i]))
            }
            lineTo(xFor(values.size - 1), padTop + chartH)
            lineTo(xFor(0), padTop + chartH)
            close()
        }
        drawPath(fillPath, color = lineColor.copy(alpha = 0.18f))

        // Line path
        val linePath = Path().apply {
            moveTo(xFor(0), yFor(values[0]))
            for (i in 1 until values.size) {
                val cpX = (xFor(i - 1) + xFor(i)) / 2
                cubicTo(cpX, yFor(values[i - 1]), cpX, yFor(values[i]), xFor(i), yFor(values[i]))
            }
        }
        drawPath(linePath, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // Data points + labels
        values.forEachIndexed { i, v ->
            if (v > 0f) {
                drawCircle(color = lineColor, radius = 3.dp.toPx(), center = Offset(xFor(i), yFor(v)))
            }
            val lbl = labels.getOrElse(i) { "" }
            if (lbl.isNotEmpty()) {
                val measured = textMeasurer.measure(lbl, labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        xFor(i) - measured.size.width / 2,
                        size.height - padBottom + 4.dp.toPx()
                    )
                )
            }
        }
    }
}

// ─── Shared grid helper ───────────────────────────────────────────────────────

private fun DrawScope.drawGrid(
    maxVal: Float,
    padLeft: Float,
    padTop: Float,
    chartW: Float,
    chartH: Float,
    gridStyle: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val gridLines = 4
    for (i in 0..gridLines) {
        val ratio = i.toFloat() / gridLines
        val y = padTop + chartH - ratio * chartH
        drawLine(
            color = Color.White.copy(alpha = 0.1f),
            start = Offset(padLeft, y),
            end = Offset(padLeft + chartW, y),
            strokeWidth = 1.dp.toPx()
        )
        if (i > 0) {
            val label = (ratio * maxVal).toInt().toString()
            val measured = textMeasurer.measure(label, gridStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    padLeft - measured.size.width - 4.dp.toPx(),
                    y - measured.size.height / 2
                )
            )
        }
    }
}
