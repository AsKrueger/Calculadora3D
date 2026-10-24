package com.tdcostmanager.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.ui.common.AppTextField
import com.tdcostmanager.app.ui.common.ErrorBanner
import com.tdcostmanager.app.ui.common.PrimaryButton
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
    val canSubmit = email.isNotBlank() && password.isNotBlank()

    AuthLayout(
        title = "Iniciar sesión",
        subtitle = "Accede a tus proyectos, catálogo y presupuestos.",
        footer = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Aún no tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onNavigateToRegister) { Text("Crear cuenta") }
            }
        }
    ) {
        AppTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Correo electrónico",
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Contraseña",
            imeAction = ImeAction.Done,
            onDone = { if (canSubmit) onLoginClick() }
        )
        if (uiState is AuthUiState.Error) {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(uiState.message)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Iniciar sesión",
            onClick = onLoginClick,
            enabled = canSubmit,
            loading = uiState is AuthUiState.Loading
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
