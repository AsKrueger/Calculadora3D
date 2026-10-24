package com.tdcostmanager.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tdcostmanager.app.ui.ViewModelFactory
import com.tdcostmanager.app.ui.account.AccountScreen
import com.tdcostmanager.app.ui.auth.AuthViewModel
import com.tdcostmanager.app.ui.auth.LoginScreen
import com.tdcostmanager.app.ui.auth.RegisterScreen
import com.tdcostmanager.app.ui.catalog.CatalogScreen
import com.tdcostmanager.app.ui.catalog.CatalogTab
import com.tdcostmanager.app.ui.machine.MachineDetailScreen
import com.tdcostmanager.app.ui.machine.MachineFormScreen
import com.tdcostmanager.app.ui.material.MaterialDetailScreen
import com.tdcostmanager.app.ui.material.MaterialFormScreen
import com.tdcostmanager.app.ui.project.ProjectDetailScreen
import com.tdcostmanager.app.ui.project.ProjectFormScreen
import com.tdcostmanager.app.ui.project.ProjectListScreen
import com.tdcostmanager.app.ui.quote.QuoteCalculatorScreen
import com.tdcostmanager.app.ui.quote.QuoteDetailScreen
import com.tdcostmanager.app.ui.quote.QuoteListScreen
import com.tdcostmanager.app.ui.tool.ToolDetailScreen
import com.tdcostmanager.app.ui.tool.ToolFormScreen

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Projects : Screen("projects")
    data object Catalog : Screen("catalog?tab={tab}") {
        fun createRoute(tab: CatalogTab) = "catalog?tab=${tab.name}"
    }
    data object Account : Screen("account")
    data object ProjectDetail : Screen("project_detail/{id}") {
        fun createRoute(id: Long) = "project_detail/$id"
    }
    data object ProjectForm : Screen("project_form?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "project_form?id=$id" else "project_form"
    }
    data object MaterialDetail : Screen("material_detail/{id}") {
        fun createRoute(id: Long) = "material_detail/$id"
    }
    data object MaterialForm : Screen("material_form?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "material_form?id=$id" else "material_form"
    }
    data object MachineDetail : Screen("machine_detail/{id}") {
        fun createRoute(id: Long) = "machine_detail/$id"
    }
    data object MachineForm : Screen("machine_form?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "machine_form?id=$id" else "machine_form"
    }
    data object ToolDetail : Screen("tool_detail/{id}") {
        fun createRoute(id: Long) = "tool_detail/$id"
    }
    data object ToolForm : Screen("tool_form?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "tool_form?id=$id" else "tool_form"
    }
    data object QuoteList : Screen("project_quotes/{projectId}") {
        fun createRoute(projectId: Long) = "project_quotes/$projectId"
    }
    data object QuoteCalculator : Screen("quote_calculator/{projectId}") {
        fun createRoute(projectId: Long) = "quote_calculator/$projectId"
    }
    data object QuoteDetail : Screen("project_quotes/{projectId}/detail/{quoteId}") {
        fun createRoute(projectId: Long, quoteId: Long) = "project_quotes/$projectId/detail/$quoteId"
    }
}

/** Secciones de la barra inferior. */
enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    Projects(Screen.Projects.route, "Proyectos", Icons.Default.ViewInAr),
    Catalog(Screen.Catalog.route, "Catálogo", Icons.Default.Inventory2),
    Account(Screen.Account.route, "Cuenta", Icons.Default.AccountCircle)
}

