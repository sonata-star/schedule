
package com.example.schedule.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedule.presentation.components.AppPalette
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
@Composable
fun SettingsScreen(
    palette: AppPalette,
    dark: Boolean,
    duration: Int,
    remindersEnabled: Boolean,
    onBack: () -> Unit,
    onToggleTheme: (Offset) -> Unit,
    onDuration: (Int) -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onDeleteSchedule: () -> Unit,
    accountType: String,
    onAccountType: (String) -> Unit,
) {
    var durationDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var accountDialog by remember { mutableStateOf(false) }
    var input by remember(duration) {
        mutableStateOf(duration.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (-12).dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = palette.text
                )
            }

            Text(
                "Настройки",
                color = palette.text,
                fontSize = 23.sp
            )
        }

        // Переключение режима аккаунта
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(palette.currentCard)
                    .clickable { accountDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (accountType == "STUDENT") {
                        Icons.Default.School
                    } else {
                        Icons.Default.Person
                    },
                    contentDescription = "Сменить режим аккаунта",
                    modifier = Modifier.size(54.dp),
                    tint = palette.text
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (accountType == "STUDENT") {
                    "Ученик"
                } else {
                    "Учитель / репетитор"
                },
                color = palette.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Нажмите, чтобы сменить режим",
                color = palette.text.copy(alpha = 0.65f),
                fontSize = 13.sp
            )
        }

        HorizontalDivider(color = palette.border)

        ReminderSettingLine(
            "Напоминания",
            remindersEnabled,
            palette,
            onToggleReminders
        )

        ThemeSettingLine(
            title = "Тема",
            value = if (dark) "Тёмная" else "Светлая",
            palette = palette,
            dark = dark,
            onToggleTheme = onToggleTheme
        )

        SettingLine(
            title = "Длительность урока",
            value = "$duration мин",
            palette = palette,
            onClick = {
                durationDialog = true
            }
        )
        val context = LocalContext.current

        SettingLine(
            "Политика конфиденциальности",
            "",
            palette,
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://docs.google.com/document/d/1WbAOZkwUFmptq_MwYecq1LyjSRcQ79MsUCYlIuQnNtM/edit?usp=sharing")
                )
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        SettingLine(
            "Удалить расписание",
            "",
            palette,
            onClick = { deleteDialog = true },
            danger = true,
        )

        SettingLine("Версия", "1.0.", palette)
    }

    if (accountDialog) {
        AlertDialog(
            onDismissRequest = { accountDialog = false },
            title = { Text("Тип аккаунта") },
            text = {
                Column {
                    AccountOption(
                        title = "Ученик",
                        description = "Моё расписание уроков",
                        selected = accountType == "STUDENT",
                        palette = palette,
                        onClick = {
                            onAccountType("STUDENT")
                            accountDialog = false
                        }
                    )

                    AccountOption(
                        title = "Учитель / репетитор",
                        description = "Занятия с моими учениками",
                        selected = accountType == "TEACHER",
                        palette = palette,
                        onClick = {
                            onAccountType("TEACHER")
                            accountDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { accountDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (durationDialog) {
        AlertDialog(
            onDismissRequest = { durationDialog = false },
            title = { Text("Длительность урока") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it.filter(Char::isDigit).take(3)
                    },
                    label = { Text("Минуты") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        input.toIntOrNull()
                            ?.takeIf { it in 1..180 }
                            ?.let(onDuration)
                        durationDialog = false
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { durationDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Удалить расписание?") },
            text = {
                Text("Все уроки будут удалены. Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteDialog = false
                        onDeleteSchedule()
                        onBack()
                    }
                ) {
                    Text(
                        "Удалить",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun AccountOption(
    title: String,
    description: String,
    selected: Boolean,
    palette: AppPalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) {
                    palette.currentCard
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = palette.text,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                description,
                color = palette.text.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        if (selected) {
            RadioButton(
                selected = true,
                onClick = onClick
            )
        } else {
            RadioButton(
                selected = false,
                onClick = onClick
            )
        }
    }

    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ReminderSettingLine(
    title: String,
    enabled: Boolean,
    palette: AppPalette,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(title, fontSize = 18.sp, color = palette.text)
            }

            Switch(
                checked = enabled,
                onCheckedChange = onCheckedChange
            )
        }

        HorizontalDivider(color = palette.border)
    }
}


@Composable
private fun ThemeSettingLine(
    title: String,
    value: String,
    palette: AppPalette,
    dark: Boolean,
    onToggleTheme: (Offset) -> Unit
) {
    var center by remember { mutableStateOf(Offset.Zero) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    center = Offset(
                        position.x + coordinates.size.width / 2f,
                        position.y + coordinates.size.height / 2f
                    )
                }
                .clickable { onToggleTheme(center) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                color = palette.text,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            )

            Text(
                text = value,
                color = palette.text.copy(alpha = 0.75f)
            )
        }

        HorizontalDivider(color = palette.border)
    }
}


@Composable
private fun SettingLine(
    title: String,
    value: String,
    palette: AppPalette,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null) {
                    onClick?.invoke()
                }
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                color = if (danger) {
                    MaterialTheme.colorScheme.error
                } else {
                    palette.text
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            )

            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    color = palette.text.copy(alpha = 0.75f)
                )
            }
        }

        HorizontalDivider(color = palette.border)
    }
}