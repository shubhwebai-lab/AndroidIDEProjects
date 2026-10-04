package com.jarvis.assistant.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class AppPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)

    fun getThemeMode(): Int {
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_YES)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
    }
}