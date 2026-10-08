package com.learning.dashboard.data.repository

import com.learning.dashboard.data.local.CourseDao
import com.learning.dashboard.data.local.toDomain
import com.learning.dashboard.data.local.toEntity
import com.learning.dashboard.data.network.NetworkMonitor
import com.learning.dashboard.data.remote.CourseApi
import com.learning.dashboard.domain.Outcome
import com.learning.dashboard.domain.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CourseRepository(
    private val api: CourseApi,
    private val dao: CourseDao,
    private val networkMonitor: NetworkMonitor,
) {
    fun observeCourses(): Flow<List<Course>> =
        dao.observeCourses().map { rows -> rows.map { it.toDomain() } }

    fun observeCourse(courseId: Int): Flow<Course?> =
        dao.observeCourse(courseId).map { it?.toDomain() }

    /**
     * Room is the source of truth after the first successful download.
     * A later refresh does not overwrite locally completed lessons.
     */
    suspend fun refreshCourses(forceRemote: Boolean = false): Outcome<Unit> {
        val hasCache = dao.courseCount() > 0
        if (hasCache && !forceRemote) {
            return Outcome.Success(Unit)
        }
        if (!networkMonitor.isOnline()) {
            return if (hasCache) {
                Outcome.Success(Unit)
            } else {
                Outcome.Error("No internet connection. Connect once to download courses.")
            }
        }
        return try {
            val remote = api.fetchCourses()
            if (!hasCache) {
                dao.replaceAll(
                    courses = remote.map { it.toEntity() },
                    lessons = remote.flatMap { course -> course.lessons.map { it.toEntity() } },
                )
            }
            Outcome.Success(Unit)
        } catch (error: Exception) {
            if (hasCache) {
                Outcome.Success(Unit)
            } else {
                Outcome.Error("Failed to load courses. Please retry.", error)
            }
        }
    }

    suspend fun markLessonCompleted(lessonId: Int) {
        dao.markLessonCompleted(lessonId)
    }

    fun isOnline(): Boolean = networkMonitor.isOnline()
}
