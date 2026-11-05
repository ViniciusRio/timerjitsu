package dev.timerjitsu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF12141C)
private val Panel = Color(0xFF1D2030)
private val Line = Color(0xFF34384C)
private val Muted = Color(0xFF9A9FB4)
private val Accent = Color(0xFFFF6B4A)

@Composable
fun TimerjitsuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, onPrimary = Ink, background = Ink, surface = Ink,
            surfaceVariant = Panel, surfaceContainer = Panel, surfaceContainerHigh = Panel,
            onSurface = Color.White, onSurfaceVariant = Muted, outline = Line,
        ),
    ) { Surface(Modifier.fillMaxSize(), color = Ink, content = content) }
}

private fun roleColor(role: Role?, timer: TimerDef, soft: Boolean): Color {
    val base = when (role) {
        Role.PREPARE -> Color(0xFFFFC247)
        Role.WORK -> Color(timer.color.argb)
        Role.REST -> Color(0xFF2EC4B6)
        Role.CYCLE_REST -> Color(0xFF8E7DFF)
        Role.COOLDOWN -> Color(0xFF5AA9FF)
        Role.OPEN -> Color(timer.color.argb)
        null -> Color(0xFF3DDC84)
    }
    return if (soft) lerp(base, Ink, 0.45f) else base
}

private fun onColor(c: Color) = if (c.luminance() > 0.35f) Ink else Color.White

private val Title = FontFamily.SansSerif
const val Slogan = "Todo segundo importa"

@Composable
private fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = Title, color = Color.White)
}

private fun summarize(t: TimerDef, pad: Boolean) = if (t.stopwatch) "Cronômetro" else formatTime(totalSeconds(t), pad)

// ---------------------------------------------------------------- Home

private enum class Tab(val label: String, val icon: ImageVector) {
    MINE("Meus timers", Icons.Filled.Timer),
    TEMPLATES("Modelos", Icons.Filled.FitnessCenter),
    SETTINGS("Ajustes", Icons.Filled.Tune),
}

@Composable
fun HomeScreen(
    timers: List<TimerDef>,
    settings: Settings,
    onSettings: (Settings) -> Unit,
    onStart: (TimerDef) -> Unit,
    onEdit: (TimerDef) -> Unit,
    onSave: (TimerDef) -> Unit,
    onDelete: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.MINE) }
    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, null) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.MINE -> MineTab(timers, settings, onStart, onEdit, onSave, onDelete)
                Tab.TEMPLATES -> TemplatesTab(settings, onStart, onSave)
                Tab.SETTINGS -> SettingsTab(settings, onSettings)
            }
        }
    }
}

@Composable
private fun SearchHeader(title: String, query: String, onQuery: (String) -> Unit) {
    var searching by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (searching) {
            OutlinedTextField(query, onQuery, Modifier.weight(1f), singleLine = true, placeholder = { Text("Buscar") })
        } else {
            Heading(title, Modifier.weight(1f))
        }
        IconButton({ searching = !searching; if (!searching) onQuery("") }) {
            Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search, "Buscar")
        }
    }
}

