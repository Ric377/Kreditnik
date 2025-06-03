package com.kreditnik.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kreditnik.app.ui.theme.KreditnikTheme
import androidx.compose.foundation.layout.padding
import androidx.activity.viewModels
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.viewmodel.LoanViewModelFactory
import com.kreditnik.app.data.LoanRepository
import com.kreditnik.app.data.DatabaseProvider
import com.kreditnik.app.ui.screens.AddLoanScreen
import com.kreditnik.app.ui.screens.CreditsScreen
import com.kreditnik.app.ui.screens.HistoryScreen
import com.kreditnik.app.ui.screens.AnalyticsScreen
import com.kreditnik.app.ui.screens.SettingsScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kreditnik.app.viewmodel.SettingsViewModel



class MainActivity : ComponentActivity() {
    private val loanViewModel: LoanViewModel by viewModels {
        LoanViewModelFactory(
            LoanRepository(
                DatabaseProvider.getDatabase(applicationContext).loanDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val darkTheme by settingsViewModel.darkModeEnabled.collectAsState()

            KreditnikTheme(darkTheme = darkTheme) {
                MainScreen(loanViewModel)
            }
        }
    }
}

enum class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    Credits("credits", Icons.AutoMirrored.Filled.List, "Кредиты"),
    History("history", Icons.Filled.History, "История"),
    Analytics("analytics", Icons.Filled.PieChart, "Аналитика"),
    Settings("settings", Icons.Filled.Settings, "Настройки")
}



@Composable
fun MainScreen(loanViewModel: LoanViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.values().forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Credits.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable(BottomNavItem.Credits.route) {
                CreditsScreen(loanViewModel, navController)
            }
            composable(BottomNavItem.History.route) { HistoryScreen() }
            composable(BottomNavItem.Analytics.route) { AnalyticsScreen() }
            composable(BottomNavItem.Settings.route) { SettingsScreen() }
            composable("addLoan") { AddLoanScreen(loanViewModel, navController) }
        }
    }
}