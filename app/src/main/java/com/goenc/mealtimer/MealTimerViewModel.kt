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
    private val _state = MutableStateFlow(repository.loadState())
    val state: StateFlow<MealTimerState> = _state.asStateFlow()

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
                val updated = current.copy(currentTime = now)
                val finished = updated.copy(
                    status = if (updated.elapsedFromStartMillis >= ExerciseDelayMillis) {
                        MealTimerStatus.Finished
                    } else {
                        MealTimerStatus.AfterMeal
                    },
                    mealEndTime = now,
                )
                repository.saveState(finished)
                MealTimerForegroundService.start(getApplication())
                finished
            }
        }
    }

    fun reset() {
        repository.clearState()
        MealTimerForegroundService.stop(getApplication())
        _state.value = MealTimerState(currentTime = System.currentTimeMillis())
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