private fun TimerDef.matches(q: String) = q.isBlank() || name.contains(q, true) || description.contains(q, true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MineTab(
    timers: List<TimerDef>,
    settings: Settings,
    onStart: (TimerDef) -> Unit,
    onEdit: (TimerDef) -> Unit,
    onSave: (TimerDef) -> Unit,
    onDelete: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Text("Timer Jitsu", color = Accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(Slogan, color = Muted, fontSize = 13.sp)
            }
            SearchHeader("Meus timers", query, { query = it })
            val shown = timers.filter { it.matches(query) }.sortedByDescending { it.lastUsed }
            if (timers.isEmpty()) {
                Text(
                    "Nenhum timer ainda.\nToque em + ou escolha um modelo.",
                    Modifier.fillMaxWidth().padding(top = 80.dp), color = Muted, textAlign = TextAlign.Center,
                )
            }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.id }) { t ->
                    TimerCard(t, settings.padMinutes, { onStart(t) }) {
                        DropdownMenuItem({ Text("Editar") }, { it(); onEdit(t) }, leadingIcon = { Icon(Icons.Filled.Edit, null) })
                        DropdownMenuItem({ Text("Duplicar") }, {
                            it()
                            onSave(t.copy(id = java.util.UUID.randomUUID().toString(), name = t.name + " (cópia)", lastUsed = 0))
                        }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) })
                        DropdownMenuItem({ Text("Excluir") }, { it(); onDelete(t.id) }, leadingIcon = { Icon(Icons.Filled.Delete, null) })
                    }
                }
            }
        }
        FloatingActionButton({ adding = true }, Modifier.align(Alignment.BottomEnd).padding(20.dp), containerColor = Accent, contentColor = Ink) {
            Icon(Icons.Filled.Add, "Adicionar")
        }
    }
    if (adding) {
        ModalBottomSheet({ adding = false }, containerColor = Panel) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Heading("Novo timer")
                fun pick(t: TimerDef) { adding = false; onEdit(t) }
                AddOption(Icons.Filled.Bolt, "Tabata", "20 s de esforço, 10 s de pausa, 8 vezes") {
                    pick(TimerDef(name = "Tabata", prepare = 10, blocks = listOf(BlockDef(8, listOf(IntervalDef("Tiro", 20), IntervalDef("Descanso", 10, Role.REST))))))
                }
                AddOption(Icons.Filled.Repeat, "Rounds", "Blocos repetidos de trabalho e pausa") {
                    pick(TimerDef(name = "Rounds", prepare = 10, color = TimerColor.SKY, blocks = listOf(BlockDef(5, listOf(IntervalDef("Round", 60), IntervalDef("Descanso", 30, Role.REST))))))
                }
                AddOption(Icons.Filled.Timer, "Cronômetro", "Conta para cima, com voltas") {
                    pick(TimerDef(name = "Cronômetro", stopwatch = true, prepare = 3, color = TimerColor.AMBER))
                }
                AddOption(Icons.Filled.Tune, "Personalizado", "Monte seus próprios intervalos") {
                    pick(TimerDef(blocks = listOf(BlockDef(1, listOf(IntervalDef("Trabalho", 30))))))
                }
            }
        }
    }
}

