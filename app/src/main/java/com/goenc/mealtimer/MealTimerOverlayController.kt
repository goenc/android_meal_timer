package com.goenc.mealtimer

import android.content.Context
import android.content.Intent

private const val OverlayPreferencesName = "meal_timer_overlay"
private const val KeyOverlayEnabled = "overlayEnabled"
private const val KeyOverlayPositionX = "overlayPositionX"
private const val KeyOverlayPositionY = "overlayPositionY"
private const val DefaultOverlayPositionX = 32
private const val DefaultOverlayPositionY = 280

class MealTimerOverlayController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        OverlayPreferencesName,
        Context.MODE_PRIVATE,
    )

    fun isOverlayEnabled(): Boolean = preferences.getBoolean(KeyOverlayEnabled, false)

    fun loadOverlayPosition(): Pair<Int, Int> {
        val x = preferences.getInt(KeyOverlayPositionX, DefaultOverlayPositionX)
        val y = preferences.getInt(KeyOverlayPositionY, DefaultOverlayPositionY)
        return x to y
    }

    fun saveOverlayPosition(x: Int, y: Int) {
        preferences.edit()
            .putInt(KeyOverlayPositionX, x)
            .putInt(KeyOverlayPositionY, y)
            .apply()
    }

    fun setOverlayEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KeyOverlayEnabled, enabled).apply()
        if (!enabled) {
            stopOverlay()
        }
    }

    fun showOverlayIfAllowed() {
        val state = MealTimerRepository(appContext).loadState()
        if (
            isOverlayEnabled() &&
            OverlayPermissionHelper.canDrawOverlays(appContext) &&
            state.status != MealTimerStatus.Idle &&
            state.mealStartTime != null
        ) {
            appContext.startService(Intent(appContext, MealTimerOverlayService::class.java))
        }
    }

    fun stopOverlay() {
        appContext.stopService(Intent(appContext, MealTimerOverlayService::class.java))
    }
}
