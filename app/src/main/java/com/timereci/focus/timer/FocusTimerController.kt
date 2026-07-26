package com.timereci.focus.timer

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.timereci.focus.data.ActiveSessionEntity
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the focus timer (PRD issue 4, recommendation 4-A).
 *
 * - **End-time based:** running state stores an [SystemClock.elapsedRealtime] target; the
 *   display value is `target - now`, so it never drifts and survives the app being killed.
 * - **Crash recovery:** every transition is mirrored to [ActiveSessionEntity]; [restore]
 *   rebuilds the exact state on next launch (including "completed while the app was dead").
 * - **Foreground service + alarm:** while running, a foreground service keeps the process
 *   alive and an exact alarm guarantees a completion notification even if it isn't.
 */
@Singleton
class FocusTimerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FocusRepository,
    private val alarms: TimerAlarmScheduler,
    settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    /** elapsedRealtime target while running; null while paused/idle. */
    private var endsAtElapsed: Long? = null
    private var ticker: Job? = null

    /** Kept current from Settings so the ticker's completion path (below) can read it
     * synchronously — these gate the in-app completion sound/vibration independently of each
     * other; the dead-process notification backstop in [TimerAlarmScheduler] always fires
     * regardless, since that one is a reliability guarantee, not a user preference. */
    @Volatile private var alertWhenSessionEnds: Boolean = true
    @Volatile private var vibrateWhenSessionEnds: Boolean = true
    init {
        scope.launch {
            settingsRepository.settings.collect {
                alertWhenSessionEnds = it.alertWhenSessionEnds
                vibrateWhenSessionEnds = it.vibrateWhenSessionEnds
            }
        }
    }

    /** Rebuilds timer state from persistence. Call once on app start. */
    fun restore() {
        scope.launch {
            val saved = repository.getActiveSession() ?: return@launch
            when {
                saved.endsAtElapsed != null -> {
                    val remaining = saved.endsAtElapsed - SystemClock.elapsedRealtime()
                    if (remaining <= 0L) {
                        // Completed while the app was dead.
                        endsAtElapsed = null
                        _state.value = saved.toState(
                            phase = TimerPhase.COMPLETED,
                            remainingMs = 0L,
                            focusedMs = saved.plannedMs,
                        )
                    } else {
                        endsAtElapsed = saved.endsAtElapsed
                        _state.value = saved.toState(TimerPhase.RUNNING, remaining)
                        startService()
                        startTicker()
                    }
                }
                saved.pausedRemainingMs != null -> {
                    endsAtElapsed = null
                    _state.value = saved.toState(TimerPhase.PAUSED, saved.pausedRemainingMs)
                }
            }
        }
    }

    fun start(plannedMs: Long, taskLabel: String, backdropFileName: String?) {
        stopAlarm() // in case a previous session's completion alarm was still ringing
        val now = SystemClock.elapsedRealtime()
        endsAtElapsed = now + plannedMs
        _state.value = TimerState(
            phase = TimerPhase.RUNNING,
            plannedMs = plannedMs,
            remainingMs = plannedMs,
            taskLabel = taskLabel,
            backdropFileName = backdropFileName,
            draftComment = "",
            startedAtEpoch = System.currentTimeMillis(),
        )
        persist()
        alarms.schedule(endsAtElapsed!!)
        startService()
        startTicker()
    }

    fun pause() {
        val end = endsAtElapsed ?: return
        val remaining = (end - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        endsAtElapsed = null
        stopTicker()
        alarms.cancel()
        _state.value = _state.value.copy(phase = TimerPhase.PAUSED, remainingMs = remaining)
        persist()
    }

    fun resume() {
        if (_state.value.phase != TimerPhase.PAUSED) return
        val now = SystemClock.elapsedRealtime()
        endsAtElapsed = now + _state.value.remainingMs
        _state.value = _state.value.copy(phase = TimerPhase.RUNNING)
        persist()
        alarms.schedule(endsAtElapsed!!)
        startService()
        startTicker()
    }

    /** Finish now, keeping only the honestly focused time. */
    fun completeNow() {
        val s = _state.value
        if (!s.isActive) return
        val remaining = endsAtElapsed?.let { (it - SystemClock.elapsedRealtime()).coerceAtLeast(0L) }
            ?: s.remainingMs
        finish(focusedMs = (s.plannedMs - remaining).coerceAtLeast(0L))
    }

    fun updateComment(text: String) {
        _state.value = _state.value.copy(draftComment = text)
        persist()
    }

    /** Abandon the session — leaves no trace, per PRD (포기 세션은 흔적 없음). */
    fun abandon() {
        endsAtElapsed = null
        stopTicker()
        alarms.cancel()
        stopService()
        stopAlarm()
        _state.value = TimerState(plannedMs = _state.value.plannedMs, remainingMs = _state.value.plannedMs)
        scope.launch { repository.clearActiveSession() }
    }

    /** After the completed session is published (or discarded), return to idle. */
    fun reset() {
        endsAtElapsed = null
        stopTicker()
        stopService()
        stopAlarm()
        _state.value = TimerState()
        scope.launch { repository.clearActiveSession() }
    }

    private fun finish(focusedMs: Long, playAlarm: Boolean = false) {
        endsAtElapsed = null
        stopTicker()
        alarms.cancel()
        stopService()
        _state.value = _state.value.copy(
            phase = TimerPhase.COMPLETED,
            remainingMs = 0L,
            focusedMs = focusedMs,
        )
        // Keep ActiveSession persisted so the publish screen can be rebuilt after a crash.
        if (playAlarm) playCompletionSound()
    }

    private var completionRingtone: Ringtone? = null

    private val _alarmActive = MutableStateFlow(false)

    /** True while the completion sound/vibration is actively ringing — drives a stop button. */
    val alarmActive: StateFlow<Boolean> = _alarmActive.asStateFlow()

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Rings the device's default alarm tone and vibrates when the countdown runs out on its
     * own. Keeps going — no auto-timeout — until [stopAlarm] is called.
     */
    private fun playCompletionSound() {
        if (!alertWhenSessionEnds && !vibrateWhenSessionEnds) return
        stopAlarm()
        if (alertWhenSessionEnds) {
            runCatching {
                val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(context, uri)?.also { ringtone ->
                    completionRingtone = ringtone
                    ringtone.play()
                }
            }
        }
        if (vibrateWhenSessionEnds) {
            runCatching {
                // Repeats the pattern (start index 0) until cancelled.
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
            }
        }
        _alarmActive.value = true
    }

    /** Stops the completion sound and vibration — call from a "끄기" button. */
    fun stopAlarm() {
        completionRingtone?.stop()
        completionRingtone = null
        runCatching { vibrator.cancel() }
        _alarmActive.value = false
    }

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch {
            while (isActive) {
                val end = endsAtElapsed ?: break
                val remaining = end - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    finish(focusedMs = _state.value.plannedMs, playAlarm = true)
                    break
                }
                _state.value = _state.value.copy(remainingMs = remaining)
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun persist() {
        val s = _state.value
        scope.launch {
            repository.saveActiveSession(
                ActiveSessionEntity(
                    plannedMs = s.plannedMs,
                    startedAtEpoch = s.startedAtEpoch,
                    endsAtElapsed = endsAtElapsed,
                    pausedRemainingMs = if (endsAtElapsed == null) s.remainingMs else null,
                    taskLabel = s.taskLabel,
                    backdropFileName = s.backdropFileName,
                    draftComment = s.draftComment,
                ),
            )
        }
    }

    private fun startService() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, FocusTimerService::class.java),
        )
    }

    private fun stopService() {
        context.stopService(Intent(context, FocusTimerService::class.java))
    }

    private fun ActiveSessionEntity.toState(
        phase: TimerPhase,
        remainingMs: Long,
        focusedMs: Long = 0L,
    ) = TimerState(
        phase = phase,
        plannedMs = plannedMs,
        remainingMs = remainingMs,
        focusedMs = focusedMs,
        taskLabel = taskLabel,
        backdropFileName = backdropFileName,
        draftComment = draftComment ?: "",
        startedAtEpoch = startedAtEpoch,
    )

    private companion object {
        const val TICK_MS = 250L
    }
}
