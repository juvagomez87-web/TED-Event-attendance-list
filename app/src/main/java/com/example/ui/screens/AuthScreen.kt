package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.TedPink
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.AppStrings
import com.example.util.Language

@Composable
fun AuthScreen(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Login, 1 = Register

    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("1234") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Register fields
    var regUsername by remember { mutableStateOf("") }
    var regDisplayName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("MEMBER") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo / Icon Header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1917)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ted_urban_bear_icon_1791446545651),
                        contentDescription = "TED PRIVATE App Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "TED PRIVATE",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = TedPink
                )

                Text(
                    text = AppStrings.get("app_subtitle", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mandatory auth banner (NO GUESTS ALLOWED)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security lock",
                            tint = TedPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == Language.NL) "Beveiligde toegang • Geen gastaccounts"
                            else if (lang == Language.ES) "Acceso seguro • Cuentas de invitado no permitidas"
                            else "Secure access • No guest accounts permitted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; errorMessage = null },
                        text = {
                            Text(
                                if (lang == Language.NL) "Inloggen" else if (lang == Language.ES) "Iniciar Sesión" else "Sign In",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_login")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; errorMessage = null },
                        text = {
                            Text(
                                if (lang == Language.NL) "Registreren" else if (lang == Language.ES) "Registrarse" else "Register",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_register")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Quick Demo Login Selector
                    Text(
                        text = if (lang == Language.NL) "Snelle selectie account:" else if (lang == Language.ES) "Selección rápida:" else "Quick account select:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = { username = "admin"; password = "1234" },
                            label = { Text("Admin (Alex)", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f).testTag("quick_login_admin")
                        )
                        SuggestionChip(
                            onClick = { username = "teddy"; password = "1234" },
                            label = { Text("TeddyGirl", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f).testTag("quick_login_teddy")
                        )
                        SuggestionChip(
                            onClick = { username = "sagittarius"; password = "1234" },
                            label = { Text("Sagittarius", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f).testTag("quick_login_sagittarius")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(if (lang == Language.NL) "Gebruikersnaam" else if (lang == Language.ES) "Usuario" else "Username") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_username")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("PIN / Wachtwoord") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_password")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                errorMessage = "Vul alle velden in."
                                return@Button
                            }
                            viewModel.login(username, password) { success, msg ->
                                if (!success) {
                                    errorMessage = msg
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("button_login"),
                        colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == Language.NL) "Inloggen & Rooster Openen"
                            else if (lang == Language.ES) "Acceder al Registro"
                            else "Sign In & Open Roster",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Registration Form
                    OutlinedTextField(
                        value = regUsername,
                        onValueChange = { regUsername = it },
                        label = { Text(if (lang == Language.NL) "Gebruikersnaam" else "Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reg_username")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = regDisplayName,
                        onValueChange = { regDisplayName = it },
                        label = { Text(if (lang == Language.NL) "Naam in clan / Roster" else "Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reg_display_name")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it },
                        label = { Text("Discord Handle / Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reg_email")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("PIN / Wachtwoord") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reg_password")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Role selector
                    Text(
                        text = if (lang == Language.NL) "Selecteer Rol:" else "Role:",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("MEMBER", "OFFICER", "ADMIN").forEach { role ->
                            FilterChip(
                                selected = regRole == role,
                                onClick = { regRole = role },
                                label = { Text(role, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (regUsername.isBlank() || regDisplayName.isBlank() || regPassword.isBlank()) {
                                errorMessage = "Gelieve alle verplichte velden in te vullen."
                                return@Button
                            }
                            viewModel.register(regUsername, regDisplayName, regEmail, regPassword, regRole) { success, msg ->
                                if (!success) {
                                    errorMessage = msg
                                } else {
                                    infoMessage = msg
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("button_register"),
                        colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == Language.NL) "Account Aanmaken"
                            else if (lang == Language.ES) "Crear Cuenta"
                            else "Create Account",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Error or Info banner
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                if (infoMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = infoMessage ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language quick toggle on auth screen
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Language / Taal:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Language.entries.forEach { l ->
                        FilterChip(
                            selected = lang == l,
                            onClick = { viewModel.setLanguage(l) },
                            label = { Text("${l.flag} ${l.name}") },
                            modifier = Modifier.testTag("auth_lang_${l.code}")
                        )
                    }
                }
            }
        }
    }
}
