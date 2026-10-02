package com.example.routineforge.feature.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.theme.SubjectPalette
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.DatePickerDialogFF
import com.example.routineforge.core.ui.EmptyState
import com.example.routineforge.core.ui.FFCard
import com.example.routineforge.core.ui.LinearBar
import com.example.routineforge.core.ui.PickerField
import com.example.routineforge.core.ui.ProgressRing
import com.example.routineforge.core.ui.ScreenHeader
import com.example.routineforge.core.ui.SectionHeader
import com.example.routineforge.core.ui.Segmented
import com.example.routineforge.core.ui.ShowMessages
import com.example.routineforge.core.ui.StatBlock
import com.example.routineforge.core.ui.SubjectBadge
import com.example.routineforge.core.ui.SubjectIcons
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.feature.home.SubjectToday
import com.example.routineforge.feature.notes.NoteCard
import com.example.routineforge.feature.tasks.AddTaskDialog
import com.example.routineforge.feature.tasks.TaskRow
import com.example.routineforge.feature.timer.TimerViewModel
import com.example.routineforge.navigation.AppNav
import com.example.routineforge.navigation.NoteEditorNav
import com.example.routineforge.navigation.SubjectDetailNav
import com.example.routineforge.navigation.SubjectEditorNav
import com.example.routineforge.navigation.TimerNav
import java.time.LocalDate

