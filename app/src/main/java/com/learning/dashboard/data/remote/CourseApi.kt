package com.learning.dashboard.data.remote

import com.learning.dashboard.domain.model.Course

interface CourseApi {
    suspend fun fetchCourses(): List<Course>
}
