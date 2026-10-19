package com.example.data.entity

data class LessonEntity(
    val id: Long = 0,
    val title: String,
    val startTime: Int,
    val endTime: Int,
    val status: String,
    val dayOfWeek: String,
    val cabinet: String = "",
    val teacher: String = "",
    val subject: String = "",
    val accountType: String = "STUDENT"
)
