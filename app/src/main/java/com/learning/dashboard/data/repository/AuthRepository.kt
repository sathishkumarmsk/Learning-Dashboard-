package com.learning.dashboard.data.repository

import com.learning.dashboard.data.remote.AuthApi
import com.learning.dashboard.data.remote.AuthException
import com.learning.dashboard.data.session.SessionStore
import com.learning.dashboard.domain.Outcome

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
) {
    val isLoggedIn: Boolean get() = sessionStore.isLoggedIn

    suspend fun login(email: String, password: String): Outcome<Unit> {
        return try {
            val token = authApi.login(email.trim(), password)
            sessionStore.token = token
            Outcome.Success(Unit)
        } catch (error: AuthException) {
            Outcome.Error(error.message ?: "Login failed")
        } catch (_: Exception) {
            Outcome.Error("Unable to reach the server. Try again.")
        }
    }

    fun logout() {
        sessionStore.clear()
    }
}
