package com.kreditnik.app.util

import android.content.Context
import android.os.PowerManager

object WakeLockHelper {
    fun acquireTemporaryWakeLock(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "kreditnik::ReminderWakeLock"
        )
        wakeLock.acquire(3 * 60 * 1000L) // держит телефон проснувшимся до 3 минут
    }
}
