package com.tdcostmanager.app.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.BuildConfig
import com.tdcostmanager.app.ui.common.*
import com.tdcostmanager.app.ui.health.HealthUiState
import com.tdcostmanager.app.ui.health.HealthViewModel
import com.tdcostmanager.app.ui.theme.Tone

@Composable
fun AccountScreen(
    email: String?,
    healthViewModel: HealthViewModel,
    onLogout: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val health by healthViewModel.uiState.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold(topBar = { AppTopBar("Cuenta") }, bottomBar = bottomBar) { padding ->
        DetailColumn(padding) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Default.Person, size = 52)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sesión iniciada como", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(email ?: "—", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            SectionCard(
                title = "Servidor",
                trailing = {
                    when (health) {
                        is HealthUiState.Loading -> StatusChip("Comprobando…", Tone.Neutral)
                        is HealthUiState.Success -> StatusChip("Conectado", Tone.Success)
                        is HealthUiState.Error -> StatusChip("Sin conexión", Tone.Danger)
                    }
                }
            ) {
                Text(BuildConfig.BASE_URL, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = healthViewModel::checkBackendHealth, contentPadding = PaddingValues(0.dp)) {
                    Text("Volver a comprobar")
                }
            }

            SectionCard(title = "Aplicación") {
                InfoRow("Versión", BuildConfig.VERSION_NAME)
                InfoRow("Compilación", if (BuildConfig.DEBUG) "Depuración" else "Release")
            }

            OutlinedButton(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Cerrar sesión",
            text = "Tendrás que volver a introducir tu correo y contraseña para entrar.",
            confirmLabel = "Cerrar sesión",
            onConfirm = onLogout,
            onDismiss = { confirmLogout = false }
        )
    }
}