@Composable
fun SubjectsScreen(nav: AppNav, timer: TimerViewModel) {
    val vm = appViewModel { SubjectsViewModel(it) }
    ShowMessages(vm)
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf<Subject?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Subjects", actions = {
            IconButton(onClick = { nav.push(SubjectEditorNav()) }) { Icon(Icons.Filled.Add, contentDescription = "Add subject") }
        })
        Segmented(SubjectFilter.entries, ui.filter, { it.label }, vm::setFilter, Modifier.padding(horizontal = 20.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!ui.loading && ui.rows.isEmpty()) {
                item {
                    when (ui.filter) {
                        SubjectFilter.ARCHIVED -> EmptyState(Icons.Filled.Archive, "Nothing archived", "Archived subjects keep their history but leave your daily list.")
                        else -> EmptyState(Icons.AutoMirrored.Filled.MenuBook, "No subjects yet", "Create a subject for each thing you study or work on.", actionLabel = "Add Subject", onAction = { nav.push(SubjectEditorNav()) })
                    }
                }
            }
            items(ui.rows, key = { it.subject.id }) { row ->
                SubjectListCard(
                    row,
                    onOpen = { nav.push(SubjectDetailNav(row.subject.id)) },
                    onStart = { timer.startForSubject(row.subject.id); nav.tab(TimerNav) },
                    onEdit = { nav.push(SubjectEditorNav(row.subject.id)) },
                    onArchive = { vm.setArchived(row.subject, !row.subject.isArchived) },
                    onDelete = { confirmDelete = row.subject }
                )
            }
        }
        Button(
            onClick = { nav.push(SubjectEditorNav()) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FF.colors.hero, contentColor = FF.colors.onHero)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Subject")
        }
    }

    confirmDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${s.name}?") },
            text = { Text("The subject is removed. Its focus sessions, notes and tasks are kept without a subject. To keep it but hide it, archive it instead.") },
            confirmButton = { TextButton(onClick = { vm.delete(s); confirmDelete = null }) { Text("Delete", color = FF.colors.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SubjectListCard(row: SubjectToday, onOpen: () -> Unit, onStart: () -> Unit, onEdit: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit) {
    val s = row.subject
    var menu by remember { mutableStateOf(false) }
    FFCard(Modifier.fillMaxWidth(), onClick = onOpen, contentPadding = PaddingValues(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(s.icon, s.color, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (s.description.isNotBlank()) Text(s.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                if (row.targetMinutes > 0) LinearBar(row.progress, color = Color(s.color), height = 5.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (row.targetMinutes > 0) "${TimeFormat.duration(row.focusedMs)} / ${TimeFormat.minutes(row.targetMinutes)} today · ${(row.progress * 100).toInt()}%"
                    else "${TimeFormat.duration(row.focusedMs)} today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!s.isArchived) {
                FilledIconButton(
                    onClick = onStart,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary)
                ) { Icon(Icons.Filled.PlayArrow, contentDescription = "Start timer for ${s.name}") }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options for ${s.name}") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Edit") }, { menu = false; onEdit() }, leadingIcon = { Icon(Icons.Filled.Edit, null) })
                    DropdownMenuItem(
                        { Text(if (s.isArchived) "Restore" else "Archive") }, { menu = false; onArchive() },
                        leadingIcon = { Icon(if (s.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive, null) }
                    )
                    DropdownMenuItem({ Text("Delete", color = FF.colors.danger) }, { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = FF.colors.danger) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectEditorScreen(subjectId: Long, nav: AppNav) {
    val vm = appViewModel(key = "subject-edit-$subjectId") { SubjectEditorViewModel(it, subjectId) }
    ShowMessages(vm)
    val f by vm.form.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    var pickExam by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(saved) { if (saved) nav.back() }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(if (subjectId == 0L) "New Subject" else "Edit Subject", onBack = nav::back, actions = {
            TextButton(onClick = vm::save, enabled = loaded) { Text("Save") }
        })
        if (!loaded) return@Column
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectBadge(f.icon, f.color, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                OutlinedTextField(
                    value = f.name, onValueChange = { v -> vm.update { it.copy(name = v.take(40)) } },
                    label = { Text("Name") }, singleLine = true, isError = f.name.isBlank() && f.id != 0L,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = f.description, onValueChange = { v -> vm.update { it.copy(description = v.take(200)) } },
                label = { Text("Description (optional)") }, placeholder = { Text("e.g. Computer Science") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Icon", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectIcons.forEach { (key, icon) ->
                    val selected = key == f.icon
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(f.color).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
                            .then(if (selected) Modifier.border(2.dp, Color(f.color), RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { vm.update { it.copy(icon = key) } }
                            .semantics { this.selected = selected; contentDescription = "Icon $key" },
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, contentDescription = null, tint = if (selected) Color(f.color) else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Text("Color", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SubjectPalette.forEachIndexed { i, c ->
                    val selected = c == f.color
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Color(c)).clickable { vm.update { it.copy(color = c) } }
                            .semantics { this.selected = selected; contentDescription = "Color ${i + 1}" },
                        contentAlignment = Alignment.Center
                    ) { if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White) }
                }
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily target", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(if (f.dailyMinutes == 0) "None" else TimeFormat.minutes(f.dailyMinutes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = f.dailyMinutes.toFloat(), onValueChange = { v -> vm.update { it.copy(dailyMinutes = (v / 15).toInt() * 15) } },
                    valueRange = 0f..600f
                )
            }
            Text("Longer targets (hours, optional)", style = MaterialTheme.typography.titleSmall)
            Text("Leave blank to use the daily target × days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoursField("Weekly", f.weeklyHours, Modifier.weight(1f)) { v -> vm.update { it.copy(weeklyHours = v) } }
                HoursField("Monthly", f.monthlyHours, Modifier.weight(1f)) { v -> vm.update { it.copy(monthlyHours = v) } }
                HoursField("Yearly", f.yearlyHours, Modifier.weight(1f)) { v -> vm.update { it.copy(yearlyHours = v) } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PickerField("Exam date", f.examDate?.let { TimeFormat.dayYear(it) } ?: "Not set", Icons.Filled.Event, Modifier.weight(1f)) { pickExam = true }
                if (f.examDate != null) IconButton(onClick = { vm.update { it.copy(examDate = null) } }) { Icon(Icons.Filled.Close, contentDescription = "Clear exam date") }
            }
            Text("Priority", style = MaterialTheme.typography.titleSmall)
            Segmented(listOf(0, 1, 2), f.priority, { listOf("Low", "Medium", "High")[it] }, { p -> vm.update { it.copy(priority = p) } })
            Spacer(Modifier.height(16.dp))
        }
    }
    if (pickExam) DatePickerDialogFF(f.examDate, { pickExam = false }) { d -> vm.update { it.copy(examDate = d) } }
}

@Composable
private fun HoursField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) },
        label = { Text(label) }, placeholder = { Text("auto") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier
    )
}

@Composable
fun SubjectDetailScreen(subjectId: Long, nav: AppNav, timer: TimerViewModel) {
    val vm = appViewModel(key = "subject-$subjectId") { SubjectDetailViewModel(it, subjectId) }
    ShowMessages(vm)
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var addTask by rememberSaveable { mutableStateOf(false) }
    val s = ui.subject
    val start: (Int?) -> Unit = { minutes -> timer.startForSubject(subjectId, minutes); nav.tab(TimerNav) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(s?.name ?: "", onBack = nav::back, subtitle = s?.description?.takeIf { it.isNotBlank() }, actions = {
            if (s != null) IconButton(onClick = { nav.push(SubjectEditorNav(subjectId)) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit subject") }
        })
        if (!ui.loading && s == null) {
            EmptyState(Icons.AutoMirrored.Filled.MenuBook, "Subject not found", "It may have been deleted.", actionLabel = "Go back", onAction = nav::back)
            return@Column
        }
        if (s == null) return@Column
        PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            listOf("Overview", "Timer", "Notes", "Tasks").forEachIndexed { i, t ->
                Tab(tab == i, { tab = i }, text = { Text(t) }, selectedContentColor = MaterialTheme.colorScheme.primary, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (tab) {
                0 -> {
                    item { GoalCard(ui) }
                    ui.daysToExam?.let { days ->
                        item {
                            Surface(shape = RoundedCornerShape(18.dp), color = FF.colors.danger.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Event, contentDescription = null, tint = FF.colors.danger)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Exam Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(TimeFormat.dayYear(LocalDate.ofEpochDay(s.examDate!!)), style = MaterialTheme.typography.titleMedium)
                                    }
                                    Surface(shape = RoundedCornerShape(12.dp), color = FF.colors.danger) {
                                        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(if (days >= 0) "$days" else "Done", style = MaterialTheme.typography.titleLarge, color = Color.White)
                                            Text(if (days == 1L) "day left" else if (days >= 0) "days left" else "", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        FFCard(Modifier.fillMaxWidth()) {
                            Row {
                                StatBlock(TimeFormat.duration(ui.todayMs), "Today", Modifier.weight(1f), accent = Color(s.color))
                                StatBlock(TimeFormat.duration(ui.weekMs), "This Week", Modifier.weight(1f), accent = FF.colors.warning)
                                StatBlock(TimeFormat.duration(ui.totalMs), "Total", Modifier.weight(1f), accent = FF.colors.success)
                            }
                        }
                    }
                    item { SectionHeader("Recent Sessions") }
                    if (ui.recent.isEmpty()) item { Text("Start your first focus session for this subject.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(ui.recent, key = { it.date.toEpochDay() }) { d ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(s.color)))
                            Spacer(Modifier.width(12.dp))
                            Text(if (d.date == ui.today) "Today" else TimeFormat.day(d.date), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("${d.sessions} session${if (d.sessions == 1) "" else "s"}  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(TimeFormat.duration(d.focusedMs), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    item { StartButton(s.isArchived) { start(null) } }
                }
                1 -> {
                    item { Text("Start a focus session for ${s.name}. It's saved to this subject automatically.", style = MaterialTheme.typography.bodyLarge) }
                    items(listOf(25, 45, 60, 90)) { m ->
                        OutlinedButton(onClick = { start(m) }, enabled = !s.isArchived, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start $m-minute session")
                        }
                    }
                }
                2 -> {
                    item {
                        OutlinedButton(onClick = { nav.push(NoteEditorNav(subjectId = subjectId)) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("New note")
                        }
                    }
                    if (ui.notes.isEmpty()) item { EmptyState(Icons.Filled.Edit, "No notes yet", "Notes you file under ${s.name} appear here.") }
                    items(ui.notes, key = { it.id }) { n -> NoteCard(n, s, onClick = { nav.push(NoteEditorNav(n.id)) }) }
                }
                else -> {
                    item {
                        OutlinedButton(onClick = { addTask = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Add task")
                        }
                    }
                    if (ui.tasks.isEmpty()) item { Text("No tasks for ${s.name}.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(ui.tasks, key = { it.id }) { t -> TaskRow(t, null, { vm.toggleTask(t.id, it) }, { vm.deleteTask(t.id) }) }
                }
            }
        }
    }
    if (addTask) AddTaskDialog(emptyList(), subjectId, onDismiss = { addTask = false }) { title, _, due -> vm.addTask(title, due); addTask = false }
}

@Composable
private fun GoalCard(ui: SubjectDetailUi) {
    val s = ui.subject ?: return
    val progress = com.example.routineforge.domain.FocusAnalytics.progress(ui.weekMs, ui.weekTargetMinutes)
    FFCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress, size = 96.dp, stroke = 10.dp, color = Color(s.color)) {
                Text(if (ui.weekTargetMinutes > 0) "${(progress * 100).toInt()}%" else "–", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text("Weekly Goal", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (ui.weekTargetMinutes > 0) "${TimeFormat.duration(ui.weekMs)} / ${TimeFormat.minutes(ui.weekTargetMinutes)}" else "No target set",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("This month: ${TimeFormat.duration(ui.monthMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StartButton(archived: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = !archived,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FF.colors.hero, contentColor = FF.colors.onHero)
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (archived) "Archived subject" else "Start Study Timer")
    }
}
