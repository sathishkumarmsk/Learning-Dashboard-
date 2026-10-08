package com.learning.dashboard.ui.dashboard

import com.learning.dashboard.domain.model.Course

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object Empty : DashboardUiState
    data class Success(
        val courses: List<Course>,
        val isOffline: Boolean,
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
