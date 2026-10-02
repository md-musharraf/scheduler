package com.example.routineforge.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.core.database.RepeatRule
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.EmptyState
import com.example.routineforge.core.ui.ScreenHeader
import com.example.routineforge.core.ui.Segmented
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.data.EventOccurrence
import com.example.routineforge.domain.StatsPeriod
import com.example.routineforge.navigation.AppNav
import com.example.routineforge.navigation.EventEditorNav
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(nav: AppNav) {
    val vm = appViewModel { CalendarViewModel(it) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val open: (EventOccurrence) -> Unit = { nav.push(EventEditorNav(it.event.id)) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Calendar", actions = {
                TextButton(onClick = vm::goToday) { Text("Today") }
            })
            Segmented(CalendarView.entries, ui.view, { it.label }, vm::setView, Modifier.padding(horizontal = 20.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.shift(-1) }) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous") }
                Text(
                    when (ui.view) {
                        CalendarView.MONTH -> TimeFormat.monthYear(ui.selected)
                        CalendarView.WEEK -> StatsPeriod.WEEK.range(ui.selected).let { "${TimeFormat.day(it.start)} – ${TimeFormat.day(it.endExclusive.minusDays(1))}" }
                        CalendarView.DAY -> TimeFormat.day(ui.selected) + " " + ui.selected.year
                        CalendarView.AGENDA -> "From ${TimeFormat.day(ui.selected)}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.shift(1) }) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next") }
            }
            when (ui.view) {
                CalendarView.MONTH -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    item { MonthGrid(ui, vm::select, Modifier.padding(horizontal = 12.dp)) }
                    item {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            (if (ui.selected == ui.today) "Today · " else "") + TimeFormat.day(ui.selected),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                    val events = ui.on(ui.selected)
                    if (events.isEmpty() && !ui.loading) item { NoEvents() }
                    items(events, key = { "${it.event.id}-${it.start}" }) { EventRow(it, ui.selected, open, Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
                }
                CalendarView.WEEK -> LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp)) {
                    StatsPeriod.WEEK.range(ui.selected).days.forEach { d ->
                        item(key = "h$d") { DayHeader(d, ui.today) { vm.select(d); vm.setView(CalendarView.DAY) } }
                        val events = ui.on(d)
                        if (events.isEmpty()) item(key = "e$d") { Text("No events", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)) }
                        items(events, key = { "$d-${it.event.id}-${it.start}" }) { EventRow(it, d, open, Modifier.padding(vertical = 3.dp)) }
                    }
                }
                CalendarView.DAY -> DayTimeline(ui, open)
                CalendarView.AGENDA -> LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp)) {
                    val byDay = ui.occurrences.groupBy { it.start.toLocalDate() }.toSortedMap()
                    if (byDay.isEmpty() && !ui.loading) item { NoEvents("No upcoming events in the next 60 days.") }
                    byDay.forEach { (d, list) ->
                        item(key = "h$d") { DayHeader(d, ui.today) { vm.select(d); vm.setView(CalendarView.DAY) } }
                        items(list, key = { "$d-${it.event.id}-${it.start}" }) { EventRow(it, d, open, Modifier.padding(vertical = 3.dp)) }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { nav.push(EventEditorNav(dateEpochDay = ui.selected.toEpochDay())) }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Add event")
        }
    }
}

@Composable
private fun NoEvents(text: String = "No events. Tap + to add one.") {
    EmptyState(Icons.Filled.EventAvailable, "Nothing scheduled", text)
}

