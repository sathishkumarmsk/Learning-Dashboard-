package com.learning.dashboard.data.repository

import com.learning.dashboard.data.local.CourseDao
import com.learning.dashboard.data.local.CourseEntity
import com.learning.dashboard.data.local.CourseWithLessons
import com.learning.dashboard.data.local.LessonEntity
import com.learning.dashboard.data.network.NetworkMonitor
import com.learning.dashboard.data.remote.CourseApi
import com.learning.dashboard.domain.Outcome
import com.learning.dashboard.domain.model.Course
import com.learning.dashboard.domain.model.Lesson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseRepositoryTest {

    @Test
    fun refreshCourses_offlineWithNoCache_returnsError() = runTest {
        val dao = InMemoryCourseDao()
        val repo = CourseRepository(
            api = StubCourseApi(),
            dao = dao,
            networkMonitor = NetworkMonitor { false },
        )

        val outcome = repo.refreshCourses()

        assertTrue(outcome is Outcome.Error)
        assertEquals("No internet connection. Connect once to download courses.", (outcome as Outcome.Error).message)
    }

    @Test
    fun refreshCourses_offlineWithExistingCache_returnsSuccessWithoutRemoteCall() = runTest {
        val dao = InMemoryCourseDao()
        dao.replaceAll(
            courses = listOf(CourseEntity(1, "Cached Title", "Cached Instructor")),
            lessons = listOf(LessonEntity(10, 1, "Cached Lesson", false)),
        )

        var apiCalled = false
        val repo = CourseRepository(
            api = object : CourseApi {
                override suspend fun fetchCourses(): List<Course> {
                    apiCalled = true
                    return emptyList()
                }
            },
            dao = dao,
            networkMonitor = NetworkMonitor { false },
        )

        val outcome = repo.refreshCourses()

        assertTrue(outcome is Outcome.Success)
        assertEquals(false, apiCalled)
        assertEquals(1, repo.observeCourses().first().size)
    }

    @Test
    fun refreshCourses_onlineWithEmptyCache_fetchesAndStoresCourses() = runTest {
        val dao = InMemoryCourseDao()
        val dummyCourses = listOf(
            Course(
                id = 1,
                title = "Android Architecture",
                instructor = "Google",
                lessons = listOf(Lesson(101, 1, "Intro", false)),
            ),
        )

        val repo = CourseRepository(
            api = object : CourseApi {
                override suspend fun fetchCourses(): List<Course> = dummyCourses
            },
            dao = dao,
            networkMonitor = NetworkMonitor { true },
        )

        val outcome = repo.refreshCourses()

        assertTrue(outcome is Outcome.Success)
        val loaded = repo.observeCourses().first()
        assertEquals(1, loaded.size)
        assertEquals("Android Architecture", loaded[0].title)
    }

    private class StubCourseApi : CourseApi {
        override suspend fun fetchCourses(): List<Course> = emptyList()
    }

    private class InMemoryCourseDao : CourseDao {
        private val courses = mutableListOf<CourseEntity>()
        private val lessons = mutableListOf<LessonEntity>()
        private val flow = MutableStateFlow<List<CourseWithLessons>>(emptyList())

        private fun emit() {
            flow.value = courses.map { c ->
                CourseWithLessons(c, lessons.filter { it.courseId == c.id })
            }
        }

        override fun observeCourses(): Flow<List<CourseWithLessons>> = flow

        override fun observeCourse(courseId: Int): Flow<CourseWithLessons?> =
            flow.map { list -> list.find { it.course.id == courseId } }

        override suspend fun courseCount(): Int = courses.size

        override suspend fun upsertCourses(courses: List<CourseEntity>) {
            this.courses.addAll(courses)
            emit()
        }

        override suspend fun upsertLessons(lessons: List<LessonEntity>) {
            this.lessons.addAll(lessons)
            emit()
        }

        override suspend fun clearCourses() {
            courses.clear()
            lessons.clear()
            emit()
        }

        override suspend fun markLessonCompleted(lessonId: Int) {
            val index = lessons.indexOfFirst { it.id == lessonId }
            if (index != -1) {
                lessons[index] = lessons[index].copy(isCompleted = true)
                emit()
            }
        }

        override suspend fun replaceAll(courses: List<CourseEntity>, lessons: List<LessonEntity>) {
            this.courses.clear()
            this.lessons.clear()
            this.courses.addAll(courses)
            this.lessons.addAll(lessons)
            emit()
        }
    }
}