@Composable
private fun AddOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, Line, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Accent)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(subtitle, color = Muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TimerCard(t: TimerDef, pad: Boolean, onClick: () -> Unit, menu: @Composable ColumnScope.(() -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val color = Color(t.color.argb)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).clickable(onClick = onClick).padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name.ifBlank { "Sem nome" }, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.description.isNotBlank()) Text(t.description, color = Muted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(summarize(t, pad), color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Box {
            IconButton({ open = true }) { Icon(Icons.Filled.MoreVert, "Opções", tint = Muted) }
            DropdownMenu(open, { open = false }) { menu { open = false } }
        }
    }
}

@Composable
private fun TemplatesTab(settings: Settings, onStart: (TimerDef) -> Unit, onSave: (TimerDef) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    Column {
        SearchHeader("Modelos", query, { query = it })
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Templates.filter { it.timer.matches(query) }.groupBy { it.category }.forEach { (category, list) ->
                item(key = "h-$category") { Text(category, color = Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
                items(list, key = { it.timer.id }) { tpl ->
                    TimerCard(tpl.timer, settings.padMinutes, { onStart(tpl.timer) }) { dismiss ->
                        DropdownMenuItem({ Text("Salvar nos meus timers") }, {
                            dismiss()
                            onSave(tpl.timer.copy(id = java.util.UUID.randomUUID().toString()))
                            android.widget.Toast.makeText(context, "Timer salvo", android.widget.Toast.LENGTH_SHORT).show()
                        }, leadingIcon = { Icon(Icons.Filled.BookmarkAdd, null) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(s: Settings, on: (Settings) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Heading("Ajustes")
        SettingGroup("Som") {
            ToggleRow("Sons", null, s.sound) { on(s.copy(sound = it)) }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text("Volume", fontWeight = FontWeight.SemiBold)
                Slider(s.volume, { on(s.copy(volume = it)) }, enabled = s.sound)
            }
            ToggleRow("Contagem regressiva", "Bipes nos últimos 3 segundos", s.countdown) { on(s.copy(countdown = it)) }
            ToggleRow("Tique-taque", "Um toque a cada segundo", s.ticking) { on(s.copy(ticking = it)) }
            ToggleRow("Voz", "Fala o nome de cada intervalo", s.voice) { on(s.copy(voice = it)) }
            ToggleRow("Vibração", null, s.vibration) { on(s.copy(vibration = it)) }
        }
        SettingGroup("Treino") {
            ToggleRow("Lanterna", "Pisca ao trocar de intervalo", s.torch) { on(s.copy(torch = it)) }
            ToggleRow("Flash na tela", "Pisca a tela ao trocar de intervalo", s.screenFlash) { on(s.copy(screenFlash = it)) }
            ToggleRow("Rotação", "Permite girar a tela durante o treino", s.rotation) { on(s.copy(rotation = it)) }
            ToggleRow("Cores suaves", "Tons menos intensos durante o treino", s.softColors) { on(s.copy(softColors = it)) }
            ToggleRow("Abrir último timer", "Abre direto o último usado ao iniciar", s.openLast) { on(s.copy(openLast = it)) }
            ToggleRow("Minutos com dois dígitos", "00:30 em vez de 0:30", s.padMinutes) { on(s.copy(padMinutes = it)) }
        }
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = Muted, fontWeight = FontWeight.SemiBold)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(vertical = 4.dp), content = content)
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 13.sp)
        }
        Switch(value, onChange)
    }
}

// ---------------------------------------------------------------- Builder

@Composable
fun BuilderScreen(initial: TimerDef, onCancel: () -> Unit, onSave: (TimerDef) -> Unit) {
    var d by remember(initial.id) { mutableStateOf(initial) }
    val valid = d.stopwatch || compile(d).any { it.role == Role.WORK || it.role == Role.REST }
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onCancel) { Icon(Icons.Filled.Close, "Cancelar") }
            Text(
                if (d.stopwatch) "Cronômetro" else "Timer · " + formatTime(totalSeconds(d)),
                Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 20.sp,
            )
            IconButton({ onSave(d.copy(name = d.name.ifBlank { "Novo timer" })) }, enabled = valid) {
                Icon(Icons.Filled.Check, "Salvar", tint = if (valid) Accent else Muted)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(d.name, { d = d.copy(name = it) }, Modifier.fillMaxWidth(), label = { Text("Nome") }, singleLine = true)
            OutlinedTextField(d.description, { d = d.copy(description = it) }, Modifier.fillMaxWidth(), label = { Text("Descrição") })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimerColor.entries.forEach { c ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color(c.argb))
                            .border(if (d.color == c) 3.dp else 0.dp, Color.White, CircleShape)
                            .clickable { d = d.copy(color = c) },
                    )
                }
            }
            DurationRow("Preparar", d.prepare) { d = d.copy(prepare = it) }
            if (d.stopwatch) {
                DurationRow("Volta automática (0 desliga)", d.lapSeconds) { d = d.copy(lapSeconds = it) }
            } else {
                d.blocks.forEachIndexed { index, block ->
                    BlockEditor(
                        block, index,
                        onChange = { nb -> d = d.copy(blocks = d.blocks.toMutableList().also { it[index] = nb }) },
                        onDelete = { d = d.copy(blocks = d.blocks.toMutableList().also { it.removeAt(index) }) },
                    )
                }
                OutlinedButton({ d = d.copy(blocks = d.blocks + BlockDef(3, listOf(IntervalDef("Trabalho", 30), IntervalDef("Descanso", 15, Role.REST)))) }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Adicionar série")
                }
                CountRow("Ciclos", d.cycles, 1) { d = d.copy(cycles = it) }
                if (d.cycles > 1) DurationRow("Descanso entre ciclos", d.restBetweenCycles) { d = d.copy(restBetweenCycles = it) }
                DurationRow("Relaxar no final", d.cooldown) { d = d.copy(cooldown = it) }
            }
            CuePicker("Som padrão", d.cue) { d = d.copy(cue = it) }
        }
    }
}

