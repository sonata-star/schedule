package com.example.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.data.entity.LessonEntity

class LessonDao(private val db: SQLiteDatabase) {
    fun getAll(accountType: String? = null): List<LessonEntity> {
        val list = mutableListOf<LessonEntity>()
        val selection = if (accountType == null) null else "accountType = ?"
        val args = if (accountType == null) null else arrayOf(accountType)
        db.query("lessons", arrayOf("id", "title", "startTime", "endTime", "status", "dayOfWeek", "cabinet", "teacher", "subject", "accountType"), selection, args, null, null, "startTime").use { c ->
            while (c.moveToNext()) list += LessonEntity(
                id = c.getLong(0), title = c.getString(1), startTime = c.getInt(2), endTime = c.getInt(3),
                status = c.getString(4), dayOfWeek = c.getString(5), cabinet = c.getString(6), teacher = c.getString(7),
                subject = c.getString(8), accountType = c.getString(9)
            )
        }
        return list
    }

    fun insert(lesson: LessonEntity): Long {
        val v = ContentValues().apply {
            put("title", lesson.title); put("startTime", lesson.startTime); put("endTime", lesson.endTime)
            put("status", lesson.status); put("dayOfWeek", lesson.dayOfWeek)
            put("cabinet", lesson.cabinet); put("teacher", lesson.teacher)
            put("subject", lesson.subject); put("accountType", lesson.accountType)
        }
        return db.insertOrThrow("lessons", null, v)
    }

    fun deleteByTitle(title: String, accountType: String) {
        db.delete("lessons", "LOWER(TRIM(title)) = ? AND accountType = ?", arrayOf(title.trim().lowercase(), accountType))
    }

    fun deleteAll(accountType: String? = null) {
        if (accountType == null) db.delete("lessons", null, null)
        else db.delete("lessons", "accountType = ?", arrayOf(accountType))
    }

    fun replaceTitle(titleToReplace: String?, title: String, slots: List<LessonEntity>, accountType: String) {
        db.beginTransaction()
        try {
            if (!titleToReplace.isNullOrBlank()) deleteByTitle(titleToReplace, accountType)
            slots.forEach { insert(it.copy(title = title, accountType = accountType)) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun updateTeacher(
        title: String,
        teacher: String,
        accountType: String
    ) {
        val values = ContentValues().apply {
            put("teacher", teacher)
        }

        db.update(
            "lessons",
            values,
            "LOWER(TRIM(title)) = ? AND accountType = ?",
            arrayOf(title.trim().lowercase(), accountType)
        )
    }
}
