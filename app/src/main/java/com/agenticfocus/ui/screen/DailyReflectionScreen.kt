package com.agenticfocus.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agenticfocus.ui.components.ReflectionField
import com.agenticfocus.ui.components.formatFrenchDate
import com.agenticfocus.ui.theme.DarkBackground
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.viewmodel.DailyReflectionViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Daily reflection (Bilan du Jour) screen — Story 24-2 Sprint 22 Epic 24.
 *
 * UX décisions Sally session 2026-05-18 :
 * - Plein écran épure narrative (pas de bottom nav, immersif)
 * - Date du jour en hero (24sp Medium)
 * - 2 ReflectionField (Day Facts + Learning) avec espacement généreux
 * - Auto-save 500ms debounce dans le ViewModel
 * - Feedback subtle "✓ Enregistré" 1.5s après chaque save
 * - Pas de bouton "Enregistrer"
 * - Tous les jours directement éditables, y compris le passé (journaling rétroactif, 2026-06-07)
 *
 * Navigation :
 * - Caller fournit userId + periodKey (default today)
 * - onBack ferme l'écran
 * - onOpenHistory placeholder pour Story 24-4 (ReflectionHistoryScreen)
 *
 * @param userId Supabase user UUID (required for sync + scoping)
 * @param periodKey YYYY-MM-DD to load (default today)
 * @param onBack Called when user presses ← Back
 * @param onOpenHistory Called when user taps "📔 Bilans précédents" (Story 24-4)
 */
@Composable
fun DailyReflectionScreen(
    userId: String,
    periodKey: String = LocalDate.now().toString(),
    onBack: () -> Unit,
    onOpenHistory: () -> Unit = {},
    viewModel: DailyReflectionViewModel = viewModel(),
) {
    LaunchedEffect(userId, periodKey) {
        viewModel.setUserId(userId)
        viewModel.loadForPeriod(periodKey)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Track "✓ Enregistré" visibility — show for 1.5s after each save
    var showSaved by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.lastSavedAt) {
        if (state.lastSavedAt != null) {
            showSaved = state.lastSavedAt!!
            delay(1500)
            if (showSaved == state.lastSavedAt) showSaved = -1L
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding(),  // évite overlap avec status bar (icône wifi, heure, batterie)
    ) {
        // TopBar : ← Back + 📔 Bilans précédents (right) — weight pour bien occuper l'espace
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
            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = onOpenHistory) {
                Text(
                    text = "📔 Bilans précédents",
                    color = TextWhite,
                    fontSize = 14.sp,
                )
            }
        }

        // Body : scrollable column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Date du jour en hero
            Text(
                text = formatFrenchDate(state.periodKey),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tous les jours sont directement éditables (y compris le passé) pour permettre
            // le journaling rétroactif sans friction — décision Philippe 2026-06-07.
            // ✨ Day Facts
            ReflectionField(
                label = "Day Facts",
                icon = "✨",
                placeholder = "Qu'est-ce qui a rendu cette journée mémorable ?",
                value = state.dayFacts,
                onValueChange = viewModel::onDayFactsChange,
                enabled = state.isEditable,
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 🌱 Learning
            ReflectionField(
                label = "Learning",
                icon = "🌱",
                placeholder = "Qu'ai-je appris aujourd'hui ?",
                value = state.learning,
                onValueChange = viewModel::onLearningChange,
                enabled = state.isEditable,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ✓ Enregistré feedback (subtle, 1.5s after save)
            AnimatedVisibility(
                visible = showSaved > 0L,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "✓ Enregistré",
                    color = SubtleWhite,
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
