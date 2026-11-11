package com.tdcostmanager.app.ui.orders

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcostmanager.app.AppContainer
import com.tdcostmanager.app.R
import com.tdcostmanager.app.container
import com.tdcostmanager.app.core.Fmt
import com.tdcostmanager.app.core.QuotePdf
import com.tdcostmanager.app.core.calc.OrderMath
import com.tdcostmanager.app.data.db.OrderEntity
import com.tdcostmanager.app.data.db.OrderLineEntity
import com.tdcostmanager.app.data.db.OrderStatus
import com.tdcostmanager.app.ui.common.ConfirmDialog
import com.tdcostmanager.app.ui.common.LedgerRow
import com.tdcostmanager.app.ui.common.LocalFmt
import com.tdcostmanager.app.ui.common.NumberField
import com.tdcostmanager.app.ui.common.PieceImage
import com.tdcostmanager.app.ui.common.Section
import com.tdcostmanager.app.ui.common.TextInput
import com.tdcostmanager.app.ui.common.label
import com.tdcostmanager.app.ui.theme.AppThemeExtras
import com.tdcostmanager.app.ui.theme.Tone
import com.tdcostmanager.app.ui.theme.tabular
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

class OrderDetailViewModel(private val c: AppContainer, val orderId: Long) : ViewModel() {
    val order = c.db.orders().observe(orderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val pieces = c.db.pieces().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val materials = c.db.materials().observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun update(transform: (OrderEntity) -> OrderEntity) = viewModelScope.launch {
        c.db.orders().byId(orderId)?.let { c.repository.updateOrder(transform(it)) }
    }

    fun setStatus(status: OrderStatus, onMoved: (Map<Long, BigDecimal>) -> Unit) = viewModelScope.launch {
        onMoved(c.repository.setStatus(orderId, status))
    }

    fun addPiece(pieceId: Long) = viewModelScope.launch { c.repository.addPieceToOrder(orderId, pieceId) }
    fun setQuantity(line: OrderLineEntity, qty: Int) = viewModelScope.launch {
        if (qty <= 0) c.repository.removeLine(line) else c.repository.updateLine(line.copy(quantity = qty))
    }
    fun remove(line: OrderLineEntity) = viewModelScope.launch { c.repository.removeLine(line) }
    fun restore(line: OrderLineEntity) = viewModelScope.launch { c.repository.restoreLine(line) }
    fun duplicate(onDone: (Long) -> Unit) = viewModelScope.launch { onDone(c.repository.duplicateOrder(orderId)) }

    /** Clientes de otros presupuestos, para autocompletar. */
    val clients = kotlinx.coroutines.flow.flow { emit(c.repository.clients()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun refreshPrices() = viewModelScope.launch { c.repository.refreshOrderPrices(orderId) }
    fun delete(onDone: () -> Unit) = viewModelScope.launch { c.repository.deleteOrder(orderId); onDone() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderDetailScreen(orderId: Long, onBack: () -> Unit, onOpenOrder: (Long) -> Unit) {
    val context = LocalContext.current
    val vm: OrderDetailViewModel = viewModel(key = "order$orderId") { OrderDetailViewModel(context.container, orderId) }
    val data by vm.order.collectAsStateWithLifecycle()
    val pieces by vm.pieces.collectAsStateWithLifecycle()
    val materials by vm.materials.collectAsStateWithLifecycle()
    val fmt = LocalFmt.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addPiece by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    val deductedMsg = stringResource(R.string.order_stock_deducted)
    val returnedMsg = stringResource(R.string.order_stock_returned)
    val pdfFailed = stringResource(R.string.order_pdf_failed)
    val shareTitle = stringResource(R.string.order_share)
    val lineRemovedMsg = stringResource(R.string.order_line_removed)
    val undoText = stringResource(R.string.action_undo)
    val clients by vm.clients.collectAsStateWithLifecycle()

    val d = data
    val o = d?.order

    // Campos de texto locales: se guardan al escribir sin que la base de datos mueva el cursor
    var title by remember(o?.id) { mutableStateOf(o?.title.orEmpty()) }
    var client by remember(o?.id) { mutableStateOf(o?.clientName.orEmpty()) }
    var contact by remember(o?.id) { mutableStateOf(o?.clientContact.orEmpty()) }
    var notes by remember(o?.id) { mutableStateOf(o?.notes.orEmpty()) }
    var discount by remember(o?.id) { mutableStateOf(o?.discountPercent?.takeIf { it.signum() > 0 }?.let(fmt::input).orEmpty()) }
    var shipping by remember(o?.id) { mutableStateOf(o?.shipping?.takeIf { it.signum() > 0 }?.let(fmt::input).orEmpty()) }

    fun sharePdf() = scope.launch {
        val current = vm.order.value ?: return@launch
        runCatching {
            val s = context.container.settings.current()
            val logo = s.business.logo?.let { context.container.images.load(it, 400) }
            val file = QuotePdf(context).create(current.order, current.lines, s.business, logo, fmt)
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, shareTitle))
            if (current.order.status == OrderStatus.DRAFT) vm.setStatus(OrderStatus.SENT) {}
        }.onFailure { snackbar.showSnackbar(pdfFailed) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(o?.let { stringResource(R.string.order_number, it.number) } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.order_refresh_prices)) },
                            leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
                            onClick = { menu = false; vm.refreshPrices() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.order_duplicate)) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                            onClick = { menu = false; vm.duplicate(onOpenOrder) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.order_delete)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = { menu = false; confirmDelete = true }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pdf_total), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            d?.let { fmt.money(OrderMath.totals(it.order, it.lines).total) } ?: "—",
                            style = MaterialTheme.typography.headlineSmall.tabular(), fontWeight = FontWeight.Bold
                        )
                    }
                    Button(onClick = { sharePdf() }, enabled = d?.lines?.isNotEmpty() == true) {
                        Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.order_send_pdf))
                    }
                }
            }
        }
    ) { padding ->
        if (d == null || o == null) return@Scaffold
        val totals = OrderMath.totals(o, d.lines)
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Section(stringResource(R.string.order_status)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderStatus.entries.forEach { st ->
                        FilterChip(
                            selected = o.status == st,
                            onClick = {
                                vm.setStatus(st) { moved ->
                                    if (moved.isEmpty()) return@setStatus
                                    val text = moved.entries.joinToString(", ") { (id, amount) ->
                                        val m = materials.firstOrNull { it.id == id }
                                        fmt.number(amount.abs(), 0) + " " + (if (m?.unit?.name == "ML") "ml" else "g") + " " + (m?.name ?: "")
                                    }
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            (if (moved.values.first().signum() > 0) deductedMsg else returnedMsg).format(text)
                                        )
                                    }
                                }
                            },
                            label = { Text(st.label()) }
                        )
                    }
                }
            }

            Section(stringResource(R.string.order_section_client)) {
                TextInput(stringResource(R.string.order_title_field), title, { title = it; vm.update { o2 -> o2.copy(title = it) } })
                ClientField(
                    value = client,
                    suggestions = clients,
                    onValueChange = { client = it; vm.update { o2 -> o2.copy(clientName = it) } },
                    onPick = { name, mail ->
                        client = name
                        if (contact.isBlank()) contact = mail
                        vm.update { o2 -> o2.copy(clientName = name, clientContact = o2.clientContact.ifBlank { mail }) }
                    }
                )
                TextInput(stringResource(R.string.order_contact), contact, { contact = it; vm.update { o2 -> o2.copy(clientContact = it) } },
                    keyboardType = KeyboardType.Email)
                OutlinedButton(onClick = { pickDate = true }) {
                    Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(o.dueDate?.let { stringResource(R.string.order_due, fmt.date(it)) } ?: stringResource(R.string.order_set_due))
                }
            }

            Section(
                stringResource(R.string.order_section_pieces),
                trailing = {
                    TextButton(onClick = { addPiece = true }, enabled = pieces.isNotEmpty()) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.order_add_piece))
                    }
                }
            ) {
                if (d.lines.isEmpty()) Text(
                    stringResource(if (pieces.isEmpty()) R.string.order_no_pieces_saved else R.string.order_no_lines),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                d.lines.sortedBy { it.position }.forEach { line ->
                    LineRow(line, fmt, onQty = { vm.setQuantity(line, it) }, onRemove = {
                        vm.remove(line)
                        scope.launch {
                            val r = snackbar.showSnackbar(lineRemovedMsg.format(line.description), actionLabel = undoText)
                            if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) vm.restore(line)
                        }
                    })
                }
            }

            Section(stringResource(R.string.order_section_totals)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(stringResource(R.string.order_discount), discount, {
                        discount = it
                        vm.update { o2 -> o2.copy(discountPercent = Fmt.parse(it) ?: BigDecimal.ZERO) }
                    }, Modifier.weight(1f), suffix = "%")
                    NumberField(stringResource(R.string.order_shipping), shipping, {
                        shipping = it
                        vm.update { o2 -> o2.copy(shipping = Fmt.parse(it) ?: BigDecimal.ZERO) }
                    }, Modifier.weight(1f), suffix = fmt.currencySymbol)
                }
                LedgerRow(stringResource(R.string.pdf_subtotal), fmt.money(totals.subtotal))
                if (totals.discount.signum() > 0) LedgerRow(
                    stringResource(R.string.pdf_discount, fmt.number(o.discountPercent)), "− " + fmt.money(totals.discount)
                )
                if (totals.shipping.signum() > 0) LedgerRow(stringResource(R.string.pdf_shipping), fmt.money(totals.shipping))
                HorizontalDivider()
                LedgerRow(stringResource(R.string.pdf_total), fmt.money(totals.total), strong = true)
                LedgerRow(stringResource(R.string.order_cost), fmt.money(totals.cost))
                LedgerRow(
                    stringResource(R.string.order_profit), fmt.money(totals.profit),
                    valueColor = if (totals.profit.signum() < 0) MaterialTheme.colorScheme.error else AppThemeExtras.tone(Tone.Success).content,
                    hint = stringResource(R.string.order_profit_hint)
                )
            }

            Section(stringResource(R.string.order_section_notes)) {
                TextInput(stringResource(R.string.order_notes), notes, { notes = it; vm.update { o2 -> o2.copy(notes = it) } },
                    singleLine = false, supporting = stringResource(R.string.order_notes_help))
            }
            Text(
                stringResource(R.string.order_created, fmt.date(o.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete && o != null) ConfirmDialog(
        title = stringResource(R.string.order_delete_title, o.number),
        body = stringResource(R.string.order_delete_body),
        confirm = stringResource(R.string.action_delete),
        onConfirm = { vm.delete(onBack) },
        onDismiss = { confirmDelete = false }
    )

    if (addPiece) AlertDialog(
        onDismissRequest = { addPiece = false },
        title = { Text(stringResource(R.string.order_add_piece)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(pieces, key = { it.id }) { p ->
                    ListItem(
                        headlineContent = { Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(fmt.money(p.unitPrice)) },
                        leadingContent = { PieceImage(p.image, 40.dp) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { vm.addPiece(p.id); addPiece = false }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { addPiece = false }) { Text(stringResource(R.string.action_cancel)) } }
    )

    if (pickDate && o != null) {
        val state = rememberDatePickerState(initialSelectedDateMillis = o.dueDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    // El selector devuelve medianoche UTC: se guarda como mediodía para que la fecha no cambie por zona
                    val picked = state.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                            .atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    vm.update { it.copy(dueDate = picked) }
                    pickDate = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { vm.update { it.copy(dueDate = null) }; pickDate = false }) {
                    Text(stringResource(R.string.order_clear_due))
                }
            }
        ) { DatePicker(state) }
    }
}

@Composable
private fun LineRow(line: OrderLineEntity, fmt: Fmt, onQty: (Int) -> Unit, onRemove: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(line.description, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.order_line_unit, fmt.money(line.unitPrice)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, stringResource(R.string.action_remove)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onQty(line.quantity - 1) }) { Icon(Icons.Outlined.Remove, stringResource(R.string.order_less)) }
            Text(line.quantity.toString(), style = MaterialTheme.typography.titleMedium.tabular())
            IconButton(onClick = { onQty(line.quantity + 1) }) { Icon(Icons.Outlined.Add, stringResource(R.string.order_more)) }
            Spacer(Modifier.weight(1f))
            Text(
                fmt.money(line.unitPrice * BigDecimal(line.quantity)),
                style = MaterialTheme.typography.titleMedium.tabular(),
                fontWeight = FontWeight.SemiBold
            )
        }
        HorizontalDivider()
    }
}

/** Nombre del cliente con sugerencias de presupuestos anteriores. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientField(
    value: String,
    suggestions: List<Pair<String, String>>,
    onValueChange: (String) -> Unit,
    onPick: (String, String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val q = com.tdcostmanager.app.ui.pieces.normalize(value.trim())
    val matches = suggestions.filter { (name, _) ->
        val n = com.tdcostmanager.app.ui.pieces.normalize(name)
        n != q && (q.isEmpty() || n.contains(q))
    }.take(5)
    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = focused && matches.isNotEmpty(),
        onExpandedChange = {}
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.order_client)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable)
                .onFocusChanged { focused = it.isFocused }
        )
        ExposedDropdownMenu(
            expanded = focused && matches.isNotEmpty(),
            onDismissRequest = { focused = false }
        ) {
            matches.forEach { (name, mail) ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(name)
                            if (mail.isNotBlank()) Text(mail, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = { onPick(name, mail); focused = false }
                )
            }
        }
    }
}
