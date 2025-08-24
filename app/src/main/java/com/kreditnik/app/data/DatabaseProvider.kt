package com.kreditnik.app.data

import android.content.Context
import androidx.room.Room

/**
 * Синглтон, предоставляющий единую точку доступа к экземпляру базы данных [LoanDatabase].
 * Реализует потокобезопасную ленивую инициализацию.
 */
object DatabaseProvider {
    @Volatile
    private var INSTANCE: LoanDatabase? = null

    fun getDatabase(context: Context): LoanDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                LoanDatabase::class.java,
                "loan_database"
            )
                // При миграции схемы данных старая база будет удалена.
                // Подходит для разработки, но требует стратегии миграции для продакшена.
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }
}