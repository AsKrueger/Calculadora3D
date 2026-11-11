package com.tdcostmanager.app.ui.orders

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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
import com.tdcostmanager.app.core.calc.OrderMath
import com.tdcostmanager.app.data.db.OrderWithLines
import com.tdcostmanager.app.ui.common.Chip
import com.tdcostmanager.app.ui.common.EmptyState
import com.tdcostmanager.app.ui.common.LocalFmt
import com.tdcostmanager.app.ui.common.label
import com.tdcostmanager.app.ui.common.tone
import com.tdcostmanager.app.ui.theme.tabular
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.TextStyle

class OrdersViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    val orders = c.db.orders().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun create(onCreated: (Long) -> Unit) = viewModelScope.launch { onCreated(c.repository.newOrder()) }
}

private enum class Filter { OPEN, ALL, DONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(onOpen: (Long) -> Unit, vm: OrdersViewModel = viewModel()) {
    val orders by vm.orders.collectAsStateWithLifecycle()
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var filter by rememberSaveable { mutableStateOf(Filter.OPEN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_orders)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.create(onOpen) },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text(stringResource(R.string.orders_new)) }
            )
        }
    ) { padding ->
        val list = orders ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                MonthCard(
                    OrderMath.summary(YearMonth.parse(month), list),
                    onPrev = { month = YearMonth.parse(month).minusMonths(1).toString() },
                    onNext = { month = YearMonth.parse(month).plusMonths(1).toString() }
                )
            }
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        Icons.AutoMirrored.Outlined.ReceiptLong,
                        stringResource(R.string.orders_empty_title),
                        stringResource(R.string.orders_empty_body)
                    )
                }
                return@LazyColumn
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Filter.entries.forEach { fl ->
                        val count = list.count { matches(it, fl) }
                        FilterChip(
                            selected = filter == fl,
                            onClick = { filter = fl },
                            label = {
                                Text(
                                    stringResource(
                                        when (fl) {
                                            Filter.OPEN -> R.string.orders_filter_open
                                            Filter.ALL -> R.string.orders_filter_all
                                            Filter.DONE -> R.string.orders_filter_done
                                        }
                                    ) + " · $count"
                                )
                            }
                        )
                    }
                }
            }
            items(list.filter { matches(it, filter) }, key = { it.order.id }) { o ->
                OrderCard(o) { onOpen(o.order.id) }
            }
        }
    }
}

private fun matches(o: OrderWithLines, f: Filter) = when (f) {
    Filter.OPEN -> o.order.status.isOpen
    Filter.ALL -> true
    Filter.DONE -> !o.order.status.isOpen
}

@Composable
private fun MonthCard(s: OrderMath.MonthSummary, onPrev: () -> Unit, onNext: () -> Unit) {
    val fmt = LocalFmt.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.month.month.getDisplayName(TextStyle.FULL_STANDALONE, fmt.locale)
                        .replaceFirstChar { it.titlecase(fmt.locale) } + " " + s.month.year,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onPrev) { Icon(Icons.Outlined.ChevronLeft, stringResource(R.string.orders_prev_month)) }
                IconButton(onClick = onNext, enabled = s.month < YearMonth.now()) {
                    Icon(Icons.Outlined.ChevronRight, stringResource(R.string.orders_next_month))
                }
            }
            Text(stringResource(R.string.summary_sales), style = MaterialTheme.typography.labelMedium)
            Text(
                fmt.money(s.sales),
                style = MaterialTheme.typography.headlineMedium.tabular(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inversePrimary
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.summary_profit), fmt.money(s.profit), Modifier.weight(1f))
                Stat(stringResource(R.string.summary_cost), fmt.money(s.cost), Modifier.weight(1f))
                Stat(
                    stringResource(R.string.summary_acceptance),
                    s.acceptanceRate?.let { "$it %" } ?: "—",
                    Modifier.weight(1f)
                )
            }
            if (s.openOrders > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    pluralStringResource(R.plurals.summary_pending, s.openOrders, s.openOrders),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleSmall.tabular())
    }
}

@Composable
private fun OrderCard(o: OrderWithLines, onClick: () -> Unit) {
    val fmt = LocalFmt.current
    val totals = OrderMath.totals(o.order, o.lines)
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${o.order.number}",
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Chip(o.order.status.label(), o.order.status.tone())
            }
            Text(
                o.order.title.ifBlank { o.lines.firstOrNull()?.description ?: stringResource(R.string.order_untitled) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    if (o.order.clientName.isNotBlank()) Text(o.order.clientName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        pluralStringResource(R.plurals.order_card_meta, totals.units, totals.units, fmt.date(o.order.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    fmt.money(totals.total),
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
