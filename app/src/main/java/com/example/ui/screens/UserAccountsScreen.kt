package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.UserAccountEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.AppStrings
import com.example.util.Language
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserAccountsScreen(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val accounts by viewModel.allUserAccounts.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, INACTIVE
    var showCreateDialog by remember { mutableStateOf(false) }

    val activeCount = accounts.count { it.lastLoginAt > 0L }
    val inactiveCount = accounts.count { it.lastLoginAt == 0L }

    val filteredAccounts = accounts.filter {
        when (selectedFilter) {
            "ACTIVE" -> it.lastLoginAt > 0L
            "INACTIVE" -> it.lastLoginAt == 0L
            else -> true
        }
    }

    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (lang == Language.NL) "GEBRUIKERS & LOGINS" else if (lang == Language.ES) "USUARIOS Y ACCESOS" else "USERS & LOGINS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = TedPink
                )
                Text(
                    text = if (lang == Language.NL) "Systeembeheer overzicht: wie gebruikt de app en wie niet"
                    else if (lang == Language.ES) "Control de uso: quién accede a la app y quién no"
                    else "Admin overview: active users vs inactive members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("admin_add_user_btn")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nieuw Account", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Activity Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACTIVITEIT STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$activeCount Actief • $inactiveCount Niet Ingelogd",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusPresent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "✔ $activeCount gebruikt",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusPresent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusAbsent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "✖ $inactiveCount nooit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusAbsent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("Alle Accounts (${accounts.size})", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("user_filter_all")
            )
            FilterChip(
                selected = selectedFilter == "ACTIVE",
                onClick = { selectedFilter = "ACTIVE" },
                label = { Text("Actief Ingelogd ($activeCount)", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("user_filter_active")
            )
            FilterChip(
                selected = selectedFilter == "INACTIVE",
                onClick = { selectedFilter = "INACTIVE" },
                label = { Text("Nooit Ingelogd ($inactiveCount)", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("user_filter_inactive")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // User Account List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredAccounts, key = { it.id }) { acc ->
                val hasLoggedIn = acc.lastLoginAt > 0L
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("user_account_item_${acc.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (hasLoggedIn) StatusPresent.copy(alpha = 0.15f) else StatusAbsent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasLoggedIn) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (hasLoggedIn) StatusPresent else StatusAbsent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = acc.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (acc.role) {
                                            "ADMIN" -> TedPink.copy(alpha = 0.15f)
                                            "OFFICER" -> PrimaryBlue.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = acc.role,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (acc.role) {
                                                "ADMIN" -> TedPink
                                                "OFFICER" -> PrimaryBlue
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "@${acc.username} • ${acc.emailOrDiscord}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = if (hasLoggedIn) "Laatst actief: ${dateFormat.format(Date(acc.lastLoginAt))} (${acc.loginCount} sessies)"
                                    else "⚠️ Nooit ingelogd op dit toestel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasLoggedIn) StatusPresent else StatusAbsent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasLoggedIn) StatusPresent.copy(alpha = 0.15f) else StatusAbsent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (hasLoggedIn) "ACTIEF" else "INACTIEF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasLoggedIn) StatusPresent else StatusAbsent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var uName by remember { mutableStateOf("") }
        var dName by remember { mutableStateOf("") }
        var dEmail by remember { mutableStateOf("") }
        var dPin by remember { mutableStateOf("1234") }
        var dRole by remember { mutableStateOf("MEMBER") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nieuw Persoonlijk Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uName,
                        onValueChange = { uName = it },
                        label = { Text("Gebruikersnaam") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dName,
                        onValueChange = { dName = it },
                        label = { Text("Volledige Naam") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dEmail,
                        onValueChange = { dEmail = it },
                        label = { Text("Discord Handle / Contact") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dPin,
                        onValueChange = { dPin = it },
                        label = { Text("PIN Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uName.isNotBlank() && dName.isNotBlank()) {
                            viewModel.register(uName, dName, dEmail, dPin, dRole) { _, _ ->
                                showCreateDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TedPink)
                ) {
                    Text("Account Toevoegen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Annuleren")
                }
            }
        )
    }
}
