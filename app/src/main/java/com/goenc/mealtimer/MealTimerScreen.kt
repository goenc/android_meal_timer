package com.goenc.mealtimer

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goenc.mealtimer.ui.theme.MealTimerTheme
import java.util.Locale

@Composable
fun MealTimerScreen(
    onPhotoCaptureClick: () -> Unit,
    onPhotoListClick: () -> Unit,
    onOverlayPermissionRequired: () -> Unit,
    viewModel: MealTimerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()

    MealTimerContent(
        state = state,
        overlayEnabled = overlayEnabled,
        onStartMeal = viewModel::startMeal,
        onFinishMeal = viewModel::finishMeal,
        onReset = viewModel::reset,
        onOverlayEnabledChange = { enabled ->
            if (enabled && !OverlayPermissionHelper.canDrawOverlays(context)) {
                onOverlayPermissionRequired()
            } else {
                viewModel.setOverlayEnabled(enabled)
            }
        },
        onPhotoCaptureClick = onPhotoCaptureClick,
        onPhotoListClick = onPhotoListClick,
    )
}

@Composable
fun MealTimerContent(
    state: MealTimerState,
    overlayEnabled: Boolean,
    onStartMeal: () -> Unit,
    onFinishMeal: () -> Unit,
    onReset: () -> Unit,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onPhotoCaptureClick: () -> Unit,
    onPhotoListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(state.timerPhase.backgroundColor()),
        color = state.timerPhase.backgroundColor(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.status.titleText(),
                    color = Color(0xFF101828),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                TimerValues(state = state)
            }

            TimerActions(
                status = state.status,
                onStartMeal = onStartMeal,
                onFinishMeal = onFinishMeal,
                onReset = onReset,
                overlayEnabled = overlayEnabled,
                onOverlayEnabledChange = onOverlayEnabledChange,
                onPhotoCaptureClick = onPhotoCaptureClick,
                onPhotoListClick = onPhotoListClick,
            )
        }
    }
}

@Composable
private fun TimerValues(
    state: MealTimerState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TimerValue(
            label = "食べ始めから",
            value = formatMillis(state.elapsedFromStartMillis),
        )
        state.elapsedAfterMealMillis?.let {
            TimerValue(
                label = "食べ終わってから",
                value = formatMillis(it),
            )
        }
        if (state.status != MealTimerStatus.Idle && state.status != MealTimerStatus.Finished) {
            TimerValue(
                label = "運動開始まで",
                value = formatMillis(state.remainingUntilExerciseMillis),
            )
        }
    }
}

@Composable
private fun TimerValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = Color(0xFF344054),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            color = Color(0xFF101828),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimerActions(
    status: MealTimerStatus,
    onStartMeal: () -> Unit,
    onFinishMeal: () -> Unit,
    onReset: () -> Unit,
    overlayEnabled: Boolean,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onPhotoCaptureClick: () -> Unit,
    onPhotoListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (status) {
            MealTimerStatus.Idle -> {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartMeal,
                ) {
                    Text(text = "食事開始")
                }
            }

            MealTimerStatus.Eating -> {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onFinishMeal,
                ) {
                    Text(text = "食べ終わった")
                }
            }

            MealTimerStatus.AfterMeal,
            MealTimerStatus.Finished,
            -> Unit
        }

        if (status != MealTimerStatus.Idle) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onReset,
            ) {
                Text(text = "停止してリセット")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "最小化時に小窓表示",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Switch(
                checked = overlayEnabled,
                onCheckedChange = onOverlayEnabledChange,
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onPhotoCaptureClick,
        ) {
            Text(text = "食事写真を撮る")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onPhotoListClick,
        ) {
            Text(text = "写真一覧")
        }
    }
}

private fun MealTimerStatus.titleText(): String = when (this) {
    MealTimerStatus.Idle -> "食事タイマー"
    MealTimerStatus.Eating -> "食事中"
    MealTimerStatus.AfterMeal -> "食後"
    MealTimerStatus.Finished -> "運動開始"
}

private fun TimerPhase.backgroundColor(): Color = when (this) {
    TimerPhase.NotStarted -> Color(0xFFF8FAFC)
    TimerPhase.Blue -> Color(0xFFD7ECFF)
    TimerPhase.Green -> Color(0xFFDDF7E8)
    TimerPhase.Yellow -> Color(0xFFFFF3B8)
    TimerPhase.Orange -> Color(0xFFFFD9B0)
    TimerPhase.Red -> Color(0xFFFFC9C9)
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.JAPAN, "%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true)
@Composable
private fun EatingPreview() {
    MealTimerTheme {
        MealTimerContent(
            state = MealTimerState(
                status = MealTimerStatus.Eating,
                mealStartTime = 0L,
                currentTime = 12 * 60 * 1_000L,
            ),
            overlayEnabled = false,
            onStartMeal = {},
            onFinishMeal = {},
            onReset = {},
            onOverlayEnabledChange = {},
            onPhotoCaptureClick = {},
            onPhotoListClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfterMealPreview() {
    MealTimerTheme {
        MealTimerContent(
            state = MealTimerState(
                status = MealTimerStatus.AfterMeal,
                mealStartTime = 0L,
                mealEndTime = 18 * 60 * 1_000L,
                currentTime = 25 * 60 * 1_000L,
            ),
            overlayEnabled = true,
            onStartMeal = {},
            onFinishMeal = {},
            onReset = {},
            onOverlayEnabledChange = {},
            onPhotoCaptureClick = {},
            onPhotoListClick = {},
        )
    }
}
