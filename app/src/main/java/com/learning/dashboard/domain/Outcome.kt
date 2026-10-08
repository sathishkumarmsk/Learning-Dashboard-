package com.learning.dashboard.domain

sealed class Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Outcome<Nothing>()
}
