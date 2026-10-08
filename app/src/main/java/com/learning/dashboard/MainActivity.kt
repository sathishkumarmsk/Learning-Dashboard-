package com.learning.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.learning.dashboard.ui.dashboard.DashboardScreen
import com.learning.dashboard.ui.dashboard.DashboardViewModel
import com.learning.dashboard.ui.details.CourseDetailsScreen
import com.learning.dashboard.ui.details.CourseDetailsViewModel
import com.learning.dashboard.ui.login.LoginScreen
import com.learning.dashboard.ui.login.LoginViewModel
import com.learning.dashboard.ui.theme.LearningDashboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LearningDashboardApp
        val container = app.container

        setContent {
            LearningDashboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation(container = container)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    container: com.learning.dashboard.di.AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val startDestination = if (container.authRepository.isLoggedIn) "dashboard" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.factory(container.authRepository),
            )
            LoginScreen(
                viewModel = loginViewModel,
                onLoggedIn = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            )
        }

        composable("dashboard") {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.factory(
                    courseRepository = container.courseRepository,
                    authRepository = container.authRepository,
                ),
            )
            DashboardScreen(
                viewModel = dashboardViewModel,
                onCourseSelected = { courseId ->
                    navController.navigate("course/$courseId")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = "course/{courseId}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.IntType },
            ),
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getInt("courseId") ?: 1
            val detailsViewModel: CourseDetailsViewModel = viewModel(
                key = "course_$courseId",
                factory = CourseDetailsViewModel.factory(
                    courseId = courseId,
                    courseRepository = container.courseRepository,
                ),
            )
            CourseDetailsScreen(
                viewModel = detailsViewModel,
                onBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
