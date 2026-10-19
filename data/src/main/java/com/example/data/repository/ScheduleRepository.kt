package com.example.data.repository

import android.content.Context
import com.example.data.dao.LessonDao
import com.example.data.dao.SettingsDao
import com.example.data.db.ScheduleDbHelper
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.Lesson
import com.example.domain.repository.ScheduleRepository as ScheduleRepositoryContract

class ScheduleRepository(context: Context) : ScheduleRepositoryContract {
    private val helper = ScheduleDbHelper(context.applicationContext)
    private val db get() = helper.writableDatabase
    private val lessons get() = LessonDao(db)
    private val settings get() = SettingsDao(db)

    override fun allLessons(accountType: String?): List<Lesson> = lessons.getAll(accountType).map { it.toDomain() }
    override fun saveTitle(oldTitle: String?, title: String, slots: List<Lesson>, accountType: String) =
        lessons.replaceTitle(oldTitle, title, slots.map { it.copy(title = title, accountType = accountType).toEntity() }, accountType)
    override fun deleteTitle(title: String, accountType: String) = lessons.deleteByTitle(title, accountType)
    override fun deleteAllLessons(accountType: String?) = lessons.deleteAll(accountType)
    override fun getSetting(key: String, default: String) = settings.get(key, default)
    override fun setSetting(key: String, value: String) = settings.set(key, value)
    override fun close() = helper.close()
}
