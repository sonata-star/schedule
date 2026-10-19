package com.example.data.mapper

import com.example.data.entity.LessonEntity
import com.example.domain.model.Lesson

fun LessonEntity.toDomain() = Lesson(
    id = id,
    title = title,
    startTime = startTime,
    endTime = endTime,
    dayOfWeek = dayOfWeek,
    cabinet = cabinet,
    teacher = teacher,
    subject = subject,
    accountType = accountType
)

fun Lesson.toEntity() = LessonEntity(
    id = id,
    title = title,
    startTime = startTime,
    endTime = endTime,
    status = "FUTURE",
    dayOfWeek = dayOfWeek,
    cabinet = cabinet,
    teacher = teacher,
    subject = subject,
    accountType = accountType
)
