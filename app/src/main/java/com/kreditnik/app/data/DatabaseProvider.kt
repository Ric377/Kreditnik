package com.kreditnik.app.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var INSTANCE: LoanDatabase? = null

    fun getDatabase(context: Context): LoanDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                LoanDatabase::class.java,
                "loan_database"
            ).fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }
}
