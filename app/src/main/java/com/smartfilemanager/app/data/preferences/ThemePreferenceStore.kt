package com.smartfilemanager.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smartfilemanager.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object ThemePreferenceStore {
    val THEME_KEY = stringPreferencesKey("theme_preference")

    fun getTheme(context: Context): Flow<AppTheme> =
        context.dataStore.data.map { prefs ->
            when (prefs[THEME_KEY]) {
                "light" -> AppTheme.LIGHT
                "dark"  -> AppTheme.DARK
                else    -> AppTheme.SYSTEM
            }
        }

    suspend fun setTheme(context: Context, theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name.lowercase()
        }
    }
}
