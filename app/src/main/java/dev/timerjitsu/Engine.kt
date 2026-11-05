package dev.timerjitsu

data class Segment(
    val role: Role,
    val label: String,
    val millis: Long,
    val cue: Cue,
    val roundsLeft: Int,
    val cyclesLeft: Int,
) {
    val open: Boolean get() = role == Role.OPEN
}

sealed interface WorkoutEvent {
    data class SegmentStarted(val segment: Segment) : WorkoutEvent
    data class Countdown(val seconds: Int) : WorkoutEvent
    data object Tick : WorkoutEvent
    data class Lap(val number: Int) : WorkoutEvent
    data object Finished : WorkoutEvent
}

enum class Status { IDLE, RUNNING, PAUSED, FINISHED }

fun compile(t: TimerDef): List<Segment> {
    val out = mutableListOf<Segment>()
    val cycles = t.cycles.coerceAtLeast(1)
    val blocks = t.blocks.filter { it.repeats > 0 && it.intervals.any { i -> i.seconds > 0 } }
    val roundsPerCycle = blocks.sumOf { it.repeats }
    fun add(role: Role, label: String, seconds: Int, cue: Cue, rounds: Int, cyclesLeft: Int) {
        if (seconds > 0) out += Segment(role, label, seconds * 1000L, cue, rounds, cyclesLeft)
    }
    add(Role.PREPARE, "Preparar", t.prepare, t.cue, roundsPerCycle, cycles)
    if (t.stopwatch) {
        out += Segment(Role.OPEN, "Tempo", Long.MAX_VALUE, t.cue, 0, 0)
        return out
    }
    for (cycle in 0 until cycles) {
        val cyclesLeft = cycles - cycle
        var rounds = roundsPerCycle
        for (block in blocks) {
            repeat(block.repeats) {
                for (i in block.intervals) add(i.role, i.name, i.seconds, i.cue ?: t.cue, rounds, cyclesLeft)
                rounds--
            }
        }
        if (cycle < cycles - 1) add(Role.CYCLE_REST, "Descanso entre ciclos", t.restBetweenCycles, t.cue, 0, cyclesLeft - 1)
    }
    add(Role.COOLDOWN, "Relaxar", t.cooldown, t.cue, 0, 0)
    return out
}

/** Total duration in seconds; stopwatches only count their prepare phase. */
fun totalSeconds(t: TimerDef): Long =
    compile(t).filter { !it.open }.sumOf { it.millis } / 1000

fun formatTime(totalSec: Long, padMinutes: Boolean = true): String {
    val s = totalSec.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return when {
        h > 0 -> "%d:%02d:%02d".format(h, m, sec)
        padMinutes -> "%02d:%02d".format(m, sec)
        else -> "%d:%02d".format(m, sec)
    }
}

/** Pure state machine: the caller reports elapsed time through [advance]. */
class WorkoutSession(val timer: TimerDef) {
    val segments: List<Segment> = compile(timer)
    var index = 0; private set
    var elapsed = 0L; private set
    var status = Status.IDLE; private set
    private val lapList = mutableListOf<Long>()
    private var lapStart = 0L
    val laps: List<Long> get() = lapList

    val current: Segment? get() = segments.getOrNull(index)
    val next: Segment? get() = segments.getOrNull(index + 1)

    val remainingMillis: Long
        get() = current?.let { if (it.open) 0L else (it.millis - elapsed).coerceAtLeast(0) } ?: 0L

    val totalRemainingMillis: Long
        get() = remainingMillis + segments.drop(index + 1).filter { !it.open }.sumOf { it.millis }

    fun toggle(): List<WorkoutEvent> = when (status) {
        Status.IDLE -> {
            if (segments.isEmpty()) {
                status = Status.FINISHED
                listOf(WorkoutEvent.Finished)
            } else {
                status = Status.RUNNING
                listOf(WorkoutEvent.SegmentStarted(segments[0]))
            }
        }
        Status.RUNNING -> { status = Status.PAUSED; emptyList() }
        Status.PAUSED -> { status = Status.RUNNING; emptyList() }
        Status.FINISHED -> emptyList()
    }

    fun advance(deltaMillis: Long): List<WorkoutEvent> {
        if (status != Status.RUNNING || deltaMillis <= 0) return emptyList()
        val events = mutableListOf<WorkoutEvent>()
        var left = deltaMillis
        while (left > 0 && status == Status.RUNNING) {
            val seg = current ?: break
            if (seg.open) {
                elapsed += left
                left = 0
                val every = timer.lapSeconds * 1000L
                while (every > 0 && elapsed - lapStart >= every) {
                    lapStart += every
                    lapList += every
                    events += WorkoutEvent.Lap(lapList.size)
                }
                break
            }
            val before = ceilSeconds(seg.millis - elapsed)
            val step = minOf(left, seg.millis - elapsed)
            elapsed += step
            left -= step
            val after = ceilSeconds(seg.millis - elapsed)
            for (n in before - 1 downTo maxOf(after, 1)) {
                events += if (n <= 3) WorkoutEvent.Countdown(n.toInt()) else WorkoutEvent.Tick
            }
            if (elapsed >= seg.millis) events += moveTo(index + 1)
        }
        return events
    }

    fun skipNext(): List<WorkoutEvent> = if (status == Status.FINISHED) emptyList() else moveTo(index + 1)

    fun skipPrevious(): List<WorkoutEvent> {
        if (status == Status.FINISHED) return emptyList()
        return if (elapsed > 2000 || index == 0) moveTo(index) else moveTo(index - 1)
    }

    fun lap() {
        if (current?.open != true || status != Status.RUNNING) return
        lapList += elapsed - lapStart
        lapStart = elapsed
    }

    fun restart() {
        index = 0
        elapsed = 0
        lapStart = 0
        lapList.clear()
        status = Status.IDLE
    }

    private fun moveTo(target: Int): List<WorkoutEvent> {
        elapsed = 0
        lapStart = 0
        index = target
        val seg = segments.getOrNull(target)
        if (seg == null) {
            index = segments.size
            status = Status.FINISHED
            return listOf(WorkoutEvent.Finished)
        }
        if (status == Status.IDLE) status = Status.PAUSED
        return listOf(WorkoutEvent.SegmentStarted(seg))
    }

    private fun ceilSeconds(ms: Long): Long = (ms + 999) / 1000
}
