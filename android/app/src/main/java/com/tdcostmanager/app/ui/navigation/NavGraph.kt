package com.tdcostmanager.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tdcostmanager.app.ui.ViewModelFactory
import com.tdcostmanager.app.ui.auth.AuthViewModel
import com.tdcostmanager.app.ui.auth.LoginScreen
import com.tdcostmanager.app.ui.auth.RegisterScreen
import com.tdcostmanager.app.ui.machine.MachineDetailScreen
import com.tdcostmanager.app.ui.machine.MachineFormScreen
import com.tdcostmanager.app.ui.machine.MachineListScreen
import com.tdcostmanager.app.ui.machine.MachineViewModel
import com.tdcostmanager.app.ui.material.MaterialDetailScreen
import com.tdcostmanager.app.ui.material.MaterialFormScreen
import com.tdcostmanager.app.ui.material.MaterialListScreen
import com.tdcostmanager.app.ui.material.MaterialViewModel
import com.tdcostmanager.app.ui.project.ProjectDetailScreen
import com.tdcostmanager.app.ui.project.ProjectFormScreen
import com.tdcostmanager.app.ui.project.ProjectListScreen
import com.tdcostmanager.app.ui.project.ProjectViewModel
import com.tdcostmanager.app.ui.quote.QuoteCalculatorScreen
import com.tdcostmanager.app.ui.quote.QuoteDetailScreen
import com.tdcostmanager.app.ui.quote.QuoteListScreen
import com.tdcostmanager.app.ui.quote.QuoteViewModel
import com.tdcostmanager.app.ui.tool.ToolDetailScreen
import com.tdcostmanager.app.ui.tool.ToolFormScreen
import com.tdcostmanager.app.ui.tool.ToolListScreen
import com.tdcostmanager.app.ui.tool.ToolViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object ProjectList : Screen("project_list")
    data object ProjectDetail : Screen("project_detail/{id}") {
        fun createRoute(id: Long) = "project_detail/$id"
    }
    data object ProjectForm : Screen("project_form?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "project_form?id=$id" else "project_form"
    }
    data object MaterialList : Screen("material_list")
    data object MaterialDetail : Screen("material_detail/{id}") {
        fun createRoute(id: Long) = "material_detail/$id"
    }
    data object MaterialForm : Screen("material_form?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "material_form?id=$id" else "material_form"
    }
    data object MachineList : Screen("machine_list")
    data object MachineDetail : Screen("machine_detail/{id}") {
        fun createRoute(id: Long) = "machine_detail/$id"
    }
    data object MachineForm : Screen("machine_form?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "machine_form?id=$id" else "machine_form"
    }
    data object ToolList : Screen("tool_list")
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

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val factory = ViewModelFactory(context)
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) Screen.Home.route else Screen.Login.route
    ) {
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
        composable(Screen.Home.route) {
            val projectViewModel: ProjectViewModel = viewModel(factory = factory)
            ProjectListScreen(
                viewModel = projectViewModel,
                onProjectClick = { id -> navController.navigate(Screen.ProjectDetail.createRoute(id)) },
                onCreateProject = { navController.navigate(Screen.ProjectForm.createRoute()) },
                onNavigateToMaterials = { navController.navigate(Screen.MaterialList.route) },
                onNavigateToMachines = { navController.navigate(Screen.MachineList.route) },
                onNavigateToTools = { navController.navigate(Screen.ToolList.route) }
            )
        }
        composable(Screen.MaterialList.route) {
            val materialViewModel: MaterialViewModel = viewModel(factory = factory)
            MaterialListScreen(
                viewModel = materialViewModel,
                onMaterialClick = { id -> navController.navigate(Screen.MaterialDetail.createRoute(id)) },
                onCreateMaterial = { navController.navigate(Screen.MaterialForm.createRoute()) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.MaterialDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val materialViewModel: MaterialViewModel = viewModel(factory = factory)
            MaterialDetailScreen(
                id = id,
                viewModel = materialViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { mid -> navController.navigate(Screen.MaterialForm.createRoute(mid)) }
            )
        }
        composable(
            route = Screen.MaterialForm.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id").let { if (it == -1L) null else it }
            val materialViewModel: MaterialViewModel = viewModel(factory = factory)
            MaterialFormScreen(
                id = id,
                viewModel = materialViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MachineList.route) {
            val machineViewModel: MachineViewModel = viewModel(factory = factory)
            MachineListScreen(
                viewModel = machineViewModel,
                onMachineClick = { id -> navController.navigate(Screen.MachineDetail.createRoute(id)) },
                onCreateMachine = { navController.navigate(Screen.MachineForm.createRoute()) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.MachineDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val machineViewModel: MachineViewModel = viewModel(factory = factory)
            MachineDetailScreen(
                id = id,
                viewModel = machineViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { mid -> navController.navigate(Screen.MachineForm.createRoute(mid)) }
            )
        }
        composable(
            route = Screen.MachineForm.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id").let { if (it == -1L) null else it }
            val machineViewModel: MachineViewModel = viewModel(factory = factory)
            MachineFormScreen(
                id = id,
                viewModel = machineViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ToolList.route) {
            val toolViewModel: ToolViewModel = viewModel(factory = factory)
            ToolListScreen(
                viewModel = toolViewModel,
                onToolClick = { id -> navController.navigate(Screen.ToolDetail.createRoute(id)) },
                onCreateTool = { navController.navigate(Screen.ToolForm.createRoute()) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ToolDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val toolViewModel: ToolViewModel = viewModel(factory = factory)
            ToolDetailScreen(
                id = id,
                viewModel = toolViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { tid -> navController.navigate(Screen.ToolForm.createRoute(tid)) }
            )
        }
        composable(
            route = Screen.ToolForm.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id").let { if (it == -1L) null else it }
            val toolViewModel: ToolViewModel = viewModel(factory = factory)
            ToolFormScreen(
                id = id,
                viewModel = toolViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ProjectDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val projectViewModel: ProjectViewModel = viewModel(factory = factory)
            ProjectDetailScreen(
                id = id,
                viewModel = projectViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { projectId -> navController.navigate(Screen.ProjectForm.createRoute(projectId)) },
                onNavigateToQuotes = { projectId -> navController.navigate(Screen.QuoteList.createRoute(projectId)) },
                onNavigateToCalculator = { projectId -> navController.navigate(Screen.QuoteCalculator.createRoute(projectId)) }
            )
        }
        composable(
            route = Screen.ProjectForm.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id").let { if (it == -1L) null else it }
            val projectViewModel: ProjectViewModel = viewModel(factory = factory)
            ProjectFormScreen(
                id = id,
                viewModel = projectViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.QuoteList.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val quoteViewModel: QuoteViewModel = viewModel(factory = factory)
            QuoteListScreen(
                projectId = projectId,
                viewModel = quoteViewModel,
                onQuoteClick = { quoteId -> navController.navigate(Screen.QuoteDetail.createRoute(projectId, quoteId)) },
                onCreateQuote = { navController.navigate(Screen.QuoteCalculator.createRoute(projectId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.QuoteCalculator.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val quoteViewModel: QuoteViewModel = viewModel(factory = factory)
            QuoteCalculatorScreen(
                projectId = projectId,
                viewModel = quoteViewModel,
                onNavigateBack = { navController.popBackStack() },
                onQuoteCreated = { quoteId -> navController.navigate(Screen.QuoteDetail.createRoute(projectId, quoteId)) {
                    popUpTo(Screen.QuoteList.createRoute(projectId))
                } }
            )
        }
        composable(
            route = Screen.QuoteDetail.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("quoteId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val quoteId = backStackEntry.arguments?.getLong("quoteId") ?: 0L
            val quoteViewModel: QuoteViewModel = viewModel(factory = factory)
            QuoteDetailScreen(
                projectId = projectId,
                quoteId = quoteId,
                viewModel = quoteViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
