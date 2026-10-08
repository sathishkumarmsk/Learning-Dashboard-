package com.learning.dashboard.data.session

import android.content.Context

interface SessionStore {
    var token: String?
    val isLoggedIn: Boolean
    fun clear()

    companion object {
        operator fun invoke(context: Context): SessionStore = SharedPrefsSessionStore(context)
    }
}

class SharedPrefsSessionStore(context: Context) : SessionStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
        }

    override val isLoggedIn: Boolean get() = !token.isNullOrBlank()

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "learning_session"
        const val KEY_TOKEN = "auth_token"
    }
}
