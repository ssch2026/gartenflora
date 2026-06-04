package de.gartenflora.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.gartenflora.R
import de.gartenflora.ui.capture.CaptureScreen
import de.gartenflora.ui.detail.DetailScreen
import de.gartenflora.ui.garden.MeinGartenScreen
import de.gartenflora.ui.results.ResultsScreen
import de.gartenflora.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Capture : Screen("capture")
    object Results : Screen("results/{imagePaths}/{organs}/{project}") {
        fun createRoute(imagePaths: String, organs: String, project: String) =
            "results/$imagePaths/$organs/$project"
    }
    object MeinGarten : Screen("mein_garten")
    object Detail : Screen("detail/{observationId}") {
        fun createRoute(observationId: Long) = "detail/$observationId"
    }
    object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: @Composable () -> Unit
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem(
            screen = Screen.Capture,
            labelRes = R.string.nav_aufnahme,
            icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) }
        ),
        BottomNavItem(
            screen = Screen.MeinGarten,
            labelRes = R.string.nav_mein_garten,
            icon = { Icon(Icons.Filled.LocalFlorist, contentDescription = null) }
        ),
        BottomNavItem(
            screen = Screen.Settings,
            labelRes = R.string.nav_einstellungen,
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
        )
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = bottomNavItems.any { item ->
                currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            }
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Capture.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Capture.route) {
                CaptureScreen(
                    onNavigateToResults = { imagePaths, organs, project ->
                        val encodedPaths = java.net.URLEncoder.encode(imagePaths, "UTF-8")
                        val encodedOrgans = java.net.URLEncoder.encode(organs, "UTF-8")
                        val encodedProject = java.net.URLEncoder.encode(project, "UTF-8")
                        navController.navigate(
                            Screen.Results.createRoute(encodedPaths, encodedOrgans, encodedProject)
                        )
                    }
                )
            }
            composable(
                route = Screen.Results.route,
                arguments = listOf(
                    navArgument("imagePaths") { type = NavType.StringType },
                    navArgument("organs") { type = NavType.StringType },
                    navArgument("project") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val imagePaths = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("imagePaths") ?: "", "UTF-8"
                )
                val organs = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("organs") ?: "", "UTF-8"
                )
                val project = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("project") ?: "all", "UTF-8"
                )
                ResultsScreen(
                    imagePaths = imagePaths,
                    organs = organs,
                    project = project,
                    onNavigateToGarden = {
                        navController.navigate(Screen.MeinGarten.route) {
                            popUpTo(Screen.Capture.route) { inclusive = false }
                        }
                    },
                    onNavigateUp = { navController.navigateUp() }
                )
            }
            composable(Screen.MeinGarten.route) {
                MeinGartenScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    }
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("observationId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val observationId = backStackEntry.arguments?.getLong("observationId") ?: 0L
                DetailScreen(
                    observationId = observationId,
                    onNavigateUp = { navController.navigateUp() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
