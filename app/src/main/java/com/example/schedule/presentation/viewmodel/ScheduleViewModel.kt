package com.example.schedule.presentation.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Lesson
import com.example.schedule.notifications.LessonNotificationScheduler
import com.example.schedule.widget.ScheduleWidgetProvider
import com.example.domain.repository.ScheduleRepository
import com.example.domain.usecase.GetLessonsUseCase
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule_preferences")

class ScheduleViewModel(
    private val repository: ScheduleRepository,
    private val appContext: Context
) : ViewModel() {
    private val getLessons = GetLessonsUseCase(repository)
    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()
    private val _selectedDay = MutableStateFlow(currentDayIndex())
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()
    private val _themeDark = MutableStateFlow(true)
    val themeDark: StateFlow<Boolean> = _themeDark.asStateFlow()
    private val _duration = MutableStateFlow(45)
    val duration: StateFlow<Int> = _duration.asStateFlow()
    private val _remindersEnabled = MutableStateFlow(true)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()
    private val _accountType = MutableStateFlow("STUDENT")
    val accountType: StateFlow<String> = _accountType.asStateFlow()

    init {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                val theme = repository.getSetting("theme", "dark") == "dark"
                val duration = repository.getSetting("lessonDuration", "45").toIntOrNull()?.coerceIn(1, 180) ?: 45
                val reminders = repository.getSetting("remindersEnabled", "true") == "true"
                val mode = runCatching {
                    appContext.scheduleDataStore.data.first()[stringPreferencesKey("account_type")] ?: "STUDENT"
                }.getOrDefault("STUDENT").let { if (it == "TEACHER") "TEACHER" else "STUDENT" }
                if (repository.getSetting("accountType", "") != mode) repository.setSetting("accountType", mode)
                SettingsAndLessons(theme, duration, reminders, mode, getLessons())
            }
            _themeDark.value = data.themeDark
            _duration.value = data.duration
            _remindersEnabled.value = data.remindersEnabled
            _accountType.value = data.accountType
            _lessons.value = data.lessons
            appContext.getSharedPreferences("schedule_runtime", Context.MODE_PRIVATE).edit().putString("account_type", data.accountType).apply()
            if (data.remindersEnabled) LessonNotificationScheduler.reschedule(appContext, data.lessons, data.accountType)
        }
    }

    fun selectDay(index: Int) { if (index in 0..6) _selectedDay.value = index }
    fun reload() {
        viewModelScope.launch {
            _lessons.value = withContext(Dispatchers.IO) { getLessons() }
            ScheduleWidgetProvider.refresh(appContext)
        }
    }
    fun saveTitle(oldTitle: String?, title: String, slots: List<Lesson>) {
        val mode = _accountType.value
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                repository.saveTitle(oldTitle, title, slots, mode)
                getLessons()
            }
            _lessons.value = updated
            ScheduleWidgetProvider.refresh(appContext)
            if (_remindersEnabled.value) LessonNotificationScheduler.reschedule(appContext, updated, mode)
        }
    }
    fun deleteTitle(title: String) {
        val mode = _accountType.value
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                repository.deleteTitle(title, mode)
                getLessons()
            }
            _lessons.value = updated
            ScheduleWidgetProvider.refresh(appContext)
            if (_remindersEnabled.value) LessonNotificationScheduler.reschedule(appContext, updated, mode)
        }
    }
    fun deleteAll() {
        val mode = _accountType.value
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.deleteAllLessons(mode)
                getLessons()
            }
            _lessons.value = result
            ScheduleWidgetProvider.refresh(appContext)
            if (_remindersEnabled.value) LessonNotificationScheduler.reschedule(appContext, result, mode)
        }
    }

    fun setAccountType(value: String) {
        val normalized = if (value == "TEACHER") "TEACHER" else "STUDENT"
        if (_accountType.value == normalized) return
        _accountType.value = normalized
        ScheduleWidgetProvider.refresh(appContext)
        appContext.getSharedPreferences("schedule_runtime", Context.MODE_PRIVATE).edit().putString("account_type", normalized).apply()
        reload()
        if (_remindersEnabled.value) viewModelScope.launch { val all = withContext(Dispatchers.IO) { getLessons() }; LessonNotificationScheduler.reschedule(appContext, all, normalized) }
        viewModelScope.launch(Dispatchers.IO) {
            appContext.scheduleDataStore.edit { it[stringPreferencesKey("account_type")] = normalized }
            repository.setSetting("accountType", normalized)
        }
    }
    fun toggleTheme() {
        val next = !_themeDark.value; _themeDark.value = next
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("theme", if (next) "dark" else "light")
        }
    }
    fun setDuration(value: Int) {
        if (value !in 1..180) return
        _duration.value = value
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("lessonDuration", value.toString())
        }
    }
    fun setRemindersEnabled(enabled: Boolean) {
        _remindersEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("remindersEnabled", enabled.toString())
            if (enabled) LessonNotificationScheduler.reschedule(appContext, getLessons(), _accountType.value)
            else LessonNotificationScheduler.reschedule(appContext, emptyList(), _accountType.value)
        }
    }
    override fun onCleared() { repository.close(); super.onCleared() }

    private data class SettingsAndLessons(val themeDark: Boolean, val duration: Int, val remindersEnabled: Boolean, val accountType: String, val lessons: List<Lesson>)
    class Factory(private val context: Context, private val repositoryProvider: () -> ScheduleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) return ScheduleViewModel(repositoryProvider(), context.applicationContext) as T
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    private fun currentDayIndex(): Int = LocalDate.now().dayOfWeek.value - 1
}
