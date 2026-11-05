package dev.timerjitsu

import java.util.UUID

enum class Role { PREPARE, WORK, REST, CYCLE_REST, COOLDOWN, OPEN }

enum class TimerColor(val argb: Long, val label: String) {
    CORAL(0xFFFF6B4A, "Coral"),
    AMBER(0xFFFFC247, "Âmbar"),
    TEAL(0xFF2EC4B6, "Turquesa"),
    SKY(0xFF5AA9FF, "Céu"),
    VIOLET(0xFF8E7DFF, "Violeta"),
    PINK(0xFFFF6FB5, "Rosa"),
}

enum class Cue(val label: String) {
    BEEP("Bipe"),
    DOUBLE_BEEP("Bipe duplo"),
    BELL("Sino"),
    GONG("Gongo"),
    WHISTLE("Apito"),
    BUZZER("Buzina"),
    SOFT("Suave"),
}

data class IntervalDef(
    val name: String,
    val seconds: Int,
    val role: Role = Role.WORK,
    val cue: Cue? = null,
)

data class BlockDef(val repeats: Int, val intervals: List<IntervalDef>)

data class TimerDef(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val color: TimerColor = TimerColor.CORAL,
    val stopwatch: Boolean = false,
    val prepare: Int = 0,
    val cooldown: Int = 0,
    val cycles: Int = 1,
    val restBetweenCycles: Int = 0,
    val blocks: List<BlockDef> = emptyList(),
    val cue: Cue = Cue.BEEP,
    val lapSeconds: Int = 0,
    val lastUsed: Long = 0,
)

data class Template(val category: String, val timer: TimerDef)

private fun work(name: String, s: Int) = IntervalDef(name, s, Role.WORK)
private fun rest(s: Int, name: String = "Descanso") = IntervalDef(name, s, Role.REST)

/** Modelos próprios do Timer Jitsu, baseados em protocolos de treino de domínio comum. */
val Templates: List<Template> = listOf(
    Template("Intervalado", TimerDef(id = "t-tabata", name = "Tabata 20/10", description = "8 tiros de 20 s com 10 s de pausa", color = TimerColor.CORAL, prepare = 10, cue = Cue.BUZZER, blocks = listOf(BlockDef(8, listOf(work("Tiro", 20), rest(10)))))),
    Template("Intervalado", TimerDef(id = "t-3030", name = "Meio a meio", description = "30 s forte, 30 s leve, 10 vezes", color = TimerColor.AMBER, prepare = 10, cue = Cue.WHISTLE, blocks = listOf(BlockDef(10, listOf(work("Forte", 30), rest(30, "Leve")))))),
    Template("Intervalado", TimerDef(id = "t-emom", name = "EMOM 12", description = "Uma série a cada minuto por 12 minutos", color = TimerColor.VIOLET, prepare = 10, cue = Cue.DOUBLE_BEEP, blocks = listOf(BlockDef(12, listOf(work("Minuto", 60)))))),
    Template("Força", TimerDef(id = "t-sets", name = "Séries pesadas", description = "5 séries de 45 s com 2 min de recuperação", color = TimerColor.SKY, prepare = 10, cue = Cue.BELL, blocks = listOf(BlockDef(5, listOf(work("Série", 45), rest(120)))))),
    Template("Força", TimerDef(id = "t-circuit", name = "Circuito A-B-C", description = "3 voltas de 3 exercícios", color = TimerColor.TEAL, prepare = 15, cycles = 3, restBetweenCycles = 90, cue = Cue.BEEP, blocks = listOf(BlockDef(1, listOf(work("Exercício A", 40), rest(15), work("Exercício B", 40), rest(15), work("Exercício C", 40)))))),
    Template("Luta", TimerDef(id = "t-box", name = "Rounds de 3 min", description = "6 rounds de 3 min com 1 min de pausa", color = TimerColor.CORAL, prepare = 15, cue = Cue.GONG, blocks = listOf(BlockDef(6, listOf(work("Round", 180), rest(60)))))),
    Template("Luta", TimerDef(id = "t-roll", name = "Rola de 5 min", description = "5 lutas de 5 min com 1 min de troca", color = TimerColor.VIOLET, prepare = 15, cue = Cue.GONG, blocks = listOf(BlockDef(5, listOf(work("Luta", 300), rest(60, "Troca")))))),
    Template("Corrida", TimerDef(id = "t-run", name = "Tiros de corrida", description = "8 tiros de 90 s com trote de 2 min", color = TimerColor.PINK, prepare = 30, cooldown = 300, cue = Cue.WHISTLE, blocks = listOf(BlockDef(8, listOf(work("Tiro", 90), rest(120, "Trote")))))),
    Template("Mobilidade", TimerDef(id = "t-stretch", name = "Alongamento", description = "10 posições de 40 s com 10 s para trocar", color = TimerColor.TEAL, prepare = 10, cue = Cue.SOFT, blocks = listOf(BlockDef(10, listOf(work("Posição", 40), rest(10, "Trocar")))))),
    Template("Mobilidade", TimerDef(id = "t-breath", name = "Respiração", description = "Ciclos lentos de 4 min", color = TimerColor.SKY, prepare = 5, cue = Cue.SOFT, blocks = listOf(BlockDef(6, listOf(work("Inspire", 4), IntervalDef("Segure", 4, Role.REST), work("Expire", 6), IntervalDef("Pausa", 2, Role.REST)))))),
    Template("Livre", TimerDef(id = "t-watch", name = "Cronômetro com voltas", description = "Conta para cima e marca volta a cada minuto", color = TimerColor.AMBER, stopwatch = true, prepare = 5, lapSeconds = 60, cue = Cue.BEEP)),
)
