package dev.timerjitsu

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Settings(
    val sound: Boolean = true,
    val volume: Float = 1f,
    val countdown: Boolean = true,
    val ticking: Boolean = false,
    val voice: Boolean = false,
    val vibration: Boolean = true,
    val screenFlash: Boolean = false,
    val torch: Boolean = false,
    val rotation: Boolean = true,
    val softColors: Boolean = false,
    val openLast: Boolean = false,
    val padMinutes: Boolean = true,
)

class Store(context: Context) {
    private val file = File(context.filesDir, "timers.json")
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _timers = MutableStateFlow(load())
    private val _settings = MutableStateFlow(loadSettings())
    val timers: StateFlow<List<TimerDef>> = _timers
    val settings: StateFlow<Settings> = _settings

    fun save(t: TimerDef) {
        val list = _timers.value
        commit(if (list.any { it.id == t.id }) list.map { if (it.id == t.id) t else it } else listOf(t) + list)
    }

    fun delete(id: String) = commit(_timers.value.filterNot { it.id == id })

    fun markUsed(id: String) = commit(_timers.value.map { if (it.id == id) it.copy(lastUsed = System.currentTimeMillis()) else it })

    fun update(s: Settings) {
        _settings.value = s
        prefs.edit()
            .putBoolean("sound", s.sound).putFloat("volume", s.volume)
            .putBoolean("countdown", s.countdown).putBoolean("ticking", s.ticking)
            .putBoolean("voice", s.voice).putBoolean("vibration", s.vibration)
            .putBoolean("screenFlash", s.screenFlash).putBoolean("torch", s.torch)
            .putBoolean("rotation", s.rotation).putBoolean("softColors", s.softColors)
            .putBoolean("openLast", s.openLast).putBoolean("padMinutes", s.padMinutes)
            .apply()
    }

    private fun loadSettings(): Settings {
        val d = Settings()
        return Settings(
            prefs.getBoolean("sound", d.sound), prefs.getFloat("volume", d.volume),
            prefs.getBoolean("countdown", d.countdown), prefs.getBoolean("ticking", d.ticking),
            prefs.getBoolean("voice", d.voice), prefs.getBoolean("vibration", d.vibration),
            prefs.getBoolean("screenFlash", d.screenFlash), prefs.getBoolean("torch", d.torch),
            prefs.getBoolean("rotation", d.rotation), prefs.getBoolean("softColors", d.softColors),
            prefs.getBoolean("openLast", d.openLast), prefs.getBoolean("padMinutes", d.padMinutes),
        )
    }

    private fun commit(list: List<TimerDef>) {
        _timers.value = list
        val tmp = File(file.parentFile, "timers.json.tmp")
        tmp.writeText(JSONArray().apply { list.forEach { put(it.toJson()) } }.toString())
        tmp.renameTo(file)
    }

    private fun load(): List<TimerDef> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val a = JSONArray(file.readText())
            (0 until a.length()).map { a.getJSONObject(it).toTimer() }
        }.getOrDefault(emptyList())
    }
}

private fun TimerDef.toJson() = JSONObject()
    .put("id", id).put("name", name).put("description", description).put("color", color.name)
    .put("stopwatch", stopwatch).put("prepare", prepare).put("cooldown", cooldown)
    .put("cycles", cycles).put("restBetweenCycles", restBetweenCycles).put("cue", cue.name)
    .put("lapSeconds", lapSeconds).put("lastUsed", lastUsed)
    .put("blocks", JSONArray().apply {
        blocks.forEach { b ->
            put(JSONObject().put("repeats", b.repeats).put("intervals", JSONArray().apply {
                b.intervals.forEach { i ->
                    put(JSONObject().put("name", i.name).put("seconds", i.seconds).put("role", i.role.name).put("cue", i.cue?.name ?: JSONObject.NULL))
                }
            }))
        }
    })

private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: fallback

private fun JSONObject.toTimer(): TimerDef {
    val b = getJSONArray("blocks")
    return TimerDef(
        id = getString("id"), name = getString("name"), description = optString("description"),
        color = enumOr(optString("color"), TimerColor.CORAL), stopwatch = optBoolean("stopwatch"),
        prepare = optInt("prepare"), cooldown = optInt("cooldown"), cycles = optInt("cycles", 1),
        restBetweenCycles = optInt("restBetweenCycles"), cue = enumOr(optString("cue"), Cue.BEEP),
        lapSeconds = optInt("lapSeconds"), lastUsed = optLong("lastUsed"),
        blocks = (0 until b.length()).map { n ->
            val o = b.getJSONObject(n)
            val iv = o.getJSONArray("intervals")
            BlockDef(o.getInt("repeats"), (0 until iv.length()).map { k ->
                val i = iv.getJSONObject(k)
                IntervalDef(
                    i.getString("name"), i.getInt("seconds"), enumOr(i.optString("role"), Role.WORK),
                    if (i.isNull("cue")) null else enumOr(i.optString("cue"), Cue.BEEP),
                )
            })
        },
    )
}
