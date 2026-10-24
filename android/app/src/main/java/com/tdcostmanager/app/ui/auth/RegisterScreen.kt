package com.tdcostmanager.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.ui.common.ErrorBanner
import com.tdcostmanager.app.ui.common.GradientButton
import com.tdcostmanager.app.ui.common.LabeledTextField

// Mismo mínimo que exige el backend en RegisterRequest
private const val MIN_PASSWORD_LENGTH = 8

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearError() }

    val emailError = if (submitted && !isValidEmail(email)) "Introduce un correo válido" else null
    val passwordError = if (submitted && password.length < MIN_PASSWORD_LENGTH) "Mínimo $MIN_PASSWORD_LENGTH caracteres" else null
    val confirmError = if (submitted && confirmPassword != password) "Las contraseñas no coinciden" else null

    val submit = {
        submitted = true
        if (isValidEmail(email) && password.length >= MIN_PASSWORD_LENGTH && confirmPassword == password) {
            viewModel.register(email.trim(), password)
        }
    }

    AuthLayout(
        title = "Crear cuenta",
        subtitle = "Cada cuenta tiene su propio catálogo, proyectos y presupuestos.",
        footer = { AuthFooter("¿Ya tienes cuenta?", "Iniciar sesión", onNavigateBack) }
    ) {
        LabeledTextField(
            value = email,
            onValueChange = { email = it },
            label = "Correo electrónico",
            placeholder = "ejemplo@taller3d.com",
            leadingIcon = Icons.Outlined.Email,
            error = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(20.dp))
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Contraseña",
            imeAction = ImeAction.Next,
            error = passwordError,
            helper = "Al menos $MIN_PASSWORD_LENGTH caracteres"
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Repite la contraseña",
            imeAction = ImeAction.Done,
            onDone = submit,
            error = confirmError
        )
        if (uiState is AuthUiState.Error) {
            Spacer(Modifier.height(16.dp))
            ErrorBanner((uiState as AuthUiState.Error).message)
        }
        Spacer(Modifier.height(28.dp))
        GradientButton(
            text = "Crear cuenta",
            onClick = submit,
            loading = uiState is AuthUiState.Loading,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
        )
    }
}
