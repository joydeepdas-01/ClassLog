package com.classlog.app

import android.app.Application
import com.classlog.app.data.local.AppDatabase
import com.classlog.app.di.AppContainer

class ClassLogApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.build(this)
    }

    val container: AppContainer by lazy {
        AppContainer(this, database)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