@Composable
fun MainBottomBar(current: TopLevel, onSelect: (TopLevel) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
        TopLevel.entries.forEach { item ->
            NavigationBarItem(
                selected = item == current,
                onClick = { if (item != current) onSelect(item) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

private val idArgument = listOf(navArgument("id") { type = NavType.LongType })
private val optionalIdArgument = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val factory = remember { ViewModelFactory(context) }
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    // El destino inicial se fija una vez; los cambios de sesión navegan limpiando la pila,
    // para que «Atrás» nunca lleve a una pantalla de la otra mitad de la app (o a una en blanco).
    val startDestination = remember { if (isAuthenticated) Screen.Projects.route else Screen.Login.route }
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(isAuthenticated) {
        val target = if (isAuthenticated) Screen.Projects.route else Screen.Login.route
        val inAuthFlow = currentEntry?.destination?.route in setOf(Screen.Login.route, Screen.Register.route)
        if (currentEntry != null && inAuthFlow == isAuthenticated) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    val selectTopLevel: (TopLevel) -> Unit = { item ->
        navController.navigate(if (item == TopLevel.Catalog) Screen.Catalog.createRoute(CatalogTab.Materials) else item.route) {
            popUpTo(Screen.Projects.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Projects.route) {
            ProjectListScreen(
                viewModel = viewModel(factory = factory),
                onProjectClick = { id -> navController.navigate(Screen.ProjectDetail.createRoute(id)) },
                onCreateProject = { navController.navigate(Screen.ProjectForm.createRoute()) },
                bottomBar = { MainBottomBar(TopLevel.Projects, selectTopLevel) }
            )
        }
        composable(
            route = Screen.Catalog.route,
            arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = CatalogTab.Materials.name })
        ) { entry ->
            val initialTab = CatalogTab.entries.firstOrNull { it.name == entry.arguments?.getString("tab") } ?: CatalogTab.Materials
            CatalogScreen(
                initialTab = initialTab,
                factory = factory,
                onMaterialClick = { navController.navigate(Screen.MaterialDetail.createRoute(it)) },
                onMachineClick = { navController.navigate(Screen.MachineDetail.createRoute(it)) },
                onToolClick = { navController.navigate(Screen.ToolDetail.createRoute(it)) },
                onCreate = { tab ->
                    navController.navigate(
                        when (tab) {
                            CatalogTab.Materials -> Screen.MaterialForm.createRoute()
                            CatalogTab.Machines -> Screen.MachineForm.createRoute()
                            CatalogTab.Tools -> Screen.ToolForm.createRoute()
                        }
                    )
                },
                bottomBar = { MainBottomBar(TopLevel.Catalog, selectTopLevel) }
            )
        }
        composable(Screen.Account.route) {
            AccountScreen(
                email = authViewModel.currentEmail,
                healthViewModel = viewModel(factory = factory),
                onLogout = { authViewModel.logout() },
                bottomBar = { MainBottomBar(TopLevel.Account, selectTopLevel) }
            )
        }

        composable(Screen.MaterialDetail.route, idArgument) { entry ->
            MaterialDetailScreen(
                id = entry.arguments?.getLong("id") ?: 0L,
                viewModel = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { navController.navigate(Screen.MaterialForm.createRoute(it)) }
            )
        }
        composable(Screen.MaterialForm.route, optionalIdArgument) { entry ->
            MaterialFormScreen(
                id = entry.arguments?.getLong("id")?.takeIf { it != -1L },
                viewModel = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MachineDetail.route, idArgument) { entry ->
            MachineDetailScreen(
                id = entry.arguments?.getLong("id") ?: 0L,
                viewModel = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { navController.navigate(Screen.MachineForm.createRoute(it)) }
            )
        }
        composable(Screen.MachineForm.route, optionalIdArgument) { entry ->
            MachineFormScreen(
                id = entry.arguments?.getLong("id")?.takeIf { it != -1L },
                viewModel = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ToolDetail.route, idArgument) { entry ->
            ToolDetailScreen(
                id = entry.arguments?.getLong("id") ?: 0L,
                viewModel = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { navController.navigate(Screen.ToolForm.createRoute(it)) }
            )
        }
        composable(Screen.ToolForm.route, optionalIdArgument) { entry ->
            ToolFormScreen(
                id = entry.arguments?.getLong("id")?.takeIf { it != -1L },
                viewModel = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ProjectDetail.route, idArgument) { entry ->
            ProjectDetailScreen(
                id = entry.arguments?.getLong("id") ?: 0L,
                viewModel = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { navController.navigate(Screen.ProjectForm.createRoute(it)) },
                onNavigateToQuotes = { navController.navigate(Screen.QuoteList.createRoute(it)) },
                onNavigateToCalculator = { navController.navigate(Screen.QuoteCalculator.createRoute(it)) }
            )
        }
        composable(Screen.ProjectForm.route, optionalIdArgument) { entry ->
            ProjectFormScreen(
                id = entry.arguments?.getLong("id")?.takeIf { it != -1L },
                viewModel = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.QuoteList.route, listOf(navArgument("projectId") { type = NavType.LongType })) { entry ->
            val projectId = entry.arguments?.getLong("projectId") ?: 0L
            QuoteListScreen(
                projectId = projectId,
                viewModel = viewModel(factory = factory),
                onQuoteClick = { quoteId -> navController.navigate(Screen.QuoteDetail.createRoute(projectId, quoteId)) },
                onCreateQuote = { navController.navigate(Screen.QuoteCalculator.createRoute(projectId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.QuoteCalculator.route, listOf(navArgument("projectId") { type = NavType.LongType })) { entry ->
            val projectId = entry.arguments?.getLong("projectId") ?: 0L
            QuoteCalculatorScreen(
                projectId = projectId,
                viewModel = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() },
                onQuoteCreated = { quoteId ->
                    navController.navigate(Screen.QuoteDetail.createRoute(projectId, quoteId)) {
                        popUpTo(Screen.QuoteCalculator.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.QuoteDetail.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("quoteId") { type = NavType.LongType }
            )
        ) { entry ->
            QuoteDetailScreen(
                projectId = entry.arguments?.getLong("projectId") ?: 0L,
                quoteId = entry.arguments?.getLong("quoteId") ?: 0L,
                viewModel = viewModel(factory = factory),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
