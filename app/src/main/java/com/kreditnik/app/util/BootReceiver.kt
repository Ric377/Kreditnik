package com.kreditnik.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kreditnik.app.data.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver, который отслеживает системные события, такие как перезагрузка
 * устройства или изменение времени, и заново планирует все активные напоминания.
 */
class BootReceiver : BroadcastReceiver() {

    private val relevantActions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_LOCKED_BOOT_COMPLETED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in relevantActions) {
            CoroutineScope(Dispatchers.IO).launch {
                val loans = DatabaseProvider.getDatabase(context).loanDao().getAllLoans()
                loans.filter { it.reminderEnabled }
                    .forEach { loan ->
                        NotificationHelper.scheduleLoanReminder(context, loan)
                    }
            }
        }
    }
}
