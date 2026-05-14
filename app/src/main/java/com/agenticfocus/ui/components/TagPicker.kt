package com.agenticfocus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.agenticfocus.data.entity.TagEntity

/**
 * Story 22-2c / Sprint 20 — TagPicker mobile composable (parity desktop TagPicker.tsx).
 *
 * Pattern :
 * - Affiche les tags sélectionnés sous forme de chips colorés (avec × pour retirer)
 * - Bouton dashed "+ Tag" → ouvre un ModalBottomSheet imbriqué avec :
 *   - OutlinedTextField search (filter case-insensitive)
 *   - LazyColumn des tags disponibles (non sélectionnés)
 *   - Tap un tag → ajouté immédiatement, sheet reste ouvert pour multi-add
 *   - Empty state si pas de tags ou aucun résultat search
 *
 * Scale OK pour 30+ tags grâce à la recherche + scroll.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagPicker(
    allTags: List<TagEntity>,
    selectedIds: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val selectedTags = remember(selectedIds, allTags) {
        selectedIds.mapNotNull { id -> allTags.find { it.id == id } }
    }
    val availableTags = remember(selectedIds, allTags, search) {
        val selSet = selectedIds.toSet()
        val filtered = allTags.filter { it.id !in selSet }
        if (search.isBlank()) filtered
        else filtered.filter { it.name.lowercase().contains(search.trim().lowercase()) }
    }

    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            selectedTags.forEach { tag ->
                val tagColor = remember(tag.color) {
                    runCatching { Color(tag.color.toColorInt()) }.getOrDefault(Color.Gray)
                }
                Surface(
                    color = tagColor,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            tag.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                                .clickable { onChange(selectedIds - tag.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Retirer ${tag.name}",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
            // "+ Tag" dashed button
            Surface(
                modifier = Modifier.clickable { sheetOpen = true },
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Ajouter un tag",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Tag", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }
    }

    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                sheetOpen = false
                search = ""
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Tags", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Rechercher un tag…", color = Color.White.copy(alpha = 0.55f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(8.dp))
                if (allTags.isEmpty()) {
                    Text(
                        "Aucun tag créé.\nCréez des tags via la Bibliothèque → 🏷 Mes Tags.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else if (availableTags.isEmpty()) {
                    Text(
                        if (search.isBlank()) "Tous les tags sont sélectionnés." else "Aucun résultat pour « $search ».",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                    ) {
                        items(availableTags, key = { it.id }) { tag ->
                            val tagColor = remember(tag.color) {
                                runCatching { Color(tag.color.toColorInt()) }.getOrDefault(Color.Gray)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChange(selectedIds + tag.id)
                                        // Garde le sheet ouvert pour multi-add
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(tagColor, CircleShape),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(tag.name, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
                if (selectedTags.isNotEmpty()) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${selectedTags.size} sélectionné${if (selectedTags.size > 1) "s" else ""}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.size(16.dp))
            }
        }
    }
}
