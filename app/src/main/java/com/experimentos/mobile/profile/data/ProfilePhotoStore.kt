package com.experimentos.mobile.profile.data

import android.content.Context
import androidx.core.content.edit

/** Stores the selected local avatar URI separately for each signed-in username. */
class ProfilePhotoStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getUri(username: String): String? = preferences.getString(keyFor(username), null)

    fun saveUri(username: String, uri: String?) {
        preferences.edit {
            if (uri.isNullOrBlank()) remove(keyFor(username)) else putString(keyFor(username), uri)
        }
    }

    private fun keyFor(username: String): String = PHOTO_PREFIX + username.trim().lowercase()

    private companion object {
        const val PREFERENCES_NAME = "profile_photo_preferences"
        const val PHOTO_PREFIX = "profile_photo_"
    }
}
