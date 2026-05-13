package com.goenc.mealtimer

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private const val OverlayTickMillis = 1_000L
private const val DragClickThresholdPx = 12
private const val OverlayMaxLines = 5
private const val OverlayTextSizeSp = 15f
private const val OverlayWidthEatingDp = 156
private const val OverlayWidthAfterMealDp = 184
private const val OverlayWidthFinishedDp = 174
private const val OverlayTextHeightEatingDp = 86
private const val OverlayTextHeightAfterMealDp = 116
private const val OverlayTextHeightFinishedDp = 96

class MealTimerOverlayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: MealTimerRepository
    private lateinit var windowManager: WindowManager
    private lateinit var contentText: TextView
    private var overlayView: View? = null
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = MealTimerRepository(this)
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (!OverlayPermissionHelper.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val state = repository.loadState()
        if (state.status == MealTimerStatus.Idle || state.mealStartTime == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay()
        startUpdateLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        updateJob?.cancel()
        removeOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showOverlay() {
        if (overlayView != null) {
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 32
            y = 180
        }

        val initialState = repository.loadState()

        contentText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = OverlayTextSizeSp
            isSingleLine = false
            maxLines = OverlayMaxLines
            includeFontPadding = true
            setPadding(0, 0, 0, dpToPx(8))
            setLineSpacing(dpToPx(4).toFloat(), 1f)
        }

        val closeText = TextView(this).apply {
            text = "×"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(6, 0, 0, 0)
            setOnClickListener { stopSelf() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.argb(230, 28, 34, 48))
            clipToPadding = false
            setPadding(10, 14, 8, 18)
            addView(
                contentText,
                LinearLayout.LayoutParams(
                    dpToPx(stateWidthDp(initialState)),
                    dpToPx(stateTextHeightDp(initialState)),
                ),
            )
            addView(closeText)
            setOnTouchListener(createDragTouchListener(params))
        }
        overlayView = root
        updateContent(initialState)
        windowManager.addView(root, params)
    }

    private fun startUpdateLoop() {
        if (updateJob?.isActive == true) {
            return
        }

        updateJob = serviceScope.launch {
            while (isActive) {
                val state = repository.loadState()
                if (state.status == MealTimerStatus.Idle || state.mealStartTime == null) {
                    stopSelf()
                    return@launch
                }
                if (state.status == MealTimerStatus.Finished) {
                    repository.saveState(state)
                }
                updateContent(state)
                delay(OverlayTickMillis)
            }
        }
    }

    private fun updateContent(state: MealTimerState) {
        contentText.text = state.overlayText()
        overlayView?.let { view ->
            contentText.layoutParams = contentText.layoutParams.apply {
                width = dpToPx(stateWidthDp(state))
                height = dpToPx(stateTextHeightDp(state))
            }
            view.requestLayout()
            runCatching {
                windowManager.updateViewLayout(view, view.layoutParams as WindowManager.LayoutParams)
            }
        }
    }

    private fun createDragTouchListener(params: WindowManager.LayoutParams): View.OnTouchListener {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0

        return View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downRawX).toInt()
                    params.y = startY + (event.rawY - downRawY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val movedX = abs(event.rawX - downRawX)
                    val movedY = abs(event.rawY - downRawY)
                    if (movedX < DragClickThresholdPx && movedY < DragClickThresholdPx) {
                        openMainActivity()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
    }

    private fun MealTimerState.overlayText(): String {
        val lines = mutableListOf(status.overlayTitle())
        lines += "食べ始めから ${formatDuration(elapsedFromStartMillis)}"
        if (status == MealTimerStatus.AfterMeal || status == MealTimerStatus.Finished) {
            elapsedAfterMealMillis?.let {
                lines += "食べ終わってから ${formatDuration(it)}"
            }
        }
        if (status == MealTimerStatus.Eating || status == MealTimerStatus.AfterMeal) {
            lines += "運動開始まで ${formatDuration(remainingUntilExerciseMillis)}"
        }
        return lines.joinToString("\n")
    }

    private fun MealTimerStatus.overlayTitle(): String = when (this) {
        MealTimerStatus.Idle -> "食事タイマー"
        MealTimerStatus.Eating -> "食事中"
        MealTimerStatus.AfterMeal -> "食後"
        MealTimerStatus.Finished -> "運動開始"
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.JAPAN, "%02d:%02d", minutes, seconds)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun stateWidthDp(state: MealTimerState): Int = when (state.status) {
        MealTimerStatus.Idle -> OverlayWidthEatingDp
        MealTimerStatus.Eating -> OverlayWidthEatingDp
        MealTimerStatus.AfterMeal -> OverlayWidthAfterMealDp
        MealTimerStatus.Finished -> OverlayWidthFinishedDp
    }

    private fun stateTextHeightDp(state: MealTimerState): Int = when (state.status) {
        MealTimerStatus.Idle -> OverlayTextHeightEatingDp
        MealTimerStatus.Eating -> OverlayTextHeightEatingDp
        MealTimerStatus.AfterMeal -> OverlayTextHeightAfterMealDp
        MealTimerStatus.Finished -> OverlayTextHeightFinishedDp
    }
}
