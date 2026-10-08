package com.learning.dashboard.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CourseDto(
    val id: Int,
    val title: String,
    val instructor: String,
    val lessons: List<LessonDto> = emptyList(),
)

@Serializable
data class LessonDto(
    val id: Int,
    val title: String,
    val completed: Boolean = false,
)
