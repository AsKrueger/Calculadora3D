package com.tdcostmanager.app.ui.pieces

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcostmanager.app.R
import com.tdcostmanager.app.container
import com.tdcostmanager.app.data.db.OrderStatus
import com.tdcostmanager.app.data.db.PieceEntity
import com.tdcostmanager.app.ui.common.ConfirmDialog
import com.tdcostmanager.app.ui.common.EmptyState
import com.tdcostmanager.app.ui.common.LocalFmt
import com.tdcostmanager.app.ui.common.PieceImage
import com.tdcostmanager.app.ui.common.label
import com.tdcostmanager.app.ui.theme.tabular
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer

class PiecesViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    val pieces = c.db.pieces().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val openOrders = c.db.orders().observeAll()
        .map { list -> list.filter { it.order.status == OrderStatus.DRAFT || it.order.status == OrderStatus.SENT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun duplicate(p: PieceEntity, suffix: String) = viewModelScope.launch { c.repository.duplicatePiece(p, suffix) }
    fun delete(p: PieceEntity) = viewModelScope.launch { c.repository.deletePiece(p) }

    /** Añade la pieza a un pedido (o a uno nuevo) y devuelve el id del pedido. */
    fun addToOrder(p: PieceEntity, orderId: Long?, onDone: (Long) -> Unit) = viewModelScope.launch {
        val id = orderId ?: c.repository.newOrder()
        c.repository.addPieceToOrder(id, p.id)
        onDone(id)
    }
}

fun normalize(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiecesScreen(
    onEdit: (Long) -> Unit,
    onNewCalculation: () -> Unit,
    onOpenOrder: (Long) -> Unit,
    vm: PiecesViewModel = viewModel()
) {
    val pieces by vm.pieces.collectAsStateWithLifecycle()
    val orders by vm.openOrders.collectAsStateWithLifecycle()
    val fmt = LocalFmt.current
    var query by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<PieceEntity?>(null) }
    var toOrder by remember { mutableStateOf<PieceEntity?>(null) }
    val copySuffix = stringResource(R.string.pieces_copy_suffix)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_pieces)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val list = pieces
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(
                Icons.Outlined.ViewInAr,
                stringResource(R.string.pieces_empty_title),
                stringResource(R.string.pieces_empty_body),
                Modifier.padding(padding)
            ) { Button(onClick = onNewCalculation) { Text(stringResource(R.string.pieces_empty_action)) } }
            else -> {
                val q = normalize(query.trim())
                val filtered = if (q.isEmpty()) list else list.filter { normalize(it.name + " " + it.notes).contains(q) }
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (list.size > 4) item {
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            leadingIcon = { Icon(Icons.Outlined.Search, null) },
                            placeholder = { Text(stringResource(R.string.pieces_search)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (filtered.isEmpty()) item {
                        Text(stringResource(R.string.search_no_results, query), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp))
                    }
                    items(filtered, key = { it.id }) { p ->
                        PieceCard(
                            p, fmt = fmt,
                            onClick = { onEdit(p.id) },
                            onDuplicate = { vm.duplicate(p, copySuffix) },
                            onAddToOrder = { toOrder = p },
                            onDelete = { toDelete = p }
                        )
                    }
                }
            }
        }
    }

    toDelete?.let { p ->
        ConfirmDialog(
            title = stringResource(R.string.pieces_delete_title, p.name),
            body = stringResource(R.string.pieces_delete_body),
            confirm = stringResource(R.string.action_delete),
            onConfirm = { vm.delete(p) },
            onDismiss = { toDelete = null }
        )
    }
    toOrder?.let { p ->
        AlertDialog(
            onDismissRequest = { toOrder = null },
            title = { Text(stringResource(R.string.pieces_add_to_order)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.orders_new)) },
                        leadingContent = { Icon(Icons.Outlined.PostAdd, null) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { vm.addToOrder(p, null, onOpenOrder); toOrder = null }
                    )
                    orders.forEach { o ->
                        HorizontalDivider()
                        ListItem(
                            headlineContent = {
                                Text(o.order.title.ifBlank { stringResource(R.string.order_number, o.order.number) },
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = { Text(o.order.clientName.ifBlank { o.order.status.label() }) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            modifier = Modifier.clickable { vm.addToOrder(p, o.order.id, onOpenOrder); toOrder = null }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { toOrder = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun PieceCard(
    p: PieceEntity,
    fmt: com.tdcostmanager.app.core.Fmt,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onAddToOrder: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PieceImage(p.image, 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.pieces_cost_line, fmt.money(p.unitCost), fmt.money(p.unitProfit)),
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (p.piecesPerPlate > 1) Text(
                    stringResource(R.string.pieces_per_plate, p.piecesPerPlate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                fmt.money(p.unitPrice),
                style = MaterialTheme.typography.titleMedium.tabular(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menu = false; onClick() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.pieces_add_to_order)) },
                        leadingIcon = { Icon(Icons.Outlined.PostAdd, null) }, onClick = { menu = false; onAddToOrder() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_duplicate)) },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
