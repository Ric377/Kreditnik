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

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WakeLockHelper.acquireTemporaryWakeLock(context)
        //тест
        android.util.Log.d("ReminderTest", "📩 onReceive вызван")


        val loanName = intent.getStringExtra("loanName") ?: return
        val loanId = intent.getLongExtra("loanId", 0L)
        val monthlyPayment = intent.getDoubleExtra("monthlyPayment", 0.0)

        val formattedPayment = (monthlyPayment * 100).roundToInt() / 100.0

        val builder = NotificationCompat.Builder(context, "loan_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Напоминание о платеже")
            .setContentText("Завтра платёж по \"$loanName\" на $formattedPayment ₽.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

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
