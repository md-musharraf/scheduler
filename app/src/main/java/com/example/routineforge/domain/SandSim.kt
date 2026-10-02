package com.example.routineforge.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Granular simulation of an hourglass, on a grid of grains inside a symmetric glass mask.
 *
 * Physics: grains in the bulk follow a falling-sand automaton (down, else diagonal, sometimes a
 * two-cell slide), which gives piles a ~30-35° angle of repose and opens a funnel in the top
 * bulb as it drains. Grains leaving the neck become particles in free fall under gravity until
 * they hit the pile, where they rejoin the automaton and roll into place.
 *
 * Logic: the neck is a gate. It only lets grains through while the top bulb holds more than the
 * target passed to [update], so the sand left on top always matches the timer's remaining
 * fraction and pausing simply closes the gate. When the target asks for a full top again (a new
 * session), the glass drains whatever is left, then turns over like a real one.
 */
class SandSim(val w: Int = 91, val h: Int = 153, seed: Int = 1) {
    enum class Phase { RUN, DRAIN, FLIP }

    /** The single row of the neck; rows above it are the top bulb. */
    val neckRow = h / 2
    private val center = w / 2f
    private val halfRows = h / 2f
    private val maxHalf = w / 2f - 1.5f

    /** 0 = empty, 1..[SHADES] = a grain with that colour shade. */
    val cells = ByteArray(w * h)
    private val wall = BooleanArray(w * h) { i -> abs(i % w + 0.5f - center) > halfWidth(i / w + 0.5f) }

    /** Grains in the glass: 55% of a bulb, so the bottom pile never reaches the neck. */
    val total: Int = (0 until neckRow * w).count { !wall[it] } * 55 / 100

    /** Grains in the top bulb, neck included. */
    var topCount = 0
        private set
    var phase = Phase.RUN
        private set
    /** 0..1 progress of the turn-over while [phase] is FLIP. */
    var flip = 0f
        private set
    /** Nothing moves and nothing is pending, so the caller can stop animating until the target changes. */
    var settled = false
        private set

    // Grains in free fall below the neck.
    private val maxFalling = 256
    val fallX = FloatArray(maxFalling)
    val fallY = FloatArray(maxFalling)
    private val fallV = FloatArray(maxFalling)
    val fallShade = ByteArray(maxFalling)
    var fallCount = 0
        private set

    private var rnd = (seed * -0x61c88647) or 1
    private var stepBudget = 0f
    private var moved = 0

    /** Half width of the glass interior, in cells, at a height of [y] cells. */
    fun halfWidth(y: Float): Float {
        val t = abs(y - halfRows) / halfRows
        val shape = if (t < SHOULDER) sin(PI / 2 * t / SHOULDER).toFloat()
        else 1f - 0.4f * ((t - SHOULDER) / (1f - SHOULDER)).let { it * it }
        return NECK_HALF + (maxHalf - NECK_HALF) * shape
    }

    fun isFree(x: Int, y: Int): Boolean = x in 0 until w && y in 0 until h && !wall[y * w + x] && cells[y * w + x] == EMPTY

    fun targetFor(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * total + 0.5f).toInt()

    /** Settled state with [fraction] of the sand on top, as if the glass had been running already. */
    fun reset(fraction: Float) {
        cells.fill(EMPTY)
        fallCount = 0
        phase = Phase.RUN
        flip = 0f
        // Pour everything into the top, filling up from the neck, centre first.
        var left = total
        val order = (0 until w).sortedBy { abs(it + 0.5f - center) }
        fill@ for (y in neckRow downTo 0) for (x in order) {
            if (left == 0) break@fill
            if (!wall[y * w + x]) { cells[y * w + x] = randomShade(); left-- }
        }
        topCount = total
        // Run the real automaton down to the target, then let everything come to rest.
        val target = targetFor(fraction)
        var guard = 0
        while (topCount > target && guard++ < 20_000) step(target, fast = true)
        for (i in 0 until 400) {
            moved = 0
            step(Int.MAX_VALUE, fast = true)
            if (moved == 0) break
        }
        settled = false
    }

    /** Advances [dt] seconds towards [targetTop] grains in the top bulb. */
    fun update(dt: Float, targetTop: Int) {
        if (phase == Phase.FLIP) {
            flip += dt / FLIP_SECONDS
            if (flip >= 1f) {
                cells.reverse() // (x, y) → (w-1-x, h-1-y): the glass is symmetric, so this is the turn-over
                topCount = (0 until (neckRow + 1) * w).count { cells[it] != EMPTY }
                flip = 0f
                phase = Phase.RUN
            }
            settled = false
            return
        }
        if (phase == Phase.RUN && targetTop >= total - REFILL_SLACK && topCount < total - REFILL_SLACK) phase = Phase.DRAIN
        val gate = if (phase == Phase.DRAIN) 0 else targetTop
        // Catch up quickly after a long jump (app back from the background, a stopped session).
        val fast = phase == Phase.DRAIN || topCount - gate > FAST_BACKLOG
        stepBudget += dt * STEPS_PER_SECOND * (if (fast) FAST_FACTOR else 1)
        val steps = stepBudget.toInt().coerceAtMost(if (fast) 60 else 6)
        stepBudget -= stepBudget.toInt()
        if (steps == 0 && fallCount == 0) return
        moved = 0
        repeat(steps) { step(gate, fast) }
        fall(dt)
        if (phase == Phase.DRAIN && topCount == 0 && fallCount == 0) phase = Phase.FLIP
        settled = moved == 0 && fallCount == 0 && phase == Phase.RUN
    }

