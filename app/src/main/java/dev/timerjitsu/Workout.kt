package dev.timerjitsu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WorkoutView(
    val timer: TimerDef,
    val status: Status,
    val segment: Segment?,
    val next: Segment?,
    val displaySeconds: Long,
    val totalSeconds: Long,
    val progress: Float,
    val laps: List<Long>,
    val flashSeq: Int,
)

/** Mantém a sessão viva fora do ciclo de vida da Activity. */
object Workout {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _view = MutableStateFlow<WorkoutView?>(null)
    val view: StateFlow<WorkoutView?> = _view
    private var session: WorkoutSession? = null
    private var loop: Job? = null
    private var feedback: Feedback? = null
    private var store: Store? = null
    private var appContext: Context? = null
    private var flashSeq = 0

    fun open(context: Context, store: Store, timer: TimerDef) {
        close()
        appContext = context.applicationContext
        this.store = store
        feedback = Feedback(context)
        session = WorkoutSession(timer)
        store.markUsed(timer.id)
        publish()
    }

    fun toggle() {
        val s = session ?: return
        handle(s.toggle())
        val ctx = appContext ?: return
        if (s.status == Status.RUNNING) {
            ctx.startForegroundService(Intent(ctx, WorkoutService::class.java))
            startLoop()
        } else {
            stopLoop()
            publish()
            if (s.status == Status.PAUSED) WorkoutService.refresh(ctx)
        }
    }

    fun skipNext() = session?.let { handle(it.skipNext()); afterJump(it) }
    fun skipPrevious() = session?.let { handle(it.skipPrevious()); afterJump(it) }
    fun lap() { session?.lap(); publish() }

    fun restart() {
        val s = session ?: return
        s.restart()
        stopLoop()
        appContext?.let { WorkoutService.dismiss(it) }
        publish()
    }

    fun close() {
        stopLoop()
        appContext?.let { WorkoutService.dismiss(it) }
        feedback?.release()
        feedback = null
        session = null
        _view.value = null
    }

    private fun afterJump(s: WorkoutSession) {
        if (s.status == Status.FINISHED) stopLoop()
        publish()
    }

    private fun startLoop() {
        stopLoop()
        loop = scope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(50)
                val now = SystemClock.elapsedRealtime()
                val s = session ?: break
                handle(s.advance(now - last))
                last = now
                publish()
                if (s.status != Status.RUNNING) break
            }
        }
    }

    private fun stopLoop() {
        loop?.cancel()
        loop = null
    }

    private fun handle(events: List<WorkoutEvent>) {
        val st = store?.settings?.value ?: return
        val fb = feedback ?: return
        val vol = if (st.sound) st.volume else 0f
        for (e in events) when (e) {
            is WorkoutEvent.SegmentStarted -> {
                if (st.sound) fb.cue(e.segment.cue, vol)
                if (st.voice && st.sound) fb.speak(e.segment.label)
                if (st.vibration) fb.vibrate(300)
                if (st.torch) fb.blinkTorch()
                if (st.screenFlash) flashSeq++
                appContext?.let { WorkoutService.refresh(it) }
            }
            is WorkoutEvent.Countdown -> {
                if (st.sound && st.countdown) fb.pip(vol)
                if (st.vibration && st.countdown) fb.vibrate(60)
            }
            WorkoutEvent.Tick -> if (st.sound && st.ticking) fb.tick(vol)
            is WorkoutEvent.Lap -> {
                val cue = session?.current?.cue ?: Cue.BEEP
                if (st.sound) fb.cue(cue, vol)
                if (st.vibration) fb.vibrate(150)
            }
            WorkoutEvent.Finished -> {
                if (st.sound) fb.cue(Cue.BELL, vol)
                if (st.vibration) fb.vibrate(600)
                appContext?.let { WorkoutService.dismiss(it) }
            }
        }
    }

    private fun publish() {
        val s = session ?: return
        val seg = s.current
        val shown = when {
            seg == null -> 0L
            seg.open -> s.elapsed / 1000
            else -> (s.remainingMillis + 999) / 1000
        }
        val progress = if (seg == null || seg.open) 0f else (s.elapsed.toFloat() / seg.millis).coerceIn(0f, 1f)
        _view.value = WorkoutView(
            s.timer, s.status, seg, s.next, shown,
            (s.totalRemainingMillis + 999) / 1000,
            (progress * 200).toInt() / 200f,
            s.laps.toList(), flashSeq,
        )
    }
}

class WorkoutService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TOGGLE) Workout.toggle()
        val n = build(this)
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(ID, n)
        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "timerjitsu:workout")
                .also { it.acquire(6 * 60 * 60 * 1000L) }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        private const val ID = 7
        private const val CHANNEL = "workout"
        private const val ACTION_TOGGLE = "dev.timerjitsu.TOGGLE"

        fun dismiss(context: Context) {
            context.stopService(Intent(context, WorkoutService::class.java))
            context.getSystemService(NotificationManager::class.java).cancel(ID)
        }

        fun refresh(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (Workout.view.value != null) manager.notify(ID, build(context))
        }

        private fun build(context: Context): Notification {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Treino em andamento", NotificationManager.IMPORTANCE_LOW))
            val v = Workout.view.value
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val toggle = PendingIntent.getService(
                context, 1, Intent(context, WorkoutService::class.java).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val paused = v?.status == Status.PAUSED
            return Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(v?.timer?.name?.ifBlank { "Timer Jitsu" } ?: "Timer Jitsu")
                .setContentText(if (paused) "Pausado" else v?.segment?.label ?: "")
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(Notification.Action.Builder(null, if (paused) "Continuar" else "Pausar", toggle).build())
                .build()
        }
    }
}
