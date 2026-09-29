package com.example.routineforge

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.service.ActiveTimerType
import com.example.routineforge.service.RoutineTimerService
import com.example.routineforge.service.TimerSessionManager
import com.example.routineforge.ui.components.FloatingMiniTimerBar
import com.example.routineforge.ui.editor.RoutineEditorScreen
import com.example.routineforge.ui.editor.RoutineEditorViewModel
import com.example.routineforge.ui.history.HistoryStatsScreen
import com.example.routineforge.ui.history.HistoryStatsViewModel
import com.example.routineforge.ui.notes.NoteEditorScreen
import com.example.routineforge.ui.notes.NoteEditorState
import com.example.routineforge.ui.notes.NotesScreen
import com.example.routineforge.ui.notes.NotesViewModel
import com.example.routineforge.ui.player.RoutinePlayerScreen
import com.example.routineforge.ui.player.RoutinePlayerViewModel
import com.example.routineforge.ui.routines.RoutineListScreen
import com.example.routineforge.ui.routines.RoutineListViewModel
import com.example.routineforge.ui.schedule.ReminderEditorScreen
import com.example.routineforge.ui.schedule.ScheduleScreen
import com.example.routineforge.ui.schedule.ScheduleViewModel
import com.example.routineforge.ui.stopwatch.StopwatchScreen
import com.example.routineforge.ui.stopwatch.StopwatchViewModel
import com.example.routineforge.ui.timer.SpecialTimerScreen
import com.example.routineforge.ui.timer.SpecialTimerViewModel
import com.example.routineforge.widget.NothingTimerWidgetProvider
import java.util.UUID

