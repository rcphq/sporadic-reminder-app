package com.sporadic.reminder.data.repository

import android.content.Context
import com.sporadic.reminder.ui.settings.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("sporadic_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)
        return try { ThemeMode.valueOf(name!!) } catch (_: Exception) { ThemeMode.SYSTEM }
    }

    companion object {
        private const val KEY_THEME = "theme_mode"
    }
}
