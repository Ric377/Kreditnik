package com.kreditnik.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Loan::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LoanDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
}

