package com.kreditnik.app.util

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kreditnik.app.R
import kotlin.math.roundToInt

/**
 * BroadcastReceiver, который срабатывает по сигналу от [AlarmManager]
 * для отображения уведомления о предстоящем платеже.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WakeLockHelper.acquireTemporaryWakeLock(context)

        val loanName = intent.getStringExtra("loanName") ?: return
        val loanId = intent.getLongExtra("loanId", -1L)
        val monthlyPayment = intent.getDoubleExtra("monthlyPayment", 0.0)

        if (loanId == -1L) return

        val intentToOpenApp = Intent(context, com.kreditnik.app.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            loanId.toInt(),
            intentToOpenApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedPayment = (monthlyPayment * 100).roundToInt() / 100.0
        val notificationText = "Завтра платёж по кредиту \"$loanName\" на сумму $formattedPayment ₽."

        val builder = NotificationCompat.Builder(context, "loan_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Напоминание о платеже")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(loanId.toInt(), builder.build())
        }
    }
}
