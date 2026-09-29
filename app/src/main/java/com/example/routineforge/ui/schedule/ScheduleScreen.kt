package com.example.routineforge.ui.schedule

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.routineforge.data.ScheduledRoutine
import com.example.routineforge.service.CalendarAlarmScheduler
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onAddReminder: () -> Unit,
    onEditReminder: (String) -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showPast by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    // Re-check permissions whenever the user comes back (e.g. from system settings).
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeTick++
        onPauseOrDispose { }
    }
    val notificationsOn = remember(resumeTick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val exactOn = remember(resumeTick) { CalendarAlarmScheduler.canScheduleExact(context) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Schedule", fontWeight = FontWeight.SemiBold) },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Routines") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                                onClick = { showMenu = false; onOpenRoutines() }
                            )
                            DropdownMenuItem(
                                text = { Text("History & stats") },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                                onClick = { showMenu = false; onOpenHistory() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (themeMode == "DARK") "Light theme" else "Dark theme") },
                                leadingIcon = {
                                    Icon(
                                        if (themeMode == "DARK") Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = null
                                    )
                                },
                                onClick = { showMenu = false; viewModel.toggleTheme() }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear all data", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { showMenu = false; confirmClear = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddReminder,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New reminder") }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!notificationsOn) {
                    item {
                        PermissionBanner("Notifications are off, so reminders can't alert you.") {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }
                    }
                }
                if (!exactOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item {
                        PermissionBanner("Allow exact alarms so reminders arrive on time.") {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .setData(android.net.Uri.parse("package:${context.packageName}"))
                            )
                        }
                    }
                }

                if (state.upcoming.isEmpty() && state.past.isEmpty()) {
                    item { EmptySchedule() }
                }

                state.upcoming.groupBy { it.next!!.toLocalDate() }.forEach { (date, entries) ->
                    item(key = "h$date") { DayHeader(dayLabel(date)) }
                    items(entries, key = { it.item.id }) { entry ->
                        ReminderRow(
                            entry = entry,
                            onClick = { onEditReminder(entry.item.id) },
                            onDoneChange = { viewModel.setDone(entry.item, it) }
                        )
                    }
                }

                if (state.past.isNotEmpty()) {
                    item(key = "past") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPast = !showPast }
                                .padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Done & past (${state.past.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (showPast) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (showPast) "Hide" else "Show"
                            )
                        }
                    }
                    if (showPast) {
                        items(state.past, key = { it.item.id }) { entry ->
                            ReminderRow(
                                entry = entry,
                                onClick = { onEditReminder(entry.item.id) },
                                onDoneChange = { viewModel.setDone(entry.item, it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all data?") },
            text = { Text("This permanently deletes all reminders, notes, routines and history.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; viewModel.clearAllData() }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PermissionBanner(message: String, onFix: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onFix,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Turn on") }
        }
    }
}

@Composable
private fun DayHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun ReminderRow(entry: ScheduleEntry, onClick: () -> Unit, onDoneChange: (Boolean) -> Unit) {
    val item = entry.item
    val faded = entry.next == null
    val details = listOfNotNull(
        repeatLabel(item.repeat).takeIf { item.isRepeating },
        "Vibrate only".takeIf { !item.soundEnabled },
        "${item.durationMinutes}-min timer".takeIf { item.durationMinutes > 0 },
        item.note.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            overlineContent = {
                Text(
                    (if (faded) dayLabel(LocalDate.ofEpochDay(item.dateEpochDay)) + " · " else "") +
                        formatTime(item.timeOfDay)
                )
            },
            headlineContent = {
                Text(
                    item.routineTitle.ifBlank { item.categoryName },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (faded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = if (details.isNotEmpty()) {
                { Text(details, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            } else null,
            trailingContent = {
                Checkbox(checked = item.isCompleted, onCheckedChange = onDoneChange)
            }
        )
    }
}

@Composable
private fun EmptySchedule() {
    Column(
        Modifier.fillMaxWidth().padding(top = 96.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.EventAvailable,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("No reminders yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap New reminder to get notified on any date.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(
            DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE, d MMM" else "EEE, d MMM yyyy")
        )
    }
}

internal fun formatTime(timeOfDay: String): String =
    runCatching { LocalTime.parse(timeOfDay).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) }
        .getOrDefault(timeOfDay)

internal fun repeatLabel(repeat: String): String = when (repeat) {
    ScheduledRoutine.REPEAT_DAILY -> "Daily"
    ScheduledRoutine.REPEAT_WEEKLY -> "Weekly"
    ScheduledRoutine.REPEAT_MONTHLY -> "Monthly"
    else -> "Never"
}
