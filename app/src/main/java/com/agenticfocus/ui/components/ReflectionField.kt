package com.agenticfocus.ui.components

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import io.noties.markwon.Markwon

/**
 * Réutilisable narrative input field — Story 24-2 Sprint 22 Epic 24.
 *
 * Used for both "Day Facts" and "Learning" sections in DailyReflectionScreen.
 *
 * Markdown (2026-06-20, demande Philippe « markdown partout ») :
 * - Édition : bouton « ⤢ Agrandir » ouvre le NoteEditorDialog (Aperçu/Éditer + Markwon).
 * - Lecture seule : rendu Markdown via Markwon (parité desktop MarkdownView).
 *
 * @param label Title displayed next to icon (e.g. "Day Facts", "Learning")
 * @param icon Emoji shown before label (e.g. "✨", "🌱")
 * @param placeholder Sub-title explaining the field
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
    var showEditor by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header : icon + label (+ Agrandir en édition)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
            )
            if (enabled) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showEditor = true }) {
                    Text("⤢ Agrandir", color = SubtleWhite, fontSize = 12.sp)
                }
            }
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
            // Edit mode — TextField multi-ligne épure (Markdown supporté)
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
            // Read-only mode — rendu Markdown (Markwon)
            if (value.isBlank()) {
                Text(
                    text = "—",
                    fontSize = 16.sp,
                    color = SubtleWhite,
                    fontStyle = FontStyle.Italic,
                )
            } else {
                val context = LocalContext.current
                val markwon = remember(context) { Markwon.create(context) }
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor(android.graphics.Color.WHITE)
                            textSize = 16f
                            setLineSpacing(0f, 1.4f)
                            movementMethod = LinkMovementMethod.getInstance()
                        }
                    },
                    update = { tv -> markwon.setMarkdown(tv, value) },
                )
            }
        }
    }

    if (showEditor) {
        NoteEditorDialog(
            value = value,
            taskTitle = label,
            onValueChange = onValueChange,
            onDismiss = { showEditor = false },
        )
    }
}
