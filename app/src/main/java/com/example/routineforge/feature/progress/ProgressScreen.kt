package com.example.routineforge.feature.progress

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.AppContainer
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.EmptyState
import com.example.routineforge.core.ui.FFCard
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.core.ui.FocusBarChart
import com.example.routineforge.core.ui.IconBadge
import com.example.routineforge.core.ui.LinearBar
import com.example.routineforge.core.ui.MonthHeatmap
import com.example.routineforge.core.ui.ScreenHeader
import com.example.routineforge.core.ui.SectionHeader
import com.example.routineforge.core.ui.Segmented
import com.example.routineforge.core.ui.StatBlock
import com.example.routineforge.core.ui.SubjectBadge
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.core.ui.todayFlow
import com.example.routineforge.domain.FocusAnalytics
import com.example.routineforge.domain.FocusStats
import com.example.routineforge.domain.StatsPeriod
import com.example.routineforge.navigation.AppNav
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Immutable
data class ProgressUi(
    val loading: Boolean = true,
    val period: StatsPeriod = StatsPeriod.WEEK,
    val anchor: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val stats: FocusStats? = null,
    val targetMinutes: Int = 0,
    val dailyTargetMinutes: Int = 0,
    val subjects: Map<Long, Subject> = emptyMap(),
    val minActiveMs: Long = 15 * 60_000L
) {
    val range get() = period.range(anchor)
    val isCurrent: Boolean get() = today in range
    val elapsedDays: Int get() = range.elapsedDays(today).coerceAtLeast(1)
    val dailyAverageMs: Long get() = (stats?.totalMs ?: 0L) / elapsedDays
    val goalProgress: Float get() = FocusAnalytics.progress(stats?.totalMs ?: 0L, targetMinutes)
    val title: String
        get() = when (period) {
            StatsPeriod.DAY -> if (anchor == today) "Today" else TimeFormat.day(anchor)
            StatsPeriod.WEEK -> "${TimeFormat.day(range.start)} – ${TimeFormat.day(range.endExclusive.minusDays(1))}"
            StatsPeriod.MONTH -> TimeFormat.monthYear(anchor)
            StatsPeriod.YEAR -> anchor.year.toString()
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(private val c: AppContainer) : FFViewModel() {
    private val period = MutableStateFlow(StatsPeriod.WEEK)
    private val anchor = MutableStateFlow(LocalDate.now())
    private val stats = combine(period, anchor) { p, a -> p.range(a) }.flatMapLatest { c.sessions.stats(it) }

    val ui: StateFlow<ProgressUi> = combine(
        combine(period, anchor, todayFlow()) { p, a, t -> Triple(p, a, t) }, stats, c.goals.goals, c.subjects.subjects, c.profiles.profile
    ) { (p, a, t), st, goals, subjects, profile ->
        val range = p.range(a)
        ProgressUi(
            loading = false, period = p, anchor = a, today = t, stats = st,
            targetMinutes = FocusAnalytics.overallTarget(goals, subjects, p, range),
            dailyTargetMinutes = FocusAnalytics.overallTarget(goals, subjects, StatsPeriod.DAY, StatsPeriod.DAY.range(a)),
            subjects = subjects.associateBy { it.id },
            minActiveMs = profile.streakMinMinutes * 60_000L
        )
    }.stateIn(ProgressUi())

    fun setPeriod(p: StatsPeriod) { period.value = p }
    fun shift(n: Long) {
        val next = period.value.shift(anchor.value, n)
        // Don't page into periods that haven't started.
        if (!period.value.range(next).start.isAfter(LocalDate.now())) anchor.value = next
    }
}

@Composable
fun ProgressScreen(nav: AppNav) {
    val vm = appViewModel { ProgressViewModel(it) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val stats = ui.stats

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("My Progress", onBack = nav::back)
        Segmented(StatsPeriod.entries, ui.period, { it.label }, vm::setPeriod, Modifier.padding(horizontal = 20.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.shift(-1) }) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous ${ui.period.label.lowercase()}") }
            Text(ui.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.shift(1) }, enabled = !ui.isCurrent) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next ${ui.period.label.lowercase()}") }
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                FFCard(Modifier.fillMaxWidth()) {
                    Row {
                        StatBlock(TimeFormat.duration(stats?.totalMs ?: 0), "Total Focus", Modifier.weight(1f))
                        if (ui.period != StatsPeriod.DAY) StatBlock(TimeFormat.duration(ui.dailyAverageMs), "Daily Average", Modifier.weight(1f))
                        else StatBlock("${stats?.sessionCount ?: 0}", "Sessions", Modifier.weight(1f))
                        StatBlock(if (ui.targetMinutes > 0) "${(ui.goalProgress * 100).toInt()}%" else "–", "Goal Achieved", Modifier.weight(1f))
                    }
                    if (ui.period == StatsPeriod.YEAR || ui.period == StatsPeriod.MONTH) {
                        Spacer(Modifier.height(12.dp))
                        Row {
                            StatBlock("${stats?.activeDays(ui.minActiveMs) ?: 0}", "Active Days", Modifier.weight(1f))
                            StatBlock("${stats?.sessionCount ?: 0}", "Sessions", Modifier.weight(1f))
                            StatBlock(if (ui.targetMinutes > 0) TimeFormat.minutes(ui.targetMinutes) else "Not set", "Goal", Modifier.weight(1f))
                        }
                    }
                }
            }
            if (stats != null && stats.totalMs == 0L && !ui.loading) {
                item { EmptyState(Icons.Filled.BarChart, "No focus time here yet", "Complete a few focus sessions to see your progress.") }
            }
            if (stats != null) item { ChartCard(ui, stats) }
            val window = stats?.peakWindow(2)
            if (window != null) {
                item {
                    FFCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.Schedule, MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Most Productive Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${TimeFormat.hour(window.first)} – ${TimeFormat.hour(window.second)}", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
            if (stats != null && stats.bySubject.isNotEmpty()) {
                item { SectionHeader("Subject Wise") }
                val total = stats.totalMs.coerceAtLeast(1)
                items(stats.bySubject.entries.sortedByDescending { it.value }.toList(), key = { it.key ?: -1L }) { (id, ms) ->
                    val s = id?.let { ui.subjects[it] }
                    val color = s?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.outline
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (s != null) SubjectBadge(s.icon, s.color, size = 36.dp) else Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(10.dp).clip(CircleShape).background(color)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(s?.name ?: "No subject", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Text(TimeFormat.duration(ms), style = MaterialTheme.typography.titleSmall)
                                Text("  ${(ms * 100 / total)}%", style = MaterialTheme.typography.labelMedium, color = FF.colors.warning, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearBar(ms.toFloat() / total, color = color, height = 4.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(ui: ProgressUi, stats: FocusStats) {
    FFCard(Modifier.fillMaxWidth()) {
        when (ui.period) {
            StatsPeriod.DAY -> {
                Text("Hourly activity", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val now = java.time.LocalTime.now().hour
                FocusBarChart(
                    values = stats.byHour,
                    labels = (0 until 24).map { if (it % 6 == 0) TimeFormat.hour(it).replace(" ", "") else "" },
                    fullLabels = (0 until 24).map { "${TimeFormat.hour(it)} – ${TimeFormat.hour(it + 1)}" },
                    highlight = if (ui.isCurrent) now else null
                )
            }
            StatsPeriod.WEEK -> {
                Text("Focus per day", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val days = ui.range.days
                FocusBarChart(
                    values = days.map { stats.byDay[it] ?: 0L },
                    labels = days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
                    fullLabels = days.map { TimeFormat.day(it) },
                    highlight = days.indexOf(ui.today).takeIf { it >= 0 },
                    target = ui.dailyTargetMinutes.takeIf { it > 0 }?.let { it * 60_000L }
                )
            }
            StatsPeriod.MONTH -> {
                Text("Daily activity", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                MonthHeatmap(ui.anchor, stats.byDay, ui.today)
            }
            StatsPeriod.YEAR -> {
                Text("Monthly trend", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val months = (0 until 12).map { ui.range.start.plusMonths(it.toLong()) }
                FocusBarChart(
                    values = months.map { m -> stats.byDay.filterKeys { it.year == m.year && it.month == m.month }.values.sum() },
                    labels = months.map { it.month.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
                    fullLabels = months.map { TimeFormat.monthYear(it) },
                    highlight = if (ui.isCurrent) ui.today.monthValue - 1 else null
                )
            }
        }
    }
}
