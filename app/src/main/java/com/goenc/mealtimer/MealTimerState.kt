package com.goenc.mealtimer

private const val SecondsPerMinute = 60L
private const val MillisPerSecond = 1_000L

const val DefaultExerciseDelayMillis = 30L * SecondsPerMinute * MillisPerSecond

enum class MealTimerStatus {
    Idle,
    Eating,
    AfterMeal,
    Finished,
}

data class MealTimerState(
    val status: MealTimerStatus = MealTimerStatus.Idle,
    val mealStartTime: Long? = null,
    val mealEndTime: Long? = null,
    val exerciseDelayMillis: Long = DefaultExerciseDelayMillis,
    val currentTime: Long = System.currentTimeMillis(),
) {
    val elapsedFromStartMillis: Long
        get() = mealStartTime?.let { (currentTime - it).coerceAtLeast(0L) } ?: 0L

    val elapsedAfterMealMillis: Long?
        get() = mealEndTime?.let { (currentTime - it).coerceAtLeast(0L) }

    val remainingUntilExerciseMillis: Long
        get() = (exerciseDelayMillis - elapsedFromStartMillis).coerceAtLeast(0L)

    val progress: Float
        get() = if (mealStartTime == null) 0f else elapsedFromStartMillis.toFloat() / exerciseDelayMillis

    val timerPhase: TimerPhase
        get() = TimerPhase.fromProgress(progress)

    fun withFinishedStatusIfNeeded(): MealTimerState {
        if (mealStartTime == null || status == MealTimerStatus.Idle) {
            return this
        }

        if (mealEndTime != null) {
            return copy(status = MealTimerStatus.AfterMeal)
        }

        if (elapsedFromStartMillis < exerciseDelayMillis) {
            return this
        }

        return copy(
            status = MealTimerStatus.AfterMeal,
            mealEndTime = mealEndTime ?: currentTime,
        )
    }
}
