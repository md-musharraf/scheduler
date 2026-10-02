package com.example.routineforge.feature.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.theme.TimerDigits
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.FFCard
import com.example.routineforge.data.Stopwatch
import com.example.routineforge.domain.StopwatchState
import kotlin.math.cos
import kotlin.math.sin

/** Classic stopwatch: a dial that sweeps once a minute, Reset / Start-Pause / Lap, and a lap table. */
@Composable
fun StopwatchPane(stopwatch: Stopwatch) {
    val s by stopwatch.state.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        StopwatchDial(s)
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            OutlinedIconButton(onClick = stopwatch::reset, enabled = !s.isIdle, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reset")
            }
            FilledIconButton(
                onClick = stopwatch::toggle,
                modifier = Modifier.size(80.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent)
            ) {
                Icon(
                    if (s.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (s.running) "Pause" else if (s.isIdle) "Start" else "Resume",
                    modifier = Modifier.size(40.dp)
                )
            }
            OutlinedIconButton(onClick = stopwatch::lap, enabled = s.running, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Flag, contentDescription = "Lap")
            }
        }
        Spacer(Modifier.height(20.dp))
        if (s.laps.isNotEmpty()) LapTable(s)
        Spacer(Modifier.height(24.dp))
    }
}

/** The only part that redraws every frame while running. */
@Composable
private fun StopwatchDial(s: StopwatchState) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s) {
        now = System.currentTimeMillis()
        while (s.running) withFrameMillis { now = System.currentTimeMillis() }
    }
    val elapsed = s.elapsed(now)
    val track = FF.colors.track
    val accent = MaterialTheme.colorScheme.primary
    val tick = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val r = size.minDimension / 2 - stroke
            val c = center
            // 60 second ticks, longer every 5 seconds, like a classic stopwatch face.
            for (i in 0 until 60) {
                val a = Math.toRadians(i * 6.0 - 90)
                val inner = r - (if (i % 5 == 0) 14.dp.toPx() else 7.dp.toPx()) - stroke
                val outer = r - stroke - 2.dp.toPx()
                drawLine(
                    tick, Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                    Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                    strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx()
                )
            }
            val arcTopLeft = Offset(c.x - r, c.y - r)
            drawArc(track, 0f, 360f, false, arcTopLeft, Size(r * 2, r * 2), style = Stroke(stroke))
            val sweep = (elapsed % 60_000L) / 60_000f * 360f
            if (elapsed > 0) drawArc(accent, -90f, sweep, false, arcTopLeft, Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
            val a = Math.toRadians(sweep - 90.0)
            drawCircle(accent, radius = stroke * 0.9f, center = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val text = TimeFormat.stopwatch(elapsed, centis = true)
            Text(text.substringBeforeLast('.'), style = TimerDigits.copy(fontSize = if (elapsed >= 3_600_000L) 44.sp else 56.sp))
            Text(".${text.substringAfterLast('.')}", style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LapTable(s: StopwatchState) {
    val lengths = s.lapLengths
    val fastest = if (lengths.size > 1) lengths.min() else null
    val slowest = if (lengths.size > 1) lengths.max() else null
    FFCard(Modifier.fillMaxWidth()) {
        LapRow("Lap", "Lap time", "Total", MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
        lengths.indices.reversed().forEach { i ->
            val color = when (lengths[i]) {
                fastest -> FF.colors.success
                slowest -> FF.colors.danger
                else -> MaterialTheme.colorScheme.onSurface
            }
            LapRow("%02d".format(i + 1), TimeFormat.stopwatch(lengths[i], centis = true), TimeFormat.stopwatch(s.laps[i], centis = true), color)
        }
    }
}

@Composable
private fun LapRow(lap: String, length: String, total: String, color: androidx.compose.ui.graphics.Color) {
    val style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(lap, style = style, color = color, modifier = Modifier.width(56.dp))
        Text(length, style = style, color = color, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text(total, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}
