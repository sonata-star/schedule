package com.example.schedule.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.di.DataContainer

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != "android.intent.action.TIME_SET" && intent.action != "android.intent.action.TIMEZONE_CHANGED") return
        val pending = goAsync()
        Thread {
            try {
                val repository = DataContainer(context.applicationContext).scheduleRepository
                try {
                    if (repository.getSetting("remindersEnabled", "true") == "true") {
                        val mode = context.getSharedPreferences("schedule_runtime", Context.MODE_PRIVATE).getString("account_type", "STUDENT") ?: "STUDENT"
                        LessonNotificationScheduler.reschedule(context, repository.allLessons(), mode)
                    }
                } finally { repository.close() }
            } finally { pending.finish() }
        }.start()
    }
}
