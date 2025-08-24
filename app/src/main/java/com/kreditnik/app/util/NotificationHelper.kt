package com.kreditnik.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.kreditnik.app.data.Loan
import com.kreditnik.app.data.DatabaseProvider
import com.kreditnik.app.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Утилитарный объект для управления планированием и отменой напоминаний о платежах.
 * Использует [AlarmManager] для установки точных "будильников", которые срабатывают
 * даже если приложение неактивно.
 */
object NotificationHelper {

    /**
     * Планирует напоминание для указанного кредита.
     * Если для этого кредита уже существует запланированное напоминание, оно будет перезаписано.
     *
     * @param context Контекст приложения.
     * @param loan Объект кредита, для которого нужно установить напоминание.
     */
    fun scheduleLoanReminder(context: Context, loan: Loan) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("loanName", loan.name)
            putExtra("loanId", loan.id)
            val monthlyRate = (loan.interestRate / 100) / 12
            val monthlyPayment = if (monthlyRate == 0.0) {
                loan.initialPrincipal / loan.months
            } else {
                loan.initialPrincipal * (monthlyRate * Math.pow(1 + monthlyRate, loan.months.toDouble())) /
                        (Math.pow(1 + monthlyRate, loan.months.toDouble()) - 1)
            }
            putExtra("monthlyPayment", monthlyPayment)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            loan.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reminderTimeInMillis = calculateReminderTime(context, loan)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeInMillis,
                    pendingIntent
                )
            } else {
                Toast.makeText(
                    context,
                    "Нет разрешения на установку точных напоминаний.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Отменяет ранее запланированное напоминание для указанного кредита.
     *
     * @param context Контекст приложения.
     * @param loan Объект кредита, для которого нужно отменить напоминание.
     */
    fun cancelLoanReminder(context: Context, loan: Loan) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            loan.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    /**
     * Вычисляет точное время в миллисекундах для срабатывания напоминания.
     *
     * @param context Контекст для доступа к [SettingsDataStore].
     * @param loan Кредит, для которого рассчитывается время.
     * @return Время срабатывания в миллисекундах с начала эпохи.
     */
    private fun calculateReminderTime(context: Context, loan: Loan): Long {
        val today = LocalDate.now()

        val paymentDay = if (loan.monthlyPaymentDay == 0) {
            today.with(TemporalAdjusters.lastDayOfMonth())
        } else {
            val tentative = today.withDayOfMonth(
                loan.monthlyPaymentDay.coerceAtMost(today.lengthOfMonth())
            )
            if (tentative.isBefore(today) || tentative == today) {
                tentative.plusMonths(1)
            } else {
                tentative
            }
        }

        val reminderDays = loan.reminderDaysBefore ?: runBlocking {
            SettingsDataStore(context).reminderDaysBeforeFlow.first()
        }
        val reminderDate = paymentDay.minusDays(reminderDays.toLong())

        val timeString = loan.reminderTime ?: runBlocking {
            SettingsDataStore(context).reminderTimeFlow.first()
        }
        val time = try {
            LocalTime.parse(timeString)
        } catch (e: Exception) {
            LocalTime.of(12, 0)
        }

        return reminderDate.atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Планирует тестовое напоминание, которое сработает через одну минуту.
     * Используется для проверки работоспособности системы уведомлений.
     */
    fun scheduleTestReminder(context: Context, loan: Loan) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("loanName", loan.name)
            putExtra("loanId", loan.id)
            val monthlyRate = (loan.interestRate / 100) / 12
            val monthlyPayment = if (monthlyRate == 0.0) {
                loan.initialPrincipal / loan.months
            } else {
                loan.initialPrincipal * (monthlyRate * Math.pow(1 + monthlyRate, loan.months.toDouble())) /
                        (Math.pow(1 + monthlyRate, loan.months.toDouble()) - 1)
            }
            putExtra("monthlyPayment", monthlyPayment)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            loan.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeInMillis = System.currentTimeMillis() + 60 * 1000

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )

        Toast.makeText(context, "Тестовое уведомление будет отправлено через 1 минуту", Toast.LENGTH_SHORT).show()
    }

    /**
     * Перепланирует напоминания для всех кредитов с включенными уведомлениями.
     * Вызывается после системных событий, таких как перезагрузка.
     */
    fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val loanDao = DatabaseProvider.getDatabase(context).loanDao()
            val loans = loanDao.getAllLoans()
            loans.filter { it.reminderEnabled }.forEach {
                scheduleLoanReminder(context, it)
            }
        }
    }
}
