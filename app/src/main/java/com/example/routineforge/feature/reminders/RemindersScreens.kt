package com.example.routineforge.feature.reminders

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.routineforge.AppContainer
import com.example.routineforge.core.database.Reminder
import com.example.routineforge.core.database.ReminderCategory
import com.example.routineforge.core.database.RepeatRule
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.notifications.Notifier
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.DatePickerDialogFF
import com.example.routineforge.core.ui.EmptyState
import com.example.routineforge.core.ui.FFCard
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.core.ui.PickerField
import com.example.routineforge.core.ui.ScreenHeader
import com.example.routineforge.core.ui.Segmented
import com.example.routineforge.core.ui.ShowMessages
import com.example.routineforge.core.ui.TimePickerDialogFF
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.core.ui.todayFlow
import com.example.routineforge.navigation.AppNav
import com.example.routineforge.navigation.ReminderEditorNav
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Immutable
data class ReminderRow(val reminder: Reminder, val next: LocalDateTime?)

@Immutable
data class RemindersUi(
    val loading: Boolean = true,
    val filter: ReminderCategory? = null,
    val sections: List<Pair<String, List<ReminderRow>>> = emptyList(),
    val total: Int = 0
)

class RemindersViewModel(private val c: AppContainer) : FFViewModel() {
    private val filter = MutableStateFlow<ReminderCategory?>(null)

    val ui: StateFlow<RemindersUi> = combine(c.reminders.reminders, filter, todayFlow()) { list, f, today ->
        val rows = list.filter { f == null || it.category == f }.map { r ->
            ReminderRow(r, c.reminders.nextTrigger(r)?.let { TimeFormat.toLocal(it) })
        }
        val (upcoming, inactive) = rows.partition { it.next != null }
        val sorted = upcoming.sortedBy { it.next }
        val sections = listOf(
            "Today" to sorted.filter { it.next!!.toLocalDate() == today },
            "Tomorrow" to sorted.filter { it.next!!.toLocalDate() == today.plusDays(1) },
            "Later" to sorted.filter { it.next!!.toLocalDate() > today.plusDays(1) },
            "Done & past" to inactive.sortedByDescending { it.reminder.triggerTime }
        ).filter { it.second.isNotEmpty() }
        RemindersUi(false, f, sections, list.size)
    }.stateIn(RemindersUi())

    fun setFilter(f: ReminderCategory?) { filter.value = f }
    fun setDone(r: Reminder, done: Boolean) = launchSafe { c.reminders.setDone(r.id, done) }
    fun setEnabled(r: Reminder, enabled: Boolean) = launchSafe {
        if (enabled && r.repeatRule == RepeatRule.NONE && r.triggerTime <= System.currentTimeMillis()) {
            say("This reminder's time has passed. Edit it to pick a new time.")
            return@launchSafe
        }
        c.reminders.setEnabled(r.id, enabled)
    }
    fun delete(r: Reminder) = launchSafe {
        c.reminders.delete(r.id)
        say("Reminder deleted")
    }
}

