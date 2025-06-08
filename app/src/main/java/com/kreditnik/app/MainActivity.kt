package com.kreditnik.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kreditnik.app.data.DatabaseProvider
import com.kreditnik.app.data.LoanRepository
import com.kreditnik.app.ui.screens.*
import com.kreditnik.app.ui.theme.KreditnikTheme
import com.kreditnik.app.viewmodel.LoanViewModel
import com.kreditnik.app.viewmodel.LoanViewModelFactory
import com.kreditnik.app.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(300L)
                .withEndAction { provider.remove() }
        }

        super.onCreate(savedInstanceState)

        setContent {
            // ВНУТРИ setContent
            val systemTheme = isSystemInDarkTheme()
            var useDarkTheme by rememberSaveable { mutableStateOf(systemTheme) }

            val settingsViewModel: SettingsViewModel = viewModel()
            val darkThemeState by settingsViewModel.darkModeEnabled.collectAsState(initial = null)

            LaunchedEffect(darkThemeState) {
                darkThemeState?.let { useDarkTheme = it }
            }

            KreditnikTheme(darkTheme = useDarkTheme) {
                val loanViewModel: LoanViewModel = viewModel(
                    factory = LoanViewModelFactory(
                        LoanRepository(
                            loanDao = DatabaseProvider.getDatabase(applicationContext).loanDao(),
                            operationDao = DatabaseProvider.getDatabase(applicationContext).operationDao()
                        )
                    )
                )

                MainScreen(loanViewModel, settingsViewModel)
            }
        }
    }
}

// Остальной код MainScreen без изменений


enum class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    Credits("credits", Icons.AutoMirrored.Filled.List, "Кредиты"),
    History("history", Icons.Filled.History, "История"),
    Analytics("analytics", Icons.Filled.PieChart, "Аналитика"),
    Settings("settings", Icons.Filled.Settings, "Настройки")
}

@Composable
fun MainScreen(
    loanViewModel: LoanViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navControllers = remember {
        BottomNavItem.values().associateWith { mutableStateOf<NavHostController?>(null) }
    }

    var selectedItem by rememberSaveable { mutableStateOf(BottomNavItem.Credits) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.values().forEach { item ->
                    NavigationBarItem(
                        selected = selectedItem == item,
                        onClick = {
                            val controller = navControllers[item]?.value
                            if (selectedItem == item && controller != null) {
                                controller.popBackStack(route = item.route, inclusive = false)
                            } else {
                                selectedItem = item
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val navController = navControllers.getValue(selectedItem).value
            ?: rememberNavController().also { navControllers.getValue(selectedItem).value = it }

        NavHost(
            navController = navController,
            startDestination = selectedItem.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(BottomNavItem.Credits.route) {
                CreditsScreen(
                    loanViewModel = loanViewModel,
                    settingsViewModel = settingsViewModel,
                    navController = navController
                )
            }
            composable(BottomNavItem.History.route) {
                HistoryScreen(viewModel = loanViewModel)
            }
            composable(BottomNavItem.Analytics.route) {
                AnalyticsScreen()
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen()
            }
            composable("addLoan") {
                AddLoanScreen(loanViewModel, navController)
            }
            composable(
                route = "editLoan/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: return@composable
                val editLoan = loanViewModel.loans.collectAsState().value.firstOrNull { it.id == loanId }
                if (editLoan != null) {
                    AddLoanScreen(loanViewModel, navController, editLoan)
                }
            }
            composable(
                route = "loanDetail/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.IntType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getInt("loanId") ?: return@composable
                val loan = loanViewModel.loans.collectAsState().value.firstOrNull { it.id == loanId.toLong() }
                val detailSettingsViewModel: SettingsViewModel = viewModel()
                if (loan != null) {
                    LoanDetailScreen(
                        loan = loan,
                        settingsViewModel = detailSettingsViewModel,
                        navController = navController,
                        loanViewModel = loanViewModel
                    )
                }
            }
            composable(
                route = "paymentSchedule/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: return@composable
                PaymentScheduleScreen(loanViewModel, loanId)
            }
        }
    }
}
