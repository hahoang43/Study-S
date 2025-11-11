package com.example.study_s.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")

    val isDarkTheme: Flow<Boolean?> = context.dataStore.data
        .map {
            it[isDarkThemeKey]
        }

    suspend fun setDarkTheme(isDarkTheme: Boolean) {
        context.dataStore.edit {
            it[isDarkThemeKey] = isDarkTheme
        }
    }
}