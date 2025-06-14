package com.kreditnik.app.util

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.roundToInt
import com.kreditnik.app.R
import android.app.PendingIntent


class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WakeLockHelper.acquireTemporaryWakeLock(context)
        //тест
        android.util.Log.d("ReminderTest", "📩 onReceive вызван")


        val loanName = intent.getStringExtra("loanName") ?: return
        val loanId = intent.getLongExtra("loanId", 0L)
        val monthlyPayment = intent.getDoubleExtra("monthlyPayment", 0.0)

        val formattedPayment = (monthlyPayment * 100).roundToInt() / 100.0

        val intentToOpenApp = Intent(context, com.kreditnik.app.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            loanId.toInt(),
            intentToOpenApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "loan_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Напоминание о платеже")
            .setContentText("Завтра платёж по \"$loanName\" на $formattedPayment ₽.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)


        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                //тест
                android.util.Log.d("ReminderTest", "🔔 Показ уведомления: \"$loanName\", $formattedPayment ₽")


                NotificationManagerCompat.from(context).notify(loanId.toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
