package com.goenc.mealtimer

data class ExerciseDelayOption(
    val label: String,
    val millis: Long,
)

val ExerciseDelayOptions = listOf(
    ExerciseDelayOption(label = "10秒", millis = 10_000L),
    ExerciseDelayOption(label = "15分", millis = 15L * 60L * 1_000L),
    ExerciseDelayOption(label = "20分", millis = 20L * 60L * 1_000L),
    ExerciseDelayOption(label = "25分", millis = 25L * 60L * 1_000L),
    ExerciseDelayOption(label = "30分", millis = 30L * 60L * 1_000L),
)
