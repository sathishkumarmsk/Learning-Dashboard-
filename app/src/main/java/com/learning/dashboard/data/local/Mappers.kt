package com.learning.dashboard.data.local

import com.learning.dashboard.domain.model.Course
import com.learning.dashboard.domain.model.Lesson

fun CourseWithLessons.toDomain(): Course = Course(
    id = course.id,
    title = course.title,
    instructor = course.instructor,
    lessons = lessons.sortedBy { it.id }.map { it.toDomain() },
)

fun LessonEntity.toDomain(): Lesson = Lesson(
    id = id,
    courseId = courseId,
    title = title,
    isCompleted = isCompleted,
)

fun Course.toEntity(): CourseEntity = CourseEntity(
    id = id,
    title = title,
    instructor = instructor,
)

fun Lesson.toEntity(): LessonEntity = LessonEntity(
    id = id,
    courseId = courseId,
    title = title,
    isCompleted = isCompleted,
)
