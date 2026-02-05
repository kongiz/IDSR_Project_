package com.idsr_project.utils

import android.app.Application

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyTheme(this)
    }
}