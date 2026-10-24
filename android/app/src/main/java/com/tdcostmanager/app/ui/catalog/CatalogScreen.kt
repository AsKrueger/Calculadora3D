package com.tdcostmanager.app.ui.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcostmanager.app.ui.ViewModelFactory
import com.tdcostmanager.app.ui.common.AppTopBar
import com.tdcostmanager.app.ui.machine.MachineListContent
import com.tdcostmanager.app.ui.material.MaterialListContent
import com.tdcostmanager.app.ui.tool.ToolListContent

enum class CatalogTab(val label: String, val newItemLabel: String) {
    Materials("Materiales", "Nuevo material"),
    Machines("Máquinas", "Nueva máquina"),
    Tools("Herramientas", "Nueva herramienta")
}

@Composable
fun CatalogScreen(
    initialTab: CatalogTab,
    factory: ViewModelFactory,
    onMaterialClick: (Long) -> Unit,
    onMachineClick: (Long) -> Unit,
    onToolClick: (Long) -> Unit,
    onCreate: (CatalogTab) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }

    Scaffold(
        topBar = { AppTopBar("Catálogo") },
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onCreate(tab) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(tab.newItemLabel) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab.ordinal, containerColor = MaterialTheme.colorScheme.background) {
                CatalogTab.entries.forEach { item ->
                    Tab(
                        selected = item == tab,
                        onClick = { tab = item },
                        text = { Text(item.label) }
                    )
                }
            }
            when (tab) {
                CatalogTab.Materials -> MaterialListContent(viewModel(factory = factory), onMaterialClick)
                CatalogTab.Machines -> MachineListContent(viewModel(factory = factory), onMachineClick)
                CatalogTab.Tools -> ToolListContent(viewModel(factory = factory), onToolClick)
            }
        }
    }
}
