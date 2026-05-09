package com.amatatsu.meditationtimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// タイマーの状態を表すenum
enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }

class TimerViewModel : ViewModel() {

    // 設定分数(1〜120分)
    private val _selectedMinutes = MutableStateFlow(10)
    val selectedMinutes: StateFlow<Int> = _selectedMinutes.asStateFlow()

    // 残り秒数
    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    // タイマーの状態
    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    fun setMinutes(minutes: Int) {
        // 実行中は変更させない
        if (_timerState.value == TimerState.IDLE) {
            _selectedMinutes.value = minutes.coerceIn(1, 120)
        }
    }

    fun start() {
        when (_timerState.value) {
            TimerState.IDLE -> {
                _remainingSeconds.value = _selectedMinutes.value * 60
                runTimer()
            }
            TimerState.PAUSED -> runTimer() // 一時停止中は再開
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
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
