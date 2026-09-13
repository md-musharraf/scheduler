package com.example.routineforge

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.ui.calendar.CalendarScreen
import com.example.routineforge.ui.calendar.CalendarViewModel
import com.example.routineforge.ui.editor.RoutineEditorScreen
import com.example.routineforge.ui.editor.RoutineEditorViewModel
import com.example.routineforge.ui.history.HistoryStatsScreen
import com.example.routineforge.ui.history.HistoryStatsViewModel
import com.example.routineforge.ui.player.RoutinePlayerScreen
import com.example.routineforge.ui.player.RoutinePlayerViewModel
import com.example.routineforge.ui.routines.RoutineListScreen
import com.example.routineforge.ui.routines.RoutineListViewModel

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val repository = remember { RoutineRepository.getInstance(context) }
    val backStack = rememberNavBackStack(RoutineListNav)

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
                val viewModel: CalendarViewModel = viewModel {
                    CalendarViewModel(repository)
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
        }
    )
}
