package com.example.routineforge.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.routineforge.MainActivity
import com.example.routineforge.container
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.LocalSnackbar
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.data.TimerEvent
import com.example.routineforge.core.database.SessionType
import com.example.routineforge.feature.calendar.CalendarScreen
import com.example.routineforge.feature.calendar.EventEditorScreen
import com.example.routineforge.feature.goals.GoalsScreen
import com.example.routineforge.feature.home.HomeScreen
import com.example.routineforge.feature.insights.InsightsScreen
import com.example.routineforge.feature.notes.DrawingScreen
import com.example.routineforge.feature.notes.NoteEditorScreen
import com.example.routineforge.feature.notes.NotesScreen
import com.example.routineforge.feature.onboarding.OnboardingScreen
import com.example.routineforge.feature.progress.ProgressScreen
import com.example.routineforge.feature.reminders.ReminderEditorScreen
import com.example.routineforge.feature.reminders.RemindersScreen
import com.example.routineforge.feature.settings.SettingsScreen
import com.example.routineforge.feature.subjects.SubjectDetailScreen
import com.example.routineforge.feature.subjects.SubjectEditorScreen
import com.example.routineforge.feature.subjects.SubjectsScreen
import com.example.routineforge.feature.timer.MiniTimerBar
import com.example.routineforge.feature.timer.TimerScreen
import com.example.routineforge.feature.timer.TimerViewModel

/** Back-stack operations shared by every screen. Tabs sit directly on top of Home. */
class AppNav(private val backStack: NavBackStack<NavKey>) {
    val current: NavKey? get() = backStack.lastOrNull()

    fun push(key: NavKey) {
        if (backStack.lastOrNull() != key) backStack.add(key)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun tab(key: NavKey) {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        if (key != backStack.first()) backStack.add(key)
    }

    /** Replaces the current screen, e.g. an editor that opens its result. */
    fun replace(key: NavKey) {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        backStack.add(key)
    }
}

private data class Tab(val key: NavKey, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    Tab(HomeNav, "Home", Icons.Filled.Home),
    Tab(SubjectsNav, "Subjects", Icons.AutoMirrored.Filled.MenuBook),
    Tab(TimerNav, "Timer", Icons.Filled.HourglassTop),
    Tab(NotesNav, "Notes", Icons.Filled.EditNote),
    Tab(CalendarNav, "Calendar", Icons.Filled.CalendarMonth)
)

@Composable
fun FocusFlowRoot(launchNav: String?, onLaunchHandled: () -> Unit) {
    val profile by LocalContext.current.container.profiles.profile.collectAsStateWithLifecycle(initialValue = null)
    val p = profile
    when {
        p == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        !p.onboardingDone -> OnboardingScreen()
        else -> MainScaffold(launchNav, onLaunchHandled)
    }
}

@Composable
private fun MainScaffold(launchNav: String?, onLaunchHandled: () -> Unit) {
    val context = LocalContext.current
    val backStack = rememberNavBackStack(HomeNav)
    val nav = remember(backStack) { AppNav(backStack) }
    // Created outside NavDisplay, so it's activity-scoped and shared by every screen that starts a timer.
    val timerVm: TimerViewModel = appViewModel { TimerViewModel(it) }
    val snackbar = remember { SnackbarHostState() }
    val current = backStack.lastOrNull()
    val onTab = TABS.any { it.key == current }

    LaunchedEffect(launchNav) {
        when (launchNav) {
            MainActivity.NAV_TIMER -> { nav.tab(TimerNav); timerVm.showStopwatch = false }
            MainActivity.NAV_STOPWATCH -> { nav.tab(TimerNav); timerVm.showStopwatch = true }
            MainActivity.NAV_REMINDERS -> { nav.tab(HomeNav); nav.push(RemindersNav) }
            MainActivity.NAV_CALENDAR -> nav.tab(CalendarNav)
            else -> return@LaunchedEffect
        }
        onLaunchHandled()
    }

    val onTimerTab by rememberUpdatedState(current == TimerNav)
    LaunchedEffect(Unit) {
        context.container.timer.events.collect { e ->
            // On the Timer tab the Session Complete screen says it instead.
            if (e is TimerEvent.Finished && e.natural && !(onTimerTab && e.type == SessionType.FOCUS)) {
                snackbar.showSnackbar(
                    if (e.type == SessionType.FOCUS) "Focus complete · ${TimeFormat.duration(e.savedMs)} saved" else "${e.type.label} over. Ready to focus?"
                )
            }
        }
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbar, if (onTab) Modifier else Modifier.navigationBarsPadding().imePadding()) },
            bottomBar = {
                if (onTab) androidx.compose.foundation.layout.Column {
                    if (current != TimerNav) MiniTimerBar(timerVm, onOpen = { nav.tab(TimerNav) }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        TABS.forEach { tab ->
                            NavigationBarItem(
                                selected = current == tab.key,
                                onClick = { if (current != tab.key) nav.tab(tab.key) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        ) { inner ->
            Box(
                Modifier.fillMaxSize().padding(inner).consumeWindowInsets(inner)
                    .then(if (onTab) Modifier else Modifier.navigationBarsPadding())
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { nav.back() },
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                    entryProvider = entryProvider {
                        entry<HomeNav> { HomeScreen(nav, timerVm) }
                        entry<SubjectsNav> { SubjectsScreen(nav, timerVm) }
                        entry<TimerNav> { TimerScreen(timerVm, nav) }
                        entry<NotesNav> { NotesScreen(nav) }
                        entry<CalendarNav> { CalendarScreen(nav) }
                        entry<SubjectDetailNav> { SubjectDetailScreen(it.subjectId, nav, timerVm) }
                        entry<SubjectEditorNav> { SubjectEditorScreen(it.subjectId, nav) }
                        entry<NoteEditorNav> { NoteEditorScreen(it.noteId, it.subjectId, nav) }
                        entry<DrawingNav> { DrawingScreen(it.drawingId, nav) }
                        entry<EventEditorNav> { EventEditorScreen(it.eventId, it.dateEpochDay, nav) }
                        entry<RemindersNav> { RemindersScreen(nav) }
                        entry<ReminderEditorNav> { ReminderEditorScreen(it.reminderId, nav) }
                        entry<ProgressNav> { ProgressScreen(nav) }
                        entry<GoalsNav> { GoalsScreen(nav) }
                        entry<InsightsNav> { InsightsScreen(nav) }
                        entry<SettingsNav> { SettingsScreen(nav) }
                    }
                )
                // Scrim so scrolled content never runs under the status bar icons.
                androidx.compose.foundation.layout.Spacer(
                    Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}
