package com.example.routineforge.feature.timer

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.routineforge.domain.SandSim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min

private val WoodLight = Color(0xFFB57E4C)
private val WoodMid = Color(0xFF85532B)
private val WoodDark = Color(0xFF4F2F17)

/** Shade levels, light to dark: 4 grain shades, plus darker levels for grains near the glass (volume). */
private val LEVELS = floatArrayOf(1.22f, 1.1f, 1f, 0.9f, 0.78f, 0.66f)

/**
 * A glass-and-wood hourglass whose sand is simulated grain by grain ([SandSim]).
 * [remaining] (0..1, read every frame) is the share of sand that belongs on top: the time left.
 */
@Composable
fun SandHourglass(remaining: () -> Float, running: Boolean, sand: Color, modifier: Modifier = Modifier) {
    val sim = remember { SandSim() }
    val currentRemaining by rememberUpdatedState(remaining)
    val isRunning by rememberUpdatedState(running)
    var ready by remember { mutableStateOf(false) }
    var frame by remember { mutableLongStateOf(0L) }
    val paints = remember(sand) {
        Array(LEVELS.size) { i ->
            val f = LEVELS[i]
            val c = if (f >= 1f) lerp(sand, Color.White, f - 1f) else lerp(sand, Color.Black, 1f - f)
            Paint().apply { isAntiAlias = true; strokeCap = Paint.Cap.ROUND; color = c.toArgb() }
        }
    }
    val points = remember { Array(LEVELS.size) { FloatArray(sim.total * 2 + 2) } }
    val counts = remember { IntArray(LEVELS.size) }

    LaunchedEffect(sim) {
        withContext(Dispatchers.Default) { sim.reset(currentRemaining()) }
        ready = true
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                val dt = if (last == 0L) 1 / 60f else ((t - last) / 1e9f).coerceAtMost(0.05f)
                last = t
                sim.update(dt, sim.targetFor(currentRemaining()))
                frame = t
            }
            // At rest (paused, idle, or waiting for the next grain of a long session): poll gently instead of every frame.
            if (sim.settled) {
                delay(if (isRunning) 60 else 150)
                last = 0L
            }
        }
    }

    Canvas(modifier.semantics { contentDescription = "Hourglass" }) {
        val t = frame
        val g = geometry(sim)
        drawShadow(g)
        val turn = sim.flip.let { (1 - cos(it * Math.PI)).toFloat() / 2 } * 180f
        rotate(turn, Offset(size.width / 2, (g.top + g.bottom) / 2)) {
            val glass = glassPath(sim, g)
            // Glass body: a faint cool tint, brighter towards the light on the left.
            drawPath(glass, Brush.horizontalGradient(listOf(Color(0x33A9B8C9), Color(0x55FFFFFF), Color(0x14A9B8C9), Color(0x2E8A9AAD)), g.ox, g.ox + g.gw))
            if (ready) {
                drawGrains(sim, g, paints, points, counts)
                if (isRunning && sim.phase == SandSim.Phase.RUN && sim.topCount > 0) drawStream(sim, g, paints[3].color, t)
            }
            drawGlass(sim, g, glass)
            drawCaps(g)
        }
    }
}

/** Where the grid sits on the canvas. */
private class Geo(val ox: Float, val oy: Float, val cell: Float, val gw: Float, val gh: Float, val top: Float, val bottom: Float, val capH: Float, val baseH: Float)

private fun DrawScope.geometry(sim: SandSim): Geo {
    val capH = size.height * 0.06f
    val baseH = size.height * 0.08f
    val top = capH * 0.8f
    val bottom = size.height - baseH * 0.85f - size.height * 0.03f
    val cell = min((bottom - top) / sim.h, size.width * 0.74f / sim.w)
    val gw = cell * sim.w
    val gh = cell * sim.h
    return Geo((size.width - gw) / 2, top + (bottom - top - gh) / 2, cell, gw, gh, top, bottom, capH, baseH)
}

private fun glassPath(sim: SandSim, g: Geo): Path = Path().apply {
    val cx = g.ox + g.gw / 2
    val pad = 0.6f
    moveTo(cx - (sim.halfWidth(0f) + pad) * g.cell, g.oy)
    for (y in 1..sim.h) lineTo(cx - (sim.halfWidth(y.toFloat()) + pad) * g.cell, g.oy + y * g.cell)
    for (y in sim.h downTo 0) lineTo(cx + (sim.halfWidth(y.toFloat()) + pad) * g.cell, g.oy + y * g.cell)
    close()
}

