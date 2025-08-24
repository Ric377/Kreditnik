package com.kreditnik.app.util

import android.content.Context
import android.os.PowerManager

/**
 * Утилита для управления [PowerManager.WakeLock].
 * Используется для кратковременного пробуждения устройства, чтобы гарантировать
 * выполнение фоновых задач, таких как отображение уведомлений.
 */
object WakeLockHelper {

    /**
     * Захватывает частичный WakeLock на короткий промежуток времени.
     * Это гарантирует, что CPU продолжит работать, даже если экран выключен,
     * позволяя [ReminderReceiver] завершить свою работу.
     *
     * @param context Контекст приложения.
     */
    fun acquireTemporaryWakeLock(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Kreditnik::ReminderWakeLockTag"
        )
        // Захватываем WakeLock с таймаутом (1 минута), чтобы он
        // автоматически освободился и не расходовал батарею.
        wakeLock.acquire(1 * 60 * 1000L)
    }
}
