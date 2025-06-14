package com.kreditnik.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kreditnik.app.data.DatabaseProvider
import com.kreditnik.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = DatabaseProvider.getDatabase(context).loanDao()
                val loans = dao.getAllLoans()
                for (loan in loans) {
                    if (loan.reminderEnabled) {
                        NotificationHelper.scheduleLoanReminder(context, loan)
                    }
                }
            }
        }
    }
}
