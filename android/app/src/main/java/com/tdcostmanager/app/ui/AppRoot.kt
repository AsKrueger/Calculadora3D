package com.tdcostmanager.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tdcostmanager.app.R
import com.tdcostmanager.app.container
import com.tdcostmanager.app.core.AdBar
import com.tdcostmanager.app.core.Fmt
import com.tdcostmanager.app.data.settings.AppSettings
import com.tdcostmanager.app.ui.calculator.CalculatorScreen
import com.tdcostmanager.app.ui.calculator.CalculatorViewModel
import com.tdcostmanager.app.ui.catalog.CatalogScreen
import com.tdcostmanager.app.ui.common.LocalFmt
import com.tdcostmanager.app.ui.orders.OrderDetailScreen
import com.tdcostmanager.app.ui.orders.OrdersScreen
import com.tdcostmanager.app.ui.pieces.PiecesScreen
import com.tdcostmanager.app.ui.settings.BusinessScreen
import com.tdcostmanager.app.ui.settings.FeedbackScreen
import com.tdcostmanager.app.ui.settings.PrivacyScreen
import com.tdcostmanager.app.ui.settings.SettingsScreen

private enum class TopLevel(val route: String, val label: Int, val icon: ImageVector) {
    CALC("calc", R.string.tab_calc, Icons.Outlined.Calculate),
    PIECES("pieces", R.string.tab_pieces, Icons.Outlined.ViewInAr),
    ORDERS("orders", R.string.tab_orders, Icons.AutoMirrored.Outlined.ReceiptLong),
    CATALOG("catalog", R.string.tab_catalog, Icons.Outlined.Inventory2),
    SETTINGS("settings", R.string.tab_settings, Icons.Outlined.Settings)
}

@Composable
fun AppRoot(settings: AppSettings) {
    val locale = LocalConfiguration.current.locales[0]
    val fmt = remember(locale, settings.currency) { Fmt(locale, settings.currency) }

    CompositionLocalProvider(LocalFmt provides fmt) {
        // La barra de anuncios va arriba, bajo la barra de estado, y solo existe si hay anuncio
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            if (settings.onboardingDone) AdBar()
            Box(Modifier.weight(1f)) {
                if (!settings.onboardingDone) WelcomeScreen() else MainNav()
            }
        }
    }
}

@Composable
private fun MainNav() {
    val nav = rememberNavController()
    val calc: CalculatorViewModel = viewModel()
    val context = LocalContext.current
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = TopLevel.entries.any { it.route == route }

    // Un archivo compartido con la app abre la calculadora
    val pending by context.container.pendingImport.collectAsState()
    LaunchedEffect(pending) { if (pending != null) nav.goTo(TopLevel.CALC.route) }

    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                TopLevel.entries.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = { nav.goTo(t.route) },
                        icon = { Icon(t.icon, null) },
                        label = { Text(stringResource(t.label), maxLines = 1) }
                    )
                }
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { padding ->
        NavHost(nav, startDestination = TopLevel.CALC.route, modifier = Modifier.fillMaxSize().padding(padding)) {
            composable(TopLevel.CALC.route) {
                CalculatorScreen(
                    vm = calc,
                    onOpenCatalog = { nav.goTo(TopLevel.CATALOG.route) },
                    onOpenPieces = { nav.goTo(TopLevel.PIECES.route) }
                )
            }
            composable(TopLevel.PIECES.route) {
                PiecesScreen(
                    onEdit = { id -> calc.loadPiece(id); nav.goTo(TopLevel.CALC.route) },
                    onNewCalculation = { calc.newCalculation(); nav.goTo(TopLevel.CALC.route) },
                    onOpenOrder = { id -> nav.navigate("order/$id") }
                )
            }
            composable(TopLevel.ORDERS.route) {
                OrdersScreen(onOpen = { id -> nav.navigate("order/$id") })
            }
            composable("order/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                OrderDetailScreen(entry.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable(TopLevel.CATALOG.route) { CatalogScreen() }
            composable(TopLevel.SETTINGS.route) {
                SettingsScreen(
                    onBusiness = { nav.navigate("business") },
                    onFeedback = { nav.navigate("feedback") },
                    onPrivacy = { nav.navigate("privacy") }
                )
            }
            composable("business") { BusinessScreen(onBack = { nav.popBackStack() }) }
            composable("feedback") { FeedbackScreen(onBack = { nav.popBackStack() }) }
            composable("privacy") { PrivacyScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

/** Navega a una pestaña sin apilar copias y conservando su estado. */
private fun NavHostController.goTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
