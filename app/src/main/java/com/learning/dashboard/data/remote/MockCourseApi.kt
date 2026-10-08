package com.learning.dashboard.data.remote

import android.content.Context
import com.learning.dashboard.data.remote.dto.CourseDto
import com.learning.dashboard.domain.model.Course
import com.learning.dashboard.domain.model.Lesson
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

class MockCourseApi(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CourseApi {
    override suspend fun fetchCourses(): List<Course> {
        delay(NETWORK_DELAY_MS)
        val payload = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        return json.decodeFromString<List<CourseDto>>(payload).map { dto ->
            Course(
                id = dto.id,
                title = dto.title,
                instructor = dto.instructor,
                lessons = dto.lessons.map { lesson ->
                    Lesson(
                        id = lesson.id,
                        courseId = dto.id,
                        title = lesson.title,
                        isCompleted = lesson.completed,
                    )
                },
            )
        }
    }

    companion object {
        private const val ASSET_NAME = "courses.json"
        private const val NETWORK_DELAY_MS = 700L
    }
}
