
package com.example.schedule

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.example.data.di.DataContainer
import com.example.schedule.presentation.components.AppPalette
import com.example.schedule.presentation.components.appPalette
import com.example.schedule.presentation.screens.HomeScreen
import com.example.schedule.presentation.screens.LessonEditorScreen
import com.example.schedule.presentation.screens.SettingsScreen
import com.example.schedule.presentation.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private enum class Screen {
    HOME, EDIT, SETTINGS
}

class MainActivity : ComponentActivity() {

    private var pendingReminderChange: ((Boolean) -> Unit)? = null

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            pendingReminderChange?.invoke(granted)
            pendingReminderChange = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val vm: ScheduleViewModel = viewModel(
                factory = ScheduleViewModel.Factory(
                    applicationContext
                ) {
                    DataContainer(applicationContext).scheduleRepository
                }
            )

            val lessons by vm.lessons.collectAsStateWithLifecycle()
            val dark by vm.themeDark.collectAsStateWithLifecycle()
            val duration by vm.duration.collectAsStateWithLifecycle()
            val remindersEnabled by
            vm.remindersEnabled.collectAsStateWithLifecycle()
            val selectedDay by vm.selectedDay.collectAsStateWithLifecycle()
            val accountType by vm.accountType.collectAsStateWithLifecycle()

            var displayedDark by remember { mutableStateOf(dark) }
            var revealTarget by remember { mutableStateOf<Boolean?>(null) }
            var revealCenter by remember { mutableStateOf(Offset.Zero) }
            val revealProgress = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()

            var screen by rememberSaveable {
                mutableStateOf(Screen.HOME)
            }
            var editingTitle by rememberSaveable {
                mutableStateOf<String?>(null)
            }

            LaunchedEffect(dark, revealTarget) {
                if (revealTarget == null && displayedDark != dark) {
                    displayedDark = dark
                }
            }

            BackHandler(enabled = screen != Screen.HOME) {
                screen = Screen.HOME
            }

            val toggleReminders: (Boolean) -> Unit = { enabled ->
                if (!enabled) {
                    vm.setRemindersEnabled(false)
                } else if (
                    Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingReminderChange = {
                        vm.setRemindersEnabled(it)
                    }
                    notificationPermission.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    vm.setRemindersEnabled(true)
                }
            }

            val toggleTheme: (Offset) -> Unit = { origin ->
                if (revealTarget == null) {
                    val next = !displayedDark
                    revealCenter = origin
                    revealTarget = next

                    scope.launch {
                        revealProgress.snapTo(0f)
                        vm.toggleTheme()

                        revealProgress.animateTo(
                            1f,
                            animationSpec = tween(durationMillis = 700)
                        )

                        displayedDark = next
                        revealTarget = null
                        revealProgress.snapTo(0f)
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                AppUi(
                    dark = displayedDark,
                    palette = appPalette(displayedDark),
                    lessons = lessons,
                    selectedDay = selectedDay,
                    duration = duration,
                    remindersEnabled = remindersEnabled,
                    accountType = accountType,
                    onAccountType = vm::setAccountType,
                    screen = screen,
                    editingTitle = editingTitle,
                    onDaySelected = vm::selectDay,
                    onSettings = { screen = Screen.SETTINGS },
                    onAdd = {
                        editingTitle = null
                        screen = Screen.EDIT
                    },
                    onEdit = {
                        editingTitle = it
                        screen = Screen.EDIT
                    },
                    onBack = { screen = Screen.HOME },
                    onSave = { old, title, slots ->
                        vm.saveTitle(old, title, slots)
                        screen = Screen.HOME
                    },
                    onDeleteTitle = {
                        vm.deleteTitle(it)
                        screen = Screen.HOME
                    },
                    onToggleTheme = toggleTheme,
                    onDuration = vm::setDuration,
                    onToggleReminders = toggleReminders,
                    onDeleteSchedule = vm::deleteAll,
                )

                val target = revealTarget

                if (target != null) {
                    AppUi(
                        dark = target,
                        palette = appPalette(target),
                        lessons = lessons,
                        selectedDay = selectedDay,
                        duration = duration,
                        remindersEnabled = remindersEnabled,
                        accountType = accountType,
                        onAccountType = vm::setAccountType,
                        screen = screen,
                        editingTitle = editingTitle,
                        onDaySelected = vm::selectDay,
                        onSettings = { screen = Screen.SETTINGS },
                        onAdd = {
                            editingTitle = null
                            screen = Screen.EDIT
                        },
                        onEdit = {
                            editingTitle = it
                            screen = Screen.EDIT
                        },
                        onBack = { screen = Screen.HOME },
                        onSave = { old, title, slots ->
                            vm.saveTitle(old, title, slots)
                            screen = Screen.HOME
                        },
                        onDeleteTitle = {
                            vm.deleteTitle(it)
                            screen = Screen.HOME
                        },
                        onToggleTheme = { _ -> },
                        onDuration = vm::setDuration,
                        onToggleReminders = toggleReminders,
                        onDeleteSchedule = vm::deleteAll,
                        modifier = Modifier
                            .fillMaxSize()
                            .circularReveal(revealProgress.value, revealCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUi(
    dark: Boolean,
    palette: AppPalette,
    lessons: List<com.example.domain.model.Lesson>,
    selectedDay: Int,
    duration: Int,
    remindersEnabled: Boolean,
    accountType: String,
    onAccountType: (String) -> Unit,
    screen: Screen,
    editingTitle: String?,
    onDaySelected: (Int) -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
    onSave: (
        String?,
        String,
        List<com.example.domain.model.Lesson>
    ) -> Unit,
    onDeleteTitle: (String) -> Unit,
    onToggleTheme: (Offset) -> Unit,
    onDuration: (Int) -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onDeleteSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(
                    background = palette.background,
                    surface = palette.card,
                    onBackground = palette.text,
                    onSurface = palette.text,
                    primary = palette.currentCard
                )
            } else {
                lightColorScheme(
                    background = palette.background,
                    surface = palette.card,
                    onBackground = palette.text,
                    onSurface = palette.text,
                    primary = palette.currentCard
                )
            }
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    lessons.filter {
                        it.accountType == accountType
                    },
                    selectedDay,
                    onDaySelected,
                    palette,
                    onSettings = onSettings,
                    onAdd = onAdd,
                    onEdit = onEdit
                )

                Screen.EDIT -> LessonEditorScreen(
                    editingTitle,
                    lessons.filter {
                        it.accountType == accountType
                    },
                    duration,
                    palette,
                    accountType = accountType,
                    onBack = onBack,
                    onSave = onSave,
                    onDelete = onDeleteTitle
                )

                Screen.SETTINGS -> SettingsScreen(
                    palette,
                    dark,
                    duration,
                    remindersEnabled,
                    onBack = onBack,
                    onToggleTheme = onToggleTheme,
                    onDuration = onDuration,
                    onToggleReminders = onToggleReminders,
                    onDeleteSchedule = onDeleteSchedule,
                    accountType = accountType,
                    onAccountType = onAccountType,
                )
            }
        }
    }
}

private fun Modifier.circularReveal(
    progress: Float,
    origin: Offset
): Modifier = drawWithContent {
    val radius =
        sqrt(size.width * size.width + size.height * size.height) * progress
    val center = origin

    val path = Path().apply {
        addOval(
            Rect(
                center.x - radius,
                center.y - radius,
                center.x + radius,
                center.y + radius
            )
        )
    }

    clipPath(path) {
        this@drawWithContent.drawContent()
    }
}
