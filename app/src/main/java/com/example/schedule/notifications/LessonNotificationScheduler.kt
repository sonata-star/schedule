package com.example.schedule.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.domain.model.Lesson
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object LessonNotificationScheduler {
    const val ACTION_NOTIFY = "com.example.schedule.ACTION_LESSON_NOTIFICATION"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val EXTRA_TIME = "time"
    const val EXTRA_REQUEST_ID = "request_id"
    const val EXTRA_NEXT_TRIGGER = "next_trigger"
    const val EXTRA_TIME_LABEL = "time_label"
    private const val NOTIFICATION_ID = 48271

    fun reschedule(context: Context, lessons: List<Lesson>, accountType: String) {
        val app = context.applicationContext
        val manager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAll(app, manager)
        val ids = mutableSetOf<String>()
        val relevant = lessons.filter { it.accountType == accountType }
        val now = LocalDateTime.now()
        relevant.groupBy { it.dayOfWeek }.forEach { (dayName, dayLessons) ->
            val dayOfWeek = dayIndex(dayName) ?: return@forEach
            val ordered = dayLessons.sortedBy { it.startTime }
            ordered.forEachIndexed { index, lesson ->
                val next = ordered.getOrNull(index + 1)
                // Первый урок можно анонсировать за 15 минут; остальные — ровно
                // в момент окончания предыдущего урока, показывая именно следующий.
                val isFirstReminder = index == 0
                val eventMinute = if (isFirstReminder) {
                    (lesson.startTime - 15).coerceAtLeast(0)
                } else {
                    lesson.endTime
                }
                val displayLesson = if (isFirstReminder) lesson else next
                if (displayLesson == null) return@forEachIndexed

                val targetDay = LocalDate.now().with(TemporalAdjusters.nextOrSame(dayOfWeek))
                var trigger = targetDay.atStartOfDay().plusMinutes(eventMinute.toLong())
                if (!trigger.isAfter(now)) trigger = trigger.plusWeeks(1)

                val title = displayLesson.title
                val timeLabel = "${format(displayLesson.startTime)}–${format(displayLesson.endTime)}"
                val gap = if (isFirstReminder) 0 else displayLesson.startTime - lesson.endTime
                val detail = buildString {
                    if (accountType == "STUDENT") {
                        if (displayLesson.cabinet.isNotBlank()) append("Кабинет ${displayLesson.cabinet} · ")
                    } else if (displayLesson.subject.isNotBlank()) {
                        append("${displayLesson.subject} · ")
                    }
                    append(timeLabel)
                    if (isFirstReminder) {
                        append("\nПервый урок сегодня")
                    } else if (gap > 0) {
                        append("\nПеремена $gap минут")
                    }
                }
                val requestId = ("${accountType}_${dayName}_${lesson.title}_${lesson.startTime}_$eventMinute").hashCode()
                ids += requestId.toString()
                val intent = Intent(app, LessonNotificationReceiver::class.java).apply {
                    action = ACTION_NOTIFY
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_TEXT, detail)
                    putExtra(EXTRA_TIME, trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    putExtra(EXTRA_REQUEST_ID, requestId)
                    putExtra(EXTRA_TIME_LABEL, timeLabel)
                    putExtra(EXTRA_NEXT_TRIGGER, NOTIFICATION_ID)
                }
                val pending = PendingIntent.getBroadcast(app, requestId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                setAlarm(manager, trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pending)
            }
        }
        app.getSharedPreferences("lesson_alarm_ids", Context.MODE_PRIVATE).edit().putStringSet("ids", ids).apply()
    }

    fun scheduleNextWeek(context: Context, intent: Intent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextTime = intent.getLongExtra(EXTRA_TIME, 0L) + 7L * 24 * 60 * 60 * 1000
        if (nextTime <= System.currentTimeMillis()) return
        intent.putExtra(EXTRA_TIME, nextTime)
        val id = intent.getIntExtra(EXTRA_REQUEST_ID, intent.hashCode())
        val pending = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        setAlarm(alarmManager, nextTime, pending)
    }

    private fun cancelAll(context: Context, manager: AlarmManager) {
        val prefs = context.getSharedPreferences("lesson_alarm_ids", Context.MODE_PRIVATE)
        prefs.getStringSet("ids", emptySet()).orEmpty().forEach { raw ->
            val id = raw.toIntOrNull() ?: return@forEach
            val intent = Intent(context, LessonNotificationReceiver::class.java).apply { action = ACTION_NOTIFY }
            PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let(manager::cancel)
        }
        prefs.edit().putStringSet("ids", emptySet()).apply()
    }

    private fun setAlarm(manager: AlarmManager, time: Long, pending: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
            else manager.setExact(AlarmManager.RTC_WAKEUP, time, pending)
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
        }
    }
    private fun dayIndex(name: String): DayOfWeek? = when (name) {
        "Понедельник" -> DayOfWeek.MONDAY; "Вторник" -> DayOfWeek.TUESDAY; "Среда" -> DayOfWeek.WEDNESDAY
        "Четверг" -> DayOfWeek.THURSDAY; "Пятница" -> DayOfWeek.FRIDAY; "Суббота" -> DayOfWeek.SATURDAY; "Воскресенье" -> DayOfWeek.SUNDAY
        else -> null
    }
    private fun format(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
}
