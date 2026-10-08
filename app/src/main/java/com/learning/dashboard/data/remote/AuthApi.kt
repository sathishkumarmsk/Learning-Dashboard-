package com.learning.dashboard.data.remote

interface AuthApi {
    suspend fun login(email: String, password: String): String
}
