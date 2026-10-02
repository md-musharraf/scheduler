package com.example.routineforge.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.AppContainer
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.FFCard
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.core.ui.LinearBar
import com.example.routineforge.core.ui.ScreenHeader
import com.example.routineforge.core.ui.SectionHeader
import com.example.routineforge.core.ui.Segmented
import com.example.routineforge.core.ui.ShowMessages
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

/** A milestone is derived from sessions, never stored, so it can't drift from the data. */
@Immutable
data class Milestone(val title: String, val current: Long, val target: Long, val unit: String) {
    val done: Boolean get() = current >= target
}

@Immutable
data class GoalsUi(
    val loading: Boolean = true,
    val period: StatsPeriod = StatsPeriod.MONTH,
    val today: LocalDate = LocalDate.now(),
    val stats: FocusStats? = null,
    val explicitMinutes: Int? = null,
    val targetMinutes: Int = 0,
    val dailyTargetMinutes: Int = 0,
    val subjects: List<Subject> = emptyList(),
    val dayTotals: Map<LocalDate, Long> = emptyMap(),
    val minActiveMs: Long = 15 * 60_000L,
    val milestones: List<Milestone> = emptyList()
) {
    val range get() = period.range(today)
    val progress: Float get() = FocusAnalytics.progress(stats?.totalMs ?: 0, targetMinutes)
    val activeDays: Int get() = range.days.count { !it.isAfter(today) && (dayTotals[it] ?: 0) >= minActiveMs }

    /** Share of days so far that met the daily goal. */
    val daysOnTarget: Int?
        get() {
            if (dailyTargetMinutes <= 0) return null
            val days = range.days.filter { !it.isAfter(today) }
            if (days.isEmpty()) return null
            return days.count { (dayTotals[it] ?: 0) >= dailyTargetMinutes * 60_000L } * 100 / days.size
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModel(private val c: AppContainer) : FFViewModel() {
    private val period = MutableStateFlow(StatsPeriod.MONTH)
    private val stats = combine(period, todayFlow()) { p, t -> p.range(t) }.flatMapLatest { c.sessions.stats(it) }
    private val lifetime = c.sessions.allFocus

    val ui: StateFlow<GoalsUi> = combine(
        combine(period, todayFlow()) { p, t -> p to t }, stats, c.goals.goals, c.subjects.subjects,
        combine(c.sessions.dayTotals(), lifetime, c.profiles.profile) { a, b, p -> Triple(a, b, p) }
    ) { (p, t), st, goals, subjects, (totals, all, profile) ->
        val minMs = profile.streakMinMinutes * 60_000L
        val lifetimeMs = all.sumOf { it.actualDurationMs }
        val longest = FocusAnalytics.longestStreak(totals, minMs).toLong()
        val last30Active = (0 until 30).count { (totals[t.minusDays(it.toLong())] ?: 0) >= minMs }.toLong()
        val hours = lifetimeMs / 3_600_000
        GoalsUi(
            loading = false, period = p, today = t, stats = st,
            explicitMinutes = goals[p.goalPeriod]?.targetMinutes,
            targetMinutes = FocusAnalytics.overallTarget(goals, subjects, p, p.range(t)),
            dailyTargetMinutes = FocusAnalytics.overallTarget(goals, subjects, StatsPeriod.DAY, StatsPeriod.DAY.range(t)),
            subjects = subjects.filter { !it.isArchived },
            dayTotals = totals, minActiveMs = minMs,
            milestones = listOf(
                Milestone("First focus session", all.size.toLong(), 1, "sessions"),
                Milestone("10 hours completed", hours, 10, "h"),
                Milestone("50 hours completed", hours, 50, "h"),
                Milestone("100 hours completed", hours, 100, "h"),
                Milestone("3-day streak", longest, 3, "days"),
                Milestone("10-day streak", longest, 10, "days"),
                Milestone("20 active days in the last 30", last30Active, 20, "days"),
                Milestone("500 hours completed", hours, 500, "h")
            )
        )
    }.stateIn(GoalsUi())

    fun setPeriod(p: StatsPeriod) { period.value = p }

    fun setTarget(hoursText: String) = launchSafe {
        val hours = hoursText.trim().replace(',', '.').toDoubleOrNull()
        require(hours != null && hours > 0) { "Enter the number of hours" }
        c.goals.set(period.value.goalPeriod, (hours * 60).toInt())
        say("Goal saved")
    }

    fun clearTarget() = launchSafe {
        c.goals.clear(period.value.goalPeriod)
        say("Using your subjects' targets")
    }
}

@Composable
fun GoalsScreen(nav: AppNav) {
    val vm = appViewModel { GoalsViewModel(it) }
    ShowMessages(vm)
    val ui by vm.ui.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    val focused = ui.stats?.totalMs ?: 0L
    val periodName = ui.period.goalPeriod.label

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Goals", onBack = nav::back)
        Segmented(StatsPeriod.entries, ui.period, { it.label }, vm::setPeriod, Modifier.padding(horizontal = 20.dp))
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                FFCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("$periodName Goal", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (ui.targetMinutes > 0) "${TimeFormat.duration(focused)} / ${TimeFormat.minutes(ui.targetMinutes)}" else "${TimeFormat.duration(focused)} · no goal",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        if (ui.targetMinutes > 0) Text("${(ui.progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit ${periodName.lowercase()} goal") }
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearBar(ui.progress, height = 10.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            ui.explicitMinutes != null -> "Your ${periodName.lowercase()} goal"
                            ui.targetMinutes > 0 -> "Sum of your subjects' targets. Tap edit to set your own."
                            else -> "Set a goal, or give your subjects daily targets."
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                FFCard(Modifier.fillMaxWidth()) {
                    Row {
                        StatBlock("${ui.activeDays}", "Days Active", Modifier.weight(1f))
                        StatBlock(TimeFormat.duration(focused / ui.range.elapsedDays(ui.today).coerceAtLeast(1)), "Daily Average", Modifier.weight(1f))
                        StatBlock(ui.daysOnTarget?.let { "$it%" } ?: "–", "Days on Target", Modifier.weight(1f))
                    }
                }
            }
            if (ui.subjects.isNotEmpty()) {
                item { SectionHeader("Subject targets") }
                items(ui.subjects, key = { it.id }) { s ->
                    val ms = ui.stats?.bySubject?.get(s.id) ?: 0L
                    val target = FocusAnalytics.subjectTarget(s, ui.period, ui.range)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SubjectBadge(s.icon, s.color, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(s.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Text(if (target > 0) "${TimeFormat.duration(ms)} / ${TimeFormat.minutes(target)}" else TimeFormat.duration(ms), style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearBar(FocusAnalytics.progress(ms, target), color = Color(s.color), height = 5.dp)
                        }
                    }
                }
            }
            item { SectionHeader("Milestones") }
            items(ui.milestones, key = { it.title }) { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (m.done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (m.done) "Achieved" else "Not yet",
                        tint = if (m.done) FF.colors.success else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(m.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (!m.done) Text("${m.current} / ${m.target} ${m.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (editing) {
        var text by rememberSaveable { mutableStateOf(ui.explicitMinutes?.let { (it / 60.0).toString().removeSuffix(".0") } ?: "") }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("$periodName goal") },
            text = {
                OutlinedTextField(
                    text, { text = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6) },
                    label = { Text("Hours") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = { TextButton(onClick = { vm.setTarget(text); editing = false }) { Text("Save") } },
            dismissButton = {
                Row {
                    if (ui.explicitMinutes != null) TextButton(onClick = { vm.clearTarget(); editing = false }) { Text("Use subjects") }
                    TextButton(onClick = { editing = false }) { Text("Cancel") }
                }
            }
        )
    }
}
