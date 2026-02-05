package com.idsr_project.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREF_NAME = "idsr_theme_pref"
    private const val KEY_THEME = "theme_mode"

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return prefs ?: synchronized(this) {
            prefs ?: context.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .also { prefs = it }
        }
    }

    fun applyTheme(context: Context) {
        val mode = getPrefs(context)
            .getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_NO)

        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun toggleTheme(context: Context) {
        val preferences = getPrefs(context)

        val currentMode =
            preferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_NO)

        val newMode =
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES)
                AppCompatDelegate.MODE_NIGHT_NO
            else
                AppCompatDelegate.MODE_NIGHT_YES

        preferences.edit().putInt(KEY_THEME, newMode).apply()
        AppCompatDelegate.setDefaultNightMode(newMode)
    }

    fun isDarkMode(context: Context): Boolean {
        return getPrefs(context)
            .getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_NO) ==
                AppCompatDelegate.MODE_NIGHT_YES
    }
}
