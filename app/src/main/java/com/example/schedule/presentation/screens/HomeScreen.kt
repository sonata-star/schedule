package com.example.schedule.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Lesson
import com.example.schedule.presentation.components.AppPalette
import com.example.schedule.presentation.components.DAY_NAMES
import com.example.schedule.presentation.components.DaySelector
import com.example.schedule.presentation.components.LessonCard
import com.example.schedule.presentation.components.dateForScheduleDay
import com.example.schedule.presentation.model.LessonStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

private val russianDateFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

@Composable
fun HomeScreen(
    lessons: List<Lesson>,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    palette: AppPalette,
    onSettings: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit
) {
    var now by remember {
        mutableIntStateOf(currentMinutes())
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = currentMinutes()
            delay(15_000)
        }
    }

    val safeDayIndex = selectedDay.coerceIn(0, 6)
    val day = DAY_NAMES[safeDayIndex]

    // Заголовок всегда показывает сегодняшний день и дату.
    val todayDate = LocalDate.now()
    val todayName = DAY_NAMES[todayDate.dayOfWeek.value - 1]
    val dateText = todayDate.format(russianDateFormatter)

    // Статусы уроков рассчитываются относительно выбранного дня.
    val isSelectedDayToday =
        dateForScheduleDay(safeDayIndex) == todayDate

    val dayLessons = lessons
        .filter { it.dayOfWeek == day }
        .sortedBy { it.startTime }

    val statuses = dayLessons.map { lesson ->
        lesson to when {
            isSelectedDayToday &&
                    now in lesson.startTime until lesson.endTime ->
                LessonStatus.CURRENT

            isSelectedDayToday && now >= lesson.endTime ->
                LessonStatus.PAST

            else ->
                LessonStatus.FUTURE
        }
    }.toMutableList()

    val next = if (isSelectedDayToday) {
        statuses.indexOfFirst {
            it.first.startTime > now &&
                    it.second == LessonStatus.FUTURE
        }
    } else {
        -1
    }

    if (next >= 0) {
        statuses[next] =
            statuses[next].first to LessonStatus.UPCOMING
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$todayName, $dateText",
                color = palette.text,
                fontSize = 20.sp,
                maxLines = 1
            )

            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Настройки",
                    tint = palette.text
                )
            }
        }

        DaySelector(
            selected = safeDayIndex,
            palette = palette,
            onSelected = onDaySelected
        )

        // Направление перелистывания определяется изменением индекса дня.
        AnimatedContent(
            targetState = safeDayIndex,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {
                val direction =
                    if (targetState > initialState) 1 else -1

                slideInHorizontally(
                    animationSpec = tween(250),
                    initialOffsetX = { fullWidth ->
                        direction * fullWidth
                    }
                ) togetherWith slideOutHorizontally(
                    animationSpec = tween(250),
                    targetOffsetX = { fullWidth ->
                        -direction * fullWidth
                    }
                ) using SizeTransform(clip = false)
            },
            label = "day_switch"
        ) { animatedDayIndex ->
            val animatedDay = DAY_NAMES[animatedDayIndex]

            val animatedIsToday =
                dateForScheduleDay(animatedDayIndex) == todayDate

            val animatedLessons = lessons
                .filter { it.dayOfWeek == animatedDay }
                .sortedBy { it.startTime }

            val animatedStatuses = animatedLessons.map { lesson ->
                lesson to when {
                    animatedIsToday &&
                            now in lesson.startTime until lesson.endTime ->
                        LessonStatus.CURRENT

                    animatedIsToday && now >= lesson.endTime ->
                        LessonStatus.PAST

                    else ->
                        LessonStatus.FUTURE
                }
            }.toMutableList()

            val nextLessonIndex = if (animatedIsToday) {
                animatedStatuses.indexOfFirst {
                    it.first.startTime > now &&
                            it.second == LessonStatus.FUTURE
                }
            } else {
                -1
            }

            if (nextLessonIndex >= 0) {
                animatedStatuses[nextLessonIndex] =
                    animatedStatuses[nextLessonIndex].first to
                            LessonStatus.UPCOMING
            }

            if (animatedStatuses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "На этот день уроков нет",
                        color = palette.text
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = animatedStatuses,
                        key = { it.first.id }
                    ) { (lesson, status) ->
                        val hasCurrentLesson = animatedStatuses.any { (item, itemStatus) ->
                            itemStatus == LessonStatus.CURRENT &&
                                    now in item.startTime until item.endTime
                        }
                        if (status == LessonStatus.UPCOMING && !hasCurrentLesson) {
                            Text(
                                text = "До начала урока ${(lesson.startTime - now).coerceAtLeast(0)} мин",
                                color = palette.text,
                                modifier = Modifier.fillMaxWidth().padding(6.dp)
                            )
                        }

                        LessonCard(
                            lesson = lesson,
                            status = status,
                            palette = palette
                        ) {
                            onEdit(lesson.title)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp),
            containerColor = palette.currentCard,
            contentColor = palette.text
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Добавить урок"
            )
        }
    }
}

private fun currentMinutes(): Int {
    val calendar = Calendar.getInstance()

    return calendar.get(Calendar.HOUR_OF_DAY) * 60 +
            calendar.get(Calendar.MINUTE)
}
