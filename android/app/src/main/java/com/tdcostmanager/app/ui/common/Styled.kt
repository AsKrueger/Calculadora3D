package com.tdcostmanager.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdcostmanager.app.ui.theme.AppThemeExtras
import com.tdcostmanager.app.ui.theme.Tone

// Piezas del lenguaje visual del diseño de referencia del login: botón en degradado y campo con etiqueta.

private val FieldShape = RoundedCornerShape(16.dp)
private val ButtonShape = RoundedCornerShape(18.dp)

/** Botón principal en degradado naranja, con sombra del mismo color e icono opcional detrás del texto. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    trailingIcon: ImageVector? = null
) {
    val active = enabled && !loading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(
                elevation = if (active) 14.dp else 0.dp,
                shape = ButtonShape,
                ambientColor = Color(0xFFF97316),
                spotColor = Color(0xFFF97316)
            )
            .clip(ButtonShape)
            .background(Brush.horizontalGradient(AppThemeExtras.accentGradient))
            .clickable(enabled = active, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                if (trailingIcon != null) {
                    Spacer(Modifier.width(10.dp))
                    Icon(trailingIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/** Etiqueta en mayúsculas que va encima de un campo, con un elemento opcional a la derecha. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
            color = AppThemeExtras.field.label,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Campo relleno con borde suave, etiqueta encima e icono de línea a la izquierda. */
@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    labelTrailing: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    error: String? = null,
    helper: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val field = AppThemeExtras.field
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label, trailing = labelTrailing)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = trailingIcon,
            singleLine = true,
            isError = error != null,
            supportingText = (error ?: helper)?.let { { Text(it) } },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = FieldShape,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = field.container,
                focusedContainerColor = field.container,
                errorContainerColor = field.container,
                unfocusedBorderColor = field.border,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Enlace de texto en el color de acento. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, bold: Boolean = true) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium),
        color = Color(0xFFEA580C),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp)
    )
}

private val SoftShadow = Color(0x1F0F172A)

/** Botón redondo blanco con sombra suave, para acciones de cabecera (buscar, ajustes…). */
@Composable
fun CircleIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, selected: Boolean = false) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .shadow(8.dp, CircleShape, ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Filtro en forma de píldora. Seleccionado: fondo grafito y texto blanco.
 * Sin seleccionar: blanco con borde o, si [outlined] es false, solo texto.
 */
@Composable
fun PillChip(label: String, selected: Boolean, onClick: () -> Unit, count: Int? = null, outlined: Boolean = true) {
    val container = when {
        selected -> MaterialTheme.colorScheme.inverseSurface
        outlined -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> Color.Transparent
    }
    val content = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(container)
            .then(
                if (!selected && outlined) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) else Modifier
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium), color = content)
        if (count != null) {
            Spacer(Modifier.width(10.dp))
            Text("$count", style = MaterialTheme.typography.titleSmall, color = content.copy(alpha = 0.6f))
        }
    }
}

/** Botón flotante extendido en melocotón con resplandor naranja. */
@Composable
fun GlowFab(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dark = AppThemeExtras.isDark
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .height(60.dp)
            .shadow(16.dp, shape, ambientColor = Color(0xFFF97316), spotColor = Color(0xFFF97316))
            .clip(shape)
            .background(
                if (dark) Brush.horizontalGradient(listOf(Color(0xFF7A2E0B), Color(0xFF9A3A10)))
                else Brush.horizontalGradient(listOf(Color(0xFFFFEDDD), Color(0xFFFFD9B8)))
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val content = if (dark) Color(0xFFFFDBC9) else Color(0xFF5A1E05)
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = content)
    }
}

/** Importe destacado en una etiqueta con el color de un tono (borrador, completado…). */
@Composable
fun PriceTag(text: String, tone: Tone) {
    val colors = AppThemeExtras.tone(tone)
    val shape = RoundedCornerShape(10.dp)
    Text(
        text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        color = colors.content,
        modifier = Modifier
            .clip(shape)
            .background(colors.container)
            .border(1.dp, colors.content.copy(alpha = 0.18f), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** Tarjeta blanca de esquinas amplias con sombra difusa; en modo oscuro, con borde. */
@Composable
fun ElevatedSectionCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val dark = AppThemeExtras.isDark
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (dark) 0.dp else 10.dp, shape, ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(if (dark) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        content = content
    )
}
