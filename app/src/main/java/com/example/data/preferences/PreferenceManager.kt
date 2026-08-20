package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

class PreferenceManager(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
        val SOUND_ALERTS_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("sound_alerts")
        val VIBRATION_ALERTS_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("vibration_alerts")
        val UNIT_CELSIUS_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("unit_celsius")
        val AUTO_SYNC_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("auto_sync")
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppTheme.LIGHT.name
        AppTheme.valueOf(themeName)
    }

    val soundAlertsFlow: Flow<Boolean> = context.dataStore.data.map { it[SOUND_ALERTS_KEY] ?: true }
    val vibrationAlertsFlow: Flow<Boolean> = context.dataStore.data.map { it[VIBRATION_ALERTS_KEY] ?: true }
    val unitCelsiusFlow: Flow<Boolean> = context.dataStore.data.map { it[UNIT_CELSIUS_KEY] ?: true }
    val autoSyncFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_SYNC_KEY] ?: true }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    suspend fun setSoundAlerts(enabled: Boolean) {
        context.dataStore.edit { it[SOUND_ALERTS_KEY] = enabled }
    }

    suspend fun setVibrationAlerts(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ALERTS_KEY] = enabled }
    }

    suspend fun setUnitCelsius(enabled: Boolean) {
        context.dataStore.edit { it[UNIT_CELSIUS_KEY] = enabled }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_SYNC_KEY] = enabled }
    }
}
