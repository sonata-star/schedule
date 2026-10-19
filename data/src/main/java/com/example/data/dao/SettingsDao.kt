package com.example.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

class SettingsDao(private val db: SQLiteDatabase) {
    fun get(key: String, default: String): String =
        db.rawQuery("SELECT value FROM app_settings WHERE key=?", arrayOf(key)).use {
            if (it.moveToFirst()) it.getString(0) else default
        }
    fun set(key: String, value: String) {
        val cv = ContentValues().apply { put("key", key); put("value", value) }
        db.insertWithOnConflict("app_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
