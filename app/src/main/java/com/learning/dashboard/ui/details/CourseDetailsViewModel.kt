package com.learning.dashboard.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learning.dashboard.data.repository.CourseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseDetailsViewModel(
    courseId: Int,
    private val courseRepository: CourseRepository,
) : ViewModel() {

    val uiState: StateFlow<CourseDetailsUiState> = courseRepository.observeCourse(courseId)
        .map { course ->
            if (course == null) CourseDetailsUiState.Missing else CourseDetailsUiState.Ready(course)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CourseDetailsUiState.Loading,
        )

    fun markLessonCompleted(lessonId: Int) {
        viewModelScope.launch {
            courseRepository.markLessonCompleted(lessonId)
        }
    }

    companion object {
        fun factory(
            courseId: Int,
            courseRepository: CourseRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CourseDetailsViewModel(courseId, courseRepository) as T
            }
        }
    }
}
