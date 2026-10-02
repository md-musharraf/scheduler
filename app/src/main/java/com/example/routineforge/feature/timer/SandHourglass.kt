package com.example.routineforge.feature.timer

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
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
import kotlin.math.cos
import kotlin.math.min
import kotlin.random.Random

private val WoodLight = Color(0xFFC08A55)
private val WoodMid = Color(0xFF8A5630)
private val WoodDark = Color(0xFF4A2B15)
private val GlassEdge = Color(0xFF5F6B7A)

/**
 * A wood-and-glass hourglass whose sand is simulated grain by grain ([SandSim]) and drawn as a
 * dense, finely textured mass: its surface follows the simulation (the funnel opening in the top
 * bulb, the cone building below), with a thin stream through the neck.
 * [remaining] (0..1, read every frame) is the share of the sand that belongs on top: the time left.
 */
@Composable
fun SandHourglass(remaining: () -> Float, running: Boolean, sand: Color, modifier: Modifier = Modifier) {
    val sim = remember { SandSim() }
    val currentRemaining by rememberUpdatedState(remaining)
    val isRunning by rememberUpdatedState(running)
    var ready by remember { mutableStateOf(false) }
    var frame by remember { mutableLongStateOf(0L) }
    val texture = remember(sand) { sandTexture(sand) }
    val textureMatrix = remember { Matrix() }
    val sandPaint = remember(texture) {
        Paint().apply { isAntiAlias = true; shader = BitmapShader(texture, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
    }
    val paths = remember { SandPaths(sim.w) }

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
        rotate(turn, Offset(size.width / 2, g.oy + g.gh / 2)) {
            val glass = glassPath(sim, g, pad = 0.9f)
            drawPath(glass, Brush.horizontalGradient(listOf(Color(0x30A9B8C9), Color(0x50FFFFFF), Color(0x12A9B8C9), Color(0x2A8A9AAD)), g.ox, g.ox + g.gw))
            if (ready) {
                textureMatrix.setScale(density * 0.85f, density * 0.85f)
                sandPaint.shader.setLocalMatrix(textureMatrix)
                clipPath(glassPath(sim, g, pad = -0.15f)) { drawSand(sim, g, paths, sandPaint, sand) }
                if (isRunning && sim.phase == SandSim.Phase.RUN && sim.topCount > 0) drawStream(sim, g, sand, t)
                for (i in 0 until sim.fallCount) {
                    drawCircle(lerp(sand, Color.Black, 0.15f), g.cell * 0.45f, Offset(g.ox + sim.fallX[i] * g.cell, g.oy + sim.fallY[i] * g.cell))
                }
            }
            drawGlass(sim, g, glass)
            drawFrame(g)
        }
    }
}

/** Where the grid sits on the canvas, and the frame around it. */
private class Geo(val ox: Float, val oy: Float, val cell: Float, val gw: Float, val gh: Float, val capH: Float, val baseH: Float)

private fun DrawScope.geometry(sim: SandSim): Geo {
    val capH = size.height * 0.055f
    val baseH = size.height * 0.075f
    val top = capH * 0.85f
    val bottom = size.height - baseH - size.height * 0.03f
    val cell = min((bottom - top) / sim.h, size.width * 0.66f / sim.w)
    val gw = cell * sim.w
    val gh = cell * sim.h
    return Geo((size.width - gw) / 2, top + (bottom - top - gh) / 2, cell, gw, gh, capH, baseH)
}

/** Outline of the glass interior, widened by [pad] cells. */
private fun glassPath(sim: SandSim, g: Geo, pad: Float): Path = Path().apply {
    val cx = g.ox + g.gw / 2
    moveTo(cx - (sim.halfWidth(0f) + pad) * g.cell, g.oy)
    for (y in 1..sim.h) lineTo(cx - (sim.halfWidth(y.toFloat()) + pad) * g.cell, g.oy + y * g.cell)
    for (y in sim.h downTo 0) lineTo(cx + (sim.halfWidth(y.toFloat()) + pad) * g.cell, g.oy + y * g.cell)
    close()
}

/** Reused every frame: per-column surface heights and the two sand bodies. */
private class SandPaths(w: Int) {
    val surface = FloatArray(w)
    val floor = IntArray(w)
    val smooth = FloatArray(w)
    val body = Path()
    val edge = Path()
}

/** Sand fill of both bulbs: a textured body under each surface, shaded for volume, with a lit top edge. */
private fun DrawScope.drawSand(sim: SandSim, g: Geo, p: SandPaths, paint: Paint, sand: Color) {
    val neckY = g.oy + (sim.neckRow + 1) * g.cell
    val inverted = sim.inverted
    for (topBulb in listOf(true, false)) {
        if (inverted && topBulb) continue // drained before the turn-over
        if (!surfaces(sim, p, topBulb, inverted)) continue
        p.body.reset()
        p.edge.reset()
        var first = -1
        var last = -1
        var inRun = false
        for (x in 0 until sim.w) {
            if (p.floor[x] < 0) { inRun = false; continue }
            val px = g.ox + (x + 0.5f) * g.cell
            val py = g.oy + p.smooth[x] * g.cell
            if (first < 0) { first = x; p.body.moveTo(g.ox + x * g.cell, py) }
            p.body.lineTo(px, py)
            last = x
            // Lit edge only where the column holds sand.
            val hasSand = if (inverted) p.surface[x] > p.floor[x] else p.surface[x] <= p.floor[x]
            if (hasSand && inRun) p.edge.lineTo(px, py) else if (hasSand) p.edge.moveTo(px, py)
            inRun = hasSand
        }
        val lowY = when {
            inverted -> neckY - g.cell * 3 // the body extends towards the neck it rests against
            topBulb -> neckY + g.cell * 3
            else -> g.oy + g.gh + g.cell * 3
        }
        p.body.lineTo(g.ox + (last + 1) * g.cell, g.oy + p.smooth[last] * g.cell)
        p.body.lineTo(g.ox + (last + 1) * g.cell, lowY)
        p.body.lineTo(g.ox + first * g.cell, lowY)
        p.body.close()
        clipRect(top = if (topBulb) g.oy else neckY, bottom = if (topBulb) neckY else g.oy + g.gh) {
            drawIntoCanvas { it.nativeCanvas.drawPath(p.body.asAndroidPath(), paint) }
            // Volume: a rounded body seen through round glass, darker towards the sides.
            drawPath(
                p.body,
                Brush.horizontalGradient(
                    0f to Color.Black.copy(0.42f), 0.22f to Color.Black.copy(0.08f), 0.4f to Color.White.copy(0.10f),
                    0.62f to Color.Transparent, 0.85f to Color.Black.copy(0.18f), 1f to Color.Black.copy(0.48f),
                    startX = g.ox + g.gw * 0.04f, endX = g.ox + g.gw * 0.96f
                )
            )
            drawPath(p.edge, lerp(sand, Color.White, 0.45f).copy(alpha = 0.7f), style = Stroke(g.cell * 0.7f, cap = StrokeCap.Round))
        }
    }
}

/**
 * Fills [SandPaths.surface] with each column's sand surface (in rows) for one bulb, scanning up from
 * the glass floor of the column; an empty column reports a surface under its floor, so nothing shows.
 * Returns false when the bulb has no columns at all.
 */
private fun surfaces(sim: SandSim, p: SandPaths, topBulb: Boolean, inverted: Boolean = false): Boolean {
    // Scan against gravity: up from the glass floor, or (inverted, bottom bulb) down from the neck end.
    val up = if (inverted) 1 else -1
    val start = when {
        topBulb -> sim.neckRow
        inverted -> sim.neckRow + 1
        else -> sim.h - 1
    }
    val end = when {
        topBulb -> 0
        inverted -> sim.h - 1
        else -> sim.neckRow + 1
    }
    var any = false
    for (x in 0 until sim.w) {
        var f = -1
        var r0 = start
        while (true) {
            if (sim.isInside(x, r0)) { f = r0; break }
            if (r0 == end) break
            r0 += up
        }
        p.floor[x] = f
        if (f < 0) continue
        any = true
        var top = f - 2 * up // empty: past the glass floor, so nothing shows
        var gap = 0
        var r = f
        while (sim.isInside(x, r)) {
            if (!sim.isFree(x, r)) { top = if (inverted) r + 1 else r; gap = 0 } else if (++gap > 2) break
            if (r == end) break
            r += up
        }
        p.surface[x] = top.toFloat()
    }
    // Light smoothing so the surface reads as poured sand, not steps.
    for (x in 0 until sim.w) {
        if (p.floor[x] < 0) continue
        var sum = p.surface[x] * 2
        var n = 2
        if (x > 0 && p.floor[x - 1] >= 0) { sum += p.surface[x - 1]; n++ }
        if (x < sim.w - 1 && p.floor[x + 1] >= 0) { sum += p.surface[x + 1]; n++ }
        p.smooth[x] = sum / n
    }
    return any
}

/** The thin falling stream from the neck to the top of the pile, with glints sliding down it. */
private fun DrawScope.drawStream(sim: SandSim, g: Geo, sand: Color, t: Long) {
    val col = sim.w / 2
    var y = sim.neckRow + 1
    while (y < sim.h && sim.isFree(col, y)) y++
    val x = g.ox + (col + 0.5f) * g.cell
    val from = Offset(x, g.oy + (sim.neckRow + 0.5f) * g.cell)
    val to = Offset(x, g.oy + (y + 0.5f) * g.cell)
    drawLine(lerp(sand, Color.Black, 0.12f), from, to, g.cell * 1.1f)
    val step = g.cell * 2.2f
    var gy = from.y + (t / 1_000_000L % 100_000) / 1000f * g.cell * 9f % step
    while (gy < to.y) {
        drawLine(lerp(sand, Color.White, 0.35f), Offset(x, gy), Offset(x, (gy + g.cell * 0.6f).coerceAtMost(to.y)), g.cell * 0.55f, StrokeCap.Round)
        gy += step
    }
}

/** Thick blown glass: a dark outer edge, a bright inner edge, and curved reflections on both bulbs. */
private fun DrawScope.drawGlass(sim: SandSim, g: Geo, glass: Path) {
    drawPath(glassPath(sim, g, pad = 1.6f), GlassEdge.copy(alpha = 0.32f), style = Stroke(g.cell * 1.0f))
    drawPath(glass, Color.White.copy(alpha = 0.8f), style = Stroke(g.cell * 0.7f))
    val cx = g.ox + g.gw / 2
    for (bulb in 0..1) {
        fun streak(from: Float, to: Float, at: Float, width: Float, alpha: Float) {
            val path = Path()
            for (k in 0..40) {
                val f = from + (to - from) * k / 40f
                val y = if (bulb == 0) f * sim.neckRow else sim.h - f * sim.neckRow
                val px = cx + sim.halfWidth(y) * at * g.cell
                val py = g.oy + y * g.cell
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(path, Color.White.copy(alpha = alpha), style = Stroke(g.cell * width, cap = StrokeCap.Round))
        }
        streak(0.12f, 0.86f, -0.74f, 2.8f, 0.42f) // broad window reflection on the left
        streak(0.22f, 0.5f, -0.6f, 1.2f, 0.85f) // bright specular glint
        streak(0.18f, 0.8f, 0.82f, 1.0f, 0.22f) // rim light on the right
    }
}

/** Turned wooden frame: top cap, two posts with beads, and a two-tier base. */
private fun DrawScope.drawFrame(g: Geo) {
    val cx = size.width / 2
    fun slab(x: Float, y: Float, w: Float, h: Float) {
        val r = CornerRadius(h * 0.4f)
        drawRoundRect(Brush.verticalGradient(listOf(WoodLight, WoodMid, WoodDark), y, y + h), Offset(x, y), Size(w, h), r)
        drawRoundRect(
            Brush.horizontalGradient(
                0f to Color.Black.copy(0.38f), 0.35f to Color.White.copy(0.16f), 0.6f to Color.Transparent, 1f to Color.Black.copy(0.42f),
                startX = x, endX = x + w
            ),
            Offset(x, y), Size(w, h), r
        )
        // Grain lines and a bevel highlight.
        for (k in 1..2) drawLine(WoodDark.copy(alpha = 0.18f), Offset(x + h, y + h * (0.35f + 0.25f * k)), Offset(x + w - h, y + h * (0.3f + 0.27f * k)), h * 0.05f)
        drawLine(Color.White.copy(alpha = 0.3f), Offset(x + h * 0.5f, y + h * 0.16f), Offset(x + w - h * 0.5f, y + h * 0.16f), h * 0.08f, StrokeCap.Round)
    }
    val capTop = g.oy - g.capH * 0.8f
    val baseTop = g.oy + g.gh - g.baseH * 0.15f
    // Posts stand just outside the widest part of the glass.
    val postW = g.gw * 0.045f
    val postTop = capTop + g.capH
    for (side in listOf(-1f, 1f)) {
        val px = cx + side * g.gw * 0.6f
        val turned = Brush.horizontalGradient(listOf(WoodDark, WoodLight, WoodMid, WoodDark), px - postW, px + postW)
        drawRoundRect(turned, Offset(px - postW / 2, postTop), Size(postW, baseTop - postTop), CornerRadius(postW / 2))
        for (f in listOf(0.08f, 0.5f, 0.92f)) {
            val by = postTop + (baseTop - postTop) * f
            drawOval(turned, Offset(px - postW * 0.95f, by - postW * 0.6f), Size(postW * 1.9f, postW * 1.2f))
        }
    }
    slab(cx - g.gw * 0.67f, capTop, g.gw * 1.34f, g.capH)
    slab(cx - g.gw * 0.68f, baseTop, g.gw * 1.36f, g.baseH * 0.6f)
    slab(cx - g.gw * 0.74f, baseTop + g.baseH * 0.52f, g.gw * 1.48f, g.baseH * 0.48f)
}

private fun DrawScope.drawShadow(g: Geo) {
    val cx = size.width / 2
    val y = g.oy + g.gh + g.baseH * 0.85f
    val w = g.gw * 1.75f
    drawOval(
        Brush.radialGradient(listOf(Color.Black.copy(0.24f), Color.Transparent), Offset(cx, y), w / 2),
        Offset(cx - w / 2, y - g.baseH * 0.4f), Size(w, g.baseH * 0.8f)
    )
}

/** A tile of fine sand: tiny grains varying around the base colour, with light quartz and dark mineral specks. */
private fun sandTexture(sand: Color): Bitmap {
    val n = 96
    val rnd = Random(7)
    val pixels = IntArray(n * n) {
        val r = rnd.nextFloat()
        val c = when {
            r < 0.05f -> lerp(sand, Color.White, 0.38f + rnd.nextFloat() * 0.22f)
            r < 0.12f -> lerp(sand, Color.Black, 0.3f + rnd.nextFloat() * 0.18f)
            else -> {
                val v = (rnd.nextFloat() - 0.5f) * 0.26f
                if (v > 0) lerp(sand, Color.White, v) else lerp(sand, Color.Black, -v)
            }
        }
        c.toArgb()
    }
    return Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888)
}
