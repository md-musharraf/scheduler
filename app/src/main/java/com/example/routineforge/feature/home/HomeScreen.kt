package com.example.routineforge.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.ActionRow
import com.example.routineforge.core.ui.EmptyState
import com.example.routineforge.core.ui.FFCard
import com.example.routineforge.core.ui.LinearBar
import com.example.routineforge.core.ui.ProgressRing
import com.example.routineforge.core.ui.SectionHeader
import com.example.routineforge.core.ui.ShowMessages
import com.example.routineforge.core.ui.SubjectBadge
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.feature.tasks.AddTaskDialog
import com.example.routineforge.feature.tasks.TaskRow
import com.example.routineforge.feature.timer.TimerViewModel
import com.example.routineforge.navigation.AppNav
import com.example.routineforge.navigation.CalendarNav
import com.example.routineforge.navigation.EventEditorNav
import com.example.routineforge.navigation.GoalsNav
import com.example.routineforge.navigation.InsightsNav
import com.example.routineforge.navigation.NoteEditorNav
import com.example.routineforge.navigation.NotesNav
import com.example.routineforge.navigation.ProgressNav
import com.example.routineforge.navigation.ReminderEditorNav
import com.example.routineforge.navigation.RemindersNav
import com.example.routineforge.navigation.SettingsNav
import com.example.routineforge.navigation.SubjectDetailNav
import com.example.routineforge.navigation.SubjectEditorNav
import com.example.routineforge.navigation.SubjectsNav
import com.example.routineforge.navigation.TimerNav
import java.time.LocalDate
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: AppNav, timer: TimerViewModel) {
    val vm = appViewModel { HomeViewModel(it) }
    ShowMessages(vm)
    val ui by vm.ui.collectAsStateWithLifecycle()
    var addTask by rememberSaveable { mutableStateOf(false) }
    var quickAdd by rememberSaveable { mutableStateOf(false) }
    val startTimer: (Long?) -> Unit = { id -> timer.startForSubject(id); nav.tab(TimerNav) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            item { Header(ui, vm.greeting(), onBell = { nav.push(RemindersNav) }, onProfile = { nav.push(SettingsNav) }) }
            item { WeekStrip(ui, vm::select, vm::shiftWeek) }
            item { FocusHeroCard(ui, Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(Icons.Filled.PlayArrow, "Start Timer", MaterialTheme.colorScheme.primary, Modifier.weight(1f), filled = true) { nav.tab(TimerNav) }
                    QuickAction(Icons.Filled.Add, "Add Task", MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { addTask = true }
                    QuickAction(Icons.Filled.EditNote, "Notes", MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { nav.tab(NotesNav) }
                    QuickAction(Icons.Filled.CalendarMonth, "Calendar", MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { nav.tab(CalendarNav) }
                }
            }
            item {
                SectionHeader(
                    if (ui.selected == ui.today) "Today's Subjects" else "Subjects on ${TimeFormat.day(ui.selected)}",
                    Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp),
                    action = if (ui.subjects.isNotEmpty()) "See All" else null,
                    onAction = { nav.tab(SubjectsNav) }
                )
            }
            if (!ui.loading && ui.subjects.isEmpty()) {
                item {
                    EmptyState(
                        Icons.AutoMirrored.Filled.MenuBook, "No subjects yet",
                        "Add what you study or work on, then track time for each.",
                        actionLabel = "Add Subject", onAction = { nav.push(SubjectEditorNav()) }
                    )
                }
            }
            items(ui.subjects, key = { it.subject.id }) { s ->
                SubjectTodayCard(s, onOpen = { nav.push(SubjectDetailNav(s.subject.id)) }, onStart = { startTimer(s.subject.id) }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp))
            }
            item {
                SectionHeader(
                    if (ui.selected == ui.today) "Today's Tasks" else "Tasks",
                    Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    action = "Add", onAction = { addTask = true }
                )
            }
            if (ui.tasks.isEmpty()) {
                item {
                    Text(
                        "No tasks for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
            items(ui.tasks, key = { "t" + it.id }) { t ->
                TaskRow(t, ui.allSubjects.firstOrNull { it.id == t.subjectId }, { vm.toggleTask(t.id, it) }, { vm.deleteTask(t.id) }, Modifier.padding(horizontal = 8.dp))
            }
            item { SectionHeader("Explore", Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp)) }
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExploreTile(Icons.Filled.BarChart, "Progress", "Day to year", Color(0xFF5B5FEF), Modifier.weight(1f)) { nav.push(ProgressNav) }
                        ExploreTile(Icons.Filled.EmojiEvents, "Goals", "Your targets", Color(0xFFF2994A), Modifier.weight(1f)) { nav.push(GoalsNav) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExploreTile(Icons.Filled.Lightbulb, "Insights", "Your patterns", Color(0xFF22B59A), Modifier.weight(1f)) { nav.push(InsightsNav) }
                        ExploreTile(Icons.Filled.NotificationsActive, "Reminders", "Alerts you set", Color(0xFF4C8DF6), Modifier.weight(1f)) { nav.push(RemindersNav) }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { quickAdd = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "Create") }
    }

    if (addTask) {
        AddTaskDialog(ui.allSubjects, initialDate = ui.selected, onDismiss = { addTask = false }) { title, sid, due ->
            vm.addTask(title, sid, due)
            addTask = false
        }
    }

    if (quickAdd) {
        ModalBottomSheet(onDismissRequest = { quickAdd = false }) {
            Column(Modifier.padding(horizontal = 12.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
                Text("Create", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                val go: (() -> Unit) -> Unit = { action -> quickAdd = false; action() }
                ActionRow(Icons.AutoMirrored.Filled.MenuBook, "Add Subject", subtitle = "Something you study or work on") { go { nav.push(SubjectEditorNav()) } }
                ActionRow(Icons.Filled.Timer, "Start Timer", subtitle = "Begin a focus session", tint = FF.colors.success) { go { nav.tab(TimerNav) } }
                ActionRow(Icons.Filled.EditNote, "Create Note", tint = Color(0xFF8B5CF6)) { go { nav.push(NoteEditorNav()) } }
                ActionRow(Icons.Filled.AddTask, "Add Task", tint = FF.colors.warning) { go { addTask = true } }
                ActionRow(Icons.Filled.Notifications, "Add Reminder", tint = Color(0xFF4C8DF6)) { go { nav.push(ReminderEditorNav()) } }
                ActionRow(Icons.Filled.Event, "Create Event", tint = FF.colors.danger) { go { nav.push(EventEditorNav(dateEpochDay = ui.selected.toEpochDay())) } }
            }
        }
    }
}

@Composable
private fun Header(ui: HomeUi, greeting: String, onBell: () -> Unit, onProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (ui.name.isBlank()) "$greeting!" else "$greeting,", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (ui.name.isNotBlank()) Text(ui.name, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(TimeFormat.dayYear(ui.today).let { "${ui.today.dayOfWeek.getDisplayName(TextStyle.SHORT, LocalConfiguration.current.locales[0])}, $it" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onBell) {
            BadgedBox(badge = { if (ui.remindersToday > 0) Badge { Text("${ui.remindersToday}") } }) {
                Icon(Icons.Filled.Notifications, contentDescription = if (ui.remindersToday > 0) "Reminders, ${ui.remindersToday} today" else "Reminders")
            }
        }
        Surface(onClick = onProfile, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp).semantics { contentDescription = "Profile and settings" }) {
            Box(contentAlignment = Alignment.Center) {
                Text(ui.name.trim().firstOrNull()?.uppercase() ?: "F", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun WeekStrip(ui: HomeUi, onSelect: (LocalDate) -> Unit, onShift: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShift(-1) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous week") }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ui.week.forEach { d ->
                val selected = d == ui.selected
                val isToday = d == ui.today
                Surface(
                    onClick = { onSelect(d) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, FF.colors.cardBorder),
                    modifier = Modifier.weight(1f).semantics { this.selected = selected; contentDescription = TimeFormat.day(d) + if (isToday) ", today" else "" }
                ) {
                    Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(d.dayOfWeek.getDisplayName(TextStyle.SHORT, LocalConfiguration.current.locales[0]), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        Text(
                            "${d.dayOfMonth}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (!selected && isToday) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Unspecified
                        )
                    }
                }
            }
        }
        IconButton(onClick = { onShift(1) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next week") }
    }
}

@Composable
private fun FocusHeroCard(ui: HomeUi, modifier: Modifier = Modifier) {
    val c = FF.colors
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = c.hero, contentColor = c.onHero) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (ui.selected == ui.today) "Today's Focus" else TimeFormat.day(ui.selected), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                if (ui.goalsTotal > 0) {
                    Text("${ui.goalsDone}/${ui.goalsTotal}", style = MaterialTheme.typography.headlineLarge)
                    Text("goals completed", style = MaterialTheme.typography.bodyMedium, color = c.onHeroMuted)
                } else {
                    Text(TimeFormat.duration(ui.focusedMs), style = MaterialTheme.typography.headlineLarge)
                    Text("focused", style = MaterialTheme.typography.bodyMedium, color = c.onHeroMuted)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        ui.goalsTotal > 0 && ui.dailyTargetMinutes > 0 -> "${TimeFormat.duration(ui.focusedMs)} focused · Goal ${TimeFormat.minutes(ui.dailyTargetMinutes)}"
                        ui.goalsTotal > 0 -> "${TimeFormat.duration(ui.focusedMs)} focused"
                        ui.dailyTargetMinutes > 0 -> "Daily goal ${TimeFormat.minutes(ui.dailyTargetMinutes)}"
                        else -> "Set a daily goal in Goals"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onHeroMuted
                )
                if (ui.streak > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.clip(CircleShape).background(c.onHero.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = c.warning, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${ui.streak}-day streak", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (ui.dailyTargetMinutes > 0) {
                ProgressRing(ui.progress, size = 100.dp, stroke = 10.dp, color = c.success, track = c.onHero.copy(alpha = 0.15f)) {
                    Text("${(ui.progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, tint: Color, modifier: Modifier = Modifier, filled: Boolean = false, onClick: () -> Unit) {
    FFCard(modifier.heightIn(min = 92.dp), onClick = onClick, contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (filled) tint else tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = if (filled) Color.White else tint) }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
fun SubjectTodayCard(s: SubjectToday, onOpen: () -> Unit, onStart: () -> Unit, modifier: Modifier = Modifier) {
    FFCard(modifier.fillMaxWidth(), onClick = onOpen, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(s.subject.icon, s.subject.color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.subject.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                if (s.targetMinutes > 0) LinearBar(s.progress, color = Color(s.subject.color))
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(
                        if (s.targetMinutes > 0) "${TimeFormat.duration(s.focusedMs)} / ${TimeFormat.minutes(s.targetMinutes)}" else "${TimeFormat.duration(s.focusedMs)} focused",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (s.targetMinutes > 0) Text("${(s.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onStart,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary)
            ) { Icon(Icons.Filled.PlayArrow, contentDescription = "Start timer for ${s.subject.name}") }
        }
    }
}

@Composable
private fun ExploreTile(icon: ImageVector, title: String, subtitle: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FFCard(modifier, onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.example.routineforge.core.ui.IconBadge(icon, tint, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}
