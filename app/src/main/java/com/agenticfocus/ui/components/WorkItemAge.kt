package com.agenticfocus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agenticfocus.data.entity.KanbanStatus
import com.agenticfocus.data.entity.ProjectEntity
import kotlin.math.floor

/**
 * Story 22-3 / Sprint 20 — Work Item Age + Cycle Time helpers + AgeBadge composable.
 * Parity with desktop workItemAge.ts Sprint 19.
 *
 * Concepts:
 * - Work Item Age (WIA) = now - started_at, applicable Todo/Doing (en cours)
 * - Cycle Time = finished_at - started_at, applicable Done (terminé)
 * - Backlog age = now - created_at, applicable Backlog (idée dormante)
 *
 * Couleurs configurables via thresholds (Story 22-5 ajoutera Settings):
 * - Vert : < t.green
 * - Jaune : t.green..t.yellow
 * - Orange : t.yellow..t.orange
 * - Rouge : > t.orange
 */

data class WiaThresholds(
    val green: Int = 7,    // < green = vert
    val yellow: Int = 21,  // green..yellow = jaune
    val orange: Int = 45,  // yellow..orange = orange (> orange = rouge)
) {
    companion object {
        val DEFAULT = WiaThresholds()
    }
}

/** CompositionLocal — Story 22-5 injectera depuis AppPreferences au top niveau de l'app. */
val LocalWiaThresholds = compositionLocalOf { WiaThresholds.DEFAULT }

enum class AgeBucket(val color: Color, val emoji: String) {
    GREEN(Color(0xFF34C759), "🟢"),
    YELLOW(Color(0xFFFFC107), "🟡"),
    ORANGE(Color(0xFFFF9800), "🟠"),
    RED(Color(0xFFE53935), "🔴"),
    NEUTRAL(Color(0xFF888888), "⚪");
}

private const val MS_PER_DAY = 1000L * 60 * 60 * 24

/**
 * Compute age in milliseconds based on project status.
 * - Done : cycle time = finishedAt - startedAt (fallback createdAt si startedAt manquant)
 * - Backlog : age = now - createdAt
 * - Todo/Doing : WIA = now - startedAt (fallback createdAt)
 */
fun computeAge(project: ProjectEntity, now: Long = System.currentTimeMillis()): Long = when (project.kanbanStatus) {
    KanbanStatus.DONE -> {
        val start = project.startedAt ?: project.createdAt
        val end = project.finishedAt ?: now
        (end - start).coerceAtLeast(0L)
    }
    KanbanStatus.BACKLOG -> (now - project.createdAt).coerceAtLeast(0L)
    else -> {
        val start = project.startedAt ?: project.createdAt
        (now - start).coerceAtLeast(0L)
    }
}

/** Compute color bucket + day count for a given age in ms. */
fun ageBucket(ageMs: Long, t: WiaThresholds = WiaThresholds.DEFAULT): Pair<AgeBucket, Int> {
    val days = (ageMs / MS_PER_DAY).toInt()
    val bucket = when {
        days < t.green -> AgeBucket.GREEN
        days < t.yellow -> AgeBucket.YELLOW
        days < t.orange -> AgeBucket.ORANGE
        else -> AgeBucket.RED
    }
    return bucket to days
}

/** Format age duration : "< 1h" / "3h" / "12j". */
fun formatDays(ms: Long): String {
    val days = floor(ms.toDouble() / MS_PER_DAY).toInt()
    if (days < 1) {
        val hours = (ms / (1000L * 60 * 60)).toInt()
        return if (hours < 1) "< 1h" else "${hours}h"
    }
    return "${days}j"
}

data class ColumnAgeStats(
    val count: Int,
    val avgDays: Double,
    val oldestDays: Double,
    val p50Days: Double,
    val p95Days: Double,
)

fun computeColumnStats(projects: List<ProjectEntity>, now: Long = System.currentTimeMillis()): ColumnAgeStats {
    if (projects.isEmpty()) return ColumnAgeStats(0, 0.0, 0.0, 0.0, 0.0)
    val ages = projects
        .map { computeAge(it, now).toDouble() / MS_PER_DAY }
        .sorted()
    val sum = ages.sum()
    val avg = sum / ages.size
    val oldest = ages.last()
    val p50 = percentile(ages, 0.50)
    val p95 = percentile(ages, 0.95)
    return ColumnAgeStats(
        count = ages.size,
        avgDays = round1(avg),
        oldestDays = round1(oldest),
        p50Days = round1(p50),
        p95Days = round1(p95),
    )
}

private fun percentile(sortedAsc: List<Double>, p: Double): Double {
    if (sortedAsc.isEmpty()) return 0.0
    val idx = (sortedAsc.size - 1) * p
    val lo = floor(idx).toInt()
    val hi = kotlin.math.ceil(idx).toInt()
    if (lo == hi) return sortedAsc[lo]
    return sortedAsc[lo] + (sortedAsc[hi] - sortedAsc[lo]) * (idx - lo)
}

private fun round1(v: Double): Double = kotlin.math.round(v * 10.0) / 10.0

// ── AgeBadge composable ─────────────────────────────────────────────────

@Composable
fun AgeBadge(
    project: ProjectEntity,
    thresholds: WiaThresholds = LocalWiaThresholds.current,
    modifier: Modifier = Modifier,
) {
    val ageMs = computeAge(project)
    when (project.kanbanStatus) {
        KanbanStatus.BACKLOG -> {
            // Idée dormante : neutral gray + 💤
            Row(
                modifier = modifier
                    .background(AgeBucket.NEUTRAL.color.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "💤 ${formatDays(ageMs)} en attente",
                    color = Color(0xFFBBBBBB),
                    fontSize = 10.sp,
                )
            }
        }
        KanbanStatus.DONE -> {
            // Cycle Time : vert ✓
            Row(
                modifier = modifier
                    .background(Color(0xFF34C759).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "✓ Cycle Time : ${formatDays(ageMs)}",
                    color = Color(0xFF34C759),
                    fontSize = 10.sp,
                )
            }
        }
        else -> {
            // Todo / Doing : bucket couleur
            val (bucket, days) = ageBucket(ageMs, thresholds)
            val label = when (bucket) {
                AgeBucket.RED -> "⚠ ${days}j"
                AgeBucket.ORANGE -> "🔥 ${days}j"
                else -> "${bucket.emoji} ${days}j"
            }
            Row(
                modifier = modifier
                    .background(bucket.color.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = bucket.color,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
