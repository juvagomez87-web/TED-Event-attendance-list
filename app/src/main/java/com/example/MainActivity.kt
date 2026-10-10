package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TedPink
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.AppStrings
import com.example.util.Language
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {
    private val viewModel: RosterViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                val lang by viewModel.currentLanguage.collectAsState()
                val currentTab by viewModel.currentTab.collectAsState()
                val showImportDialog by viewModel.showImportDialog.collectAsState()
                val showScreenshotDialog by viewModel.showScreenshotDialog.collectAsState()
                val showSyncDialog by viewModel.showSyncDialog.collectAsState()
                val showBackupDialog by viewModel.showBackupDialog.collectAsState()
                val qrCheckInEvent by viewModel.qrCheckInEvent.collectAsState()
                val isOnline by viewModel.isOnline.collectAsState()
                val conflictAttendances by viewModel.conflictAttendances.collectAsState()
                val snackBarMessage by viewModel.snackBarMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                var showLangMenu by remember { mutableStateOf(false) }
                var showProfileMenu by remember { mutableStateOf(false) }

                LaunchedEffect(snackBarMessage) {
                    snackBarMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSnackBar()
                    }
                }

                if (currentUser == null) {
                    // Mandatory personal login (no guest access allowed)
                    AuthScreen(viewModel = viewModel)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1C1917)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ted_urban_bear_icon_1791446545651),
                                                contentDescription = "TED Bear Icon",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "TED PRIVATE",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = TedPink
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = TedPink.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "LAND OF JAILS",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TedPink,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${currentUser?.displayName ?: "JustineValentina"} • Admin",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TedPink
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    // Language Switcher Dropdown
                                    Box {
                                        TextButton(
                                            onClick = { showLangMenu = true },
                                            modifier = Modifier.testTag("top_lang_selector")
                                        ) {
                                            Text("${lang.flag} ${lang.name}")
                                        }

                                        DropdownMenu(
                                            expanded = showLangMenu,
                                            onDismissRequest = { showLangMenu = false }
                                        ) {
                                            Language.entries.forEach { l ->
                                                DropdownMenuItem(
                                                    text = { Text("${l.flag} ${l.displayName}") },
                                                    onClick = {
                                                        viewModel.setLanguage(l)
                                                        showLangMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Cloud & Offline Sync Pill
                                    IconButton(
                                        onClick = { viewModel.openSyncDialog() },
                                        modifier = Modifier.testTag("top_sync_btn")
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (conflictAttendances.isNotEmpty()) {
                                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                        Text("${conflictAttendances.size}")
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isOnline) Icons.Default.CloudSync else Icons.Default.CloudOff,
                                                contentDescription = "Cloud Sync",
                                                tint = if (conflictAttendances.isNotEmpty()) MaterialTheme.colorScheme.error
                                                else if (isOnline) Color(0xFF10B981)
                                                else Color(0xFFF59E0B)
                                            )
                                        }
                                    }

                                    // PDF Export
                                    IconButton(
                                        onClick = { viewModel.exportPdfReport() },
                                        modifier = Modifier.testTag("top_export_pdf_btn")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = TedPink)
                                    }

                                    // Encrypted Backup
                                    IconButton(
                                        onClick = { viewModel.openBackupDialog() },
                                        modifier = Modifier.testTag("top_backup_btn")
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = "Database Backup", tint = Color(0xFFF59E0B))
                                    }

                                    // Profile & Logout Menu
                                    Box {
                                        IconButton(
                                            onClick = { showProfileMenu = true },
                                            modifier = Modifier.testTag("top_profile_btn")
                                        ) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                                        }

                                        DropdownMenu(
                                            expanded = showProfileMenu,
                                            onDismissRequest = { showProfileMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(currentUser?.displayName ?: "", fontWeight = FontWeight.Bold)
                                                        Text(currentUser?.role ?: "", style = MaterialTheme.typography.labelSmall, color = TedPink)
                                                    }
                                                },
                                                onClick = {}
                                            )
                                            Divider()
                                            DropdownMenuItem(
                                                text = { Text("Versleutelde Backups") },
                                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                                onClick = {
                                                    viewModel.openBackupDialog()
                                                    showProfileMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Afmelden / Logout") },
                                                leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
                                                onClick = {
                                                    viewModel.logout()
                                                    showProfileMenu = false
                                                }
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == "EVENTS",
                                    onClick = { viewModel.setTab("EVENTS") },
                                    icon = { Icon(Icons.Default.EventNote, contentDescription = "Events") },
                                    label = { Text(AppStrings.get("nav_events", lang), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_events")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "ROSTER",
                                    onClick = { viewModel.setTab("ROSTER") },
                                    icon = { Icon(Icons.Default.Groups, contentDescription = "Roster") },
                                    label = { Text(AppStrings.get("nav_roster", lang), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_roster")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "STATS",
                                    onClick = { viewModel.setTab("STATS") },
                                    icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Stats") },
                                    label = { Text(AppStrings.get("nav_stats", lang), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_stats")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "USERS",
                                    onClick = { viewModel.setTab("USERS") },
                                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Users") },
                                    label = { Text("Logins", fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_users")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "AUDIT",
                                    onClick = { viewModel.setTab("AUDIT") },
                                    icon = { Icon(Icons.Default.History, contentDescription = "Audit") },
                                    label = { Text(AppStrings.get("nav_audit", lang), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_audit")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "UPDATES",
                                    onClick = { viewModel.setTab("UPDATES") },
                                    icon = { Icon(Icons.Default.Campaign, contentDescription = "Updates") },
                                    label = { Text(AppStrings.get("nav_updates", lang), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_updates")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Bloodthirsty Ted custom background
                            Image(
                                painter = painterResource(id = R.drawable.ted_app_background),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.48f
                            )

                            when (currentTab) {
                                "EVENTS" -> EventsScreen(viewModel = viewModel)
                                "ROSTER" -> MembersScreen(viewModel = viewModel)
                                "STATS" -> DashboardScreen(viewModel = viewModel)
                                "USERS" -> UserAccountsScreen(viewModel = viewModel)
                                "AUDIT" -> AuditLogScreen(viewModel = viewModel)
                                "UPDATES" -> UpdatesScreen(viewModel = viewModel)
                                else -> EventsScreen(viewModel = viewModel)
                            }
                        }
                    }

                    // PDF Import Dialog modal
                    if (showImportDialog) {
                        PdfImportDialog(
                            viewModel = viewModel,
                            onDismiss = { viewModel.closeImportDialog() }
                        )
                    }

                    // Cloud & Offline Sync Dialog
                    if (showSyncDialog) {
                        SyncManagerDialog(
                            viewModel = viewModel,
                            onDismiss = { viewModel.closeSyncDialog() }
                        )
                    }

                    // 1-Tap QR Check-in Dialog
                    qrCheckInEvent?.let { ev ->
                        QrCheckInDialog(
                            event = ev,
                            viewModel = viewModel,
                            onDismiss = { viewModel.closeQrCheckIn() }
                        )
                    }

                    // Encrypted Backup Dialog modal
                    if (showBackupDialog) {
                        BackupDialog(
                            viewModel = viewModel,
                            onDismiss = { viewModel.closeBackupDialog() }
                        )
                    }

                    // Screenshot Scanner Dialog modal
                    if (showScreenshotDialog) {
                        ScreenshotScannerDialog(
                            viewModel = viewModel,
                            onDismiss = { viewModel.closeScreenshotDialog() }
                        )
                    }
                }
            }
        }
    }
}
