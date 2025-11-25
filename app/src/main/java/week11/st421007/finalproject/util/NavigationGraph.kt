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
import week11.st421007.finalproject.ui.screens.auth.ForgotPasswordScreen
import week11.st421007.finalproject.ui.screens.auth.LoginScreen
import week11.st421007.finalproject.ui.screens.auth.SignupScreen
import week11.st421007.finalproject.ui.screens.main.AddEntryScreen
import week11.st421007.finalproject.ui.screens.main.EditEntryScreen
import week11.st421007.finalproject.ui.screens.main.JournalListScreen
import week11.st421007.finalproject.ui.screens.main.MapScreen
import week11.st421007.finalproject.viewmodel.AuthViewModel
import week11.st421007.finalproject.viewmodel.JournalViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object Main : Screen("main")
    object AddEntry : Screen("add_entry")
    object EditEntry : Screen("edit_entry/{entryId}") {
        fun createRoute(entryId: String) = "edit_entry/$entryId"
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    journalViewModel: JournalViewModel = viewModel()
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    val startDestination = if (isAuthenticated) {
        Screen.Main.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onSignupSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onResetSuccess = {
                    navController.popBackStack()
                }
            )
        }

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
                            authViewModel = authViewModel,
                            journalViewModel = journalViewModel,
                            onNavigateToAddEntry = {
                                navController.navigate(Screen.AddEntry.route)
                            },
                            onNavigateToEditEntry = { entryId ->
                                navController.navigate(Screen.EditEntry.createRoute(entryId))
                            },
                            onLogout = {
                                authViewModel.signOut()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                        1 -> MapScreen()
                    }
                }
            }
        }

        composable(Screen.AddEntry.route) {
            AddEntryScreen(
                authViewModel = authViewModel,
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
                authViewModel = authViewModel,
                journalViewModel = journalViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
