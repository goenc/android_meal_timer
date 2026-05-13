package com.goenc.mealtimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TimerTickMillis = 1_000L

class MealTimerViewModel : ViewModel() {
    private val _state = MutableStateFlow(MealTimerState())
    val state: StateFlow<MealTimerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(TimerTickMillis)
                tick(System.currentTimeMillis())
            }
        }
    }

    fun startMeal() {
        val now = System.currentTimeMillis()
        _state.value = MealTimerState(
            status = MealTimerStatus.Eating,
            mealStartTime = now,
            currentTime = now,
        )
    }

    fun finishMeal() {
        val now = System.currentTimeMillis()
        _state.update { current ->
            if (current.status != MealTimerStatus.Eating) {
                current
            } else {
                val updated = current.copy(currentTime = now)
                updated.copy(
                    status = if (updated.elapsedFromStartMillis >= ExerciseDelayMillis) {
                        MealTimerStatus.Finished
                    } else {
                        MealTimerStatus.AfterMeal
                    },
                    mealEndTime = now,
                )
            }
        }
    }

    fun reset() {
        _state.value = MealTimerState(currentTime = System.currentTimeMillis())
    }

    private fun tick(now: Long) {
        _state.update { current ->
            val updated = current.copy(currentTime = now)
            if (
                updated.mealStartTime != null &&
                updated.status != MealTimerStatus.Idle &&
                updated.elapsedFromStartMillis >= ExerciseDelayMillis
            ) {
                updated.copy(status = MealTimerStatus.Finished)
            } else {
                updated
            }
        }
    }
}
