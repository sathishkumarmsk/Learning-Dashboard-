package com.learning.dashboard.domain

object ProgressCalculator {
    fun percent(completed: Int, total: Int): Int {
        if (total <= 0) return 0
        return ((completed.toDouble() / total) * 100).toInt()
    }
}
