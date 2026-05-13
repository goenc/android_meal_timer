package com.goenc.mealtimer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val ServiceTickMillis = 1_000L

class MealTimerForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: MealTimerRepository
    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = MealTimerRepository(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val state = repository.loadState()
        if (state.status == MealTimerStatus.Idle || state.mealStartTime == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            MealTimerNotificationId,
            MealTimerNotification.build(this, state),
        )
        startNotificationLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        notificationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startNotificationLoop() {
        if (notificationJob?.isActive == true) {
            return
        }

        notificationJob = serviceScope.launch {
            while (isActive) {
                val state = repository.loadState()
                if (state.status == MealTimerStatus.Idle || state.mealStartTime == null) {
                    stopSelf()
                    return@launch
                }

                if (state.status == MealTimerStatus.Finished) {
                    repository.saveState(state)
                }
                startForeground(
                    MealTimerNotificationId,
                    MealTimerNotification.build(this@MealTimerForegroundService, state),
                )
                delay(ServiceTickMillis)
            }
        }
    }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MealTimerForegroundService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MealTimerForegroundService::class.java))
        }
    }
}
