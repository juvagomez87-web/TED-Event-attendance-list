package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.StatusPresent
import com.example.ui.theme.TedPink
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.BackupFileInfo
import com.example.util.BackupManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupDialog(
    viewModel: RosterViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var passphrase by remember { mutableStateOf("") }
    var restorePassphrase by remember { mutableStateOf("") }
    var selectedBackupForRestore by remember { mutableStateOf<BackupFileInfo?>(null) }
    var isBackingUp by remember { mutableStateOf(false) }
    var backupStatusMsg by remember { mutableStateOf<String?>(null) }
    var autoBackupEnabled by remember { mutableStateOf(true) }
    var lastExportedFile by remember { mutableStateOf<File?>(null) }

    var backupList by remember { mutableStateOf(BackupManager.listBackups(context)) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = TedPink, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Versleutelde Database Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AES-256-GCM encryptie voor cloud drive & veilige opslag",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Manual Backup Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Handmatige Backup Aanmaken",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Exporteer de volledige Room database (leden, aanwezigheid, audit logs en accounts) als een versleuteld .tedbackup bestand.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = passphrase,
                                    onValueChange = { passphrase = it },
                                    label = { Text("Optioneel: Beveiligingswachtwoord / PIN") },
                                    placeholder = { Text("Laat leeg voor standaard clan-encryptie") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_backup_passphrase")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            isBackingUp = true
                                            backupStatusMsg = "Bezig met versleutelen en exporteren..."
                                            viewModel.createManualBackup(passphrase) { result ->
                                                isBackingUp = false
                                                if (result.isSuccess) {
                                                    val file = result.getOrNull()
                                                    lastExportedFile = file
                                                    backupStatusMsg = "Backup succesvol aangemaakt (${file?.name})!"
                                                    backupList = BackupManager.listBackups(context)
                                                } else {
                                                    backupStatusMsg = "Fout: ${result.exceptionOrNull()?.message}"
                                                }
                                            }
                                        },
                                        enabled = !isBackingUp,
                                        colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("btn_create_backup")
                                    ) {
                                        if (isBackingUp) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                        } else {
                                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Maak Versleutelde Backup")
                                    }

                                    lastExportedFile?.let { file ->
                                        Button(
                                            onClick = { BackupManager.shareBackupToStorageOrCloud(context, file) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("btn_share_drive")
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Naar Drive / Cloud")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Automated Backup Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Automatische Backups",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Maakt automatisch periodiek een rolling versleutelde snapshot aan in de beveiligde app-opslag (max. 5 recente bewaard).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = autoBackupEnabled,
                                        onCheckedChange = { autoBackupEnabled = it },
                                        modifier = Modifier.testTag("switch_auto_backup")
                                    )
                                }
                            }
                        }
                    }

                    // Backup History & Restore List
                    item {
                        Text(
                            text = "LOKALE VERSLEUTELDE BACKUPS (${backupList.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (backupList.isEmpty()) {
                        item {
                            Text(
                                text = "Nog geen lokale backup bestanden aanwezig. Tik hierboven op 'Maak Versleutelde Backup'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(backupList) { bInfo ->
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("backup_item_${bInfo.name}"),
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
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (bInfo.isAutomated) Icons.Default.Schedule else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (bInfo.isAutomated) PrimaryBlue else TedPink,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = bInfo.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (bInfo.isAutomated) PrimaryBlue.copy(alpha = 0.15f) else TedPink.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (bInfo.isAutomated) "AUTOMATISCH" else "HANDMATIG",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (bInfo.isAutomated) PrimaryBlue else TedPink,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${dateFormat.format(Date(bInfo.timestamp))} • ${bInfo.sizeBytes / 1024} KB • AES-256",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { BackupManager.shareBackupToStorageOrCloud(context, bInfo.file) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Exporteer / Deel", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                        }

                                        IconButton(
                                            onClick = { selectedBackupForRestore = bInfo },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = "Herstel", tint = StatusPresent, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Status message
                backupStatusMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Restore Confirmation Dialog
    if (selectedBackupForRestore != null) {
        val b = selectedBackupForRestore!!
        AlertDialog(
            onDismissRequest = { selectedBackupForRestore = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = StatusPresent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Database Herstellen")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Weet u zeker dat u de database wilt herstellen vanaf '${b.name}'? Huidige records worden bijgewerkt met de inhoud van de backup.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = restorePassphrase,
                        onValueChange = { restorePassphrase = it },
                        label = { Text("Wachtwoord / PIN (indien ingesteld)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_restore_passphrase")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(b.file, restorePassphrase) { success, msg ->
                            backupStatusMsg = msg
                            selectedBackupForRestore = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusPresent),
                    modifier = Modifier.testTag("btn_confirm_restore")
                ) {
                    Text("Bevestig & Herstel")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBackupForRestore = null }) {
                    Text("Annuleren")
                }
            }
        )
    }
}