@Composable
private fun BlockEditor(block: BlockDef, index: Int, onChange: (BlockDef) -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Série ${index + 1}", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            IconButton(onDelete) { Icon(Icons.Filled.Delete, "Remover série", tint = Muted) }
        }
        CountRow("Repetições", block.repeats, 1) { onChange(block.copy(repeats = it)) }
        block.intervals.forEachIndexed { i, iv ->
            fun set(n: IntervalDef) = onChange(block.copy(intervals = block.intervals.toMutableList().also { it[i] = n }))
            HorizontalDivider(color = Line)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(iv.name, { set(iv.copy(name = it)) }, Modifier.weight(1f), label = { Text("Intervalo") }, singleLine = true)
                IconButton({ set(iv.copy(role = if (iv.role == Role.WORK) Role.REST else Role.WORK)) }) {
                    Icon(if (iv.role == Role.WORK) Icons.Filled.Bolt else Icons.Filled.Spa, if (iv.role == Role.WORK) "Trabalho" else "Descanso", tint = if (iv.role == Role.WORK) Accent else Color(0xFF2EC4B6))
                }
                IconButton({ onChange(block.copy(intervals = block.intervals.toMutableList().also { it.removeAt(i) })) }) {
                    Icon(Icons.Filled.Close, "Remover intervalo", tint = Muted)
                }
            }
            DurationRow("Duração", iv.seconds) { set(iv.copy(seconds = it)) }
        }
        TextButton({ onChange(block.copy(intervals = block.intervals + IntervalDef("Intervalo", 30))) }) {
            Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Adicionar intervalo")
        }
    }
}

@Composable
private fun DurationRow(label: String, seconds: Int, onChange: (Int) -> Unit) {
    fun bump(by: Int) = onChange((seconds + by).coerceIn(0, 6 * 3600))
    Column {
        Text(label, color = Muted, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton({ bump(-60) }) { Text("-1m") }
            TextButton({ bump(-5) }) { Text("-5s") }
            Text(formatTime(seconds.toLong()), Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            TextButton({ bump(5) }) { Text("+5s") }
            TextButton({ bump(60) }) { Text("+1m") }
        }
    }
}

@Composable
private fun CountRow(label: String, value: Int, min: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        IconButton({ onChange((value - 1).coerceAtLeast(min)) }) { Icon(Icons.Filled.Remove, "Menos") }
        Text("$value", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        IconButton({ onChange((value + 1).coerceAtMost(999)) }) { Icon(Icons.Filled.Add, "Mais") }
    }
}

@Composable
private fun CuePicker(label: String, value: Cue, onChange: (Cue) -> Unit) {
    val context = LocalContext.current
    val preview = remember { Feedback(context) }
    DisposableEffect(Unit) { onDispose { preview.release() } }
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { open = true }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text(value.label, color = Accent, fontWeight = FontWeight.Bold)
        Box {
            Icon(Icons.Filled.ExpandMore, null)
            DropdownMenu(open, { open = false }) {
                Cue.entries.forEach {
                    DropdownMenuItem({ Text(it.label) }, { open = false; onChange(it); preview.cue(it, 1f) })
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Workout

@Composable
fun WorkoutScreen(view: WorkoutView, settings: Settings, onSettings: (Settings) -> Unit, onEdit: () -> Unit) {
    val hostView = LocalView.current
    DisposableEffect(Unit) {
        hostView.keepScreenOn = true
        onDispose { hostView.keepScreenOn = false }
    }
    val done = view.status == Status.FINISHED
    val color = roleColor(if (done) null else view.segment?.role, view.timer, settings.softColors)
    val on = onColor(color)
    val flash = remember { Animatable(0f) }
    LaunchedEffect(view.flashSeq) {
        if (view.flashSeq > 0) {
            flash.snapTo(0.9f)
            flash.animateTo(0f, tween(350))
        }
    }
    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Ink).systemBarsPadding()) {
            if (maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize()) {
                    MainCard(view, settings, onSettings, onEdit, done, color, on, Modifier.weight(1f).fillMaxHeight().padding(vertical = 8.dp))
                    Column(Modifier.width(280.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                        TopRow(view, settings)
                        Details(view, settings, done)
                        ControlRow(view, done)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    TopRow(view, settings)
                    MainCard(view, settings, onSettings, onEdit, done, color, on, Modifier.weight(1f).fillMaxWidth())
                    Details(view, settings, done)
                    ControlRow(view, done)
                }
            }
        }
        if (flash.value > 0f) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
    }
}

@Composable
private fun MainCard(
    view: WorkoutView,
    settings: Settings,
    onSettings: (Settings) -> Unit,
    onEdit: () -> Unit,
    done: Boolean,
    color: Color,
    on: Color,
    modifier: Modifier,
) {
    Column(modifier.padding(horizontal = 10.dp).clip(RoundedCornerShape(24.dp)).background(color)) {
        if (view.status != Status.RUNNING && !done) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onEdit) {
                    Icon(Icons.Filled.Edit, null, tint = on); Spacer(Modifier.width(4.dp)); Text("Editar", color = on, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                IconButton({ onSettings(settings.copy(sound = !settings.sound)) }) {
                    Icon(if (settings.sound) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, "Som", tint = on)
                }
            }
        } else {
            Spacer(Modifier.height(14.dp))
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val time = formatTime(view.displaySeconds, settings.padMinutes)
            val size = minOf(maxWidth.value / (time.length * 0.56f), maxHeight.value * 0.6f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (done) "Concluído" else view.segment?.label.orEmpty(),
                    color = on, fontWeight = FontWeight.Black, fontSize = (size * 0.3f).coerceIn(18f, 40f).sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp),
                )
                if (!done) Text(time, color = on, fontWeight = FontWeight.Black, fontSize = size.sp, lineHeight = size.sp, maxLines = 1)
            }
        }
        if (!done && view.segment?.open != true) {
            LinearProgressIndicator(
                { view.progress },
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(6.dp).clip(CircleShape),
                color = on, trackColor = on.copy(alpha = 0.2f), drawStopIndicator = {},
            )
        }
        if (!done) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(Workout::skipPrevious) { Icon(Icons.Filled.SkipPrevious, "Anterior", tint = on) }
                Spacer(Modifier.weight(1f))
                if (view.segment?.open == true && view.status == Status.RUNNING) {
                    TextButton(Workout::lap) { Text("Marcar volta", color = on, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.weight(1f))
                }
                IconButton(Workout::skipNext) { Icon(Icons.Filled.SkipNext, if (view.segment?.open == true) "Encerrar" else "Próximo", tint = on) }
            }
        }
    }
}