@Composable
fun MainNavigation(
    launch: Intent?,
    onLaunchHandled: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val repository = remember { RoutineRepository.getInstance(context) }
    // CalendarNav is the Schedule tab (key kept so saved back stacks stay valid).
    val backStack = rememberNavBackStack(CalendarNav)

    // No per-entry ViewModel store here, so these are activity-scoped: the same instances the
    // screens below get, which lets a notification start the timer before the Timer tab exists.
    val timerViewModel: SpecialTimerViewModel = viewModel { SpecialTimerViewModel(application, repository) }
    val scheduleViewModel: ScheduleViewModel = viewModel { ScheduleViewModel(application, repository) }

    val tabs = listOf(
        Triple(CalendarNav, "Schedule", Icons.Filled.Event),
        Triple(SpecialTimerNav, "Timer", Icons.Filled.Timer),
        Triple(NotesNav, "Notes", Icons.Filled.EditNote)
    )
    val selectTab: (NavKey) -> Unit = { tab ->
        // Tabs sit directly on top of the Schedule root, so Back from any tab returns home.
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        if (tab != CalendarNav) backStack.add(tab)
    }

    LaunchedEffect(launch) {
        val intent = launch ?: return@LaunchedEffect
        val routineId = intent.getStringExtra(RoutineTimerService.EXTRA_ROUTINE_ID)
        when {
            routineId != null -> if (backStack.lastOrNull() != RoutinePlayerNav(routineId)) {
                backStack.add(RoutinePlayerNav(routineId))
            }
            intent.getStringExtra(NothingTimerWidgetProvider.EXTRA_TARGET_NAV) == NothingTimerWidgetProvider.NAV_SPECIAL_TIMER -> {
                selectTab(SpecialTimerNav)
                val seconds = intent.getIntExtra(MainActivity.EXTRA_TIMER_SECONDS, 0)
                if (seconds > 0) {
                    timerViewModel.startWith(intent.getStringExtra(MainActivity.EXTRA_TIMER_LABEL).orEmpty(), seconds)
                }
            }
            intent.getStringExtra(NothingTimerWidgetProvider.EXTRA_TARGET_NAV) == MainActivity.NAV_SCHEDULE -> selectTab(CalendarNav)
        }
        onLaunchHandled()
    }

    val currentNav = backStack.lastOrNull()
    val activeSession by TimerSessionManager.session.collectAsState()

    val isCurrentScreenSameAsTimer = when (activeSession.type) {
        ActiveTimerType.SPECIAL_TIMER -> currentNav is SpecialTimerNav
        ActiveTimerType.STOPWATCH -> currentNav is StopwatchNav
        ActiveTimerType.ROUTINE -> currentNav is RoutinePlayerNav
        ActiveTimerType.NONE -> true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (tabs.any { it.first == currentNav }) {
                NavigationBar {
                    tabs.forEach { (key, label, icon) ->
                        NavigationBarItem(
                            selected = currentNav == key,
                            onClick = { if (currentNav != key) selectTab(key) },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .consumeWindowInsets(inner)
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<CalendarNav> {
                        ScheduleScreen(
                            viewModel = scheduleViewModel,
                            onAddReminder = { backStack.add(ReminderEditorNav()) },
                            onEditReminder = { id -> backStack.add(ReminderEditorNav(reminderId = id)) },
                            onOpenRoutines = { backStack.add(RoutineListNav) },
                            onOpenHistory = { backStack.add(HistoryStatsNav) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    entry<ReminderEditorNav> { navKey ->
                        ReminderEditorScreen(
                            viewModel = scheduleViewModel,
                            reminderId = navKey.reminderId,
                            routineId = navKey.routineId,
                            onNavigateBack = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    entry<SpecialTimerNav> {
                        SpecialTimerScreen(
                            viewModel = timerViewModel,
                            onOpenStopwatch = { backStack.add(StopwatchNav) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    entry<NotesNav> {
                        val viewModel: NotesViewModel = viewModel { NotesViewModel(repository) }
                        NotesScreen(
                            viewModel = viewModel,
                            onOpenNote = { id -> backStack.add(NoteEditorNav(id ?: UUID.randomUUID().toString())) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    entry<NoteEditorNav> { navKey ->
                        val editor = remember(navKey.noteId) { NoteEditorState(repository, navKey.noteId) }
                        NoteEditorScreen(
                            editor = editor,
                            onNavigateBack = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    entry<RoutineListNav> {
                        val viewModel: RoutineListViewModel = viewModel { RoutineListViewModel(repository) }
                        RoutineListScreen(
                            viewModel = viewModel,
                            onStartRoutine = { routineId -> backStack.add(RoutinePlayerNav(routineId)) },
                            onEditRoutine = { routineId -> backStack.add(RoutineEditorNav(routineId)) },
                            onScheduleRoutine = { routineId -> backStack.add(ReminderEditorNav(routineId = routineId)) },
                            onNavigateBack = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    entry<RoutineEditorNav> { navKey ->
                        val viewModel: RoutineEditorViewModel = viewModel(key = navKey.routineId ?: "new") {
                            RoutineEditorViewModel(repository, navKey.routineId)
                        }
                        RoutineEditorScreen(
                            viewModel = viewModel,
                            onNavigateBack = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    entry<RoutinePlayerNav> { navKey ->
                        val viewModel: RoutinePlayerViewModel = viewModel(key = navKey.routineId) {
                            RoutinePlayerViewModel(application, repository, navKey.routineId)
                        }
                        RoutinePlayerScreen(
                            viewModel = viewModel,
                            onNavigateBack = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    entry<HistoryStatsNav> {
                        val viewModel: HistoryStatsViewModel = viewModel { HistoryStatsViewModel(repository) }
                        HistoryStatsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    entry<StopwatchNav> {
                        val viewModel: StopwatchViewModel = viewModel { StopwatchViewModel() }
                        StopwatchScreen(
                            viewModel = viewModel,
                            onNavigateBack = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            )

            // Docked In-App Floating Mini-Timer Pill
            if (!isCurrentScreenSameAsTimer) {
                FloatingMiniTimerBar(
                    session = activeSession,
                    onNavigateToTimer = { type, routineId ->
                        when (type) {
                            ActiveTimerType.SPECIAL_TIMER -> selectTab(SpecialTimerNav)
                            ActiveTimerType.STOPWATCH -> backStack.add(StopwatchNav)
                            ActiveTimerType.ROUTINE -> if (routineId != null) backStack.add(RoutinePlayerNav(routineId))
                            ActiveTimerType.NONE -> {}
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 86.dp)
                )
            }
        }
    }
}
