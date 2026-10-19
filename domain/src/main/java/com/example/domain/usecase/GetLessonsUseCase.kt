package com.example.domain.usecase

import com.example.domain.model.Lesson
import com.example.domain.repository.ScheduleRepository

class GetLessonsUseCase(private val repository: ScheduleRepository) {
    operator fun invoke(): List<Lesson> = repository.allLessons()
}
