package dev.timerjitsu

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val store by lazy { Store(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TimerjitsuTheme { App(store) } }
    }
}

@Composable
private fun App(store: Store) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val timers by store.timers.collectAsStateWithLifecycle()
    val settings by store.settings.collectAsStateWithLifecycle()
    val workout by Workout.view.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf<TimerDef?>(null) }
    var openedLast by rememberSaveable { mutableStateOf(false) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun start(t: TimerDef) {
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        Workout.open(context, store, t)
    }

    if (!openedLast) {
        openedLast = true
        if (settings.openLast && workout == null) timers.maxByOrNull { it.lastUsed }?.takeIf { it.lastUsed > 0 }?.let { Workout.open(context, store, it) }
    }

    val current = workout
    val editing = draft
    val inWorkout = current != null && editing == null
    DisposableEffect(inWorkout, settings.rotation) {
        activity.requestedOrientation =
            if (inWorkout && settings.rotation) ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {}
    }

    when {
        editing != null -> {
            BackHandler { draft = null }
            BuilderScreen(
                initial = editing,
                onCancel = { draft = null },
                onSave = {
                    store.save(it)
                    draft = null
                    if (current != null && current.timer.id == it.id) Workout.open(context, store, it)
                },
            )
        }
        current != null -> {
            BackHandler { Workout.close() }
            WorkoutScreen(
                view = current,
                settings = settings,
                onSettings = store::update,
                onEdit = { draft = current.timer },
            )
        }
        else -> HomeScreen(
            timers = timers,
            settings = settings,
            onSettings = store::update,
            onStart = ::start,
            onEdit = { draft = it },
            onSave = store::save,
            onDelete = store::delete,
        )
    }
}
