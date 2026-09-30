package com.example.routineforge.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routineforge.data.Bucket
import com.example.routineforge.data.Candle
import com.example.routineforge.data.FocusGoal
import com.example.routineforge.data.FocusStats
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.data.StatsRange
import com.example.routineforge.util.FocusTracker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

enum class ChartType { BARS, LINE, CANDLE }

private val UpGreen = Color(0xFF22C55E)

/** "2h 05m" style that never collapses to seconds, so dashboards stay steady. */
private fun hm(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

private fun subjectColor(name: String, goals: List<FocusGoal>): Color =
    Color(goals.firstOrNull { it.title == name }?.colorHex ?: FocusGoal.PALETTE[abs(name.hashCode()) % FocusGoal.PALETTE.size])

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: RoutineRepository,
    onStartGoal: (FocusGoal, Int) -> Unit,
    onStartPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goals by repository.goals.collectAsState()
    val log by repository.focusLog.collectAsState()
    val today = remember(log) { LocalDate.now() }
    val days = remember(log) { FocusStats.days(log) }
    val todayMap = days[today.toEpochDay()].orEmpty()
    val streak = remember(days, goals) { FocusStats.streak(goals, days, today) }

    var range by rememberSaveable { mutableStateOf(StatsRange.WEEK) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var chartType by rememberSaveable { mutableStateOf(ChartType.BARS) }
    var editing by remember { mutableStateOf<FocusGoal?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Today", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { HeroCard(goals, todayMap, streak, today, onStartPlan) }

                item {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Daily goals", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            val used = goals.map { it.colorHex }
                            editing = FocusGoal(title = "", minutes = 60, colorHex = FocusGoal.PALETTE.firstOrNull { it !in used } ?: FocusGoal.PALETTE[0])
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add")
                        }
                    }
                }
                if (goals.isEmpty()) {
                    item {
                        Text(
                            "Split your day into subjects with a target each — e.g. Math 2h, Physics 2h, Chemistry 2h. " +
                                "Every timer you run for a subject fills its bar here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(goals, key = { it.id }) { goal ->
                    GoalRow(
                        goal = goal,
                        doneSeconds = todayMap[goal.title] ?: 0,
                        onStart = { onStartGoal(goal, (goal.targetSeconds - (todayMap[goal.title] ?: 0)).coerceAtLeast(60)) },
                        onEdit = { editing = goal }
                    )
                }

                item {
                    Text("Progress", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                }
                item {
                    ProgressCard(
                        log = log,
                        days = days,
                        goals = goals,
                        today = today,
                        range = range,
                        offset = offset,
                        chartType = chartType,
                        onRange = { range = it; offset = 0 },
                        onOffset = { offset = it },
                        onChartType = { chartType = it }
                    )
                }
                item {
                    val period = FocusStats.periodDays(range, FocusStats.shift(range, today, offset))
                    BreakdownCard(FocusStats.subjectTotals(days, period), goals)
                }
            }
        }
    }

    editing?.let { goal ->
        GoalDialog(
            goal = goal,
            isNew = goals.none { it.id == goal.id },
            takenTitles = goals.filter { it.id != goal.id }.map { it.title.lowercase() },
            onSave = { repository.saveGoal(it); editing = null },
            onDelete = { repository.deleteGoal(goal.id); editing = null },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun HeroCard(
    goals: List<FocusGoal>,
    todayMap: Map<String, Int>,
    streak: Int,
    today: LocalDate,
    onStartPlan: () -> Unit
) {
    val target = FocusStats.dailyTarget(goals)
    val progress = FocusStats.goalProgress(goals, todayMap)
    val fraction = if (target > 0) progress / target.toFloat() else 0f
    val animated by animateFloatAsState(fraction, label = "ring")
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                    drawArc(primary, -90f, 360f * animated.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text(
                    if (target > 0) "${(fraction * 100).toInt()}%" else "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(hm(todayMap.values.sum()), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (target > 0) "focused · goal ${hm(target)}" else "focused today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (streak > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("🔥 $streak-day streak", style = MaterialTheme.typography.labelLarge, color = primary)
                }
            }
        }
        when {
            target > 0 && progress < target -> Button(
                onClick = onStartPlan,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start today's plan · ${hm(target - progress)} left")
            }
            target > 0 -> Text(
                "All goals done today ✓",
                style = MaterialTheme.typography.titleSmall,
                color = primary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun GoalRow(goal: FocusGoal, doneSeconds: Int, onStart: () -> Unit, onEdit: () -> Unit) {
    val color = Color(goal.colorHex)
    val fraction = (doneSeconds / goal.targetSeconds.toFloat()).coerceIn(0f, 1f)
    val animated by animateFloatAsState(fraction, label = "goal")
    OutlinedCard(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(
                        "${hm(doneSeconds)} / ${hm(goal.targetSeconds)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(color.copy(alpha = 0.15f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(animated).clip(CircleShape).background(color))
                }
            }
            Spacer(Modifier.width(8.dp))
            if (fraction >= 1f) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Done", tint = color, modifier = Modifier.padding(8.dp).size(32.dp))
            } else {
                FilledTonalIconButton(onClick = onStart) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Start ${goal.title} timer")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressCard(
    log: Map<Long, Map<String, Int>>,
    days: Map<Long, Map<String, Int>>,
    goals: List<FocusGoal>,
    today: LocalDate,
    range: StatsRange,
    offset: Int,
    chartType: ChartType,
    onRange: (StatsRange) -> Unit,
    onOffset: (Int) -> Unit,
    onChartType: (ChartType) -> Unit
) {
    val anchor = FocusStats.shift(range, today, offset)
    val target = FocusStats.dailyTarget(goals)
    val now = remember(log) { LocalDateTime.now() }
    val buckets = remember(log, range, anchor, target) { FocusStats.buckets(log, days, range, anchor, target, now) }
    val candles = remember(buckets, chartType) {
        if (chartType != ChartType.CANDLE) emptyList()
        else FocusStats.candles(buckets, FocusStats.buckets(log, days, range, FocusStats.shift(range, anchor, -1), target, now).last().seconds)
    }
    val currentIndex = when {
        offset != 0 -> buckets.lastIndex
        range == StatsRange.DAY -> now.hour
        range == StatsRange.YEAR -> today.monthValue - 1
        else -> FocusStats.periodDays(range, anchor).indexOf(today)
    }
    var selected by remember(range, offset) { mutableIntStateOf(currentIndex) }
    val shown = buckets.getOrNull(selected) ?: buckets.last()

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                StatsRange.entries.forEachIndexed { i, r ->
                    SegmentedButton(
                        selected = r == range,
                        onClick = { onRange(r) },
                        shape = SegmentedButtonDefaults.itemShape(i, StatsRange.entries.size),
                        icon = {}
                    ) { Text(r.label) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onOffset(offset - 1) }) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous") }
                Text(
                    FocusStats.periodTitle(range, anchor, today),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onOffset(offset + 1) }, enabled = offset < 0) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(shown.fullLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(hm(shown.seconds), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                listOf(
                    Triple(ChartType.BARS, Icons.Filled.BarChart, "Bar chart"),
                    Triple(ChartType.LINE, Icons.AutoMirrored.Filled.ShowChart, "Line graph"),
                    Triple(ChartType.CANDLE, Icons.Filled.CandlestickChart, "Candle chart")
                ).forEach { (type, icon, label) ->
                    IconToggleButton(checked = chartType == type, onCheckedChange = { onChartType(type) }) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (chartType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            FocusChart(buckets, candles, chartType, selected, onSelect = { selected = it })
            if (chartType == ChartType.CANDLE) {
                Text(
                    "Each candle opens at the previous total and closes at this one. Green = more than before.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val elapsed = FocusStats.periodDays(range, anchor).filter { !it.isAfter(today) }
            val total = elapsed.sumOf { days[it.toEpochDay()]?.values?.sum() ?: 0 }
            val met = elapsed.count { FocusStats.goalMet(goals, days[it.toEpochDay()]) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Total", hm(total), Modifier.weight(1f))
                if (range == StatsRange.DAY) {
                    val peak = buckets.maxBy { it.seconds }
                    StatTile("Peak hour", if (peak.seconds > 0) peak.fullLabel.substringBefore(" ") else "—", Modifier.weight(1f))
                    StatTile("Goal", if (met > 0) "Met ✓" else "—", Modifier.weight(1f))
                } else {
                    StatTile("Daily avg", hm(if (elapsed.isEmpty()) 0 else total / elapsed.size), Modifier.weight(1f))
                    StatTile("Goal days", "$met/${elapsed.size}", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private val AxisGutter = 34.dp

/** Rounds the axis top up to a readable step: 15 min, 1 h or 10 h. */
private fun niceMax(raw: Int): Int {
    val unit = when {
        raw <= 3600 -> 900
        raw <= 36_000 -> 3600
        else -> 36_000
    }
    return (ceil(raw / unit.toDouble()).toInt() * unit).coerceAtLeast(unit)
}

@Composable
private fun FocusChart(
    buckets: List<Bucket>,
    candles: List<Candle>,
    type: ChartType,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val select by rememberUpdatedState(onSelect)
    val n = buckets.size
    val max = niceMax(maxOf(buckets.maxOf { maxOf(it.seconds, it.targetSeconds) }, candles.maxOfOrNull { it.high } ?: 0))

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(n) {
                detectTapGestures {
                    val gutter = AxisGutter.toPx()
                    select(((it.x - gutter) / ((size.width - gutter) / n.toFloat())).toInt().coerceIn(0, n - 1))
                }
            }
    ) {
        val top = 18.dp.toPx()
        val left = AxisGutter.toPx()
        val slot = (size.width - left) / n
        fun y(v: Int) = top + (size.height - top) * (1f - v / max.toFloat())
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

        drawText(measurer, "0", Offset(0f, size.height - 14.dp.toPx()), labelStyle)
        listOf(max, max / 2).forEach { v ->
            drawLine(grid, Offset(left, y(v)), Offset(size.width, y(v)), 1.dp.toPx(), pathEffect = dash)
            drawText(measurer, hm(v), Offset(0f, y(v) - 7.dp.toPx()), labelStyle)
        }
        drawLine(grid, Offset(left, size.height), Offset(size.width, size.height), 1.dp.toPx())
        translate(left = left) {
            drawRect(onSurface.copy(alpha = 0.05f), Offset(selected * slot, top), Size(slot, size.height - top))

            when (type) {
                ChartType.BARS -> buckets.forEachIndexed { i, b ->
                    if (b.seconds <= 0) return@forEachIndexed
                    val w = slot * 0.6f
                    val met = b.targetSeconds > 0 && b.seconds >= b.targetSeconds
                    val color = when {
                        met -> primary
                        i == selected -> onSurface.copy(alpha = 0.7f)
                        else -> onSurface.copy(alpha = 0.3f)
                    }
                    val barTop = minOf(y(b.seconds), size.height - 2.dp.toPx())
                    drawRoundRect(color, Offset(i * slot + (slot - w) / 2, barTop), Size(w, size.height - barTop), CornerRadius(minOf(w / 2, 4.dp.toPx())))
                }
                ChartType.LINE -> {
                    val points = buckets.filterNot { it.future }.mapIndexed { i, b -> Offset(i * slot + slot / 2, y(b.seconds)) }
                    val line = Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
                    val area = Path().apply {
                        addPath(line)
                        lineTo(points.last().x, size.height)
                        lineTo(points.first().x, size.height)
                        close()
                    }
                    drawPath(area, primary.copy(alpha = 0.12f))
                    drawPath(line, primary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                    points.forEachIndexed { i, p ->
                        if (i == selected) drawCircle(primary, 5.dp.toPx(), p)
                        else if (n <= 12) drawCircle(primary, 3.dp.toPx(), p)
                    }
                }
                ChartType.CANDLE -> candles.forEachIndexed { i, c ->
                    if (c.high <= 0 || buckets[i].future) return@forEachIndexed
                    val color = if (c.close >= c.open) UpGreen else primary
                    val cx = i * slot + slot / 2
                    val w = slot * 0.55f
                    drawLine(color, Offset(cx, y(c.high)), Offset(cx, y(c.low)), 1.5.dp.toPx())
                    val bodyTop = y(maxOf(c.open, c.close))
                    val bodyH = maxOf(y(minOf(c.open, c.close)) - bodyTop, 2.dp.toPx())
                    drawRect(color, Offset(cx - w / 2, bodyTop), Size(w, bodyH), alpha = if (i == selected) 1f else 0.85f)
                }
            }

            if (buckets.any { it.targetSeconds > 0 }) {
                val goalLine = Path().apply {
                    buckets.forEachIndexed { i, b ->
                        if (i == 0) moveTo(0f, y(b.targetSeconds)) else lineTo(i * slot, y(b.targetSeconds))
                        lineTo((i + 1) * slot, y(b.targetSeconds))
                    }
                }
                drawPath(goalLine, primary.copy(alpha = 0.6f), style = Stroke(1.dp.toPx(), pathEffect = dash))
            }
            }
    }
    val step = ceil(n / 8.0).toInt()
    Row(Modifier.fillMaxWidth().padding(start = AxisGutter, top = 4.dp)) {
        buckets.forEachIndexed { i, b ->
            Text(
                if (i % step == 0) b.label else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (i == selected) MaterialTheme.colorScheme.primary else labelColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BreakdownCard(totals: List<Pair<String, Int>>, goals: List<FocusGoal>) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("By subject", style = MaterialTheme.typography.titleSmall)
            if (totals.isEmpty()) {
                Text(
                    "No focus time in this period yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val sum = totals.sumOf { it.second }.coerceAtLeast(1)
            totals.forEach { (name, sec) ->
                val color = subjectColor(name, goals)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(color, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(hm(sec), style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${sec * 100 / sum}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(44.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(color.copy(alpha = 0.12f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(sec / sum.toFloat()).clip(CircleShape).background(color))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalDialog(
    goal: FocusGoal,
    isNew: Boolean,
    takenTitles: List<String>,
    onSave: (FocusGoal) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(goal.title) }
    var minutesText by remember { mutableStateOf(goal.minutes.toString()) }
    var color by remember { mutableLongStateOf(goal.colorHex) }
    var logged by remember { mutableIntStateOf(0) }
    val minutes = minutesText.toIntOrNull() ?: 0
    val duplicate = title.trim().lowercase() in takenTitles
    val valid = title.isNotBlank() && !duplicate && minutes in 1..1440

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New goal" else "Edit goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(40) },
                    label = { Text("Subject") },
                    placeholder = { Text("e.g. Math") },
                    isError = duplicate,
                    supportingText = if (duplicate) ({ Text("You already have this goal") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { v -> minutesText = v.filter { it.isDigit() }.take(4) },
                    label = { Text("Daily target (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90, 120, 180).forEach { m ->
                        FilterChip(selected = minutes == m, onClick = { minutesText = m.toString() }, label = { Text(hm(m * 60)) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusGoal.PALETTE.forEach { c ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(3.dp, if (c == color) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                .clickable { color = c }
                        )
                    }
                }
                if (!isNew) {
                    Text(
                        if (logged > 0) "Logged ${hm(logged)} for today" else "Studied without the timer? Add it:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 60).forEach { m ->
                            FilterChip(
                                selected = false,
                                onClick = { FocusTracker.record(context, goal.title, m * 60); logged += m * 60 },
                                label = { Text("+${hm(m * 60)}") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(goal.copy(title = title, minutes = minutes, colorHex = color)) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
