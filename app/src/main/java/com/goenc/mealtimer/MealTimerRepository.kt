package com.goenc.mealtimer

import android.content.Context

private const val TimerPreferencesName = "meal_timer_state"
private const val KeyStatus = "status"
private const val KeyMealStartTime = "mealStartTime"
private const val KeyMealEndTime = "mealEndTime"
private const val KeyConfiguredExerciseDelayMillis = "configuredExerciseDelayMillis"
private const val KeyStateExerciseDelayMillis = "stateExerciseDelayMillis"
private const val MissingTime = -1L

class MealTimerRepository(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        TimerPreferencesName,
        Context.MODE_PRIVATE,
    )

    fun loadState(now: Long = System.currentTimeMillis()): MealTimerState {
        val configuredDelayMillis = loadExerciseDelayMillis()
        val startTime = preferences.getLong(KeyMealStartTime, MissingTime)
        if (startTime == MissingTime) {
            return MealTimerState(
                exerciseDelayMillis = configuredDelayMillis,
                currentTime = now,
            )
        }

        val statusName = preferences.getString(KeyStatus, MealTimerStatus.Eating.name)
        val savedStatus = statusName
            ?.let { runCatching { MealTimerStatus.valueOf(it) }.getOrNull() }
            ?: MealTimerStatus.Eating
        val endTime = preferences.getLong(KeyMealEndTime, MissingTime).takeIf { it != MissingTime }
        val stateExerciseDelayMillis = preferences.getLong(
            KeyStateExerciseDelayMillis,
            configuredDelayMillis,
        )
        val restored = MealTimerState(
            status = savedStatus,
            mealStartTime = startTime,
            mealEndTime = endTime,
            exerciseDelayMillis = stateExerciseDelayMillis,
            currentTime = now,
        )
        return restored.withFinishedStatusIfNeeded()
    }

    fun saveState(state: MealTimerState) {
        val startTime = state.mealStartTime ?: return
        preferences.edit()
            .putString(KeyStatus, state.status.name)
            .putLong(KeyMealStartTime, startTime)
            .putLong(KeyMealEndTime, state.mealEndTime ?: MissingTime)
            .putLong(KeyStateExerciseDelayMillis, state.exerciseDelayMillis)
            .apply()
    }

    fun loadExerciseDelayMillis(): Long {
        return preferences.getLong(KeyConfiguredExerciseDelayMillis, DefaultExerciseDelayMillis)
    }

    fun saveExerciseDelayMillis(millis: Long) {
        preferences.edit().putLong(KeyConfiguredExerciseDelayMillis, millis).apply()
    }

    fun clearState() {
        preferences.edit()
            .remove(KeyStatus)
            .remove(KeyMealStartTime)
            .remove(KeyMealEndTime)
            .remove(KeyStateExerciseDelayMillis)
            .apply()
    }
}
