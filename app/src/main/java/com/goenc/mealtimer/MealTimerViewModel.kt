package com.goenc.mealtimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TimerTickMillis = 1_000L

class MealTimerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = MealTimerRepository(application)
    private val overlayController = MealTimerOverlayController(application)
    private val _state = MutableStateFlow(repository.loadState())
    val state: StateFlow<MealTimerState> = _state.asStateFlow()
    private val _configuredExerciseDelayMillis = MutableStateFlow(repository.loadExerciseDelayMillis())
    val configuredExerciseDelayMillis: StateFlow<Long> = _configuredExerciseDelayMillis.asStateFlow()
    private val _overlayEnabled = MutableStateFlow(overlayController.isOverlayEnabled())
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

    init {
        if (_state.value.status != MealTimerStatus.Idle && _state.value.mealStartTime != null) {
            MealTimerForegroundService.start(getApplication())
        }

        viewModelScope.launch {
            while (isActive) {
                delay(TimerTickMillis)
                tick(System.currentTimeMillis())
            }
        }
    }

    fun startMeal() {
        val now = System.currentTimeMillis()
        val started = MealTimerState(
            status = MealTimerStatus.Eating,
            mealStartTime = now,
            exerciseDelayMillis = _configuredExerciseDelayMillis.value,
            currentTime = now,
        )
        repository.saveState(started)
        _state.value = started
        MealTimerForegroundService.start(getApplication())
    }

    fun finishMeal() {
        val now = System.currentTimeMillis()
        _state.update { current ->
            if (current.status != MealTimerStatus.Eating) {
                current
            } else {
                val finished = current
                    .copy(currentTime = now, mealEndTime = now)
                    .withFinishedStatusIfNeeded()
                repository.saveState(finished)
                MealTimerForegroundService.start(getApplication())
                finished
            }
        }
    }

    fun reset() {
        repository.clearState()
        MealTimerForegroundService.stop(getApplication())
        overlayController.stopOverlay()
        _state.value = MealTimerState(currentTime = System.currentTimeMillis())
    }

    fun setOverlayEnabled(enabled: Boolean) {
        overlayController.setOverlayEnabled(enabled)
        _overlayEnabled.value = enabled
    }

    fun setExerciseDelayMillis(millis: Long) {
        repository.saveExerciseDelayMillis(millis)
        _configuredExerciseDelayMillis.value = millis
    }

    private fun tick(now: Long) {
        _state.update { current ->
            val updated = current.copy(currentTime = now)
            val resolved = updated.withFinishedStatusIfNeeded()
            if (resolved.status != updated.status) {
                repository.saveState(resolved)
            }
            resolved
        }
    }
}
