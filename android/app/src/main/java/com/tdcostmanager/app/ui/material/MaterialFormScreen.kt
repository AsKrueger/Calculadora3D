package com.tdcostmanager.app.ui.material

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.material.MaterialCreateRequest
import com.tdcostmanager.app.data.remote.dto.material.MaterialUpdateRequest
import com.tdcostmanager.app.domain.model.MaterialCategory
import com.tdcostmanager.app.domain.model.UnitType
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.*

// Unidades en las que tiene sentido comprar un material (las horas son para mano de obra)
private val purchaseUnits = listOf(UnitType.G, UnitType.KG, UnitType.ML, UnitType.L, UnitType.UNIT)

@Composable
fun MaterialFormScreen(
    id: Long?,
    viewModel: MaterialViewModel,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.materialDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(UnitType.KG) }
    var category by rememberSaveable { mutableStateOf(MaterialCategory.FILAMENT) }
    var active by rememberSaveable { mutableStateOf(true) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) { if (id != null) viewModel.loadMaterialDetail(id) }
    LaunchedEffect(detailState) {
        val s = detailState
        if (id != null && !loaded && s is UiState.Success) {
            val m = s.data
            name = m.name
            description = m.description.orEmpty()
            price = Format.forInput(m.purchasePrice)
            quantity = Format.forInput(m.quantity)
            unit = m.unit
            category = m.category
            active = m.active
            loaded = true
        }
    }
    LaunchedEffect(operationState) {
        if (operationState is UiState.Success) {
            viewModel.resetOperationState()
            onNavigateBack()
        }
    }

    val nameError = if (submitted && name.isBlank()) "Obligatorio" else null
    val priceError = if (submitted) validateNumber(price) else null
    val quantityError = if (submitted) validateNumber(quantity, positive = true) else null

    FormScaffold(
        title = if (id != null) "Editar material" else "Nuevo material",
        onBack = onNavigateBack,
        saveLabel = "Guardar material",
        saving = operationState is UiState.Loading,
        error = (operationState as? UiState.Error)?.error?.toFriendlyMessage(),
        onSave = {
            submitted = true
            val p = Format.parseDecimal(price)
            val q = Format.parseDecimal(quantity)
            if (name.isNotBlank() && validateNumber(price) == null && validateNumber(quantity, positive = true) == null && p != null && q != null) {
                val desc = description.ifBlank { null }
                if (id != null) {
                    viewModel.updateMaterial(id, MaterialUpdateRequest(name.trim(), desc, p, q, unit, category, active))
                } else {
                    viewModel.createMaterial(MaterialCreateRequest(name.trim(), desc, p, q, unit, category))
                }
            }
        }
    ) {
        SectionCard(title = "Material") {
            AppTextField(name, { name = it }, "Nombre", error = nameError)
            Spacer(Modifier.height(4.dp))
            DropdownField("Categoría", MaterialCategory.entries, category, { it.label }, { category = it })
            Spacer(Modifier.height(12.dp))
            AppTextField(description, { description = it }, "Descripción (opcional)", singleLine = false, minLines = 2)
        }
        SectionCard(title = "Compra") {
            NumberField(price, { price = it }, "Precio pagado", suffix = "€", error = priceError)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(quantity, { quantity = it }, "Cantidad", error = quantityError, modifier = Modifier.weight(1f))
                DropdownField("Unidad", purchaseUnits, unit, { it.symbol }, { unit = it }, Modifier.weight(1f))
            }
            val p = Format.parseDecimal(price)
            val q = Format.parseDecimal(quantity)
            if (p != null && q != null && q > 0) {
                Text(
                    "Coste por ${unit.symbol}: ${Format.number(p / q, 4)} €",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (id != null) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Activo", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Los materiales inactivos se conservan pero se marcan en el catálogo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        }
    }
}
