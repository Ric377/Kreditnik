package com.kreditnik.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
import com.kreditnik.app.data.LoanType
import com.kreditnik.app.data.Loan
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        requestExactAlarmPermissionIfNeeded()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(300L)
                .withEndAction { provider.remove() }
        }

        setContent {
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
                        )
                    )
                )

                // === НАЧАЛО: автосоздание тестового кредита ===
                LaunchedEffect(Unit) {
                    val existingLoans = loanViewModel.loans.value
                    if (existingLoans.isEmpty()) {
                        loanViewModel.addLoan(
                            com.kreditnik.app.data.Loan(
                                name = "Тестовый кредит",
                                type = LoanType.CREDIT,
                                principal = 10000.0,
                                accruedInterest = 500.0,
                                initialPrincipal = 10000.0,
                                interestRate = 10.0,
                                months = 12,
                                startDate = LocalDate.now().minusMonths(1),
                                monthlyPaymentDay = 15,
                                reminderEnabled = true,
                                logo = "",
                                gracePeriodDays = 0,
                                mandatoryPaymentDay = 0,
                                gracePeriodEndDate = null,
                                debtDueDate = null
                            )
                        )
                    }
                }
                // === КОНЕЦ: автосоздание тестового кредита ===

                MainScreen(loanViewModel, settingsViewModel)
            }
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "loan_channel",
                "Кредиты",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о платежах и советах"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val builder = NotificationCompat.Builder(this, "loan_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Напоминание о платеже")
            .setContentText("Завтра платёж по кредиту на 8000 ₽.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            notify(1001, builder.build())
        }
    }
}

enum class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    Credits("credits", Icons.AutoMirrored.Filled.List, "Кредиты"),
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
        }
    }
}