private fun DrawScope.drawGrains(sim: SandSim, g: Geo, paints: Array<Paint>, points: Array<FloatArray>, counts: IntArray) {
    counts.fill(0)
    val w = sim.w
    val cells = sim.cells
    for (i in cells.indices) {
        val s = cells[i].toInt()
        if (s == 0) continue
        val x = i % w
        val y = i / w
        // Grains near the glass sit deeper in the round bulb: draw them darker.
        val edge = abs(x + 0.5f - w / 2f) / sim.halfWidth(y + 0.5f)
        val level = (s - 1 + if (edge > 0.84f) 2 else if (edge > 0.62f) 1 else 0).coerceAtMost(LEVELS.size - 1)
        // A fixed per-cell offset breaks up the grid so the grains read as loose sand.
        val hash = i * -0x61c88647
        val jx = ((hash ushr 8) and 255) / 255f - 0.5f
        val jy = ((hash ushr 16) and 255) / 255f - 0.5f
        val n = counts[level]
        points[level][n] = g.ox + (x + 0.5f + jx * 0.45f) * g.cell
        points[level][n + 1] = g.oy + (y + 0.5f + jy * 0.35f) * g.cell
        counts[level] = n + 2
    }
    for (i in 0 until sim.fallCount) {
        val level = sim.fallShade[i] - 1
        val n = counts[level]
        if (n + 2 > points[level].size) continue
        points[level][n] = g.ox + sim.fallX[i] * g.cell
        points[level][n + 1] = g.oy + sim.fallY[i] * g.cell
        counts[level] = n + 2
    }
    drawIntoCanvas { c ->
        for (l in paints.indices) {
            paints[l].strokeWidth = g.cell * (1.42f - l * 0.03f)
            if (counts[l] > 0) c.nativeCanvas.drawPoints(points[l], 0, counts[l], paints[l])
        }
    }
}

/** The thin falling stream, from the neck to the top of the pile, shimmering downwards. */
private fun DrawScope.drawStream(sim: SandSim, g: Geo, color: Int, t: Long) {
    val col = sim.w / 2
    var y = sim.neckRow + 1
    while (y < sim.h && sim.isFree(col, y)) y++
    val x = g.ox + (col + 0.5f) * g.cell
    val dash = g.cell * 0.9f
    val from = Offset(x, g.oy + sim.neckRow * g.cell)
    val to = Offset(x, g.oy + y * g.cell)
    drawLine(Color(color).copy(alpha = 0.35f), from, to, g.cell * 0.55f)
    drawLine(
        Color(color).copy(alpha = 0.9f), from, to, g.cell * 0.75f, cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash * 0.6f, dash), -(t / 2_000_000L % 10_000) * g.cell * 0.5f)
    )
}

private fun DrawScope.drawGlass(sim: SandSim, g: Geo, glass: Path) {
    drawPath(glass, Color(0xFF6E7A8A).copy(alpha = 0.30f), style = Stroke(g.cell * 1.8f))
    drawPath(glass, Color.White.copy(alpha = 0.85f), style = Stroke(g.cell * 0.8f))
    // Reflections: a soft wide one on the left of each bulb, a thin one on the right.
    val cx = g.ox + g.gw / 2
    for (bulb in 0..1) {
        val left = Path()
        val right = Path()
        var started = false
        for (k in 0..50) {
            val f = 0.12f + 0.74f * k / 50f
            val y = if (bulb == 0) f * sim.neckRow else sim.h - f * sim.neckRow
            val hw = sim.halfWidth(y)
            val py = g.oy + y * g.cell
            if (!started) { left.moveTo(cx - hw * 0.74f * g.cell, py); right.moveTo(cx + hw * 0.8f * g.cell, py); started = true }
            else { left.lineTo(cx - hw * 0.74f * g.cell, py); right.lineTo(cx + hw * 0.8f * g.cell, py) }
        }
        drawPath(left, Color.White.copy(alpha = 0.5f), style = Stroke(g.cell * 2.6f, cap = StrokeCap.Round))
        drawPath(right, Color.White.copy(alpha = 0.22f), style = Stroke(g.cell * 1.1f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawCaps(g: Geo) {
    val cx = size.width / 2
    fun wood(x: Float, y: Float, w: Float, h: Float) {
        val r = CornerRadius(h * 0.32f)
        drawRoundRect(Brush.verticalGradient(listOf(WoodLight, WoodMid, WoodDark), y, y + h), Offset(x, y), Size(w, h), r)
        // Cylinder shading: darker at the sides, a soft highlight just left of centre.
        drawRoundRect(
            Brush.horizontalGradient(
                0f to Color.Black.copy(0.35f), 0.38f to Color.White.copy(0.14f), 0.62f to Color.Transparent, 1f to Color.Black.copy(0.4f),
                startX = x, endX = x + w
            ),
            Offset(x, y), Size(w, h), r
        )
        drawLine(Color.White.copy(alpha = 0.25f), Offset(x + h * 0.4f, y + h * 0.18f), Offset(x + w - h * 0.4f, y + h * 0.18f), h * 0.08f, StrokeCap.Round)
    }
    val capW = g.gw * 1.06f
    wood(cx - capW / 2, g.oy - g.capH * 0.75f, capW, g.capH)
    val baseW = g.gw * 1.2f
    val baseTop = g.oy + g.gh - g.baseH * 0.2f
    wood(cx - baseW / 2, baseTop, baseW, g.baseH)
}

private fun DrawScope.drawShadow(g: Geo) {
    val cx = size.width / 2
    val y = g.oy + g.gh + g.baseH * 0.8f
    val w = g.gw * 1.5f
    drawOval(
        Brush.radialGradient(listOf(Color.Black.copy(0.22f), Color.Transparent), Offset(cx, y), w / 2),
        Offset(cx - w / 2, y - g.baseH * 0.45f), Size(w, g.baseH * 0.9f)
    )
}
