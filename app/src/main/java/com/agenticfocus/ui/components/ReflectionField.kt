package com.agenticfocus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite

/**
 * Réutilisable narrative input field — Story 24-2 Sprint 22 Epic 24.
 *
 * Used for both "Day Facts" and "Learning" sections in DailyReflectionScreen.
 *
 * UX décisions Sally session 2026-05-18 :
 * - Épure narrative : bordure subtle, pas de cadre quand vide en lecture, placeholder en sous-titre
 * - Multi-ligne libre (minLines = 4) — l'utilisateur peut écrire 1 phrase ou un paragraphe
 * - Whitespace préservé (pas de trim, retours à la ligne intentionnels) — décision Sophia (Storyteller) Party Mode 2026-05-21
 * - Plain text uniquement v1 (pas de rendu markdown) — décision Sally
 *
 * Mode lecture seule (`enabled = false` — Story 24-3) :
 * - Pas de TextField : juste Text avec typo agréable (lineHeight 1.6× pour respiration)
 * - Pas de bordure
 * - Si value vide → placeholder italique grisé "—"
 *
 * @param label Title displayed next to icon (e.g. "Day Facts", "Learning")
 * @param icon Emoji shown before label (e.g. "✨", "🌱")
 * @param placeholder Sub-title explaining the field (e.g. "Qu'est-ce qui a rendu cette journée mémorable ?")
 * @param value Current text value
 * @param onValueChange Called when user types
 * @param enabled If false, render in read-only mode (Story 24-3)
 */
@Composable
fun ReflectionField(
    label: String,
    icon: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header : icon + label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Placeholder / sub-title
        Text(
            text = placeholder,
            fontSize = 13.sp,
            color = SubtleWhite,
            fontStyle = FontStyle.Italic,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (enabled) {
            // Edit mode — TextField multi-ligne épure
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                singleLine = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = SubtleWhite,
                    unfocusedBorderColor = SubtleWhite.copy(alpha = 0.3f),
                    cursorColor = TextWhite,
                ),
            )
        } else {
            // Read-only mode (Story 24-3) — typo agréable, no border, lineHeight généreuse
            if (value.isBlank()) {
                Text(
                    text = "—",
                    fontSize = 16.sp,
                    color = SubtleWhite,
                    fontStyle = FontStyle.Italic,
                )
            } else {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = TextWhite,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}
