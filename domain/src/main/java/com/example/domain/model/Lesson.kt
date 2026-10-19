package com.example.domain.model

/** Framework-independent lesson data used by the application/domain layers. */
data class Lesson(
    val id: Long = 0,
    /** Student mode: subject. Teacher mode: student name or class. */
    val title: String,
    val startTime: Int,
    val endTime: Int,
    val dayOfWeek: String,
    val cabinet: String = "",
    val teacher: String = "",
    /** Optional subject shown on teacher/tutor cards. */
    val subject: String = "",
    /** STUDENT or TEACHER. Existing database rows migrate to STUDENT. */
    val accountType: String = "STUDENT"
)
