package com.example.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ScheduleDbHelper(context: Context) :
    SQLiteOpenHelper(context, "schedule_v3.db", null, 4) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS lessons (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER NOT NULL,
                status TEXT NOT NULL,
                dayOfWeek TEXT NOT NULL,
                cabinet TEXT NOT NULL DEFAULT '',
                teacher TEXT NOT NULL DEFAULT '',
                subject TEXT NOT NULL DEFAULT '',
                accountType TEXT NOT NULL DEFAULT 'STUDENT'
            )""".trimIndent()
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
        db.execSQL("INSERT OR IGNORE INTO app_settings(key,value) VALUES('lessonDuration','45')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            runCatching { db.execSQL("ALTER TABLE lessons ADD COLUMN cabinet TEXT NOT NULL DEFAULT ''") }
            runCatching { db.execSQL("ALTER TABLE lessons ADD COLUMN teacher TEXT NOT NULL DEFAULT ''") }
        }
        if (oldVersion < 3) {
            runCatching { db.execSQL("ALTER TABLE lessons ADD COLUMN subject TEXT NOT NULL DEFAULT ''") }
        }
        if (oldVersion < 4) {
            runCatching { db.execSQL("ALTER TABLE lessons ADD COLUMN accountType TEXT NOT NULL DEFAULT 'STUDENT'") }
        }
    }
}
