package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.MemberEntity
import com.example.ui.theme.TedPink
import com.example.util.AppStrings
import com.example.util.Language

@Composable
fun NoteEditDialog(
    member: MemberEntity,
    currentNote: String,
    lang: Language,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteText by remember { mutableStateOf(currentNote) }

    val presetReasons = listOf(
        "Late 15m",
        "Equipment issue",
        "Family notice",
        "Work schedule",
        "Raid caller",
        "Exam week",
        "Discord DC"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Comment, contentDescription = null, tint = TedPink)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = AppStrings.get("notes", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${member.name} (${member.rank} • ${member.groupRole})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick tags:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetReasons.take(3).forEach { reason ->
                        SuggestionChip(
                            onClick = {
                                noteText = if (noteText.isBlank()) reason else "$noteText; $reason"
                            },
                            label = { Text(reason, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetReasons.drop(3).take(3).forEach { reason ->
                        SuggestionChip(
                            onClick = {
                                noteText = if (noteText.isBlank()) reason else "$noteText; $reason"
                            },
                            label = { Text(reason, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Real-time note / reason") },
                    placeholder = { Text(AppStrings.get("note_placeholder", lang)) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("input_note_text")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(noteText) },
                colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                modifier = Modifier.testTag("save_note_button")
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
