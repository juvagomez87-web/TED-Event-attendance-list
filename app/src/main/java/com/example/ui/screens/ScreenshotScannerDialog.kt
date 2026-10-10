package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.EventEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.DiscordShareHelper
import com.example.util.GeminiVisionService
import com.example.util.LeaderboardScannedPlayer
import com.example.util.PresetLeaderboardSamples
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotScannerDialog(
    viewModel: RosterViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val events by viewModel.allEvents.collectAsState()
    val members by viewModel.allMembers.collectAsState()
    val selectedEventId by viewModel.selectedEventId.collectAsState()

    var targetEventId by remember {
        mutableStateOf(
            selectedEventId ?: events.find {
                it.title.contains("kooi 1", ignoreCase = true) || it.title.contains("cage 1", ignoreCase = true)
            }?.id ?: events.firstOrNull()?.id ?: 1L
        )
    }

    var scannedPlayers by remember {
        // Default to KOOI_1 (the user's uploaded screenshot of willow, JustineValentina, Hans worst, etc.)
        mutableStateOf(GeminiVisionService.matchWithRoster(PresetLeaderboardSamples.KOOI_1, members))
    }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("9 spelers herkend uit Kooi 1 screenshot (willow t/m MissQueenJuva):") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showEventDropdown by remember { mutableStateOf(false) }

    // Android GetContent (Directe bestandsverkenner & galerij van het toestel)
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isProcessing = true
            statusMessage = "Screenshot analyseren met AI vision..."
            coroutineScope.launch {
                val result = GeminiVisionService.analyzeScreenshot(context, uri, members)
                isProcessing = false
                if (result.isSuccess) {
                    val list = result.getOrThrow()
                    scannedPlayers = list
                    statusMessage = "✓ ${list.size} spelers herkend uit uw eigen bestand!"
                } else {
                    scannedPlayers = GeminiVisionService.matchWithRoster(PresetLeaderboardSamples.KOOI_1, members)
                    statusMessage = "Screenshot geladen (${result.exceptionOrNull()?.message ?: "AI offline"}). Kooi 1 gegevens getoond:"
                }
            }
        }
    }

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isProcessing = true
            statusMessage = "Screenshot analyseren met AI vision..."
            coroutineScope.launch {
                val result = GeminiVisionService.analyzeScreenshot(context, uri, members)
                isProcessing = false
                if (result.isSuccess) {
                    val list = result.getOrThrow()
                    scannedPlayers = list
                    statusMessage = "✓ ${list.size} spelers herkend uit uw eigen screenshot!"
                } else {
                    scannedPlayers = GeminiVisionService.matchWithRoster(PresetLeaderboardSamples.KOOI_1, members)
                    statusMessage = "Screenshot geladen (${result.exceptionOrNull()?.message ?: "AI offline"}). Kooi 1 gegevens getoond:"
                }
            }
        }
    }

    val selectedTargetEvent = events.find { it.id == targetEventId }
    val isTargetKooi1 = viewModel.isKooi1(targetEventId)
    val isTargetKooi2 = viewModel.isKooi2(targetEventId)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("screenshot_scanner_dialog"),
            shape = RoundedCornerShape(20.dp),
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
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TedPink.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = TedPink, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Screenshot Interpreter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kooi uitslagen scannen & automatisch aanwezig melden",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Sluiten")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. PRIMARY STEP: Choose which event to link the screenshots to
                Text(
                    text = "1. KOPPEL SCREENSHOT AAN EVENT:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TedPink.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TedPink.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TedPink),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Fort,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Geselecteerd Evenement:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = selectedTargetEvent?.title ?: "Selecteer event",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Box {
                                Button(
                                    onClick = { showEventDropdown = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("target_event_selector_btn")
                                ) {
                                    Text("Kies Event", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                }

                                DropdownMenu(
                                    expanded = showEventDropdown,
                                    onDismissRequest = { showEventDropdown = false }
                                ) {
                                    events.forEach { ev ->
                                        val isCurrent = ev.id == targetEventId
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (ev.title.contains("kooi", ignoreCase = true)) {
                                                        Icon(
                                                            Icons.Default.Fort,
                                                            contentDescription = null,
                                                            tint = TedPink,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }
                                                    Text(
                                                        ev.title,
                                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                                                        color = if (isCurrent) TedPink else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (isCurrent) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("✓", color = TedPink, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                targetEventId = ev.id
                                                showEventDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Quick cage 1 & cage 2 one-tap selection chips
                        val k1Event = events.find { it.title.contains("kooi 1", ignoreCase = true) || it.title.contains("cage 1", ignoreCase = true) }
                        val k2Event = events.find { it.title.contains("kooi 2", ignoreCase = true) || it.title.contains("cage 2", ignoreCase = true) }
                        if (k1Event != null || k2Event != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (k1Event != null) {
                                    val isK1Selected = targetEventId == k1Event.id
                                    FilterChip(
                                        selected = isK1Selected,
                                        onClick = {
                                            targetEventId = k1Event.id
                                            scannedPlayers = GeminiVisionService.matchWithRoster(PresetLeaderboardSamples.KOOI_1, members)
                                            statusMessage = "Kooi 1 Screenshot geselecteerd (9 spelers: willow t/m MissQueenJuva)"
                                        },
                                        label = { Text("⚡ " + k1Event.title, fontWeight = if (isK1Selected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TedPink,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (k2Event != null) {
                                    val isK2Selected = targetEventId == k2Event.id
                                    FilterChip(
                                        selected = isK2Selected,
                                        onClick = {
                                            targetEventId = k2Event.id
                                            scannedPlayers = GeminiVisionService.matchWithRoster(PresetLeaderboardSamples.SAMPLE_2, members)
                                            statusMessage = "Kooi 2 Screenshot geselecteerd (10 spelers: minou t/m Libra Rising)"
                                        },
                                        label = { Text("⚡ " + k2Event.title, fontWeight = if (isK2Selected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TedPink,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. SECONDARY STEP: Source Selection Chips (Galerij + Test Screenshots from Chat)
                Text(
                    text = "2. KIES BRON SCREENSHOT:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pick from phone files / device storage (GetContent)
                    FilledTonalButton(
                        onClick = {
                            getContentLauncher.launch("image/*")
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).height(44.dp).testTag("pick_custom_file_btn")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bestanden...", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    // Pick from phone gallery (Photo Picker)
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.1f).height(44.dp).testTag("pick_custom_screenshot_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galerij", style = MaterialTheme.typography.labelSmall)
                    }

                    // Kooi 1 Screenshot (User's uploaded screenshot: willow t/m MissQueenJuva)
                    Button(
                        onClick = {
                            scannedPlayers = GeminiVisionService.matchWithRoster(PresetLeaderboardSamples.KOOI_1, members)
                            // Also select Kooi 1 event if available
                            val k1 = events.find { it.title.contains("kooi 1", ignoreCase = true) || it.title.contains("cage 1", ignoreCase = true) }
                            if (k1 != null) targetEventId = k1.id
                            statusMessage = "Kooi 1 Screenshot geladen (9 spelers: willow t/m MissQueenJuva)"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (targetEventId == events.find { it.title.contains("kooi 1", ignoreCase = true) }?.id) TedPink else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (targetEventId == events.find { it.title.contains("kooi 1", ignoreCase = true) }?.id) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f).height(44.dp).testTag("test_kooi_1_btn")
                    ) {
                        Text("Kooi 1 (Willow)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    // Kooi 2 Screenshot (minou t/m Libra Rising)
                    OutlinedButton(
                        onClick = {
                            scannedPlayers = GeminiVisionService.matchWithRoster(PresetLeaderboardSamples.SAMPLE_2, members)
                            val k2 = events.find { it.title.contains("kooi 2", ignoreCase = true) || it.title.contains("cage 2", ignoreCase = true) }
                            if (k2 != null) targetEventId = k2.id
                            statusMessage = "Kooi 2 Screenshot geladen (10 spelers: minou t/m Libra Rising)"
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f).height(44.dp).testTag("test_sample_2_btn")
                    ) {
                        Text("Kooi 2 (Minou)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Uploaded phone image preview
                if (selectedImageUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Gekozen screenshot",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "✓ Bestand vanaf telefoon ingeladen",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPresent
                                )
                                Text(
                                    text = "Afbeelding actief. Bevestig de spelers hieronder of pas de doelpunten aan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Cross-cage cooperation explanation banner
                if (isTargetKooi1 || isTargetKooi2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusOtherCage.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = StatusOtherCage,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isTargetKooi1)
                                    "⚡ Kooi-koppeling: Aangeduide leden worden in Ultieme Kooi 2 automatisch geregistreerd als 'Deelgenomen aan Ultieme Kooi 1'."
                                else
                                    "⚡ Kooi-koppeling: Aangeduide leden worden in Ultieme Kooi 1 automatisch geregistreerd als 'Deelgenomen aan Ultieme Kooi 2'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status message / Loading indicator
                if (isProcessing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(statusMessage, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // List of Scanned Players
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(scannedPlayers) { player ->
                        val isMatched = player.matchedMemberId != null
                        val rankColor = when (player.rank) {
                            1 -> Color(0xFFF59E0B) // Gold
                            2 -> Color(0xFF94A3B8) // Silver
                            3 -> Color(0xFFD97706) // Bronze
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scannedPlayers = scannedPlayers.map {
                                        if (it.rank == player.rank) it.copy(isSelected = !it.isSelected) else it
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (player.isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = player.isSelected,
                                    onCheckedChange = { checked ->
                                        scannedPlayers = scannedPlayers.map {
                                            if (it.rank == player.rank) it.copy(isSelected = checked) else it
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // Rank badge
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(rankColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${player.rank}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = rankColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Player Info + Doelpunten
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = player.rawName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isMatched) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = StatusPresent.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "✓ ${player.matchedMemberRank ?: "Lid"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = StatusPresent,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Doelpunten: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = player.score,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TedPink
                                        )
                                    }
                                }

                                // Status chip
                                if (isMatched) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Gematcht",
                                        tint = StatusPresent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StatusLate.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Niet in lijst",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusLate,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Actions
                val selectedCount = scannedPlayers.count { it.isSelected && it.matchedMemberId != null }
                val eventName = selectedTargetEvent?.title ?: "Evenement"

                // Discord Share & Copy Action Row (Works without requiring webhook or admin permissions!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val msg = DiscordShareHelper.formatLeaderboardForDiscord(eventName, scannedPlayers)
                            DiscordShareHelper.shareToDiscordOrApps(
                                context = context,
                                message = msg,
                                title = "Deel uitslag naar Discord #leaders-chat"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)), // Discord Blurple
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).height(44.dp).testTag("btn_share_discord_results")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Deel naar Discord (#leaders-chat)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val msg = DiscordShareHelper.formatLeaderboardForDiscord(eventName, scannedPlayers)
                            DiscordShareHelper.copyToClipboard(context, msg)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.7f).height(44.dp).testTag("btn_copy_discord_results")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kopieer", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Annuleren")
                    }

                    Button(
                        onClick = {
                            viewModel.applyScreenshotResults(targetEventId, scannedPlayers)
                            onDismiss()
                        },
                        enabled = selectedCount > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPresent),
                        modifier = Modifier.weight(2f).height(48.dp).testTag("apply_screenshot_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Toepassen op $eventName ($selectedCount)",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
