package com.example.schedule.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.schedule.R
import androidx.core.app.NotificationCompat
import com.example.schedule.MainActivity

class LessonNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Расписание уроков", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
            })
        }
        val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = intent.getStringExtra(LessonNotificationScheduler.EXTRA_TITLE) ?: "Следующий урок"
        val detail = intent.getStringExtra(LessonNotificationScheduler.EXTRA_TEXT).orEmpty()
        val timeLabel = intent.getStringExtra(LessonNotificationScheduler.EXTRA_TIME_LABEL).orEmpty()
        val compact = RemoteViews(context.packageName, R.layout.notification_lesson).apply {
            setTextViewText(R.id.notification_title, title)
            setTextViewText(R.id.notification_time, timeLabel)
            setTextViewText(R.id.notification_detail, detail)
            // Notification text stays dark regardless of the system theme.
            setTextColor(R.id.notification_title, android.graphics.Color.BLACK)
            setTextColor(R.id.notification_time, android.graphics.Color.BLACK)
            setTextColor(R.id.notification_detail, android.graphics.Color.BLACK)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(detail)
            .setCustomContentView(compact)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setSilent(true)
            .setVibrate(longArrayOf(0L))
            .build()
        manager.notify(48271, notification)
        // Restore this alarm for the same weekday next week.
        LessonNotificationScheduler.scheduleNextWeek(context, intent)
    }

    companion object { const val CHANNEL_ID = "lesson_schedule_silent" }
}
