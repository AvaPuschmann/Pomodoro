package com.agenticfocus.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.ProjectEntity
import com.agenticfocus.data.repository.ProjectStats
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite

/**
 * Mode Projet — Story 18-3 / Sprint 18.
 * Card projet pour ProjectsKanbanScreen (Story 18-2).
 * - Bordure gauche 4px couleur domaine (fallback gris #6C6C70 si domain null/supprimé) [F14/D55]
 * - Titre 1 ligne ellipsis
 * - Footer ligne 1 : 📁 domaine • 🎯 target_date|—
 * - Footer ligne 2 : 📝 N tâches • 🍅 X/Y
 * - Archived : opacity 0.6
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(
    project: ProjectEntity,
    stats: ProjectStats?,
    domains: List<DomainEntity>,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val domain = domains.find { it.id == project.domainId }
    val borderColor = parseHexColor(domain?.color) ?: FALLBACK_GRAY
    val domainLabel = domain?.name ?: "—"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (project.isArchived) 0.6f else 1.0f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(8.dp),
        color = GlassWhite,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Bordure gauche couleur domaine — 4dp
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.name,
                    color = TextWhite,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Footer ligne 1 — domaine + target date
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        text = "📁 $domainLabel",
                        color = SubtleWhite,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🎯 ${project.targetDate?.let { formatDueDate(it) } ?: "—"}",
                        color = SubtleWhite,
                        fontSize = 12.sp,
                    )
                }
                // Footer ligne 2 — stats : N tâches + X/Y pomodoros
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        text = "📝 ${stats?.taskCount ?: 0} tâches",
                        color = SubtleWhite,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🍅 ${stats?.completedSum ?: 0}/${stats?.plannedSum ?: 0}",
                        color = SubtleWhite,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

private val FALLBACK_GRAY = Color(0xFF6C6C70)

/** Parse "#RRGGBB" string → Color. Returns null if invalid or null input. */
private fun parseHexColor(hex: String?): Color? {
    if (hex == null) return null
    return try {
        val clean = hex.removePrefix("#")
        val rgb = clean.toLong(16)
        Color(0xFF000000 or (rgb and 0xFFFFFF))
    } catch (_: Exception) {
        null
    }
}
