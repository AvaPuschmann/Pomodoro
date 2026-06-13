package com.agenticfocus.ui.screen

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agenticfocus.data.entity.DailyReflectionEntity
import com.agenticfocus.ui.components.formatFrenchDate
import com.agenticfocus.ui.theme.DarkBackground
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.viewmodel.ReflectionHistoryViewModel

/**
 * Reflection history screen (Bilans précédents) — Story 24-4 Sprint 22 Epic 24.
 *
 * Liste chronologique inverse des bilans non-vides (filtre Sally 2026-05-18).
 * Tap sur une carte → ouvre DailyReflectionScreen(periodKey) en lecture seule (logique Story 24-3).
 *
 * UX :
 * - TopBar : ← Retour + titre "Bilans précédents"
 * - LazyColumn de cartes : date FR + extrait (Day Facts prioritaire, Learning fallback)
 * - État vide : message non-culpabilisant centré
 *
 * @param userId Supabase user UUID (required for scoped query)
 * @param onBack Called when user presses ← Back
 * @param onOpenReflection Called with periodKey when user taps a card → opens DailyReflectionScreen
 */
@Composable
fun ReflectionHistoryScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenReflection: (periodKey: String) -> Unit,
    viewModel: ReflectionHistoryViewModel = viewModel(),
) {
    LaunchedEffect(userId) {
        viewModel.setUserId(userId)
    }

    val reflections by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding(),  // évite overlap avec status bar (icône wifi, heure, batterie)
    ) {
        // TopBar : ← Back + titre
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = TextWhite,
                )
            }
            Text(
                text = "Bilans précédents",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        if (reflections.isEmpty()) {
            // État vide global — non-culpabilisant
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Aucun bilan pour l'instant.\nReviens ce soir 📔",
                    fontSize = 15.sp,
                    color = SubtleWhite,
                    fontStyle = FontStyle.Italic,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(reflections, key = { it.id }) { entry ->
                    ReflectionHistoryCard(
                        entry = entry,
                        onClick = { onOpenReflection(entry.periodKey) },
                    )
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun ReflectionHistoryCard(
    entry: DailyReflectionEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Date FR
            Text(
                text = formatFrenchDate(entry.periodKey),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Extrait : Day Facts prioritaire, Learning fallback
            val (icon, excerpt) = when {
                entry.dayFacts.isNotBlank() -> "✨" to entry.dayFacts
                entry.learning.isNotBlank() -> "🌱" to entry.learning
                else -> "" to ""
            }

            if (excerpt.isNotBlank()) {
                val truncated = if (excerpt.length > 80) excerpt.take(80) + "…" else excerpt
                // Whitespace collapse pour preview ligne unique
                val singleLine = truncated.replace(Regex("\\s+"), " ").trim()
                Text(
                    text = "$icon $singleLine",
                    fontSize = 13.sp,
                    color = SubtleWhite,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}
