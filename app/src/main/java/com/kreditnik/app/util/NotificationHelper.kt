package com.kreditnik.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.kreditnik.app.data.Loan
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object NotificationHelper {

    fun scheduleLoanReminder(context: Context, loan: Loan) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    context,
                    "Приложение не имеет разрешения на точные напоминания.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("loanName", loan.name)
            putExtra("loanId", loan.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            loan.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reminderTime = calculateReminderTime(loan)

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
            Toast.makeText(
                context,
                "Напоминание установлено на ${loan.name} за день до платежа в 12:00.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Ошибка при установке напоминания: нет разрешения.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

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

        Toast.makeText(
            context,
            "Напоминание отменено для ${loan.name}.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun calculateReminderTime(loan: Loan): Long {
        val today = LocalDate.now()
        val paymentDay = if (loan.monthlyPaymentDay == 0) {
            today.with(TemporalAdjusters.lastDayOfMonth())
        } else {
            val tentative = today.withDayOfMonth(loan.monthlyPaymentDay.coerceAtMost(today.lengthOfMonth()))
            if (tentative.isBefore(today) || tentative == today) {
                tentative.plusMonths(1)
            } else {
                tentative
            }
        }

        val reminderDate = paymentDay.minusDays(1)
        val time = LocalTime.of(12, 0) // 12:00 дня

        return reminderDate.atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
