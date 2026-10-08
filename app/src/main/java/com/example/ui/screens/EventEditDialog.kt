package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.EventEntity
import com.example.ui.theme.TedPink
import com.example.util.AppStrings
import com.example.util.Language

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventEditDialog(
    initialEvent: EventEntity?,
    lang: Language,
    onDismiss: () -> Unit,
    onSave: (EventEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var category by remember { mutableStateOf(initialEvent?.category ?: "Ultimate Cage") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var location by remember { mutableStateOf(initialEvent?.locationOrChannel ?: "Discord Voice HQ") }
    var status by remember { mutableStateOf(initialEvent?.status ?: "ACTIVE") }
    var targetGroup by remember { mutableStateOf(initialEvent?.targetGroup ?: "All") }

    val categories = listOf("Ultimate Cage", "Turmoil", "Irons Holds", "Cristal Clash", "Sand Sea", "SVS", "Supreme")
    val statuses = listOf("ACTIVE", "COMPLETED", "UPCOMING")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialEvent == null) AppStrings.get("new_event", lang) else "Edit Event",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("event_title_input")
                )

                Text("Land of Jails Event:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                if (title.isBlank() || categories.contains(title)) {
                                    title = cat
                                }
                            },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Objectives") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Discord Channel / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Status:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statuses.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val event = (initialEvent ?: EventEntity(title = title)).copy(
                            title = title.trim(),
                            category = category,
                            description = description.trim(),
                            locationOrChannel = location.trim(),
                            status = status,
                            targetGroup = targetGroup
                        )
                        onSave(event)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                modifier = Modifier.testTag("save_event_btn")
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
