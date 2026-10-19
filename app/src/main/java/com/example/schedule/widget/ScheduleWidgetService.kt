package com.example.schedule.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.data.di.DataContainer
import com.example.domain.model.Lesson
import com.example.schedule.R
import java.time.LocalDate
import java.util.Calendar

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ScheduleWidgetFactory(applicationContext)
    }
}

private class ScheduleWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private var rows: List<Lesson> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        var repository: com.example.domain.repository.ScheduleRepository? = null
        rows = try {
            repository = DataContainer(context).scheduleRepository
            val accountType = context.getSharedPreferences("schedule_runtime", Context.MODE_PRIVATE)
                .getString("account_type", "STUDENT") ?: "STUDENT"
            val today = listOf(
                "Понедельник", "Вторник", "Среда", "Четверг",
                "Пятница", "Суббота", "Воскресенье"
            )[LocalDate.now().dayOfWeek.value - 1]

            repository.allLessons(accountType)
                .filter { it.dayOfWeek.equals(today, ignoreCase = true) }
                .sortedBy { it.startTime }
        } catch (_: Exception) {
            // A data-loading failure should leave an empty widget instead of crashing its service.
            emptyList()
        } finally {
            try {
                repository?.close()
            } catch (_: Exception) {
                // Ignore close failures in the widget's background service.
            }
        }
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews? {
        val lesson = rows.getOrNull(position) ?: return null
        val row = RemoteViews(context.packageName, R.layout.widget_schedule_row)
        row.setTextViewText(R.id.widget_subject, lesson.title)
        row.setTextViewText(
            R.id.widget_time,
            "%02d:%02d–%02d:%02d".format(
                lesson.startTime / 60,
                lesson.startTime % 60,
                lesson.endTime / 60,
                lesson.endTime % 60
            )
        )

        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        val textColor = if (lesson.endTime <= now) 0xFFAAAAAA.toInt() else 0xFFFFFFFF.toInt()
        row.setTextColor(R.id.widget_subject, textColor)
        row.setTextColor(R.id.widget_time, textColor)
        return row
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true
    override fun onDestroy() { rows = emptyList() }
}
