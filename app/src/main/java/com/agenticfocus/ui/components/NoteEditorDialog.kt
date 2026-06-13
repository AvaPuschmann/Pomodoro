package com.agenticfocus.ui.components

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agenticfocus.ui.theme.DarkBackground
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import io.noties.markwon.Markwon

/**
 * Éditeur de note plein écran (Dialog) — parité avec le NoteEditor desktop.
 *
 * Surface confortable pour lire/écrire une note longue, avec bascule Aperçu / Éditer.
 * Aperçu = rendu Markdown via Markwon (TextView dans un AndroidView, thème sombre,
 * liens cliquables). Édition = grand champ texte. Contrôlé : la note est éditée en
 * mémoire du formulaire parent (persistance via le bouton Enregistrer du formulaire).
 */
@Composable
fun NoteEditorDialog(
    value: String,
    taskTitle: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var preview by remember { mutableStateOf(value.isNotBlank()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // ── Header : fermer + titre + bascule Aperçu/Éditer ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = TextWhite)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("NOTE", color = SubtleWhite, fontSize = 11.sp)
                    Text(
                        taskTitle,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { preview = true }) {
                        Text("Aperçu", color = if (preview) TextWhite else SubtleWhite, fontSize = 13.sp)
                    }
                    TextButton(onClick = { preview = false }) {
                        Text("Éditer", color = if (!preview) TextWhite else SubtleWhite, fontSize = 13.sp)
                    }
                }
            }

            HorizontalDivider(color = GlassWhite)

            // ── Corps ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                if (!preview) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = {
                            Text(
                                "Écris ta note… (Markdown : # titres, - listes, **gras**, [lien](url))",
                                color = SubtleWhite,
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TomatoRed,
                            unfocusedBorderColor = GlassWhite,
                            focusedContainerColor = GlassWhite,
                            unfocusedContainerColor = GlassWhite,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = TomatoRed,
                        ),
                    )
                } else if (value.isNotBlank()) {
                    val context = LocalContext.current
                    val markwon = remember(context) { Markwon.create(context) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { ctx ->
                                TextView(ctx).apply {
                                    setTextColor(android.graphics.Color.WHITE)
                                    textSize = 15f
                                    setLineSpacing(0f, 1.3f)
                                    movementMethod = LinkMovementMethod.getInstance()
                                }
                            },
                            update = { tv -> markwon.setMarkdown(tv, value) },
                        )
                    }
                } else {
                    Text(
                        "Aucune note. Bascule sur « Éditer » pour en écrire une.",
                        color = SubtleWhite,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
