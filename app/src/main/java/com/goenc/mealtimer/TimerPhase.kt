package com.goenc.mealtimer

enum class TimerPhase {
    NotStarted,
    Blue,
    Green,
    Yellow,
    Orange,
    Red,
    ;

    companion object {
        fun fromProgress(progress: Float): TimerPhase = when {
            progress <= 0f -> NotStarted
            progress < 0.33f -> Blue
            progress < 0.66f -> Green
            progress < 0.83f -> Yellow
            progress < 1f -> Orange
            else -> Red
        }
    }
}
