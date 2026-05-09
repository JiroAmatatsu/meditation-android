package com.amatatsu.meditationtimer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }

class TimerViewModel : ViewModel() {

    private val _selectedMinutes = MutableStateFlow(10)
    val selectedMinutes: StateFlow<Int> = _selectedMinutes.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartedAt: Long = 0L

    // DBはContextが必要なので initDb() で初期化する
    private var dao: MeditationDao? = null

    // 履歴一覧(DAOのFlowをStateFlowに変換してUIに公開)
    private val _sessions = MutableStateFlow<List<MeditationSession>>(emptyList())
    val sessions: StateFlow<List<MeditationSession>> = _sessions.asStateFlow()

    // SoundPool は初期化にContextが必要なので遅延初期化
    private var soundPool: SoundPool? = null
    private var bellSoundId: Int = 0

    fun initDb(context: Context) {
        if (dao != null) return
        dao = MeditationDatabase.getInstance(context).meditationDao()
        // DBの変更をsessionsに流す
        viewModelScope.launch {
            dao!!.getAll().collect { _sessions.value = it }
        }
    }

    fun initSound(context: Context) {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build()
        bellSoundId = soundPool!!.load(context, R.raw.bell, 1)
    }

    fun setMinutes(minutes: Int) {
        if (_timerState.value == TimerState.IDLE) {
            _selectedMinutes.value = minutes.coerceIn(1, 120)
        }
    }

    fun start() {
        when (_timerState.value) {
            TimerState.IDLE -> {
                sessionStartedAt = System.currentTimeMillis()
                _remainingSeconds.value = _selectedMinutes.value * 60
                runTimer()
            }
            TimerState.PAUSED -> runTimer()
            else -> {}
        }
    }

    fun pause() {
        if (_timerState.value == TimerState.RUNNING) {
            timerJob?.cancel()
            _timerState.value = TimerState.PAUSED
        }
    }

    fun reset() {
        timerJob?.cancel()
        _timerState.value = TimerState.IDLE
        _remainingSeconds.value = 0
    }

    private fun runTimer() {
        _timerState.value = TimerState.RUNNING
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value--
            }
            _timerState.value = TimerState.FINISHED
            // 完了レコードをDBに保存
            dao?.insert(MeditationSession(startedAt = sessionStartedAt, durationMinutes = _selectedMinutes.value))
        }
    }

    fun playBellAndVibrate(context: Context) {
        // 鐘を鳴らす
        soundPool?.play(bellSoundId, 1f, 1f, 0, 0, 1f)

        // 3秒振動(API 31以上のVibratorManagerを使用)
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val effect = VibrationEffect.createOneShot(3000, VibrationEffect.DEFAULT_AMPLITUDE)
        vibratorManager.defaultVibrator.vibrate(effect)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundPool?.release()
        soundPool = null
    }
}
