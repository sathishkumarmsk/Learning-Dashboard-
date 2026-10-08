package com.learning.dashboard.domain.model

import com.learning.dashboard.domain.ProgressCalculator

data class Course(
    val id: Int,
    val title: String,
    val instructor: String,
    val lessons: List<Lesson>,
) {
    val lessonCount: Int get() = lessons.size
    val completedCount: Int get() = lessons.count { it.isCompleted }
    val progressPercent: Int get() = ProgressCalculator.percent(completedCount, lessonCount)
}