@Composable
private fun MonthGrid(ui: CalendarUi, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val start = monthGridStart(ui.selected)
    val marked = ui.daysWithEvents
    Column(modifier) {
        Row {
            (0 until 7).forEach { i ->
                Text(
                    start.plusDays(i.toLong()).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                )
            }
        }
        val rows = if (start.plusDays(35).month == ui.selected.month) 6 else 5
        (0 until rows).forEach { r ->
            Row {
                (0 until 7).forEach { c ->
                    val d = start.plusDays((r * 7 + c).toLong())
                    val inMonth = d.month == ui.selected.month
                    val selected = d == ui.selected
                    val isToday = d == ui.today
                    Column(
                        Modifier.weight(1f).height(48.dp).padding(2.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onSelect(d) }
                            .semantics { this.selected = selected; contentDescription = TimeFormat.day(d) + (if (isToday) ", today" else "") + (if (d in marked) ", has events" else "") },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "${d.dayOfMonth}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isToday || selected) FontWeight.Bold else null,
                            color = when {
                                isToday -> MaterialTheme.colorScheme.primary
                                !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(if (d in marked) MaterialTheme.colorScheme.primary.copy(alpha = if (inMonth) 0.8f else 0.3f) else Color.Transparent))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(d: LocalDate, today: LocalDate, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            when (d) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                else -> d.dayOfWeek.getDisplayName(TextStyle.FULL, LocalConfiguration.current.locales[0])
            },
            style = MaterialTheme.typography.titleSmall,
            color = if (d == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text("  ${TimeFormat.dayYear(d)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EventRow(o: EventOccurrence, day: LocalDate, onClick: (EventOccurrence) -> Unit, modifier: Modifier = Modifier) {
    val color = Color(o.event.color)
    val minutes = Duration.between(o.start, o.end).toMinutes()
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(72.dp).padding(top = 10.dp)) {
            Text(if (o.start.toLocalDate() < day) "Cont." else TimeFormat.time(o.start), style = MaterialTheme.typography.labelLarge)
            Text(TimeFormat.time(o.end), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            onClick = { onClick(o) },
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.10f),
            modifier = Modifier.weight(1f)
        ) {
            Row(Modifier.heightIn(min = 52.dp)) {
                Box(Modifier.width(4.dp).fillMaxHeight().heightIn(min = 52.dp).background(color))
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).weight(1f)) {
                    Text(o.event.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${o.event.category.label} · ${TimeFormat.minutes(minutes.toInt())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (o.event.repeatRule != RepeatRule.NONE) {
                    Icon(Icons.Filled.Repeat, contentDescription = "Repeats ${o.event.repeatRule.label.lowercase()}", modifier = Modifier.padding(10.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private val HOUR = 56.dp

@Composable
private fun DayTimeline(ui: CalendarUi, open: (EventOccurrence) -> Unit) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val day = ui.selected
    val events = ui.on(day)
    LaunchedEffect(day) {
        val firstHour = events.minOfOrNull { if (it.start.toLocalDate() < day) 0 else it.start.hour } ?: 7
        scroll.scrollTo(with(density) { (HOUR * (firstHour - 1).coerceAtLeast(0)).roundToPx() })
    }
    // Greedy lanes so overlapping events sit side by side.
    val lanes = ArrayList<LocalDateTime>()
    val laneOf = events.map { e ->
        val lane = lanes.indexOfFirst { !it.isAfter(e.start) }.takeIf { it >= 0 } ?: lanes.size.also { lanes.add(e.end) }
        lanes[lane] = e.end
        lane
    }
    val laneCount = lanes.size.coerceAtLeast(1)
    Box(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 96.dp)) {
        Column {
            (0 until 24).forEach { h ->
                Row(Modifier.height(HOUR)) {
                    Text(TimeFormat.hour(h), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(60.dp).padding(start = 12.dp))
                    HorizontalDivider(Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        BoxWithConstraints(Modifier.padding(start = 64.dp, end = 12.dp).fillMaxWidth().height(HOUR * 24)) {
            val laneWidth = maxWidth / laneCount
            events.forEachIndexed { i, e ->
                val startMin = if (e.start.toLocalDate() < day) 0 else e.start.hour * 60 + e.start.minute
                val endMin = if (e.end.toLocalDate() > day) 24 * 60 else e.end.hour * 60 + e.end.minute
                val color = Color(e.event.color)
                Surface(
                    onClick = { open(e) },
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.16f),
                    modifier = Modifier
                        .offset(x = laneWidth * laneOf[i], y = HOUR * (startMin / 60f) + 8.dp)
                        .width(laneWidth - 4.dp)
                        .height((HOUR * ((endMin - startMin).coerceAtLeast(20) / 60f)).coerceAtLeast(26.dp))
                ) {
                    Row {
                        Box(Modifier.width(3.dp).fillMaxHeight().background(color))
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(e.event.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${TimeFormat.time(e.start)} – ${TimeFormat.time(e.end)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
            if (day == ui.today) {
                val now = java.time.LocalTime.now()
                Box(Modifier.offset(y = HOUR * ((now.hour * 60 + now.minute) / 60f) + 8.dp).fillMaxWidth().height(2.dp).background(FF.colors.danger))
            }
        }
    }
}
