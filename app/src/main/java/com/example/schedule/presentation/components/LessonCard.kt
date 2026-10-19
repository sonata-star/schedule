package com.example.schedule.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.domain.model.Lesson
import com.example.schedule.presentation.model.LessonStatus

@Composable
fun LessonCard(
    lesson: Lesson,
    status: LessonStatus,
    palette: AppPalette,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .background(
                if (status == LessonStatus.CURRENT) palette.currentCard
                else if (status == LessonStatus.PAST) palette.card.copy(alpha = 0.55f)
                else palette.card,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 25.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            val primary = if (lesson.accountType == "STUDENT") {
                if (lesson.cabinet.isBlank()) lesson.title
                else "${lesson.title}, ${lesson.cabinet}"
            } else lesson.title

            Text(
                primary,
                color = if (status == LessonStatus.PAST) Color.Gray else palette.text,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium
            )

            val secondary =
                if (lesson.accountType == "STUDENT") lesson.teacher else lesson.subject

            if (secondary.isNotBlank()) {
                Text(
                    secondary,
                    color = if (status == LessonStatus.PAST) Color.Gray
                    else palette.text.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }
        }

        Text(
            "${timeText(lesson.startTime)} – ${timeText(lesson.endTime)}",
            color = if (status == LessonStatus.PAST) Color.Gray else palette.text,
            fontSize = 14.sp
        )
    }
}

fun timeText(value: Int) =
    "%02d:%02d".format(value / 60, value % 60)