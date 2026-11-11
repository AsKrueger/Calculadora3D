package com.tdcostmanager.app.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.machine.MachineResponse
import com.tdcostmanager.app.data.remote.dto.material.MaterialResponse
import com.tdcostmanager.app.data.remote.dto.project.ProjectCreateRequest
import com.tdcostmanager.app.data.remote.dto.tool.ToolResponse
import com.tdcostmanager.app.domain.model.UnitType
import com.tdcostmanager.app.domain.util.compatibleUnits
import com.tdcostmanager.app.ui.common.*

// Borradores de las líneas del formulario: los números se guardan como texto hasta validarlos

data class MaterialLine(val materialId: Long? = null, val quantity: String = "", val unit: UnitType = UnitType.G)
data class MachineLine(val machineId: Long? = null, val hours: String = "")
data class ToolLine(val toolId: Long? = null, val uses: String = "1")

fun MaterialLine.error(): String? = if (materialId == null) "Elige un material" else validateNumber(quantity, positive = true)
fun MachineLine.error(): String? = if (machineId == null) "Elige una máquina" else validateNumber(hours, positive = true)
fun ToolLine.error(): String? = if (toolId == null) "Elige una herramienta" else validateNumber(uses, positive = true)

fun MaterialLine.toRequest() = ProjectCreateRequest.MaterialRequest(materialId!!, Format.parseDecimal(quantity)!!, unit)
fun MachineLine.toRequest() = ProjectCreateRequest.MachineRequest(machineId!!, Format.parseDecimal(hours)!!)
fun ToolLine.toRequest() = ProjectCreateRequest.ToolRequest(toolId!!, Format.parseDecimal(uses)!!)

/** Elementos que se pueden elegir: los activos y, si ya estaba en la línea, también uno desactivado. */
private fun <T> selectable(items: List<T>, isActive: (T) -> Boolean, id: (T) -> Long, selectedId: Long?) =
    items.filter { isActive(it) || id(it) == selectedId }

@Composable
fun MaterialLinesCard(
    lines: List<MaterialLine>,
    catalog: List<MaterialResponse>,
    showErrors: Boolean,
    onChange: (Int, MaterialLine) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit
) {
    LinesCard("Materiales", "Añadir material", lines.size, catalog.any { it.active }, "Crea materiales en el Catálogo para añadirlos.", onAdd) {
        lines.forEachIndexed { index, line ->
            val material = catalog.firstOrNull { it.id == line.materialId }
            LineRow(onRemove = { onRemove(index) }, error = if (showErrors) line.error() else null) {
                DropdownField(
                    label = "Material",
                    options = selectable(catalog, { it.active }, { it.id }, line.materialId).map { it.id },
                    selected = line.materialId ?: -1L,
                    optionLabel = { id -> catalog.firstOrNull { it.id == id }?.name ?: "Elige un material" },
                    onSelected = { id ->
                        val m = catalog.first { it.id == id }
                        val units = compatibleUnits(m.unit, m.densityGPerMl)
                        onChange(index, line.copy(materialId = id, unit = if (line.unit in units) line.unit else units.first()))
                    }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(line.quantity, { onChange(index, line.copy(quantity = it)) }, "Cantidad", modifier = Modifier.weight(1f))
                    DropdownField(
                        label = "Unidad",
                        options = material?.let { compatibleUnits(it.unit, it.densityGPerMl) } ?: listOf(line.unit),
                        selected = line.unit,
                        optionLabel = { it.symbol },
                        onSelected = { onChange(index, line.copy(unit = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MachineLinesCard(
    lines: List<MachineLine>,
    catalog: List<MachineResponse>,
    showErrors: Boolean,
    onChange: (Int, MachineLine) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit
) {
    LinesCard("Máquinas", "Añadir máquina", lines.size, catalog.any { it.active }, "Crea máquinas en el Catálogo para añadirlas.", onAdd) {
        lines.forEachIndexed { index, line ->
            LineRow(onRemove = { onRemove(index) }, error = if (showErrors) line.error() else null) {
                DropdownField(
                    label = "Máquina",
                    options = selectable(catalog, { it.active }, { it.id }, line.machineId).map { it.id },
                    selected = line.machineId ?: -1L,
                    optionLabel = { id -> catalog.firstOrNull { it.id == id }?.name ?: "Elige una máquina" },
                    onSelected = { onChange(index, line.copy(machineId = it)) }
                )
                Spacer(Modifier.height(8.dp))
                NumberField(line.hours, { onChange(index, line.copy(hours = it)) }, "Horas de impresión", suffix = "h")
            }
        }
    }
}

@Composable
fun ToolLinesCard(
    lines: List<ToolLine>,
    catalog: List<ToolResponse>,
    showErrors: Boolean,
    onChange: (Int, ToolLine) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit
) {
    LinesCard("Herramientas", "Añadir herramienta", lines.size, catalog.any { it.active }, "Crea herramientas en el Catálogo para añadirlas.", onAdd) {
        lines.forEachIndexed { index, line ->
            LineRow(onRemove = { onRemove(index) }, error = if (showErrors) line.error() else null) {
                DropdownField(
                    label = "Herramienta",
                    options = selectable(catalog, { it.active }, { it.id }, line.toolId).map { it.id },
                    selected = line.toolId ?: -1L,
                    optionLabel = { id -> catalog.firstOrNull { it.id == id }?.name ?: "Elige una herramienta" },
                    onSelected = { onChange(index, line.copy(toolId = it)) }
                )
                Spacer(Modifier.height(8.dp))
                NumberField(line.uses, { onChange(index, line.copy(uses = it)) }, "Usos", suffix = "usos")
            }
        }
    }
}

@Composable
private fun LinesCard(
    title: String,
    addLabel: String,
    count: Int,
    canAdd: Boolean,
    emptyCatalogHint: String,
    onAdd: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    SectionCard(
        title = title,
        trailing = { Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        content()
        if (canAdd) {
            TextButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(addLabel)
            }
        } else if (count == 0) {
            Text(emptyCatalogHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LineRow(onRemove: () -> Unit, error: String?, content: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            content()
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Outlined.Delete, contentDescription = "Quitar línea", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
