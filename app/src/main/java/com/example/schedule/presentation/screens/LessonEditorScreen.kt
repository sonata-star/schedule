package com.example.schedule.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.Lesson
import com.example.schedule.presentation.components.AppPalette
import com.example.schedule.presentation.components.DAY_NAMES
import com.example.schedule.presentation.components.currentScheduleDayIndex
import com.example.schedule.presentation.components.timeText
import java.util.Locale

private data class EditSlot(
    var day: String,
    var start: Int,
    var end: Int
)

@Composable
fun LessonEditorScreen(
    oldTitle: String?,
    allLessons: List<Lesson>,
    duration: Int,
    palette: AppPalette,
    accountType: String,
    onBack: () -> Unit,
    onSave: (String?, String, List<Lesson>) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember(oldTitle) {
        mutableStateOf(oldTitle.orEmpty())
    }

    val matchingLessons = allLessons.filter {
        oldTitle != null &&
                it.title.equals(oldTitle, ignoreCase = true) &&
                it.accountType == accountType
    }

    val cabinetValue = matchingLessons.firstNotNullOfOrNull {
        it.cabinet.takeIf(String::isNotBlank)
    }.orEmpty()

    val teacherValue = matchingLessons.firstNotNullOfOrNull {
        it.teacher.takeIf(String::isNotBlank)
    }.orEmpty()

    val subjectValue = matchingLessons.firstNotNullOfOrNull {
        it.subject.takeIf(String::isNotBlank)
    }.orEmpty()

    var cabinet by remember(oldTitle, accountType, allLessons) {
        mutableStateOf(cabinetValue)
    }

    var teacher by remember(oldTitle, accountType, allLessons) {
        mutableStateOf(teacherValue)
    }

    var subject by remember(oldTitle, accountType, allLessons) {
        mutableStateOf(subjectValue)
    }

    val slots = remember(oldTitle, allLessons, duration, accountType) {
        mutableStateListOf<EditSlot>().apply {
            val matching = allLessons.filter {
                oldTitle != null &&
                        it.title.equals(oldTitle, ignoreCase = true) &&
                        it.accountType == accountType
            }

            if (matching.isEmpty()) {
                val start = 480
                add(
                    EditSlot(
                        DAY_NAMES[currentScheduleDayIndex()],
                        start,
                        (start + duration).coerceAtMost(1440)
                    )
                )
            } else {
                matching.forEach {
                    add(
                        EditSlot(
                            day = it.dayOfWeek,
                            start = it.startTime,
                            end = it.endTime
                        )
                    )
                }
            }
        }
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var editingTime by remember {
        mutableStateOf<Pair<Int, Boolean>?>(null)
    }

    // Данные для диалога выбора учителя.
    var teacherConflict by remember {
        mutableStateOf<Pair<String, String>?>(null)
    }

    var pendingSaveTitle by remember {
        mutableStateOf("")
    }

    // Сохраняем расписание с выбранным учителем.
    fun saveLessons(
        normalizedTitle: String,
        selectedTeacher: String
    ) {
        onSave(
            oldTitle,
            normalizedTitle,
            slots.map {
                Lesson(
                    title = normalizedTitle,
                    startTime = it.start,
                    endTime = it.end,
                    dayOfWeek = it.day,
                    cabinet = cabinet.trim(),
                    teacher = selectedTeacher.trim(),
                    subject = subject.trim(),
                    accountType = accountType
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = palette.text
                )
            }

            TextButton(
                onClick = {
                    val normalized = title.trim().replaceFirstChar {
                        if (it.isLowerCase()) {
                            it.titlecase(Locale("ru"))
                        } else {
                            it.toString()
                        }
                    }

                    // Проверяем пересечения между занятиями в редакторе.
                    val internalOverlap = slots.indices.any { i ->
                        (i + 1 until slots.size).any { j ->
                            val first = slots[i]
                            val second = slots[j]

                            first.day == second.day &&
                                    first.start < second.end &&
                                    second.start < first.end
                        }
                    }

                    // Проверяем пересечения с другими сохранёнными занятиями.
                    val externalOverlap = allLessons.any { lesson ->
                        lesson.accountType == accountType &&
                                (
                                        oldTitle == null ||
                                                !lesson.title.equals(
                                                    oldTitle,
                                                    ignoreCase = true
                                                )
                                        ) &&
                                slots.any { slot ->
                                    slot.day == lesson.dayOfWeek &&
                                            slot.start < lesson.endTime &&
                                            lesson.startTime < slot.end
                                }
                    }

                    when {
                        normalized.isBlank() -> {
                            error = if (accountType == "STUDENT") {
                                "Введите название предмета"
                            } else {
                                "Введите ФИО ученика или класс."
                            }
                        }

                        slots.isEmpty() -> {
                            error = "Добавьте хотя бы один день"
                        }

                        slots.any {
                            it.start !in 0..1439 ||
                                    it.end !in 1..1440 ||
                                    it.end <= it.start
                        } -> {
                            error = "Проверьте время начала и окончания"
                        }

                        internalOverlap || externalOverlap -> {
                            error =
                                "Уроки не могут пересекаться по времени в один день"
                        }

                        else -> {
                            /*
                             * Ищем учителя, уже сохранённого для этого названия.
                             * Это работает и при редактировании, и при создании
                             * ещё одной записи с существующим названием.
                             */
                            val savedTeacher = allLessons.firstNotNullOfOrNull {
                                    lesson ->
                                lesson.teacher
                                    .takeIf {
                                        lesson.accountType == accountType &&
                                                lesson.title.equals(
                                                    normalized,
                                                    ignoreCase = true
                                                ) &&
                                                it.isNotBlank()
                                    }
                            }.orEmpty()

                            val newTeacher = teacher.trim()

                            if (
                                accountType == "STUDENT" &&
                                savedTeacher.isNotBlank() &&
                                newTeacher.isNotBlank() &&
                                !savedTeacher.equals(
                                    newTeacher,
                                    ignoreCase = true
                                )
                            ) {
                                // Не сохраняем автоматически: сначала выбор.
                                pendingSaveTitle = normalized
                                teacherConflict = savedTeacher to newTeacher
                            } else {
                                // Если имя не введено, используем сохранённое.
                                val teacherToSave = newTeacher.ifBlank {
                                    savedTeacher
                                }

                                teacher = teacherToSave
                                saveLessons(normalized, teacherToSave)
                            }
                        }
                    }
                }
            ) {
                Text("Сохранить", color = palette.text)
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it.take(20)
            },
            label = {
                Text(
                    if (accountType == "STUDENT") {
                        "Название предмета"
                    } else {
                        "ФИО ученика или класс"
                    }
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (accountType == "STUDENT") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = teacher,
                    onValueChange = {
                        teacher = it.take(80)
                    },
                    label = {
                        Text("ФИО учителя")
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = cabinet,
                    onValueChange = {
                        cabinet = it.take(30)
                    },
                    label = {
                        Text("Кабинет")
                    },
                    singleLine = true,
                    modifier = Modifier.width(112.dp)
                )
            }
        } else {
            OutlinedTextField(
                value = subject,
                onValueChange = {
                    subject = it.take(60)
                },
                label = {
                    Text("Предмет (необязательно)")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = 0.dp
            )
        ) {
            itemsIndexed(slots) { index, slot ->
                Column(
                    Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        var expanded by remember {
                            mutableStateOf(false)
                        }

                        Box {
                            TextButton(
                                onClick = {
                                    expanded = true
                                }
                            ) {
                                Text(
                                    slot.day,
                                    color = palette.text
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = {
                                    expanded = false
                                }
                            ) {
                                DAY_NAMES.forEach { day ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(day)
                                        },
                                        onClick = {
                                            slots[index] = slot.copy(day = day)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    editingTime = index to true
                                },
                                modifier = Modifier.heightIn(min = 64.dp)
                            ) {
                                Text(
                                    timeText(slot.start),
                                    color = palette.text
                                )
                            }

                            Text(
                                "—",
                                color = palette.text
                            )

                            TextButton(
                                onClick = {
                                    editingTime = index to false
                                },
                                modifier = Modifier.heightIn(min = 64.dp)
                            ) {
                                Text(
                                    timeText(slot.end),
                                    color = palette.text
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (slots.size > 1) {
                                        slots.removeAt(index)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить день",
                                    tint = palette.text
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = palette.border)
                }
            }

            item {
                TextButton(
                    onClick = {
                        slots.add(
                            EditSlot(
                                DAY_NAMES.first(),
                                480,
                                (480 + duration).coerceAtMost(1440)
                            )
                        )
                    }
                ) {
                    Text(
                        "+ Добавить день",
                        color = palette.text
                    )
                }
            }

            if (oldTitle != null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = {
                                onDelete(oldTitle)
                            }
                        ) {
                            Text(
                                "Удалить все уроки",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // Диалог выбора: уже сохранённый учитель или введённый сейчас.
    teacherConflict?.let { (savedTeacher, newTeacher) ->
        AlertDialog(
            onDismissRequest = {
                teacherConflict = null
            },
            title = {
                Text("Учитель уже записан")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Для предмета «${pendingSaveTitle}» уже сохранён учитель: " +
                                "«$savedTeacher»."
                    )

                    Text("Выберите, какое имя использовать:")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        teacher = savedTeacher
                        saveLessons(
                            pendingSaveTitle,
                            savedTeacher
                        )
                        teacherConflict = null
                    }
                ) {
                    Text("Оставить сохранённого")
                }
            },
            dismissButton = {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    TextButton(
                        onClick = {
                            teacher = newTeacher
                            saveLessons(
                                pendingSaveTitle,
                                newTeacher
                            )
                            teacherConflict = null
                        }
                    ) {
                        Text("Использовать нового")
                    }

                    TextButton(
                        onClick = {
                            teacherConflict = null
                        }
                    ) {
                        Text("Закрыть")
                    }
                }
            }
        )
    }

    // Диалог ошибок валидации.
    if (error != null) {
        AlertDialog(
            onDismissRequest = {
                error = null
            },
            title = {
                Text("Ошибка")
            },
            text = {
                Text(error!!)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        error = null
                    }
                ) {
                    Text("ОК")
                }
            }
        )
    }

    // Диалог редактирования времени.
    editingTime?.let { (index, isStart) ->
        val slot = slots.getOrNull(index)

        if (slot != null) {
            val initialTime = if (isStart) {
                slot.start
            } else {
                slot.end
            }

            var hours by remember(index, isStart, initialTime) {
                mutableStateOf(
                    (initialTime / 60).toString().padStart(2, '0')
                )
            }

            var minutes by remember(index, isStart, initialTime) {
                mutableStateOf(
                    (initialTime % 60).toString().padStart(2, '0')
                )
            }

            AlertDialog(
                onDismissRequest = {
                    editingTime = null
                },
                title = {
                    Text(
                        if (isStart) {
                            "Время начала"
                        } else {
                            "Время окончания"
                        }
                    )
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = hours,
                            onValueChange = {
                                if (
                                    it.length <= 2 &&
                                    it.all(Char::isDigit)
                                ) {
                                    hours = it
                                }
                            },
                            label = {
                                Text("Часы")
                            },
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )

                        Text(
                            ":",
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        OutlinedTextField(
                            value = minutes,
                            onValueChange = {
                                if (
                                    it.length <= 2 &&
                                    it.all(Char::isDigit)
                                ) {
                                    minutes = it
                                }
                            },
                            label = {
                                Text("Минуты")
                            },
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val h = hours.toIntOrNull()
                            val m = minutes.toIntOrNull()

                            if (
                                h != null &&
                                m != null &&
                                h in 0..23 &&
                                m in 0..59
                            ) {
                                val value = h * 60 + m

                                slots[index] = if (isStart) {
                                    slot.copy(
                                        start = value,
                                        end = (value + duration)
                                            .coerceAtMost(1440)
                                    )
                                } else {
                                    slot.copy(end = value)
                                }

                                editingTime = null
                            } else {
                                error = "Введите корректное время"
                            }
                        }
                    ) {
                        Text("ОК")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            editingTime = null
                        }
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}