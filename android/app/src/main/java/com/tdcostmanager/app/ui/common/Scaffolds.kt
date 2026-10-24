package com.tdcostmanager.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla de formulario: campos con scroll y el botón de guardar fijo abajo,
 * por encima del teclado.
 */
@Composable
fun FormScaffold(
    title: String,
    onBack: () -> Unit,
    saveLabel: String,
    onSave: () -> Unit,
    saving: Boolean,
    error: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = { AppTopBar(title, onBack = onBack) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = ScreenPadding, vertical = 12.dp)
                ) {
                    if (error != null) {
                        ErrorBanner(error)
                        Spacer(Modifier.height(12.dp))
                    }
                    PrimaryButton(text = saveLabel, onClick = onSave, loading = saving)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Columna con scroll y el margen estándar para pantallas de detalle. */
@Composable
fun DetailColumn(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}
