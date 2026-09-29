package com.example.routineforge.ui.schedule

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.routineforge.data.ScheduledRoutine
import java.time.LocalDate
import java.time.LocalDateTime

private const val DAY_MILLIS = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorScreen(
    viewModel: ScheduleViewModel,
    reminderId: String?,
    routineId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val original = remember(reminderId, routineId) { viewModel.draftFor(reminderId, routineId) }
    val isNew = original.id != reminderId
    var draft by remember { mutableStateOf(original) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val next = remember(draft) { draft.copy(isCompleted = false).nextOccurrence(LocalDateTime.now()) }
    val canSave = draft.routineTitle.isNotBlank() && next != null

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                },
                title = { Text(if (isNew) "New reminder" else "Edit reminder") },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            viewModel.save(draft.copy(isCompleted = false))
                            onNavigateBack()
                        }
                    ) { Text("Save", fontWeight = FontWeight.SemiBold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 680.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = draft.routineTitle,
                onValueChange = { draft = draft.copy(routineTitle = it.take(80)) },
                label = { Text("Title") },
                placeholder = { Text("What should I remind you about?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                        Text(dayLabel(LocalDate.ofEpochDay(draft.dateEpochDay)), Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null)
                        Text(formatTime(draft.timeOfDay), Modifier.padding(start = 8.dp))
                    }
                }
                if (next == null) {
                    Text(
                        "This time has passed. Pick a future date or time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
                } else if (draft.isRepeating) {
                    Text(
                        "Next: ${dayLabel(next.toLocalDate())} at ${formatTime(draft.timeOfDay)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
                }
            }

            Section("Remind me early") {
                listOf(0, 5, 15, 30, 60).forEach { m ->
                    FilterChip(
                        selected = draft.remindBeforeMinutes == m,
                        onClick = { draft = draft.copy(remindBeforeMinutes = m) },
                        label = { Text(if (m == 0) "Off" else if (m < 60) "$m min" else "1 hour") }
                    )
                }
            }

            Section("Repeat") {
                ScheduledRoutine.REPEAT_OPTIONS.forEach { r ->
                    FilterChip(
                        selected = draft.repeat == r,
                        onClick = { draft = draft.copy(repeat = r) },
                        label = { Text(repeatLabel(r)) }
                    )
                }
            }

            Column {
                SectionTitle("Alert")
                // Never silent: switching one off while the other is off turns the other back on.
                AlertSwitch("Sound", draft.soundEnabled) {
                    draft = draft.copy(soundEnabled = it, vibrationEnabled = draft.vibrationEnabled || !it)
                }
                AlertSwitch("Vibration", draft.vibrationEnabled) {
                    draft = draft.copy(vibrationEnabled = it, soundEnabled = draft.soundEnabled || !it)
                }
            }

            Section("Timer from notification") {
                (listOf(0, 5, 15, 25, 30, 60) + draft.durationMinutes).distinct().sorted().forEach { m ->
                    FilterChip(
                        selected = draft.durationMinutes == m,
                        onClick = { draft = draft.copy(durationMinutes = m) },
                        label = { Text(if (m == 0) "None" else "$m min") }
                    )
                }
            }

            OutlinedTextField(
                value = draft.note,
                onValueChange = { draft = draft.copy(note = it.take(500)) },
                label = { Text("Note (optional)") },
                placeholder = { Text("Shown in the notification") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isNew) {
                HorizontalDivider()
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete reminder", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = draft.dateEpochDay * DAY_MILLIS)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    // The picker returns UTC midnight, so dividing by a day gives the epoch day directly.
                    state.selectedDateMillis?.let { draft = draft.copy(dateEpochDay = it / DAY_MILLIS) }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    if (pickTime) {
        val (h, m) = draft.timeOfDay.split(":").map { it.toInt() }
        val state = rememberTimePickerState(h, m, DateFormat.is24HourFormat(LocalContext.current))
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    draft = draft.copy(timeOfDay = "%02d:%02d".format(state.hour, state.minute))
                    pickTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete reminder?") },
            text = { Text("You won't be notified for \"${draft.routineTitle}\" anymore.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(original.id)
                    onNavigateBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(title: String, chips: @Composable () -> Unit) {
    Column {
        SectionTitle(title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    }
}

@Composable
private fun AlertSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onChange(!checked) }
    )
}
