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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.ui.common.ErrorBanner
import com.tdcostmanager.app.ui.common.GradientButton
import com.tdcostmanager.app.ui.common.LabeledTextField
import com.tdcostmanager.app.ui.theme.AppTheme

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearError() }

    LoginScreenContent(
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        uiState = uiState,
        onLoginClick = { viewModel.login(email.trim(), password) },
        onNavigateToRegister = onNavigateToRegister
    )
}

@Composable
fun LoginScreenContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    uiState: AuthUiState,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var submitted by rememberSaveable { mutableStateOf(false) }
    val emailError = if (submitted && email.isBlank()) "Introduce tu correo" else null
    val passwordError = if (submitted && password.isBlank()) "Introduce tu contraseña" else null
    val submit = {
        submitted = true
        if (email.isNotBlank() && password.isNotBlank()) onLoginClick()
    }

    AuthLayout(
        title = "Iniciar sesión",
        subtitle = "Accede a tus proyectos, catálogo y presupuestos.",
        footer = { AuthFooter("¿Aún no tienes cuenta?", "Crear cuenta", onNavigateToRegister) }
    ) {
        LabeledTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Correo electrónico",
            placeholder = "ejemplo@taller3d.com",
            leadingIcon = Icons.Outlined.Email,
            error = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(20.dp))
        PasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Contraseña",
            imeAction = ImeAction.Done,
            onDone = submit,
            error = passwordError
        )
        if (uiState is AuthUiState.Error) {
            Spacer(Modifier.height(16.dp))
            ErrorBanner(uiState.message)
        }
        Spacer(Modifier.height(28.dp))
        GradientButton(
            text = "Iniciar sesión",
            onClick = submit,
            loading = uiState is AuthUiState.Loading,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
fun LoginScreenPreview() {
    AppTheme {
        LoginScreenContent(
            email = "taller@ejemplo.es",
            onEmailChange = {},
            password = "",
            onPasswordChange = {},
            uiState = AuthUiState.Idle,
            onLoginClick = {},
            onNavigateToRegister = {}
        )
    }
}
