package com.learning.dashboard.ui.details

import com.learning.dashboard.data.local.CourseDao
import com.learning.dashboard.data.local.CourseEntity
import com.learning.dashboard.data.local.CourseWithLessons
import com.learning.dashboard.data.local.LessonEntity
import com.learning.dashboard.data.network.NetworkMonitor
import com.learning.dashboard.data.remote.CourseApi
import com.learning.dashboard.data.repository.CourseRepository
import com.learning.dashboard.domain.model.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeCourseDao
    private lateinit var repository: CourseRepository
    private lateinit var viewModel: CourseDetailsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeCourseDao()
        repository = CourseRepository(
            api = FakeCourseApi(),
            dao = fakeDao,
            networkMonitor = NetworkMonitor { true },
        )

        // Prepopulate course 1 with 2 lessons (1 pending, 1 completed)
        fakeDao.populateCourse(
            course = CourseEntity(id = 1, title = "Python 101", instructor = "Guido"),
            lessons = listOf(
                LessonEntity(id = 10, courseId = 1, title = "Intro", isCompleted = true),
                LessonEntity(id = 11, courseId = 1, title = "Variables", isCompleted = false),
            ),
        )

        viewModel = CourseDetailsViewModel(courseId = 1, courseRepository = repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_startsLoading_thenEmitsReadyWithAccurateProgress() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CourseDetailsUiState.Ready)
        val course = (state as CourseDetailsUiState.Ready).course

        assertEquals("Python 101", course.title)
        assertEquals(2, course.lessonCount)
        assertEquals(1, course.completedCount)
        assertEquals(50, course.progressPercent)
    }

    @Test
    fun markLessonCompleted_updatesLessonStatusAndRecalculatesProgress() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Mark second lesson as completed
        viewModel.markLessonCompleted(11)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CourseDetailsUiState.Ready)
        val course = (state as CourseDetailsUiState.Ready).course

        assertEquals(2, course.completedCount)
        assertEquals(100, course.progressPercent)
        assertTrue(course.lessons.all { it.isCompleted })
    }

    @Test
    fun uiState_emitsMissing_whenCourseDoesNotExist() = runTest {
        val missingVm = CourseDetailsViewModel(courseId = 999, courseRepository = repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            missingVm.uiState.collect()
        }
        advanceUntilIdle()

        val state = missingVm.uiState.value
        assertEquals(CourseDetailsUiState.Missing, state)
    }

    private class FakeCourseApi : CourseApi {
        override suspend fun fetchCourses(): List<Course> = emptyList()
    }

    private class FakeCourseDao : CourseDao {
        private val coursesFlow = MutableStateFlow<Map<Int, CourseWithLessons>>(emptyMap())

        fun populateCourse(course: CourseEntity, lessons: List<LessonEntity>) {
            coursesFlow.update { current ->
                current + (course.id to CourseWithLessons(course, lessons))
            }
        }

        override fun observeCourses(): Flow<List<CourseWithLessons>> =
            coursesFlow.map { it.values.toList() }

        override fun observeCourse(courseId: Int): Flow<CourseWithLessons?> =
            coursesFlow.map { it[courseId] }

        override suspend fun courseCount(): Int = coursesFlow.value.size

        override suspend fun upsertCourses(courses: List<CourseEntity>) {}

        override suspend fun upsertLessons(lessons: List<LessonEntity>) {}

        override suspend fun clearCourses() {
            coursesFlow.value = emptyMap()
        }

        override suspend fun markLessonCompleted(lessonId: Int) {
            coursesFlow.update { current ->
                current.mapValues { (_, courseWithLessons) ->
                    val updatedLessons = courseWithLessons.lessons.map {
                        if (it.id == lessonId) it.copy(isCompleted = true) else it
                    }
                    courseWithLessons.copy(lessons = updatedLessons)
                }
            }
        }

        override suspend fun replaceAll(courses: List<CourseEntity>, lessons: List<LessonEntity>) {}
    }
}
