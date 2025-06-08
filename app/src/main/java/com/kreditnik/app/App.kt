package com.kreditnik.app

import android.app.Application
import com.kreditnik.app.data.DatabaseProvider

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Принудительно инициализируем базу заранее
        DatabaseProvider.getDatabase(this)
    }
}
