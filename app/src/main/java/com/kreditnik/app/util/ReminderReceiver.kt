package com.kreditnik.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kreditnik.app.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val loanName = intent.getStringExtra("loanName") ?: "Кредит"
        val loanId = intent.getLongExtra("loanId", 0)

        val notification = NotificationCompat.Builder(context, "loan_channel")
            .setSmallIcon(R.drawable.ic_notification) // добавь свою иконку в res/drawable
            .setContentTitle("Завтра платёж по кредиту")
            .setContentText("Кредит: $loanName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(1000 + loanId.toInt(), notification)
        }
    }
}
