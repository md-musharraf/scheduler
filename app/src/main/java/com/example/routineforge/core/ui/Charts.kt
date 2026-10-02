package com.example.routineforge.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.time.TimeFormat
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Vertical bars of focused time. Tap a bar to read its exact value. [target] draws a dashed
 * line (same unit as [values], ms). Labels are shown for every [labelEvery]th bar.
 */
@Composable
fun FocusBarChart(
    values: List<Long>,
    labels: List<String>,
    fullLabels: List<String>,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
    target: Long? = null,
    labelEvery: Int = 1,
    color: Color = MaterialTheme.colorScheme.primary
) {
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val grow = remember { Animatable(0f) }
    LaunchedEffect(values) { grow.snapTo(0f); grow.animateTo(1f, tween(500)) }
    val track = FF.colors.track
    val muted = color.copy(alpha = 0.45f)
    val targetColor = FF.colors.warning
    val max = maxOf(values.maxOrNull() ?: 0L, target ?: 0L, 60_000L)
    val summary = values.indices.filter { values[it] > 0 }.joinToString { "${fullLabels[it]} ${TimeFormat.duration(values[it])}" }

    Column(modifier.semantics(mergeDescendants = true) { contentDescription = if (summary.isEmpty()) "No focus time in this period" else summary }) {
        Row(Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
            val i = selected
            Text(
                when {
                    i != null -> "${fullLabels[i]} · ${TimeFormat.duration(values[i])}"
                    target != null && target > 0 -> "Dashed line: daily goal ${TimeFormat.duration(target)}"
                    else -> "Tap a bar for details"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    i != null -> MaterialTheme.colorScheme.onSurface
                    target != null && target > 0 -> FF.colors.warning
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Canvas(
            Modifier.fillMaxWidth().height(160.dp).pointerInput(values) {
                detectTapGestures { p ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    val idx = (p.x / slot).toInt().coerceIn(0, values.lastIndex)
                    selected = if (selected == idx) null else idx
                }
            }
        ) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val barW = (slot * 0.62f).coerceAtMost(28.dp.toPx())
            val radius = CornerRadius(barW / 2.5f, barW / 2.5f)
            values.forEachIndexed { i, v ->
                val x = slot * i + (slot - barW) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), radius)
                val h = size.height * (v.toFloat() / max) * grow.value
                if (h > 0f) {
                    val c = when {
                        selected == i -> color
                        highlight == null || highlight == i -> color
                        else -> muted
                    }
                    drawRoundRect(c, Offset(x, size.height - h), Size(barW, h), radius)
                }
            }
            if (target != null && target > 0) {
                val y = size.height * (1f - target.toFloat() / max)
                drawLine(targetColor, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            labels.forEachIndexed { i, l ->
                Text(
                    if (i % labelEvery == 0) l else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (i == highlight) FontWeight.Bold else null,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Heat level for a day's focus: 0 none, 1 ≤30m, 2 ≤90m, 3 ≤180m, 4 more. */
fun heatLevel(ms: Long): Int {
    val m = ms / 60_000
    return when {
        m <= 0 -> 0
        m <= 30 -> 1
        m <= 90 -> 2
        m <= 180 -> 3
        else -> 4
    }
}

/** Classic month grid (Sunday first) with subtle shading per activity level. */
@Composable
fun MonthHeatmap(month: LocalDate, byDay: Map<LocalDate, Long>, today: LocalDate, modifier: Modifier = Modifier) {
    val first = month.withDayOfMonth(1)
    val lead = first.dayOfWeek.value % 7 // Sunday = 0
    val cells = (0 until 42).map { first.minusDays(lead.toLong()).plusDays(it.toLong()) }.let { all ->
        if (all.subList(35, 42).none { it.month == month.month }) all.take(35) else all
    }
    val base = MaterialTheme.colorScheme.primary
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY).forEach {
                Text(it.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { d ->
                    val inMonth = d.month == month.month
                    val level = if (inMonth) heatLevel(byDay[d] ?: 0L) else 0
                    val bg = if (level == 0) FF.colors.track.copy(alpha = if (inMonth) 0.6f else 0.2f) else base.copy(alpha = 0.18f + 0.2f * level)
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(bg)
                            .semantics { contentDescription = "${TimeFormat.day(d)}: ${TimeFormat.duration(byDay[d] ?: 0L)}" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (inMonth) "${d.dayOfMonth}" else "",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (d == today) FontWeight.Bold else null,
                            color = when {
                                d == today -> MaterialTheme.colorScheme.primary
                                level >= 3 -> Color.White
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            (0..4).forEach { l ->
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(if (l == 0) FF.colors.track else base.copy(alpha = 0.18f + 0.2f * l)))
            }
            Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
