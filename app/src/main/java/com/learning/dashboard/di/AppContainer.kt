package com.learning.dashboard.di

import android.content.Context
import androidx.room.Room
import com.learning.dashboard.data.local.AppDatabase
import com.learning.dashboard.data.network.AndroidNetworkMonitor
import com.learning.dashboard.data.remote.MockAuthApi
import com.learning.dashboard.data.remote.MockCourseApi
import com.learning.dashboard.data.repository.AuthRepository
import com.learning.dashboard.data.repository.CourseRepository
import com.learning.dashboard.data.session.SessionStore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "learning_dashboard.db",
    ).build()

    val authRepository = AuthRepository(
        authApi = MockAuthApi(),
        sessionStore = SessionStore(appContext),
    )

    val courseRepository = CourseRepository(
        api = MockCourseApi(appContext),
        dao = database.courseDao(),
        networkMonitor = AndroidNetworkMonitor(appContext),
    )
}
