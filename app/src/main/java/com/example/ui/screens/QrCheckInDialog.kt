package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.EventEntity
import com.example.ui.theme.StatusPresent
import com.example.ui.theme.TedPink
import com.example.ui.viewmodel.RosterViewModel
import java.util.Random

@Composable
fun QrCheckInDialog(
    event: EventEntity,
    viewModel: RosterViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val checkInCode = "TED-${event.category.take(3).uppercase()}-${event.id * 137 % 900 + 100}"

    var inputCode by remember { mutableStateOf("") }
    var checkInSuccess by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = TedPink, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "1-Tap Aanwezigheidsregistratie",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Matrix Canvas
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridSize = 19
                        val cellSize = size.width / gridSize
                        val rand = Random((event.id * 7331 + 42).toLong())

                        for (r in 0 until gridSize) {
                            for (c in 0 until gridSize) {
                                // Corner anchors
                                val isTopLeftAnchor = (r in 0..5 && c in 0..5)
                                val isTopRightAnchor = (r in 0..5 && c in (gridSize - 6) until gridSize)
                                val isBottomLeftAnchor = (r in (gridSize - 6) until gridSize && c in 0..5)

                                val shouldFill = if (isTopLeftAnchor) {
                                    r == 0 || r == 5 || c == 0 || c == 5 || (r in 2..3 && c in 2..3)
                                } else if (isTopRightAnchor) {
                                    r == 0 || r == 5 || c == gridSize - 6 || c == gridSize - 1 || (r in 2..3 && c in (gridSize - 4)..(gridSize - 3))
                                } else if (isBottomLeftAnchor) {
                                    r == gridSize - 6 || r == gridSize - 1 || c == 0 || c == 5 || (r in (gridSize - 4)..(gridSize - 3) && c in 2..3)
                                } else {
                                    rand.nextBoolean()
                                }

                                if (shouldFill) {
                                    drawRect(
                                        color = Color(0xFF0F172A),
                                        topLeft = Offset(c * cellSize, r * cellSize),
                                        size = Size(cellSize, cellSize)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fast Check-In Code Badge
                Text(
                    text = "DISCORD SNELCODE:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = checkInCode,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (checkInSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusPresent.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusPresent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aanwezigheid geregistreerd voor ${currentUser?.displayName}!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusPresent
                            )
                        }
                    }
                } else {
                    // Check-in input or 1-tap claim
                    currentUser?.memberId?.let { mId ->
                        Button(
                            onClick = {
                                viewModel.setMemberAttendance(event.id, mId, "PRESENT", "1-Tap QR Check-in verified")
                                checkInSuccess = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_1tap_checkin")
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1-Tap Check-in als ${currentUser?.displayName}", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Manual code validator for other attendees
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it.uppercase() },
                        label = { Text("Of voer code in") },
                        placeholder = { Text(checkInCode) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_checkin_code")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            if (inputCode.trim() == checkInCode) {
                                currentUser?.memberId?.let { mId ->
                                    viewModel.setMemberAttendance(event.id, mId, "PRESENT", "Code check-in verified")
                                }
                                checkInSuccess = true
                            }
                        },
                        enabled = inputCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("btn_verify_code")
                    ) {
                        Text("Verifieer Code")
                    }
                }
            }
        }
    }
}
