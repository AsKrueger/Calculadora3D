package com.tdcostmanager.app.ui.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.material.MaterialResponse
import com.tdcostmanager.app.domain.model.MaterialCategory
import com.tdcostmanager.app.ui.common.*
import com.tdcostmanager.app.ui.theme.Tone
import com.tdcostmanager.app.ui.theme.tabular

val MaterialCategory.icon: ImageVector
    get() = when (this) {
        MaterialCategory.FILAMENT -> Icons.Default.Album
        MaterialCategory.RESIN -> Icons.Default.WaterDrop
        MaterialCategory.PAINT -> Icons.Default.FormatPaint
        MaterialCategory.GLUE -> Icons.Default.Science
        MaterialCategory.SANDPAPER -> Icons.Default.GridOn
        MaterialCategory.OTHER -> Icons.Default.Category
    }

/** Precio por unidad de compra, el que usa el cálculo (€/g, €/ml…). */
fun MaterialResponse.unitCostLabel(): String =
    if (quantity > 0) "${Format.number(purchasePrice / quantity, 4)} €/${unit.symbol}" else "—"

@Composable
fun MaterialListContent(viewModel: MaterialViewModel, onMaterialClick: (Long) -> Unit) {
    val state by viewModel.materialsState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadMaterials() }

    when (val s = state) {
        is UiState.Loading, UiState.Idle -> LoadingState()
        is UiState.Error -> ErrorState(s.error, onRetry = viewModel::loadMaterials)
        is UiState.Success -> if (s.data.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Album,
                title = "Sin materiales",
                message = "Añade el filamento, la resina o la pintura que compras para calcular su coste por gramo o mililitro."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(s.data, key = { it.id }) { material ->
                    ListRowCard(
                        title = material.name,
                        subtitle = "${material.category.label} · ${Format.quantity(material.quantity, material.unit)} por ${Format.euros(material.purchasePrice)}",
                        icon = material.category.icon,
                        dimmed = !material.active,
                        onClick = { onMaterialClick(material.id) },
                        trailing = {
                            if (material.active) {
                                Text(
                                    material.unitCostLabel(),
                                    style = MaterialTheme.typography.labelLarge.tabular(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                StatusChip("Inactivo", Tone.Neutral)
                            }
                        }
                    )
                }
            }
        }
    }
}
