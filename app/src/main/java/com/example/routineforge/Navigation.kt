package com.example.routineforge

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.navigation3.runtime.NavKey
import com.example.routineforge.ui.notes.NoteEditorScreen
import com.example.routineforge.ui.notes.NoteEditorState
import com.example.routineforge.ui.notes.NotesScreen
import com.example.routineforge.ui.notes.NotesViewModel
import java.util.UUID
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.service.ActiveTimerType
import com.example.routineforge.service.TimerSessionManager
import com.example.routineforge.ui.calendar.CalendarScreen
import com.example.routineforge.ui.calendar.CalendarViewModel
import com.example.routineforge.ui.components.FloatingMiniTimerBar
import com.example.routineforge.ui.editor.RoutineEditorScreen
import com.example.routineforge.ui.editor.RoutineEditorViewModel
import com.example.routineforge.ui.history.HistoryStatsScreen
import com.example.routineforge.ui.history.HistoryStatsViewModel
import com.example.routineforge.ui.player.RoutinePlayerScreen
import com.example.routineforge.ui.player.RoutinePlayerViewModel
import com.example.routineforge.ui.routines.RoutineListScreen
import com.example.routineforge.ui.routines.RoutineListViewModel
import com.example.routineforge.ui.stopwatch.StopwatchScreen
import com.example.routineforge.ui.stopwatch.StopwatchViewModel
import com.example.routineforge.ui.timer.SpecialTimerScreen
import com.example.routineforge.ui.timer.SpecialTimerViewModel

@Composable
fun MainNavigation(
    initialRoutineId: String? = null,
    targetNav: String? = null
) {
    val context = LocalContext.current
    val repository = remember { RoutineRepository.getInstance(context) }
    val backStack = rememberNavBackStack(RoutineListNav)

    androidx.compose.runtime.LaunchedEffect(initialRoutineId, targetNav) {
        if (initialRoutineId != null && backStack.lastOrNull() !is RoutinePlayerNav) {
            backStack.add(RoutinePlayerNav(initialRoutineId))
        } else if (targetNav == "SPECIAL_TIMER" && backStack.lastOrNull() !is SpecialTimerNav) {
            backStack.add(SpecialTimerNav)
        }
    }

    val currentNav = backStack.lastOrNull()
    val activeSession by TimerSessionManager.session.collectAsState()

    val isCurrentScreenSameAsTimer = when (activeSession.type) {
        ActiveTimerType.SPECIAL_TIMER -> currentNav is SpecialTimerNav
        ActiveTimerType.STOPWATCH -> currentNav is StopwatchNav
        ActiveTimerType.ROUTINE -> currentNav is RoutinePlayerNav
        ActiveTimerType.NONE -> true
    }

    val tabs = listOf(
        Triple(RoutineListNav, "Routines", Icons.AutoMirrored.Filled.ListAlt),
        Triple(SpecialTimerNav, "Timer", Icons.Filled.Timer),
        Triple(NotesNav, "Notes", Icons.Filled.EditNote)
    )
    val selectTab: (NavKey) -> Unit = { tab ->
        // Tabs sit directly on top of the Routines root, so Back from any tab returns home.
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        if (tab != RoutineListNav) backStack.add(tab)
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
                entry<RoutineListNav> {
                    val viewModel: RoutineListViewModel = viewModel {
                        RoutineListViewModel(repository)
                    }
                    RoutineListScreen(
                        viewModel = viewModel,
                        onStartRoutine = { routineId ->
                            backStack.add(RoutinePlayerNav(routineId))
                        },
                        onEditRoutine = { routineId ->
                            backStack.add(RoutineEditorNav(routineId))
                        },
                        onOpenHistory = {
                            backStack.add(HistoryStatsNav)
                        },
                        onOpenCalendar = {
                            backStack.add(CalendarNav)
                        },
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
                    val application = context.applicationContext as Application
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
                    val viewModel: HistoryStatsViewModel = viewModel {
                        HistoryStatsViewModel(repository)
                    }
                    HistoryStatsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                entry<CalendarNav> {
                    val application = context.applicationContext as Application
                    val viewModel: CalendarViewModel = viewModel {
                        CalendarViewModel(application, repository)
                    }
                    CalendarScreen(
                        viewModel = viewModel,
                        onStartRoutine = { routineId ->
                            backStack.add(RoutinePlayerNav(routineId))
                        },
                        onNavigateBack = { backStack.removeLastOrNull() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                entry<StopwatchNav> {
                    val viewModel: StopwatchViewModel = viewModel {
                        StopwatchViewModel()
                    }
                    StopwatchScreen(
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                entry<SpecialTimerNav> {
                    val application = context.applicationContext as Application
                    val viewModel: SpecialTimerViewModel = viewModel {
                        SpecialTimerViewModel(application, repository)
                    }
                    SpecialTimerScreen(
                        viewModel = viewModel,
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
