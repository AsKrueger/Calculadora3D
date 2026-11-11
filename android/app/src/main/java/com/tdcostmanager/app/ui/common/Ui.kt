package com.tdcostmanager.app.ui.common

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.R
import com.tdcostmanager.app.container
import com.tdcostmanager.app.core.Fmt
import com.tdcostmanager.app.data.db.OrderStatus
import com.tdcostmanager.app.ui.theme.AppThemeExtras
import com.tdcostmanager.app.ui.theme.Tone
import com.tdcostmanager.app.ui.theme.tabular
import java.util.Locale

val LocalFmt = staticCompositionLocalOf { Fmt(Locale.getDefault(), "EUR") }

/** Bloque del formulario con título; tarjeta blanca sobre el fondo gris. */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                trailing()
            }
            content()
        }
    }
}

/**
 * Bloque plegable: cerrado muestra una línea de resumen de lo que contiene, para que la calculadora
 * quepa en una pantalla sin esconder lo que cuenta en el precio.
 */
@Composable
fun FoldSection(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.animateContentSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = title, onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!expanded) Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium.tabular(),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) Column(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/** Campo numérico: acepta coma o punto, marca el error sin borrar lo escrito. */
@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    supporting: String? = null,
    isError: Boolean = false,
    integer: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    val invalid = isError || (value.isNotBlank() && Fmt.parse(value) == null)
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() || (!integer && (it == ',' || it == '.')) }) },
        label = { Text(label, maxLines = 1) },
        suffix = suffix?.let { { Text(it) } },
        supportingText = when {
            invalid && value.isNotBlank() && Fmt.parse(value) == null -> { { Text(stringResource(R.string.error_number)) } }
            supporting != null -> { { Text(supporting) } }
            else -> null
        },
        isError = invalid,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.tabular(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal,
            imeAction = imeAction
        ),
        modifier = modifier
    )
}

@Composable
fun TextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    supporting: String? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
        modifier = modifier.fillMaxWidth()
    )
}

/** Desplegable de solo lectura. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Picker(
    label: String,
    options: List<T>,
    selected: T?,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable (T) -> Unit)? = null,
    placeholder: String = "",
    footer: (@Composable (close: () -> Unit) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let { labelOf(it) } ?: placeholder,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            leadingIcon = if (leading != null && selected != null) { { leading(selected) } } else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelOf(option)) },
                    leadingIcon = leading?.let { { it(option) } },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
            footer?.invoke { expanded = false }
        }
    }
}

/** Muestra de color redonda, como el lateral de una bobina. */
@Composable
fun Swatch(color: Long, size: Dp = 18.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(color))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .size(size / 3)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

@Composable
fun Chip(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val c = AppThemeExtras.tone(tone)
    Surface(color = c.container, contentColor = c.content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

fun OrderStatus.tone(): Tone = when (this) {
    OrderStatus.DRAFT -> Tone.Neutral
    OrderStatus.SENT -> Tone.Info
    OrderStatus.ACCEPTED -> Tone.Progress
    OrderStatus.PRINTED -> Tone.Warning
    OrderStatus.DELIVERED -> Tone.Success
    OrderStatus.CANCELLED -> Tone.Danger
}

@Composable
fun OrderStatus.label(): String = stringResource(
    when (this) {
        OrderStatus.DRAFT -> R.string.status_draft
        OrderStatus.SENT -> R.string.status_sent
        OrderStatus.ACCEPTED -> R.string.status_accepted
        OrderStatus.PRINTED -> R.string.status_printed
        OrderStatus.DELIVERED -> R.string.status_delivered
        OrderStatus.CANCELLED -> R.string.status_cancelled
    }
)

/** Estado vacío con una acción clara, sin ilustraciones de relleno. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(8.dp))
            action()
        }
    }
}

/** Imagen de una pieza (copia propia o de la galería), con hueco neutro si no hay. */
@Composable
fun PieceImage(ref: String?, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(ref) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(ref) {
        bitmap = ref?.let { context.container.images.load(it, 512) }
    }
    Box(
        modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val b = bitmap
        if (b != null) {
            Image(b.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(size / 3))
        }
    }
}

/** Confirmación de una acción destructiva que nombra lo que se va a perder. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** Fila «etiqueta ······ importe», como un ticket de taller. */
@Composable
fun LedgerRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    dot: Color? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    hint: String? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Text(
            value,
            style = (if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium).tabular(),
            fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}

/** Barra apilada con el peso de cada partida. */
@Composable
fun ProportionBar(parts: List<Pair<Float, Color>>, modifier: Modifier = Modifier) {
    val total = parts.sumOf { it.first.toDouble() }.toFloat()
    Row(
        modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (total > 0f) parts.filter { it.first > 0f }.forEach { (v, c) ->
            Box(Modifier.weight(v / total).height(10.dp).background(c))
        }
    }
}
