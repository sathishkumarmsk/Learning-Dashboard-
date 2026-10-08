package com.learning.dashboard

import android.app.Application
import com.learning.dashboard.di.AppContainer

class LearningDashboardApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
