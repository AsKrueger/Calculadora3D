package com.tdcostmanager.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var workshopName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedSpecialty by remember { mutableStateOf("FDM Industrial") }
    var acceptedTerms by remember { mutableStateOf(true) }

    val primaryColor = Color(0xFF534391)
    val backgroundColor = Color(0xFFFBF8FF)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onNavigateBack,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Text(text = "Volver al Login", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = primaryColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Crear Cuenta de Taller",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1D1B20)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Empieza a calcular costes de fabricación aditiva con precisión determinista y control total de overheads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8DEF8),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = "NUEVO TALLER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Form Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Workshop Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Nombre del Taller / Maker", style = MaterialTheme.typography.labelMedium, color = primaryColor)
                        Text(text = "Requerido", style = MaterialTheme.typography.labelSmall, color = Color(0xFF49454F))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = workshopName,
                        onValueChange = { workshopName = it },
                        placeholder = { Text("TechMaker Studio Pro") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF49454F)) },
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

                    // Email
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
                        onValueChange = { email = it },
                        placeholder = { Text("usuario@ejemplo.com") },
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

                    // Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Contraseña de Taller", style = MaterialTheme.typography.labelMedium, color = primaryColor)
                        Text(text = "Mín. 8 caracteres", style = MaterialTheme.typography.labelSmall, color = Color(0xFF49454F))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF49454F)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
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

                    // Security Strength Bar & Checklist
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF7F2FA),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Seguridad:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF49454F))
                                Text(
                                    text = if (password.length >= 8) "Fuerte (Óptima para Taller)" else "Débil",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (password.length >= 8) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { if (password.length >= 8) 1f else (password.length / 8f).coerceIn(0.1f, 0.9f) },
                                color = if (password.length >= 8) Color(0xFF10B981) else Color(0xFF534391),
                                trackColor = Color(0xFFE7E0EC),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SecurityCheckItem(text = "✓ 8+ car.", met = password.length >= 8)
                                SecurityCheckItem(text = "✓ Números", met = password.any { it.isDigit() })
                                SecurityCheckItem(text = "✓ Símbolo", met = password.any { !it.isLetterOrDigit() })
                                SecurityCheckItem(text = "✓ Mayús", met = password.any { it.isUpperCase() })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password
                    Text(text = "Confirmar Contraseña", style = MaterialTheme.typography.labelMedium, color = primaryColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFF49454F)) },
                        trailingIcon = {
                            if (confirmPassword.isNotBlank() && confirmPassword == password) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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

                    // Specialty Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Especialidad Principal", style = MaterialTheme.typography.labelMedium, color = primaryColor)
                        Text(text = "Opcional", style = MaterialTheme.typography.labelSmall, color = Color(0xFF49454F))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpecialtyChip(title = "FDM Industrial", selected = selectedSpecialty == "FDM Industrial", onClick = { selectedSpecialty = "FDM Industrial" })
                        SpecialtyChip(title = "Resina SLA", selected = selectedSpecialty == "Resina SLA", onClick = { selectedSpecialty = "Resina SLA" })
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Terms Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = { acceptedTerms = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                        )
                        Text(
                            text = "Acepto los Términos de Servicio de Cost Manager y la Política de Cifrado AES-256 para fórmulas de taller.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF49454F)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error display
                    if (uiState is AuthUiState.Error) {
                        Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Main Action Button
                    Button(
                        onClick = { viewModel.register(email, password) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = email.isNotBlank() && password.isNotBlank() && acceptedTerms && uiState !is AuthUiState.Loading
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Crear Cuenta y Configurar Taller",
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

            // Footer
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "¿Ya tienes una cuenta de taller? ", style = MaterialTheme.typography.bodySmall, color = Color(0xFF49454F))
                TextButton(onClick = onNavigateBack, contentPadding = PaddingValues(0.dp)) {
                    Text(text = "Iniciar Sesión", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = primaryColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        text = "Almacenamiento Seguro AES-256 (EncryptedSharedPreferences) • JWT Auth",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF49454F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SecurityCheckItem(text: String, met: Boolean) {
    val color = if (met) Color(0xFF10B981) else Color(0xFF49454F)
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
fun SpecialtyChip(title: String, selected: Boolean, onClick: () -> Unit) {
    val primaryColor = Color(0xFF534391)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFE8DEF8) else Color(0xFFF7F2FA),
        border = if (selected) BorderStroke(1.dp, primaryColor) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                color = if (selected) primaryColor else Color(0xFF49454F)
            )
        }
    }
}
