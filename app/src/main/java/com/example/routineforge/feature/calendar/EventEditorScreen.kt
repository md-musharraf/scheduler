package com.example.routineforge.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.core.database.EventCategory
import com.example.routineforge.core.database.RepeatRule
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.DatePickerDialogFF
import com.example.routineforge.core.ui.PickerField
import com.example.routineforge.core.ui.ScreenHeader
import com.example.routineforge.core.ui.ShowMessages
import com.example.routineforge.core.ui.TimePickerDialogFF
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.navigation.AppNav
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventEditorScreen(eventId: Long, dateEpochDay: Long?, nav: AppNav) {
    val vm = appViewModel(key = "event-$eventId-$dateEpochDay") { EventEditorViewModel(it, eventId, dateEpochDay?.let(LocalDate::ofEpochDay)) }
    ShowMessages(vm)
    val f by vm.form.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var reminderMenu by remember { mutableStateOf(false) }
    LaunchedEffect(done) { if (done) nav.back() }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(if (eventId == 0L) "New Event" else "Edit Event", onBack = nav::back, actions = {
            if (eventId != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete event") }
            TextButton(onClick = vm::save, enabled = loaded) { Text("Save") }
        })
        if (!loaded) return@Column
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                f.title, { v -> vm.update { it.copy(title = v.take(80)) } }, label = { Text("Title") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth()
            )
            PickerField("Date", TimeFormat.dayYear(f.date), Icons.Filled.Event, Modifier.fillMaxWidth()) { picker = "date" }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField("Starts", TimeFormat.time(f.date.atTime(f.start)), Icons.Filled.Schedule, Modifier.weight(1f)) { picker = "start" }
                PickerField("Ends", TimeFormat.time(f.date.atTime(f.end)) + if (!f.end.isAfter(f.start)) " (+1 day)" else "", Icons.Filled.Schedule, Modifier.weight(1f)) { picker = "end" }
            }
            Text("Category", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventCategory.entries.forEach { cat ->
                    FilterChip(
                        f.category == cat, { vm.update { it.copy(category = cat) } }, { Text(cat.label) },
                        leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(Color(cat.color))) }
                    )
                }
            }
            Text("Repeat", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatRule.entries.forEach { r -> FilterChip(f.repeat == r, { vm.update { it.copy(repeat = r) } }, { Text(if (r == RepeatRule.NONE) "Once" else r.label) }) }
            }
            if (f.repeat != RepeatRule.NONE) {
                Text("Changes apply to every occurrence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                PickerField("Reminder", REMINDER_OPTIONS.first { it.first == f.remindBefore }.second, Icons.Filled.Notifications, Modifier.fillMaxWidth()) { reminderMenu = true }
                DropdownMenu(reminderMenu, { reminderMenu = false }) {
                    REMINDER_OPTIONS.forEach { (m, label) -> DropdownMenuItem({ Text(label) }, { vm.update { it.copy(remindBefore = m) }; reminderMenu = false }) }
                }
            }
            OutlinedTextField(
                f.description, { v -> vm.update { it.copy(description = v.take(500)) } }, label = { Text("Notes (optional)") },
                minLines = 3, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    when (picker) {
        "date" -> DatePickerDialogFF(f.date, { picker = null }) { d -> vm.update { it.copy(date = d) } }
        "start" -> TimePickerDialogFF(f.start, { picker = null }) { t ->
            // Keep the event's length when moving its start.
            val length = java.time.Duration.between(f.start, f.end).let { if (it.isNegative || it.isZero) it.plusDays(1) else it }
            vm.update { it.copy(start = t, end = t.plus(length)) }
        }
        "end" -> TimePickerDialogFF(f.end, { picker = null }) { t -> vm.update { it.copy(end = t) } }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete event?") },
            text = { Text(if (f.repeat != RepeatRule.NONE) "All occurrences and the event's reminder will be deleted." else "The event and its reminder will be deleted.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("Delete", color = FF.colors.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
