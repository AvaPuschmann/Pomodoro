package com.agenticfocus.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.agenticfocus.BuildConfig
import com.agenticfocus.ui.components.WiaThresholds
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
import com.agenticfocus.data.auth.StandaloneMode
import com.agenticfocus.data.sync.SyncStatusManager
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed

@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    onManualSync: () -> Unit,
    onClose: (() -> Unit)? = null,  // Story 18-1 — null = back-compat (pas d'overlay)
    onSaveWiaThresholds: ((WiaThresholds) -> Unit)? = null,  // Story 22-5
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

        // ── Section: Seuils Work Item Age (Mode Projet) — Story 22-5 ─────────────
        if (BuildConfig.FEATURE_PROJECTS) {
            SectionHeader("Seuils Work Item Age", topPadding = true)
            WiaThresholdsPanel(
                initialGreen = prefs.wiaThresholdsGreen,
                initialYellow = prefs.wiaThresholdsYellow,
                initialOrange = prefs.wiaThresholdsOrange,
                onSave = { g, y, o ->
                    // Auto-correction green < yellow < orange.
                    val safeGreen = g.coerceAtLeast(1)
                    val safeYellow = y.coerceAtLeast(safeGreen + 1)
                    val safeOrange = o.coerceAtLeast(safeYellow + 1)
                    prefs.wiaThresholdsGreen = safeGreen
                    prefs.wiaThresholdsYellow = safeYellow
                    prefs.wiaThresholdsOrange = safeOrange
                    onSaveWiaThresholds?.invoke(WiaThresholds(safeGreen, safeYellow, safeOrange))
                    Triple(safeGreen, safeYellow, safeOrange)
                },
                onReset = {
                    prefs.wiaThresholdsGreen = 7
                    prefs.wiaThresholdsYellow = 21
                    prefs.wiaThresholdsOrange = 45
                    onSaveWiaThresholds?.invoke(WiaThresholds(7, 21, 45))
                }
            )
        }

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

        // ── Section: Bilan du jour (Story 24-6 Sprint 22 Epic 24) ────────────────
        SectionHeader("📔 Bilan du jour", topPadding = true)

        var reflectionNotifEnabled by remember { mutableStateOf(prefs.reflectionNotifEnabled) }
        var reflectionNotifHour by remember { mutableStateOf(prefs.reflectionNotifHour) }
        var reflectionNotifMinute by remember { mutableStateOf(prefs.reflectionNotifMinute) }

        SettingToggleRow(
            title = "Recevoir un rappel quotidien",
            checked = reflectionNotifEnabled,
            onCheckedChange = { enabled ->
                reflectionNotifEnabled = enabled
                prefs.reflectionNotifEnabled = enabled
                if (enabled) {
                    com.agenticfocus.worker.ReflectionReminderScheduler.schedule(context, reflectionNotifHour, reflectionNotifMinute)
                } else {
                    com.agenticfocus.worker.ReflectionReminderScheduler.cancel(context)
                }
            }
        )

        if (reflectionNotifEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Heure du rappel",
                    color = TextWhite,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = "%02d:%02d".format(reflectionNotifHour, reflectionNotifMinute),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.width(110.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Quick preset 21:00
                        reflectionNotifHour = 21; reflectionNotifMinute = 0
                        prefs.reflectionNotifHour = 21; prefs.reflectionNotifMinute = 0
                        com.agenticfocus.worker.ReflectionReminderScheduler.schedule(context, 21, 0)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("21:00", color = TextWhite, fontSize = 13.sp) }
                OutlinedButton(
                    onClick = {
                        reflectionNotifHour = 22; reflectionNotifMinute = 0
                        prefs.reflectionNotifHour = 22; prefs.reflectionNotifMinute = 0
                        com.agenticfocus.worker.ReflectionReminderScheduler.schedule(context, 22, 0)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("22:00", color = TextWhite, fontSize = 13.sp) }
                OutlinedButton(
                    onClick = {
                        reflectionNotifHour = 20; reflectionNotifMinute = 30
                        prefs.reflectionNotifHour = 20; prefs.reflectionNotifMinute = 30
                        com.agenticfocus.worker.ReflectionReminderScheduler.schedule(context, 20, 30)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("20:30", color = TextWhite, fontSize = 13.sp) }
            }
            Text(
                text = "Le rappel apparaîtra chaque jour à cette heure (sauf en mode batterie économe).",
                color = SubtleWhite,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (BuildConfig.DEBUG) {
                OutlinedButton(
                    onClick = { com.agenticfocus.worker.ReflectionReminderScheduler.scheduleOneTimeDebug(context, 1L) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) { Text("🧪 Tester dans 1 minute", color = TextWhite, fontSize = 13.sp) }
            }
        }

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

        // Story 31-7 — en mode local, la sync manuelle est un no-op (flush() sort
        // immédiatement). La proposer active laissait croire qu'elle pouvait aboutir.
        // Pendant mobile de ce qui était déjà fait sur le ProfilePanel desktop.
        val isStandalone = StandaloneMode.isActive

        if (isStandalone) {
            Text(
                text = "Vos modifications sont enregistrées sur cet appareil et partiront " +
                       "à la prochaine connexion réussie.",
                color = SubtleWhite,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedButton(
            onClick = onManualSync,
            enabled = !isStandalone && syncStatus != SyncStatusManager.SyncStatus.SYNCING,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    isStandalone -> "Sync indisponible (mode local)"
                    syncStatus == SyncStatusManager.SyncStatus.SYNCING -> "Synchronisation en cours…"
                    else -> "Synchroniser maintenant"
                },
                color = TextWhite
            )
        }

        // ── À propos ─────────────────────────────────────────────────────────────
        SectionHeader(title = "À propos", topPadding = true)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "🍅", fontSize = 32.sp)
            Column {
                Text(
                    text = "AgenticFocus",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} — Mobile",
                    color = SubtleWhite,
                    fontSize = 13.sp
                )
                Text(
                    text = "© 2024-2025 Philippe Puschmann",
                    color = SubtleWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }

        // ── Logout ───────────────────────────────────────────────────────────────
        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = TomatoRed),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            // Le comportement était déjà correct (signOut() détecte le mode local et sort
            // sans appel réseau) ; seul le libellé mentait.
            Text(
                text = if (isStandalone) "Quitter le mode local" else "Se déconnecter",
                color = TextWhite,
            )
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
private fun WiaThresholdsPanel(
    initialGreen: Int,
    initialYellow: Int,
    initialOrange: Int,
    onSave: (g: Int, y: Int, o: Int) -> Triple<Int, Int, Int>,
    onReset: () -> Unit,
) {
    var green by remember { mutableStateOf(initialGreen.toString()) }
    var yellow by remember { mutableStateOf(initialYellow.toString()) }
    var orange by remember { mutableStateOf(initialOrange.toString()) }
    var feedback by remember { mutableStateOf<String?>(null) }

    Text(
        text = "Configure les seuils (en jours) qui déterminent la couleur du badge âge sur les cards Kanban.",
        color = SubtleWhite,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = green,
            onValueChange = { green = it.filter(Char::isDigit).take(3) },
            label = { Text("🟢 Vert <", color = SubtleWhite) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
            ),
            singleLine = true,
        )
        OutlinedTextField(
            value = yellow,
            onValueChange = { yellow = it.filter(Char::isDigit).take(3) },
            label = { Text("🟡 Jaune <", color = SubtleWhite) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
            ),
            singleLine = true,
        )
        OutlinedTextField(
            value = orange,
            onValueChange = { orange = it.filter(Char::isDigit).take(3) },
            label = { Text("🟠 Orange <", color = SubtleWhite) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
            ),
            singleLine = true,
        )
    }

    if (feedback != null) {
        Text(
            text = feedback ?: "",
            color = SubtleWhite,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                val g = green.toIntOrNull() ?: 7
                val y = yellow.toIntOrNull() ?: 21
                val o = orange.toIntOrNull() ?: 45
                val (sg, sy, so) = onSave(g, y, o)
                green = sg.toString(); yellow = sy.toString(); orange = so.toString()
                feedback = if (g != sg || y != sy || o != so)
                    "Seuils auto-corrigés (vert < jaune < orange)"
                else "Enregistré"
            },
            colors = ButtonDefaults.buttonColors(containerColor = TomatoRed),
            modifier = Modifier.weight(1f)
        ) {
            Text("Enregistrer", color = TextWhite)
        }
        OutlinedButton(
            onClick = {
                onReset()
                green = "7"; yellow = "21"; orange = "45"
                feedback = "Réinitialisé"
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Réinitialiser", color = TextWhite)
        }
    }
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
