package com.example.routineforge

import com.example.routineforge.domain.SandSim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SandSimTest {
    private fun SandSim.grains() = cells.count { it != 0.toByte() } + fallCount

    @Test fun startsWithTheRequestedShareOnTopAndKeepsEveryGrain() {
        val sim = SandSim()
        sim.reset(1f)
        assertEquals(sim.total, sim.topCount)
        sim.reset(0.4f)
        assertEquals(sim.targetFor(0.4f), sim.topCount)
        assertEquals(sim.total, sim.grains())
    }

    @Test fun topTracksTheTimerAndPauseHoldsTheSand() {
        val sim = SandSim()
        sim.reset(1f)
        // A 60 s session at 60 fps: the top never holds less than the time left, and never lags far behind.
        for (frame in 1..3600) {
            val target = sim.targetFor(1f - frame / 3600f)
            sim.update(1 / 60f, target)
            assertTrue(sim.topCount >= target)
            if (frame == 1800) assertTrue(sim.topCount - target < 40)
            if (frame == 1800) repeat(600) { sim.update(1 / 60f, target) } // paused for 10 s
            if (frame == 1800) assertEquals(target, sim.topCount)
        }
        repeat(300) { sim.update(1 / 60f, 0) }
        assertEquals(0, sim.topCount)
        assertEquals(sim.total, sim.grains())
    }

    @Test fun newSessionDrainsThenTurnsTheGlassOver() {
        val sim = SandSim()
        sim.reset(0.3f) // stopped early with sand left on top
        var sawFlip = false
        var movedWhileInverted = false
        repeat(2000) {
            val before = sim.cells.copyOf()
            sim.update(1 / 60f, sim.total)
            sawFlip = sawFlip || sim.phase == SandSim.Phase.FLIP
            if (sim.inverted && !before.contentEquals(sim.cells)) movedWhileInverted = true
        }
        assertTrue(sawFlip)
        assertTrue(movedWhileInverted)
        assertEquals(sim.total, sim.topCount)
        assertEquals(sim.total, sim.grains())
    }
}
