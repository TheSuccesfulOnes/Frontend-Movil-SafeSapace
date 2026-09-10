package com.experimentos.mobile.shared.data

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Session(
    val userId: Long?,
    val token: String,
    val username: String,
    val displayName: String,
    val role: String,
) {
    /** Uses an immutable server identifier so renamed accounts keep their preferences. */
    val accountKey: String
        get() = userId?.let { "user:$it" } ?: "legacy:$username"
}

/** Stores the minimum session data using Android Keystore-backed encryption. */
class SessionStore(private val context: Context) {
    private val preferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val mutableSession = MutableStateFlow(readSession())
    val session: StateFlow<Session?> = mutableSession.asStateFlow()

    fun currentToken(): String? = mutableSession.value?.token

    suspend fun save(session: Session) {
        preferences.edit {
            if (session.userId == null) {
                remove(USER_ID_KEY)
            } else {
                putLong(USER_ID_KEY, session.userId)
            }
            putString(TOKEN_KEY, session.token)
            putString(USERNAME_KEY, session.username)
            putString(DISPLAY_NAME_KEY, session.displayName)
            putString(ROLE_KEY, session.role)
        }
        mutableSession.value = session
    }

    suspend fun clear() {
        clearImmediately()
    }

    /** Updates the non-sensitive session claims after a profile identifier change. */
    suspend fun updateAccount(
        token: String,
        username: String,
        displayName: String,
        userId: Long? = mutableSession.value?.userId,
    ) {
        val current = mutableSession.value ?: return
        save(current.copy(userId = userId, token = token, username = username, displayName = displayName))
    }

    /** Clears the local session from non-suspending infrastructure such as OkHttp interceptors. */
    fun clearImmediately() {
        preferences.edit { clear() }
        mutableSession.value = null
    }

    private fun readSession(): Session? {
        val token = preferences.getString(TOKEN_KEY, null)
        val userId = if (preferences.contains(USER_ID_KEY)) preferences.getLong(USER_ID_KEY, 0L) else null
        val username = preferences.getString(USERNAME_KEY, null)
        val displayName = preferences.getString(DISPLAY_NAME_KEY, username)
        val role = preferences.getString(ROLE_KEY, null)
        return if (token.isNullOrBlank() || username.isNullOrBlank() || role.isNullOrBlank()) {
            null
        } else {
            Session(userId, token, username, displayName.orEmpty().ifBlank { username }, role)
        }
    }

    private companion object {
        const val USER_ID_KEY = "user_id"
        const val TOKEN_KEY = "access_token"
        const val USERNAME_KEY = "username"
        const val DISPLAY_NAME_KEY = "display_name"
        const val ROLE_KEY = "role"
    }
}
