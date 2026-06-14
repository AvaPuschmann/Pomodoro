package com.agenticfocus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agenticfocus.data.entity.GoalEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.util.UUID

// ── Period key helpers ────────────────────────────────────────────────────────

fun dayKey(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

fun weekKey(date: LocalDate): String {
    val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    val year = date.get(IsoFields.WEEK_BASED_YEAR)
    return "$year-W${week.toString().padStart(2, '0')}"
}

fun monthKey(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("yyyy-MM"))

// ── Single goal row ───────────────────────────────────────────────────────────

@Composable
private fun GoalRow(
    label: String,
    icon: String,
    goal: GoalEntity?,
    onSave: (String) -> Unit,
    onToggle: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val isDone = goal?.isCompleted == 1
    val isMissed = goal?.isMissed == 1
    val text = goal?.text ?: ""

    LaunchedEffect(editing) {
        if (editing) {
            draft = text
            focusRequester.requestFocus()
        }
    }

    fun commit() {
        val trimmed = draft.trim()
        if (trimmed.isNotEmpty() && trimmed != text) onSave(trimmed)
        editing = false
        focusManager.clearFocus()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        // Checkbox tri-état : à faire → atteint ✓ → raté ✗
        val markColor = when {
            isDone -> Color(0xFF34C759)
            isMissed -> Color(0xFFFF453A)
            else -> null
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = markColor ?: Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 1.dp,
                    color = markColor ?: Color.White.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable(enabled = goal != null) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (isDone || isMissed) {
                Text(
                    if (isDone) "✓" else "✗",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Label
        Text(
            text = "$icon $label",
            color = Color.White.copy(alpha = 0.70f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(72.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Text or input
        if (editing) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.50f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .focusRequester(focusRequester)
            )
        } else {
            Text(
                text = if (text.isNotEmpty()) text else "Appuyer pour définir…",
                color = when {
                    isMissed -> Color(0xFFFF453A).copy(alpha = 0.65f)
                    isDone -> Color.White.copy(alpha = 0.50f)
                    text.isNotEmpty() -> Color.White
                    else -> Color.White.copy(alpha = 0.45f)
                },
                fontSize = 14.sp,
                textDecoration = if (isDone || isMissed) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable { editing = true }
            )
        }
    }
}

// ── Divider ───────────────────────────────────────────────────────────────────

@Composable
private fun GoalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.12f))
    )
}

// ── GoalsCard ─────────────────────────────────────────────────────────────────

@Composable
fun GoalsCard(
    goals: List<GoalEntity>,
    selectedDate: LocalDate,
    userId: String,
    onSave: (GoalEntity) -> Unit,
    onToggle: (GoalEntity) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val dKey = dayKey(selectedDate)
    val wKey = weekKey(selectedDate)
    val mKey = monthKey(selectedDate)

    val dayGoal   = goals.find { it.type == "day"   && it.periodKey == dKey }
    val weekGoal  = goals.find { it.type == "week"  && it.periodKey == wKey }
    val monthGoal = goals.find { it.type == "month" && it.periodKey == mKey }

    val existingGoals = listOfNotNull(dayGoal, weekGoal, monthGoal)
    val completedCount = existingGoals.count { it.isCompleted == 1 }
    val totalCount = existingGoals.size

    fun buildGoal(type: String, periodKey: String, text: String): GoalEntity {
        val existing = goals.find { it.type == type && it.periodKey == periodKey }
        return GoalEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            userId = userId,
            type = type,
            periodKey = periodKey,
            text = text,
            isCompleted = existing?.isCompleted ?: 0,
            isMissed = existing?.isMissed ?: 0,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 6.dp)
            .background(Color(0xFF1A1A1A).copy(alpha = 0.82f), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
    ) {
        // ── Header — always visible ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Chevron
            Text(
                text = if (expanded) "▼" else "▶",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Day goal preview or placeholder
            Text(
                text = "☀️ ${dayGoal?.text ?: "Définir l'objectif du jour…"}",
                color = when {
                    dayGoal == null -> Color.White.copy(alpha = 0.45f)
                    dayGoal.isMissed == 1 -> Color(0xFFFF453A).copy(alpha = 0.65f)
                    dayGoal.isCompleted == 1 -> Color.White.copy(alpha = 0.45f)
                    else -> Color.White
                },
                textDecoration = if (dayGoal?.isCompleted == 1 || dayGoal?.isMissed == 1) TextDecoration.LineThrough else TextDecoration.None,
                fontSize = 14.sp,
                fontWeight = if (dayGoal != null && dayGoal.isCompleted != 1 && dayGoal.isMissed != 1) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Completion badge
            if (totalCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$completedCount/$totalCount ✓",
                    color = if (completedCount == totalCount) Color(0xFF34C759) else Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Expanded content ──
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp)) {
                    GoalRow(
                        label = "Mois", icon = "🗓", goal = monthGoal,
                        onSave = { text -> onSave(buildGoal("month", mKey, text)) },
                        onToggle = { monthGoal?.let { onToggle(it) } }
                    )
                    GoalDivider()
                    GoalRow(
                        label = "Semaine", icon = "📅", goal = weekGoal,
                        onSave = { text -> onSave(buildGoal("week", wKey, text)) },
                        onToggle = { weekGoal?.let { onToggle(it) } }
                    )
                    GoalDivider()
                    GoalRow(
                        label = "Jour", icon = "☀️", goal = dayGoal,
                        onSave = { text -> onSave(buildGoal("day", dKey, text)) },
                        onToggle = { dayGoal?.let { onToggle(it) } }
                    )
                }
            }
        }
    }
}
