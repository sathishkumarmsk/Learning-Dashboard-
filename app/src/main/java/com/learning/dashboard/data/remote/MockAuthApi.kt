package com.learning.dashboard.data.remote

import kotlinx.coroutines.delay

class MockAuthApi : AuthApi {
    override suspend fun login(email: String, password: String): String {
        delay(NETWORK_DELAY_MS)
        val valid = email.equals(DEMO_EMAIL, ignoreCase = true) && password == DEMO_PASSWORD
        if (!valid) {
            throw AuthException("Invalid email or password")
        }
        return "mock-token-${email.hashCode()}"
    }

    companion object {
        const val DEMO_EMAIL = "student@learn.com"
        const val DEMO_PASSWORD = "password123"
        private const val NETWORK_DELAY_MS = 900L
    }
}

class AuthException(message: String) : Exception(message)
