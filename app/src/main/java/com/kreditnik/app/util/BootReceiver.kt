package com.kreditnik.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kreditnik.app.data.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.kreditnik.app.util.NotificationHelper

class BootReceiver : BroadcastReceiver() {

    private val relevantActions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_DATE_CHANGED,
        "android.intent.action.TIME_SET"
    )

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in relevantActions) return

        Log.d("ReminderTest", "🔄 BootReceiver получил событие: $action")

        Log.d("ReminderTest", "BootReceiver received $action")

        CoroutineScope(Dispatchers.IO).launch {
            val loans = DatabaseProvider.getDatabase(context).loanDao().getAllLoans()
            loans.filter { it.reminderEnabled }
                .forEach { NotificationHelper.scheduleLoanReminder(context, it) }
        }
    }
}
