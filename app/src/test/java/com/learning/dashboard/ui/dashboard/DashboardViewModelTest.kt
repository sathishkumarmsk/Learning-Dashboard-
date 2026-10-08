package com.learning.dashboard.ui.dashboard

import com.learning.dashboard.data.local.CourseDao
import com.learning.dashboard.data.local.CourseEntity
import com.learning.dashboard.data.local.CourseWithLessons
import com.learning.dashboard.data.local.LessonEntity
import com.learning.dashboard.data.network.NetworkMonitor
import com.learning.dashboard.data.remote.AuthApi
import com.learning.dashboard.data.remote.CourseApi
import com.learning.dashboard.data.repository.AuthRepository
import com.learning.dashboard.data.repository.CourseRepository
import com.learning.dashboard.data.session.SessionStore
import com.learning.dashboard.domain.model.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
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
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeDashboardDao
    private lateinit var courseRepository: CourseRepository
    private lateinit var authRepository: AuthRepository
    private var isOnline = true

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeDashboardDao()
        courseRepository = CourseRepository(
            api = FakeCourseApi(),
            dao = fakeDao,
            networkMonitor = NetworkMonitor { isOnline },
        )
        authRepository = AuthRepository(
            authApi = object : AuthApi {
                override suspend fun login(email: String, password: String) = "token"
            },
            sessionStore = object : SessionStore {
                override var token: String? = "mock"
                override val isLoggedIn: Boolean get() = true
                override fun clear() { token = null }
            },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_emitsSuccess_whenCoursesArePresent() = runTest {
        fakeDao.populate(
            listOf(
                CourseWithLessons(
                    course = CourseEntity(1, "Python", "John"),
                    lessons = listOf(LessonEntity(101, 1, "Intro", true)),
                ),
            ),
        )

        val viewModel = DashboardViewModel(courseRepository, authRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success
        assertEquals(1, success.courses.size)
        assertEquals("Python", success.courses[0].title)
        assertEquals(false, success.isOffline)
    }

    @Test
    fun uiState_reflectsOffline_whenNetworkMonitorIsOffline() = runTest {
        isOnline = false
        fakeDao.populate(
            listOf(
                CourseWithLessons(
                    course = CourseEntity(2, "AI Basics", "Sarah"),
                    lessons = emptyList(),
                ),
            ),
        )

        val viewModel = DashboardViewModel(courseRepository, authRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success
        assertTrue(success.isOffline)
    }

    private class FakeCourseApi : CourseApi {
        override suspend fun fetchCourses(): List<Course> = emptyList()
    }

    private class FakeDashboardDao : CourseDao {
        private val flow = MutableStateFlow<List<CourseWithLessons>>(emptyList())

        fun populate(list: List<CourseWithLessons>) {
            flow.value = list
        }

        override fun observeCourses(): Flow<List<CourseWithLessons>> = flow
        override fun observeCourse(courseId: Int): Flow<CourseWithLessons?> =
            flow.map { it.find { c -> c.course.id == courseId } }
        override suspend fun courseCount(): Int = flow.value.size
        override suspend fun upsertCourses(courses: List<CourseEntity>) {}
        override suspend fun upsertLessons(lessons: List<LessonEntity>) {}
        override suspend fun clearCourses() { flow.value = emptyList() }
        override suspend fun markLessonCompleted(lessonId: Int) {}
        override suspend fun replaceAll(courses: List<CourseEntity>, lessons: List<LessonEntity>) {
            flow.value = courses.map { c ->
                CourseWithLessons(c, lessons.filter { it.courseId == c.id })
            }
        }
    }
}
