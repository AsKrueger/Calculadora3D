package com.tdcostmanager.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.ui.common.LabeledTextField
import com.tdcostmanager.app.ui.common.ScreenPadding
import com.tdcostmanager.app.ui.common.TextLink
import com.tdcostmanager.app.ui.theme.AppThemeExtras

/** Marca de la app: cubo blanco sobre un degradado naranja, con un halo claro alrededor. */
@Composable
fun AppLogo(size: Int = 72) {
    val radius = (size * 0.3f).dp
    Box(
        modifier = Modifier
            .shadow(18.dp, RoundedCornerShape(radius + 6.dp), ambientColor = Color(0xFFF97316), spotColor = Color(0xFFF97316))
            .clip(RoundedCornerShape(radius + 6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(radius))
                .background(Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFC2410C)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ViewInAr,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size((size * 0.5f).dp)
            )
        }
    }
}

/** Estructura común de login y registro: marca arriba y formulario en una tarjeta con sombra suave. */
@Composable
fun AuthLayout(
    title: String,
    subtitle: String,
    footer: @Composable () -> Unit,
    form: @Composable ColumnScope.() -> Unit
) {
    val dark = AppThemeExtras.isDark
    val cardShape = RoundedCornerShape(28.dp)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding + 4.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogo()
            Spacer(Modifier.height(24.dp))
            Text(
                "3D Cost Manager",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Costes y presupuestos para tu taller de impresión 3D",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(36.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(if (dark) 0.dp else 18.dp, cardShape, ambientColor = Color(0x140F172A), spotColor = Color(0x1F0F172A))
                    .clip(cardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .then(if (dark) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), cardShape) else Modifier)
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                form()
            }
            Spacer(Modifier.height(28.dp))
            footer()
        }
    }
}

/** Pie de las pantallas de acceso: «¿Aún no tienes cuenta? Crear cuenta». */
@Composable
fun AuthFooter(question: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$question ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextLink(action, onAction)
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    onDone: () -> Unit = {},
    error: String? = null,
    helper: String? = null,
    labelTrailing: @Composable (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    LabeledTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = "••••••••",
        leadingIcon = Icons.Outlined.Lock,
        labelTrailing = labelTrailing,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        error = error,
        helper = helper,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() })
    )
}

/** Comprobación básica en cliente; la validación definitiva la hace el backend. */
fun isValidEmail(email: String): Boolean =
    Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())
