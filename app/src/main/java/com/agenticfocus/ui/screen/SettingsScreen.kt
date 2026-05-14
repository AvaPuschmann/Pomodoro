package com.agenticfocus.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.data.AppPreferences
import com.agenticfocus.data.sync.SyncStatusManager
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed

@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    onManualSync: () -> Unit,
    onClose: (() -> Unit)? = null,  // Story 18-1 — null = back-compat (pas d'overlay)
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var autoChain     by remember { mutableStateOf(prefs.autoChain) }
    var featureGoals  by remember { mutableStateOf(prefs.featureGoals) }
    var featureRoutines by remember { mutableStateOf(prefs.featureRoutines) }
    var soundEnabled  by remember { mutableStateOf(prefs.soundEnabled) }
    var sound10min    by remember { mutableStateOf(prefs.sound10min) }
    var sound5min     by remember { mutableStateOf(prefs.sound5min) }
    var sound3min     by remember { mutableStateOf(prefs.sound3min) }
    var sound1min     by remember { mutableStateOf(prefs.sound1min) }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Story 18-1 — Header with optional back button (overlay mode)
        if (onClose != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = TextWhite
                    )
                }
                Text(
                    text = "Réglages",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = "Réglages",
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // ── Section: Timer ───────────────────────────────────────────────────────
        SectionHeader("Timer")

        SettingToggleRow(
            title = "Enchaînement automatique",
            subtitle = "Démarre les pauses et les sessions focus sans intervention",
            checked = autoChain,
            onCheckedChange = { autoChain = it; prefs.autoChain = it }
        )

        // ── Section: Fonctionnalités ─────────────────────────────────────────────
        SectionHeader("Fonctionnalités", topPadding = true)

        SettingToggleRow(
            title = "Objectifs",
            subtitle = "Affiche les objectifs du jour, de la semaine et du mois",
            checked = featureGoals,
            onCheckedChange = { featureGoals = it; prefs.featureGoals = it }
        )

        SettingToggleRow(
            title = "Routines",
            subtitle = "Affiche le module Routines (matin/soir) dans la bibliothèque",
            checked = featureRoutines,
            onCheckedChange = { featureRoutines = it; prefs.featureRoutines = it }
        )

        // ── Section: Sons ────────────────────────────────────────────────────────
        SectionHeader("Sons", topPadding = true)

        SettingToggleRow(
            title = "Sons activés",
            subtitle = "Active ou désactive tous les sons du timer",
            checked = soundEnabled,
            onCheckedChange = { soundEnabled = it; prefs.soundEnabled = it }
        )

        // Individual milestone toggles — only enabled when master sound is on
        SettingToggleRow(
            title = "Alerte 10 minutes",
            checked = sound10min,
            enabled = soundEnabled,
            onCheckedChange = { sound10min = it; prefs.sound10min = it }
        )

        SettingToggleRow(
            title = "Alerte 5 minutes",
            checked = sound5min,
            enabled = soundEnabled,
            onCheckedChange = { sound5min = it; prefs.sound5min = it }
        )

        SettingToggleRow(
            title = "Alerte 3 minutes",
            checked = sound3min,
            enabled = soundEnabled,
            onCheckedChange = { sound3min = it; prefs.sound3min = it }
        )

        SettingToggleRow(
            title = "Alerte 1 minute",
            checked = sound1min,
            enabled = soundEnabled,
            onCheckedChange = { sound1min = it; prefs.sound1min = it }
        )

        // ── Section: Synchronisation ─────────────────────────────────────────────
        SectionHeader("Synchronisation", topPadding = true)

        val syncStatus by SyncStatusManager.status.collectAsStateWithLifecycle()
        val lastSyncAt by SyncStatusManager.lastSyncAt.collectAsStateWithLifecycle()
        val (statusEmoji, statusLabel) = when (syncStatus) {
            SyncStatusManager.SyncStatus.SYNCED -> {
                val timeStr = lastSyncAt?.let { formatRelativeTime(it) } ?: ""
                "✅" to if (timeStr.isNotEmpty()) "Synchronisé · $timeStr" else "Synchronisé"
            }
            SyncStatusManager.SyncStatus.SYNCING -> "🔄" to "Synchronisation…"
            SyncStatusManager.SyncStatus.OFFLINE -> "📴" to "Hors ligne"
            SyncStatusManager.SyncStatus.ERROR   -> "❌" to "Erreur sync"
        }

        Text(
            text = "$statusEmoji $statusLabel",
            color = SubtleWhite,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        OutlinedButton(
            onClick = onManualSync,
            enabled = syncStatus != SyncStatusManager.SyncStatus.SYNCING,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (syncStatus == SyncStatusManager.SyncStatus.SYNCING) "Synchronisation en cours…"
                       else "Synchroniser maintenant",
                color = TextWhite
            )
        }

        // ── Logout ───────────────────────────────────────────────────────────────
        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = TomatoRed),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text("Se déconnecter", color = TextWhite)
        }
    }
}

private fun formatRelativeTime(timestampMs: Long): String {
    val deltaMs = System.currentTimeMillis() - timestampMs
    val minutes = deltaMs / 60_000
    val hours = deltaMs / 3_600_000
    return when {
        minutes < 1  -> "à l'instant"
        minutes < 60 -> "il y a ${minutes}m"
        hours < 24   -> "il y a ${hours}h"
        else         -> "il y a ${hours / 24}j"
    }
}

@Composable
private fun SectionHeader(title: String, topPadding: Boolean = false) {
    Text(
        text = title.uppercase(),
        color = SubtleWhite,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = if (topPadding) 16.dp else 8.dp)
    )
    HorizontalDivider(color = SubtleWhite.copy(alpha = 0.2f))
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) TextWhite else SubtleWhite.copy(alpha = 0.5f),
                fontSize = 15.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = SubtleWhite.copy(alpha = if (enabled) 1f else 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextWhite,
                checkedTrackColor = TomatoRed,
                uncheckedThumbColor = SubtleWhite,
                uncheckedTrackColor = SubtleWhite.copy(alpha = 0.3f)
            ),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
