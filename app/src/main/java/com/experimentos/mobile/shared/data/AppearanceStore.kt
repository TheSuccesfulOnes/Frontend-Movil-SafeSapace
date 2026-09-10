package com.experimentos.mobile.shared.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stores non-sensitive presentation preferences locally for immediate UI updates. */
class AppearanceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private var activeAccountKey: String? = null
    private val mutableTheme = MutableStateFlow(DEFAULT_THEME)
    val theme: StateFlow<String> = mutableTheme.asStateFlow()
    private val mutableLanguage = MutableStateFlow(DEFAULT_LANGUAGE)
    val language: StateFlow<String> = mutableLanguage.asStateFlow()

    /** Loads preferences for the authenticated account and resets to defaults when signed out. */
    @Synchronized
    fun activateAccount(accountKey: String?) {
        activeAccountKey = accountKey?.takeIf(String::isNotBlank)
        val key = activeAccountKey
        mutableTheme.value = if (key == null) {
            DEFAULT_THEME
        } else {
            normalizeTheme(preferences.getString(scopedKey(key, THEME_KEY), DEFAULT_THEME))
        }
        mutableLanguage.value = if (key == null) {
            DEFAULT_LANGUAGE
        } else {
            normalizeLanguage(preferences.getString(scopedKey(key, LANGUAGE_KEY), DEFAULT_LANGUAGE))
        }
    }

    fun saveTheme(theme: String) {
        val normalizedTheme = normalizeTheme(theme)
        activeAccountKey?.let { key ->
            preferences.edit { putString(scopedKey(key, THEME_KEY), normalizedTheme) }
        }
        mutableTheme.value = normalizedTheme
    }

    fun saveLanguage(language: String) {
        val normalizedLanguage = normalizeLanguage(language)
        activeAccountKey?.let { key ->
            preferences.edit { putString(scopedKey(key, LANGUAGE_KEY), normalizedLanguage) }
        }
        mutableLanguage.value = normalizedLanguage
    }

    /** Applies server preferences to the same immutable account scope used by the local UI. */
    @Synchronized
    fun syncAccountPreferences(accountKey: String, language: String, theme: String) {
        activateAccount(accountKey)
        saveLanguage(language)
        saveTheme(theme)
    }

    private fun normalizeLanguage(language: String?): String =
        if (language.equals("en", ignoreCase = true)) "en" else DEFAULT_LANGUAGE

    private fun normalizeTheme(theme: String?): String =
        if (theme.equals("DARK", ignoreCase = true)) "DARK" else DEFAULT_THEME

    private fun scopedKey(accountKey: String, preferenceKey: String): String =
        "account_${accountKey}_$preferenceKey"

    private companion object {
        const val PREFERENCES_NAME = "appearance_preferences"
        const val THEME_KEY = "theme"
        const val LANGUAGE_KEY = "language"
        const val DEFAULT_LANGUAGE = "es"
        const val DEFAULT_THEME = "LIGHT"
    }
}
