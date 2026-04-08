package com.smartfilemanager.app.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.smartfilemanager.app.ui.screen.FileBrowserScreen
import com.smartfilemanager.app.ui.screen.HistoryDetailScreen
import com.smartfilemanager.app.ui.screen.HistoryScreen
import com.smartfilemanager.app.ui.screen.RuleBuilderScreen
import com.smartfilemanager.app.ui.screen.RulesListScreen
import com.smartfilemanager.app.ui.screen.RunScreen
import com.smartfilemanager.app.ui.screen.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Files : Screen("files", "Files", Icons.Filled.VideoFile)
    object Rules : Screen("rules", "Rules", Icons.Filled.Rule)
    object Run : Screen("run", "Run", Icons.Filled.PlayArrow)
    object History : Screen("history", "History", Icons.Filled.History)
}

val bottomNavItems = listOf(Screen.Files, Screen.Rules, Screen.Run, Screen.History)

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
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

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Files.route,
        modifier = modifier
    ) {
        composable(Screen.Files.route) {
            FileBrowserScreen(
                onCreateRuleForFolder = { folder ->
                    navController.navigate("rule_builder?folder=${Uri.encode(folder)}")
                }
            )
        }

        composable(Screen.Rules.route) {
            RulesListScreen(
                onAddRule = { navController.navigate("rule_builder") },
                onEditRule = { ruleId -> navController.navigate("rule_builder?ruleId=$ruleId") }
            )
        }
        composable(
            route = "rule_builder?ruleId={ruleId}&folder={folder}",
            arguments = listOf(
                navArgument("ruleId") { type = NavType.IntType; defaultValue = -1 },
                navArgument("folder") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getInt("ruleId").takeIf { it != -1 }
            val folder = backStackEntry.arguments?.getString("folder")
                ?.let { Uri.decode(it) }
                ?.ifEmpty { null }
            RuleBuilderScreen(
                ruleId = ruleId,
                prefilledFolder = folder,
                onNavigateBack = { navController.popBackStack() },
                onRuleSaved = {
                    if (ruleId == null) {
                        // New rule: go to Run tab with the rule pre-selected
                        navController.popBackStack()
                        navController.navigate(Screen.Run.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        // Edit: just go back
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(Screen.Run.route) { RunScreen() }

        composable(Screen.History.route) {
            HistoryScreen(
                onViewDetail = { logId -> navController.navigate("history_detail/$logId") }
            )
        }
        composable(
            route = "history_detail/{logId}",
            arguments = listOf(navArgument("logId") { type = NavType.IntType })
        ) { backStackEntry ->
            val logId = backStackEntry.arguments!!.getInt("logId")
            HistoryDetailScreen(
                logId = logId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
