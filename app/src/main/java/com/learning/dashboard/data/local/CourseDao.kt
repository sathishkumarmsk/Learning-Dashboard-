package com.learning.dashboard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Transaction
    @Query("SELECT * FROM courses ORDER BY id ASC")
    fun observeCourses(): Flow<List<CourseWithLessons>>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :courseId")
    fun observeCourse(courseId: Int): Flow<CourseWithLessons?>

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun courseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourses(courses: List<CourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessons(lessons: List<LessonEntity>)

    @Query("DELETE FROM courses")
    suspend fun clearCourses()

    @Query("UPDATE lessons SET isCompleted = 1 WHERE id = :lessonId")
    suspend fun markLessonCompleted(lessonId: Int)

    @Transaction
    suspend fun replaceAll(courses: List<CourseEntity>, lessons: List<LessonEntity>) {
        clearCourses()
        upsertCourses(courses)
        upsertLessons(lessons)
    }
}
