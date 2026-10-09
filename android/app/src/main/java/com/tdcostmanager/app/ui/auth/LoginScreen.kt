package com.tdcostmanager.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Iniciar Sesión, 1: Registrar Taller

    LoginScreenContent(
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        passwordVisible = passwordVisible,
        onPasswordVisibleChange = { passwordVisible = it },
        rememberMe = rememberMe,
        onRememberMeChange = { rememberMe = it },
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        uiState = uiState,
        onLoginClick = { viewModel.login(email, password) },
        onNavigateToRegister = onNavigateToRegister
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleChange: (Boolean) -> Unit,
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    uiState: AuthUiState,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val primaryColor = Color(0xFF534391) // Deep purple from mockup
    val backgroundColor = Color(0xFFFBF8FF)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Top App Icon Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Subtitle
            Text(
                text = "3D Cost Manager",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1D1B20)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Gestión y Cotización Determinista de Fabricación Aditiva",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF49454F)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Segmented Tab Switcher ("Iniciar Sesión" / "Registrar Taller")
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFE7E0EC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabButton(
                        text = "Iniciar Sesión",
                        selected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(0) }
                    )
                    TabButton(
                        text = "Registrar Taller",
                        selected = selectedTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onTabSelected(1)
                            onNavigateToRegister()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Access Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Acceso al Taller",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1D1B20)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Introduce tus credenciales para acceder a la gestión del taller.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Email Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Correo Electrónico", style = MaterialTheme.typography.labelMedium, color = primaryColor)
                        Text(text = "Requerido", style = MaterialTheme.typography.labelSmall, color = Color(0xFF49454F))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF49454F)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFE7E0EC),
                            focusedContainerColor = Color(0xFFF7F2FA),
                            unfocusedContainerColor = Color(0xFFF7F2FA)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Contraseña de Taller", style = MaterialTheme.typography.labelMedium, color = primaryColor)
                        TextButton(onClick = {}, contentPadding = PaddingValues(0.dp)) {
                            Text(text = "¿Olvidaste tu contraseña?", style = MaterialTheme.typography.labelSmall, color = primaryColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF49454F)) },
                        trailingIcon = {
                            IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFF49454F)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFE7E0EC),
                            focusedContainerColor = Color(0xFFF7F2FA),
                            unfocusedContainerColor = Color(0xFFF7F2FA)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Remember Me Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = onRememberMeChange,
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                        )
                        Text(text = "Recordar sesión en este equipo", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1D1B20))
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF49454F), modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Message Display
                    if (uiState is AuthUiState.Error) {
                        Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Main Action Button ("Acceder al Panel")
                    Button(
                        onClick = onLoginClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = email.isNotBlank() && password.isNotBlank() && uiState !is AuthUiState.Loading
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Acceder al Panel",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Security Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE7E0EC)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF49454F), modifier = Modifier.size(14.dp))
                    Text(
                        text = "Almacenamiento Seguro AES-256 • Conexión TLS / JWT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF49454F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register prompt footer
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "¿No tienes cuenta de taller? ", style = MaterialTheme.typography.bodySmall, color = Color(0xFF49454F))
                TextButton(onClick = onNavigateToRegister, contentPadding = PaddingValues(0.dp)) {
                    Text(text = "Registra tu taller 3D", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = primaryColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) Color.White else Color.Transparent
    val textColor = if (selected) Color(0xFF1D1B20) else Color(0xFF49454F)
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
                color = textColor
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreenContent(
            email = "admin@empresa.com",
            onEmailChange = {},
            password = "password123",
            onPasswordChange = {},
            passwordVisible = false,
            onPasswordVisibleChange = {},
            rememberMe = true,
            onRememberMeChange = {},
            selectedTab = 0,
            onTabSelected = {},
            uiState = AuthUiState.Idle,
            onLoginClick = {},
            onNavigateToRegister = {}
        )
    }
}