@Composable
fun RemindersScreen(nav: AppNav) {
    val vm = appViewModel { RemindersViewModel(it) }
    ShowMessages(vm)
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf<Reminder?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Reminders", onBack = nav::back)
        PermissionBanners()
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(ui.filter == null, { vm.setFilter(null) }, { Text("All") }) }
            items(ReminderCategory.entries) { cat -> FilterChip(ui.filter == cat, { vm.setFilter(cat) }, { Text(cat.label) }) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            if (!ui.loading && ui.sections.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.Notifications, if (ui.total == 0) "No reminders yet" else "Nothing in this category",
                        "Reminders notify you even when FocusFlow is closed.", actionLabel = "Add Reminder", onAction = { nav.push(ReminderEditorNav()) }
                    )
                }
            }
            ui.sections.forEach { (title, rows) ->
                item(key = "h-$title") { Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp)) }
                items(rows, key = { it.reminder.id }) { row ->
                    ReminderItem(row, onDone = { vm.setDone(row.reminder, it) }, onEnabled = { vm.setEnabled(row.reminder, it) },
                        onEdit = { nav.push(ReminderEditorNav(row.reminder.id)) }, onDelete = { confirmDelete = row.reminder })
                }
            }
        }
        Button(
            onClick = { nav.push(ReminderEditorNav()) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FF.colors.hero, contentColor = FF.colors.onHero)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Reminder")
        }
    }
    confirmDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete reminder?") },
            text = { Text("\"${r.title}\" won't notify you again.") },
            confirmButton = { TextButton(onClick = { vm.delete(r); confirmDelete = null }) { Text("Delete", color = FF.colors.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ReminderItem(row: ReminderRow, onDone: (Boolean) -> Unit, onEnabled: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val r = row.reminder
    var menu by remember { mutableStateOf(false) }
    val inactive = row.next == null
    Surface(onClick = onEdit, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = r.isDone, onCheckedChange = onDone)
            Icon(
                if (r.enabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff, contentDescription = null,
                tint = when {
                    !r.enabled || inactive -> MaterialTheme.colorScheme.outline
                    r.priority >= 2 -> FF.colors.danger
                    else -> FF.colors.warning
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    r.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (r.isDone) TextDecoration.LineThrough else null,
                    color = if (inactive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                val at = row.next ?: TimeFormat.toLocal(r.triggerTime)
                Text(
                    listOfNotNull(
                        if (row.next != null && at.toLocalDate() != LocalDate.now()) TimeFormat.day(at.toLocalDate()) else if (row.next == null) TimeFormat.day(at.toLocalDate()) else null,
                        TimeFormat.time(at),
                        r.repeatRule.takeIf { it != RepeatRule.NONE }?.label,
                        r.category.label
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (r.priority >= 2) Icon(Icons.Filled.Flag, contentDescription = "High priority", tint = FF.colors.danger, modifier = Modifier.size(18.dp))
            Switch(checked = r.enabled && !r.isDone, onCheckedChange = onEnabled, enabled = !r.isDone, modifier = Modifier.padding(horizontal = 4.dp))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options for ${r.title}") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Edit") }, { menu = false; onEdit() }, leadingIcon = { Icon(Icons.Filled.Edit, null) })
                    DropdownMenuItem({ Text("Delete", color = FF.colors.danger) }, { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = FF.colors.danger) })
                }
            }
        }
    }
}

/** Explains and fixes missing notification / exact-alarm permission. Re-checked on every resume. */
@Composable
fun PermissionBanners() {
    val context = LocalContext.current
    var canPost by remember { mutableStateOf(Notifier.canPost(context)) }
    var canExact by remember { mutableStateOf(Notifier.canScheduleExact(context)) }
    var asked by rememberSaveable { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        canPost = Notifier.canPost(context)
        canExact = Notifier.canScheduleExact(context)
        onPauseOrDispose {}
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canPost = granted && Notifier.canPost(context)
        asked++
    }
    val openNotificationSettings = {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    if (!canPost) {
        Banner(
            Icons.Filled.NotificationsOff, "Notifications are off",
            "Reminders and timer alerts can't reach you.", "Allow"
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && asked == 0) request.launch(Manifest.permission.POST_NOTIFICATIONS)
            else openNotificationSettings()
        }
    }
    if (canPost && !canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Banner(Icons.Filled.Alarm, "Exact timing is off", "Reminders may arrive a few minutes late.", "Allow") {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
private fun Banner(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, action: String, onAction: () -> Unit) {
    FFCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), color = FF.colors.warning.copy(alpha = 0.12f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = FF.colors.warning)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Immutable
data class ReminderForm(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0),
    val repeat: RepeatRule = RepeatRule.NONE,
    val category: ReminderCategory = ReminderCategory.STUDY,
    val priority: Int = 1,
    val subjectId: Long? = null,
    val enabled: Boolean = true,
    val original: Reminder? = null
)

class ReminderEditorViewModel(private val c: AppContainer, private val id: Long) : FFViewModel() {
    val form = MutableStateFlow(ReminderForm().let { if (it.time < LocalTime.now()) it.copy(date = LocalDate.now().plusDays(1)) else it })
    val loaded = MutableStateFlow(id == 0L)
    val done = MutableStateFlow(false)
    val subjects: StateFlow<List<Subject>> = c.subjects.subjects.stateIn(emptyList())

    init {
        if (id != 0L) viewModelScope.launch {
            val r = c.reminders.get(id)
            if (r == null) {
                say("This reminder no longer exists")
                done.value = true
                return@launch
            }
            val t = TimeFormat.toLocal(r.triggerTime)
            form.value = ReminderForm(r.id, r.title, r.description, t.toLocalDate(), t.toLocalTime(), r.repeatRule, r.category, r.priority, r.subjectId, r.enabled, r)
            loaded.value = true
        }
    }

    fun update(t: (ReminderForm) -> ReminderForm) { form.value = t(form.value) }

    fun save() = launchSafe {
        val f = form.value
        val trigger = TimeFormat.toMillis(f.date.atTime(f.time))
        val base = f.original ?: Reminder(title = f.title, triggerTime = trigger)
        c.reminders.save(
            base.copy(
                title = f.title, description = f.description, triggerTime = trigger, repeatRule = f.repeat,
                category = f.category, priority = f.priority, subjectId = f.subjectId,
                // Saving an edited reminder turns it back on.
                enabled = true, isDone = false
            )
        )
        say(if (f.id == 0L) "Reminder set for ${TimeFormat.day(f.date)}, ${TimeFormat.time(f.date.atTime(f.time))}" else "Reminder updated")
        done.value = true
    }

    fun delete() = launchSafe {
        c.reminders.delete(id)
        say("Reminder deleted")
        done.value = true
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorScreen(reminderId: Long, nav: AppNav) {
    val vm = appViewModel(key = "reminder-$reminderId") { ReminderEditorViewModel(it, reminderId) }
    ShowMessages(vm)
    val f by vm.form.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(done) { if (done) nav.back() }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(if (reminderId == 0L) "New Reminder" else "Edit Reminder", onBack = nav::back, actions = {
            if (reminderId != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete reminder") }
            TextButton(onClick = vm::save, enabled = loaded) { Text("Save") }
        })
        if (!loaded) return@Column
        PermissionBanners()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                f.title, { v -> vm.update { it.copy(title = v.take(80)) } }, label = { Text("Title") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField("Date", TimeFormat.dayYear(f.date), Icons.Filled.Event, Modifier.weight(1f)) { picker = "date" }
                PickerField("Time", TimeFormat.time(f.date.atTime(f.time)), Icons.Filled.Schedule, Modifier.weight(1f)) { picker = "time" }
            }
            Text("Repeat", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatRule.entries.forEach { r -> FilterChip(f.repeat == r, { vm.update { it.copy(repeat = r) } }, { Text(if (r == RepeatRule.NONE) "Once" else r.label) }) }
            }
            Text("Category", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderCategory.entries.forEach { cat -> FilterChip(f.category == cat, { vm.update { it.copy(category = cat) } }, { Text(cat.label) }) }
            }
            Text("Priority", style = MaterialTheme.typography.titleSmall)
            Segmented(listOf(0, 1, 2), f.priority, { listOf("Low", "Normal", "High")[it] }, { p -> vm.update { it.copy(priority = p) } })
            if (subjects.any { !it.isArchived }) {
                Text("Subject (optional)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(f.subjectId == null, { vm.update { it.copy(subjectId = null) } }, { Text("None") })
                    subjects.filter { !it.isArchived }.forEach { s -> FilterChip(f.subjectId == s.id, { vm.update { it.copy(subjectId = s.id) } }, { Text(s.name) }) }
                }
            }
            OutlinedTextField(
                f.description, { v -> vm.update { it.copy(description = v.take(300)) } }, label = { Text("Details (optional)") },
                minLines = 2, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }
    }
    when (picker) {
        "date" -> DatePickerDialogFF(f.date, { picker = null }) { d -> vm.update { it.copy(date = d) } }
        "time" -> TimePickerDialogFF(f.time, { picker = null }) { t -> vm.update { it.copy(time = t) } }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete reminder?") },
            text = { Text("It will be cancelled and removed.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("Delete", color = FF.colors.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
