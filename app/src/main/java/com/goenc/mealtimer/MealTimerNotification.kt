package com.goenc.mealtimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Locale

const val MealTimerNotificationId = 1001
private const val MealTimerChannelId = "meal_timer_tracking"

object MealTimerNotification {
    fun build(
        context: Context,
        state: MealTimerState,
    ): Notification {
        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, MealTimerChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(state.status.notificationTitle())
            .setContentText(state.notificationSummary())
            .setStyle(NotificationCompat.BigTextStyle().bigText(state.notificationSummary()))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            MealTimerChannelId,
            "食事タイマー計測",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "食事タイマーの計測状態を表示します"
        }
        manager.createNotificationChannel(channel)
    }

    private fun MealTimerStatus.notificationTitle(): String = when (this) {
        MealTimerStatus.Idle -> "食事タイマー"
        MealTimerStatus.Eating -> "食事中"
        MealTimerStatus.AfterMeal -> "食後"
        MealTimerStatus.Finished -> "食後"
    }

    private fun MealTimerState.notificationSummary(): String {
        val parts = mutableListOf("食べ始めから ${formatDuration(elapsedFromStartMillis)}")
        elapsedAfterMealMillis?.let {
            parts += "食べ終わってから ${formatDuration(it)}"
        }
        if (status == MealTimerStatus.Eating) {
            parts += "運動開始まで ${formatDuration(remainingUntilExerciseMillis)}"
        }
        return parts.joinToString("\n")
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.JAPAN, "%02d:%02d", minutes, seconds)
    }
}
