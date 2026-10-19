package com.example.domain.repository

import com.example.domain.model.Lesson

interface ScheduleRepository {
    fun allLessons(accountType: String? = null): List<Lesson>
    fun saveTitle(oldTitle: String?, title: String, slots: List<Lesson>, accountType: String)
    fun deleteTitle(title: String, accountType: String)
    fun deleteAllLessons(accountType: String? = null)
    fun getSetting(key: String, default: String): String
    fun setSetting(key: String, value: String)
    fun close()
}