@Composable
private fun Details(view: WorkoutView, settings: Settings, done: Boolean) {
    val next = view.next
    if (view.segment?.open == true && view.laps.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().heightIn(max = 110.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            view.laps.withIndex().reversed().forEach { (i, ms) ->
                Row(Modifier.fillMaxWidth()) {
                    Text("Volta ${i + 1}", Modifier.weight(1f), color = Muted)
                    Text(formatTime(ms / 1000, settings.padMinutes), fontWeight = FontWeight.Bold)
                }
            }
        }
    } else if (!done && next != null && !next.open) {
        val nc = roleColor(next.role, view.timer, settings.softColors)
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 8.dp).clip(RoundedCornerShape(16.dp)).border(BorderStroke(2.dp, nc), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Depois", color = Muted, fontSize = 14.sp)
            Spacer(Modifier.width(12.dp))
            Text(next.label, Modifier.weight(1f), color = nc, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatTime(next.millis / 1000, settings.padMinutes), color = nc, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TopRow(view: WorkoutView, settings: Settings) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(Workout::close) { Icon(Icons.Filled.Close, "Fechar") }
        Text(view.timer.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (view.segment?.open != true && view.status != Status.FINISHED) {
            Text(formatTime(view.totalSeconds, settings.padMinutes), fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
        }
    }
}

@Composable
private fun ControlRow(view: WorkoutView, done: Boolean) {
    val seg = view.segment
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Counter("Rounds", seg?.roundsLeft ?: 0, Modifier.weight(1f))
        when {
            done -> FilledIconButton(Workout::restart, Modifier.size(76.dp), shape = RoundedCornerShape(24.dp)) { Icon(Icons.Filled.Replay, "Repetir", Modifier.size(38.dp)) }
            else -> FilledIconButton(Workout::toggle, Modifier.size(76.dp), shape = RoundedCornerShape(24.dp)) {
                Icon(if (view.status == Status.RUNNING) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (view.status == Status.RUNNING) "Pausar" else "Iniciar", Modifier.size(42.dp))
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (view.status == Status.PAUSED) {
                TextButton(Workout::restart) { Icon(Icons.Filled.Replay, null); Spacer(Modifier.width(4.dp)); Text("Reiniciar") }
            } else {
                Counter("Ciclos", seg?.cyclesLeft ?: 0, Modifier)
            }
        }
    }
}

@Composable
private fun Counter(label: String, value: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (value > 0) {
            Text("$value", fontWeight = FontWeight.Black, fontSize = 30.sp)
            Text(label, color = Muted, fontSize = 13.sp)
        }
    }
}
