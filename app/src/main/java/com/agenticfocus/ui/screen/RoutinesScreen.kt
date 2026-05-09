package com.agenticfocus.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.R
import com.agenticfocus.data.entity.RoutineEntity
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.viewmodel.RoutineViewModel

@Composable
fun RoutinesScreen(
    viewModel: RoutineViewModel,
    onSelectRoutine: (id: String) -> Unit,
    onBack: () -> Unit,
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
                .background(Color.Black.copy(alpha = 0.30f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = contentPadding.calculateBottomPadding())
        ) {
            // Top bar — back button + title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextWhite)
                }
                Text(
                    text = "Mes Routines",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            RoutineCard(
                routine = state.morning,
                emoji = "☀️",
                fallbackName = "Routine matinale",
                itemsCount = state.itemsByRoutine[state.morning?.id]?.size ?: 0,
                onClick = { state.morning?.let { onSelectRoutine(it.id) } }
            )

            RoutineCard(
                routine = state.evening,
                emoji = "🌙",
                fallbackName = "Routine du soir",
                itemsCount = state.itemsByRoutine[state.evening?.id]?.size ?: 0,
                onClick = { state.evening?.let { onSelectRoutine(it.id) } }
            )
        }
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineEntity?,
    emoji: String,
    fallbackName: String,
    itemsCount: Int,
    onClick: () -> Unit
) {
    val name = routine?.name?.takeIf { it.isNotBlank() } ?: fallbackName
    val triggerTime = routine?.triggerTime ?: "—"
    val isActive = routine?.isActive == 1

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(enabled = routine != null) { onClick() },
        color = Color.Black.copy(alpha = 0.50f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (routine == null) "Chargement…"
                           else "$triggerTime · $itemsCount tâche${if (itemsCount > 1) "s" else ""}${if (!isActive) " · désactivée" else ""}",
                    color = SubtleWhite,
                    fontSize = 12.sp
                )
            }
            Text("›", color = SubtleWhite, fontSize = 18.sp)
        }
    }
}
