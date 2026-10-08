package com.learning.dashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = lightColorScheme(
    primary = Color(0xFF1F4E79),
    onPrimary = Color.White,
    secondary = Color(0xFF2E7D4F),
    background = Color(0xFFF6F7F9),
    surface = Color.White,
    error = Color(0xFFB3261E),
)

@Composable
fun LearningDashboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content,
    )
}
