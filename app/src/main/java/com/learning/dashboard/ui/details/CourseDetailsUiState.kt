package com.learning.dashboard.ui.details

import com.learning.dashboard.domain.model.Course

sealed interface CourseDetailsUiState {
    data object Loading : CourseDetailsUiState
    data class Ready(val course: Course) : CourseDetailsUiState
    data object Missing : CourseDetailsUiState
}
