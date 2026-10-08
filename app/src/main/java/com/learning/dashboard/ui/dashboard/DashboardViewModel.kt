package com.learning.dashboard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.dashboard.data.repository.AuthRepository
import com.learning.dashboard.data.repository.CourseRepository
import com.learning.dashboard.domain.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val courseRepository: CourseRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val loadError = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(true)

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                courseRepository.observeCourses(),
                loadError,
                isRefreshing,
            ) { courses, error, refreshing ->
                when {
                    error != null && courses.isEmpty() -> DashboardUiState.Error(error)
                    refreshing && courses.isEmpty() -> DashboardUiState.Loading
                    courses.isEmpty() -> DashboardUiState.Empty
                    else -> DashboardUiState.Success(
                        courses = courses,
                        isOffline = !courseRepository.isOnline(),
                    )
                }
            }.onStart { refresh() }
                .collect { _uiState.value = it }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            loadError.value = null
            when (val result = courseRepository.refreshCourses()) {
                is Outcome.Success -> loadError.value = null
                is Outcome.Error -> loadError.value = result.message
            }
            isRefreshing.value = false
        }
    }

    fun logout() {
        authRepository.logout()
    }

    companion object {
        fun factory(
            courseRepository: CourseRepository,
            authRepository: AuthRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(courseRepository, authRepository) as T
            }
        }
    }
}
