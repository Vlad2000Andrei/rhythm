package com.rhythm.habittracker.widget

import android.content.Context

object WidgetPreferencesStore {

    private const val PREFS_NAME = "rhythm_widget_prefs"
    private const val KEY_THEME_PREFIX = "widget_theme_"
    private const val KEY_HABIT_PREFIX = "widget_habit_"
    const val NO_HABIT_ID = -1L

    fun getThemeMode(context: Context, appWidgetId: Int): WidgetThemeMode {
        val raw = prefs(context).getString(KEY_THEME_PREFIX + appWidgetId, WidgetThemeMode.LIGHT.name)
        return WidgetThemeMode.entries.firstOrNull { it.name == raw } ?: WidgetThemeMode.LIGHT
    }

    fun setThemeMode(context: Context, appWidgetId: Int, mode: WidgetThemeMode) {
        prefs(context).edit()
            .putString(KEY_THEME_PREFIX + appWidgetId, mode.name)
            .apply()
    }

    fun getHabitId(context: Context, appWidgetId: Int): Long? {
        val stored = prefs(context).getLong(KEY_HABIT_PREFIX + appWidgetId, NO_HABIT_ID)
        return if (stored > 0L) stored else null
    }

    fun setHabitId(context: Context, appWidgetId: Int, habitId: Long?) {
        prefs(context).edit()
            .putLong(KEY_HABIT_PREFIX + appWidgetId, habitId ?: NO_HABIT_ID)
            .apply()
    }

    fun saveConfig(
        context: Context,
        appWidgetId: Int,
        themeMode: WidgetThemeMode,
        habitId: Long?,
    ) {
        prefs(context).edit()
            .putString(KEY_THEME_PREFIX + appWidgetId, themeMode.name)
            .putLong(KEY_HABIT_PREFIX + appWidgetId, habitId ?: NO_HABIT_ID)
            .apply()
    }

    fun deleteConfig(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove(KEY_THEME_PREFIX + appWidgetId)
            .remove(KEY_HABIT_PREFIX + appWidgetId)
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
