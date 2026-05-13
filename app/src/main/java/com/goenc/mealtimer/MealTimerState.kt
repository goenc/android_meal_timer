package com.goenc.mealtimer

private const val SecondsPerMinute = 60L
private const val MillisPerSecond = 1_000L

const val ExerciseDelayMinutes = 30L
const val ExerciseDelayMillis = ExerciseDelayMinutes * SecondsPerMinute * MillisPerSecond

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
    val currentTime: Long = System.currentTimeMillis(),
) {
    val elapsedFromStartMillis: Long
        get() = mealStartTime?.let { (currentTime - it).coerceAtLeast(0L) } ?: 0L

    val elapsedAfterMealMillis: Long?
        get() = mealEndTime?.let { (currentTime - it).coerceAtLeast(0L) }

    val remainingUntilExerciseMillis: Long
        get() = (ExerciseDelayMillis - elapsedFromStartMillis).coerceAtLeast(0L)

    val progress: Float
        get() = if (mealStartTime == null) 0f else elapsedFromStartMillis.toFloat() / ExerciseDelayMillis

    val timerPhase: TimerPhase
        get() = TimerPhase.fromProgress(progress)
}
