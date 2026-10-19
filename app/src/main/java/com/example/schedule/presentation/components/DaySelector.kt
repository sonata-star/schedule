package com.example.schedule.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

val DAY_NAMES = listOf(
    "Понедельник",
    "Вторник",
    "Среда",
    "Четверг",
    "Пятница",
    "Суббота",
    "Воскресенье"
)

private val DAY_SHORT = listOf(
    "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"
)

fun currentScheduleDayIndex(): Int =
    LocalDate.now().dayOfWeek.value - 1

fun dateForScheduleDay(index: Int): LocalDate {
    require(index in 0..6)

    val monday = LocalDate.now()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    return monday.plusDays(index.toLong())
}

@Composable
fun DaySelector(
    selected: Int,
    palette: AppPalette,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DAY_SHORT.forEachIndexed { index, label ->
            val date = dateForScheduleDay(index)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 0.dp)
                    .height(68.dp)
                    .background(
                        color = if (selected == index) {
                            palette.currentCard
                        } else {
                            palette.dayButton
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        onSelected(index)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    color = palette.text,
                    fontSize = 18.sp
                )

                Text(
                    text = date.dayOfMonth.toString(),
                    color = palette.text,
                    fontSize = 16.sp
                )
            }
        }
    }
}