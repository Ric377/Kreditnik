package com.kreditnik.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
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

/**
 * Главная и единственная Activity в приложении.
 * Отвечает за настройку окружения, создание ViewModel, управление темой
 * и размещение основного Composable-компонента [MainScreen].
 */
class MainActivity : ComponentActivity() {

    // Регистрация лаунчера для запроса разрешений.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        installSplashScreen()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val useDarkTheme by settingsViewModel.darkModeEnabled.collectAsState(
                initial = isSystemInDarkTheme()
            )

            KreditnikTheme(darkTheme = useDarkTheme) {
                val loanViewModel: LoanViewModel = viewModel(
                    factory = LoanViewModelFactory(
                        LoanRepository(
                            loanDao = DatabaseProvider.getDatabase(applicationContext).loanDao(),
                        ),
                        applicationContext
                    )
                )
                MainScreen(loanViewModel, settingsViewModel)
            }
        }
    }

    /**
     * Проверяет и при необходимости запрашивает разрешение на установку точных будильников.
     * Актуально для Android 12 (API 31) и выше.
     */
    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    /**
     * Создает канал уведомлений, необходимый для Android 8.0 (API 26) и выше.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "loan_channel",
                "Напоминания о кредитах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для отправки напоминаний о предстоящих платежах."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Отправляет тестовое уведомление.
     * @param loanViewModel ViewModel для получения данных о кредите.
     */
    private fun sendNotification(loanViewModel: LoanViewModel) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val loan = loanViewModel.loans.value.firstOrNull() ?: return

        val total = loan.initialPrincipal / loan.months + loan.accruedInterest / loan.months
        val paymentText = "Завтра платёж по кредиту «${loan.name}» на сумму ${"%.2f".format(total)} ₽."

        val builder = NotificationCompat.Builder(this, "loan_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Напоминание о платеже")
            .setContentText(paymentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            notify(1001, builder.build())
        }
    }
}

/**
 * Определяет элементы нижней навигационной панели.
 */
private enum class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    Credits("credits", Icons.AutoMirrored.Filled.List, "Кредиты"),
    Settings("settings", Icons.Filled.Settings, "Настройки")
}

/**
 * Основной Composable-компонент, который строит UI приложения.
 */
@Composable
fun MainScreen(
    loanViewModel: LoanViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    var selectedItem by rememberSaveable { mutableStateOf(BottomNavItem.Credits) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.values().forEach { item ->
                    NavigationBarItem(
                        selected = selectedItem == item,
                        onClick = {
                            selectedItem = item
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Credits.route) {
                CreditsScreen(loanViewModel, settingsViewModel, navController)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(settingsViewModel)
            }
            composable("addLoan") {
                AddLoanScreen(loanViewModel, navController)
            }
            composable(
                route = "editLoan/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId")
                val loanToEdit = loanViewModel.loans.collectAsState().value.firstOrNull { it.id == loanId }
                if (loanToEdit != null) {
                    AddLoanScreen(loanViewModel, navController, loanToEdit)
                }
            }
            composable(
                route = "loanDetail/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId")
                val loan = loanViewModel.loans.collectAsState().value.firstOrNull { it.id == loanId }
                if (loan != null) {
                    LoanDetailScreen(loan, settingsViewModel, navController, loanViewModel)
                }
            }
        }
    }
}