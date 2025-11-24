package week11.st421007.finalproject.util

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import week11.st421007.finalproject.ui.screens.main.AddEntryScreen
import week11.st421007.finalproject.ui.screens.main.EditEntryScreen
import week11.st421007.finalproject.ui.screens.main.JournalListScreen
import week11.st421007.finalproject.ui.screens.main.MapScreen
import week11.st421007.finalproject.viewmodel.JournalViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object AddEntry : Screen("add_entry")
    object EditEntry : Screen("edit_entry/{entryId}") {
        fun createRoute(entryId: String) = "edit_entry/$entryId"
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    journalViewModel: JournalViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            var selectedTab by remember { mutableStateOf(0) }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = "Journal") },
                            label = { Text("Journal") },
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                            label = { Text("Map") },
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                    when (selectedTab) {
                        0 -> JournalListScreen(
                            journalViewModel = journalViewModel,
                            onNavigateToAddEntry = {
                                navController.navigate(Screen.AddEntry.route)
                            },
                            onNavigateToEditEntry = { entryId ->
                                navController.navigate(Screen.EditEntry.createRoute(entryId))
                            }
                        )
                        1 -> MapScreen()
                    }
                }
            }
        }

        composable(Screen.AddEntry.route) {
            AddEntryScreen(
                journalViewModel = journalViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditEntry.route,
            arguments = listOf(
                navArgument("entryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            EditEntryScreen(
                entryId = entryId,
                journalViewModel = journalViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}