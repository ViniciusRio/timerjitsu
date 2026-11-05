package dev.timerjitsu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    private val interval = TimerDef(
        name = "x", prepare = 5, cooldown = 7, cycles = 2, restBetweenCycles = 9,
        blocks = listOf(BlockDef(2, listOf(IntervalDef("W", 10), IntervalDef("R", 4, Role.REST)))),
    )

    @Test fun compilesInOrderWithCycleRestOnlyBetweenCycles() {
        val roles = compile(interval).map { it.role }
        val round = listOf(Role.WORK, Role.REST, Role.WORK, Role.REST)
        assertEquals(listOf(Role.PREPARE) + round + Role.CYCLE_REST + round + Role.COOLDOWN, roles)
        assertEquals(5 + 28 + 9 + 28 + 7L, totalSeconds(interval))
    }

    @Test fun zeroDurationsAreDropped() {
        val t = TimerDef(blocks = listOf(BlockDef(1, listOf(IntervalDef("W", 10), IntervalDef("R", 0, Role.REST)))))
        assertEquals(listOf(Role.WORK), compile(t).map { it.role })
    }

    @Test fun roundsAndCyclesCountDown() {
        val segs = compile(interval)
        assertEquals(listOf(2, 2, 2, 1, 1), segs.take(5).map { it.roundsLeft })
        assertEquals(2, segs[1].cyclesLeft)
        assertEquals(1, segs.last { it.role == Role.WORK }.cyclesLeft)
    }

    @Test fun countdownFiresOncePerSecondForLastThree() {
        val s = WorkoutSession(TimerDef(blocks = listOf(BlockDef(1, listOf(IntervalDef("W", 5))))))
        s.toggle()
        val events = (1..50).flatMap { s.advance(100) }
        assertEquals(listOf(3, 2, 1), events.filterIsInstance<WorkoutEvent.Countdown>().map { it.seconds })
        assertEquals(WorkoutEvent.Finished, events.last())
        assertEquals(Status.FINISHED, s.status)
    }

    @Test fun largeDeltaCarriesOverSegments() {
        val s = WorkoutSession(interval)
        s.toggle()
        val events = s.advance(5_000 + 10_000 + 1_500)
        assertEquals(2, events.filterIsInstance<WorkoutEvent.SegmentStarted>().size)
        assertEquals(Role.REST, s.current?.role)
        assertEquals(2_500L, s.remainingMillis)
    }

    @Test fun pausedSessionDoesNotAdvance() {
        val s = WorkoutSession(interval)
        s.toggle(); s.toggle()
        assertTrue(s.advance(10_000).isEmpty())
        assertEquals(Status.PAUSED, s.status)
        assertEquals(5_000L, s.remainingMillis)
    }

    @Test fun skipPreviousRestartsCurrentAfterTwoSeconds() {
        val s = WorkoutSession(interval)
        s.toggle()
        s.skipNext()
        s.advance(3_000)
        s.skipPrevious()
        assertEquals(1, s.index)
        assertEquals(0L, s.elapsed)
        s.skipPrevious()
        assertEquals(0, s.index)
    }

    @Test fun skippingPastEndFinishes() {
        val s = WorkoutSession(TimerDef(blocks = listOf(BlockDef(1, listOf(IntervalDef("W", 5))))))
        s.toggle()
        assertEquals(listOf<WorkoutEvent>(WorkoutEvent.Finished), s.skipNext())
    }

    @Test fun stopwatchCountsUpWithAutoAndManualLaps() {
        val s = WorkoutSession(TimerDef(stopwatch = true, lapSeconds = 10))
        s.toggle()
        val events = s.advance(25_000)
        assertEquals(2, events.filterIsInstance<WorkoutEvent.Lap>().size)
        assertEquals(25_000L, s.elapsed)
        s.lap()
        assertEquals(listOf(10_000L, 10_000L, 5_000L), s.laps)
    }

    @Test fun restartClearsEverything() {
        val s = WorkoutSession(interval)
        s.toggle(); s.advance(7_000); s.restart()
        assertEquals(0, s.index)
        assertEquals(0L, s.elapsed)
        assertEquals(Status.IDLE, s.status)
    }

    @Test fun formatsTime() {
        assertEquals("00:05", formatTime(5))
        assertEquals("1:05", formatTime(65, padMinutes = false))
        assertEquals("1:01:01", formatTime(3661))
    }
}
