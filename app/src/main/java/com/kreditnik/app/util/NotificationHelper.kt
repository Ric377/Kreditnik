package com.kreditnik.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.kreditnik.app.data.Loan
import com.kreditnik.app.util.ReminderReceiver
import android.util.Log
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object NotificationHelper {

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

        alarmManager.cancel(pendingIntent) // ← ✅ отменяем старую




        val reminderTime = calculateReminderTime(loan)
        val now = System.currentTimeMillis()

        val paymentDay = reminderTime + 24 * 60 * 60 * 1000L // 12:00 следующего дня
        val latestAllowedTime = paymentDay - 60_000 // 23:59:00

        val finalReminderTime = if (now in reminderTime..latestAllowedTime) {
            Log.d("ReminderTest", "🟡 Системное время попадает в диапазон 12:00 до 23:59 — срабатываем немедленно")
            now + 5_000 // через 5 секунд
        } else {
            reminderTime
        }

        Log.d("ReminderTest", "🔧 Reminder будет установлен на: " +
                java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(java.util.Date(finalReminderTime))
        )


        //ТЕСТ
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
        android.util.Log.d("ReminderTest", "⏰ Устанавливаем уведомление на ${sdf.format(java.util.Date(reminderTime))}")
        //КОНЕЦ ТЕСТА

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    finalReminderTime,
                    pendingIntent
                )

            } else {
                Toast.makeText(
                    context,
                    "Приложение не имеет разрешения на точные напоминания.",
                    Toast.LENGTH_LONG
                ).show()
            }
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

        val reminderDate = paymentDay.minusDays(loan.reminderDaysBefore?.toLong() ?: 1)
        val time = try {
            LocalTime.parse(loan.reminderTime ?: "12:00")
        } catch (e: Exception) {
            LocalTime.of(12, 0)
        }


        return reminderDate.atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

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

        val timeInMillis = System.currentTimeMillis() + 60 * 1000 // через 1 минуту

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )

        Toast.makeText(context, "Тестовое уведомление через 1 минуту", Toast.LENGTH_SHORT).show()
    }

}
