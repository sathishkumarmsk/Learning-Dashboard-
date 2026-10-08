package com.learning.dashboard.domain

import com.learning.dashboard.domain.model.Course
import com.learning.dashboard.domain.model.Lesson
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {

    @Test
    fun percent_returnsZero_whenTotalIsZero() {
        assertEquals(0, ProgressCalculator.percent(completed = 0, total = 0))
        assertEquals(0, ProgressCalculator.percent(completed = 5, total = 0))
    }

    @Test
    fun percent_returnsZero_whenNoneCompleted() {
        assertEquals(0, ProgressCalculator.percent(completed = 0, total = 10))
    }

    @Test
    fun percent_calculatesCorrectFraction() {
        // 2 of 5 is 40%
        assertEquals(40, ProgressCalculator.percent(completed = 2, total = 5))
        // 3 of 5 is 60%
        assertEquals(60, ProgressCalculator.percent(completed = 3, total = 5))
        // 1 of 4 is 25%
        assertEquals(25, ProgressCalculator.percent(completed = 1, total = 4))
        // 13 of 20 is 65%
        assertEquals(65, ProgressCalculator.percent(completed = 13, total = 20))
    }

    @Test
    fun percent_returnsHundred_whenAllCompleted() {
        assertEquals(100, ProgressCalculator.percent(completed = 10, total = 10))
        assertEquals(100, ProgressCalculator.percent(completed = 4, total = 4))
    }

    @Test
    fun courseModel_computesProgressCorrectlyFromLessons() {
        val lessons = listOf(
            Lesson(id = 1, courseId = 1, title = "Intro", isCompleted = true),
            Lesson(id = 2, courseId = 1, title = "Variables", isCompleted = true),
            Lesson(id = 3, courseId = 1, title = "Functions", isCompleted = false),
            Lesson(id = 4, courseId = 1, title = "Classes", isCompleted = false),
        )
        val course = Course(
            id = 1,
            title = "Kotlin Basics",
            instructor = "Jane Doe",
            lessons = lessons,
        )

        assertEquals(4, course.lessonCount)
        assertEquals(2, course.completedCount)
        assertEquals(50, course.progressPercent)
    }
}