    /** One automaton pass, bottom row first so a grain moves at most one cell per pass. */
    private fun step(gate: Int, fast: Boolean) {
        for (y in h - 2 downTo 0) {
            val ltr = next() and 1 == 0
            for (i in 0 until w) {
                val x = if (ltr) i else w - 1 - i
                val idx = y * w + x
                val g = cells[idx]
                if (g == EMPTY) continue
                if (y == neckRow) {
                    // The only way from top to bottom, open while the top holds more than the target.
                    if (topCount <= gate || !isFree(x, y + 1)) continue
                    cells[idx] = EMPTY
                    topCount--
                    moved++
                    if (fast || fallCount == maxFalling) rollDown(x, y + 1, g) else launch(x, y + 1, g)
                    continue
                }
                val to = moveTarget(x, y)
                if (to >= 0) {
                    cells[to] = g
                    cells[idx] = EMPTY
                    moved++
                }
            }
        }
    }

    /** Cell the grain at (x, y) moves into, or -1 if it rests. Never called for the neck row. */
    private fun moveTarget(x: Int, y: Int): Int {
        val below = (y + 1) * w
        if (isFree(x, y + 1)) return below + x
        val d = if (next() and 1 == 0) 1 else -1
        if (isFree(x + d, y + 1)) return below + x + d
        if (isFree(x - d, y + 1)) return below + x - d
        // An occasional two-cell slide lowers the angle of repose from 45° towards real sand.
        if (next() and 3 == 0) {
            if (isFree(x + d, y) && isFree(x + 2 * d, y + 1)) return below + x + 2 * d
            if (isFree(x - d, y) && isFree(x - 2 * d, y + 1)) return below + x - 2 * d
        }
        return -1
    }

    private fun launch(x: Int, y: Int, g: Byte) {
        val i = fallCount++
        fallX[i] = x + 0.5f + ((next() and 255) / 255f - 0.5f) * 0.6f
        fallY[i] = y.toFloat()
        fallV[i] = EXIT_SPEED
        fallShade[i] = g
    }

    /** Free fall under gravity; a grain that reaches the pile joins it and the automaton takes over. */
    private fun fall(dt: Float) {
        var i = 0
        while (i < fallCount) {
            fallV[i] += GRAVITY * dt
            val ny = fallY[i] + fallV[i] * dt
            val x = fallX[i].toInt()
            var landed = -1
            for (cy in fallY[i].toInt() + 1..ny.toInt()) if (!isFree(x, cy)) { landed = cy - 1; break }
            if (landed < 0) {
                fallY[i] = ny
                i++
                continue
            }
            place(x, landed, fallShade[i])
            fallCount--
            fallX[i] = fallX[fallCount]; fallY[i] = fallY[fallCount]; fallV[i] = fallV[fallCount]; fallShade[i] = fallShade[fallCount]
        }
        if (fallCount > 0) moved++
    }

    /** Drops a grain from (x, y) and lets it roll to rest at once (used when catching up). */
    private fun rollDown(x: Int, y: Int, g: Byte) {
        var idx = freeAbove(x, y)
        if (idx < 0) return place(x, y, g)
        var guard = h * 2
        while (guard-- > 0) {
            val to = moveTarget(idx % w, idx / w).takeIf { it >= 0 } ?: break
            idx = to
        }
        cells[idx] = g
    }

    /** Puts a grain at the first free cell at or above (x, y) in the bottom bulb. */
    private fun place(x: Int, y: Int, g: Byte) {
        val idx = freeAbove(x, y.coerceAtLeast(neckRow + 1))
        if (idx >= 0) { cells[idx] = g; return }
        // The column is full up to the neck (can't happen at 55% fill): any free bottom cell keeps the count right.
        val any = ((neckRow + 1) * w until w * h).first { !wall[it] && cells[it] == EMPTY }
        cells[any] = g
    }

    private fun freeAbove(x: Int, y: Int): Int {
        var cy = y
        while (cy > neckRow && !isFree(x, cy)) cy--
        return if (cy > neckRow) cy * w + x else -1
    }

    private fun randomShade(): Byte = (1 + (next() ushr 1) % SHADES).toByte()

    /** xorshift32: cheap, allocation-free randomness for the automaton. */
    private fun next(): Int {
        var x = rnd
        x = x xor (x shl 13); x = x xor (x ushr 17); x = x xor (x shl 5)
        rnd = x
        return x
    }

    companion object {
        const val SHADES = 4
        const val FLIP_SECONDS = 0.9f
        private const val EMPTY: Byte = 0
        private const val NECK_HALF = 1.5f
        private const val SHOULDER = 0.62f
        private const val STEPS_PER_SECOND = 180f
        private const val FAST_FACTOR = 12
        private const val FAST_BACKLOG = 40
        private const val REFILL_SLACK = 3
        /** A cell is about a millimetre of a real 15 cm hourglass, so g = 9.8 m/s² ≈ 9000 cells/s². */
        private const val GRAVITY = 9000f
        private const val EXIT_SPEED = 40f
    }
}
