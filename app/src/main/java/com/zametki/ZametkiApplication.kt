package com.zametki

import android.app.Application
import androidx.room.Room
import com.zametki.data.AppDatabase
import com.zametki.data.MIGRATION_1_2

class ZametkiApplication : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "zametki_db"
        ).fallbackToDestructiveMigration().build()
    }
}
