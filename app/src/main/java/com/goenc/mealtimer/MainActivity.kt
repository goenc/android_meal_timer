package com.goenc.mealtimer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goenc.mealtimer.ui.theme.MealTimerTheme

class MainActivity : ComponentActivity() {
    private lateinit var overlayController: MealTimerOverlayController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayController = MealTimerOverlayController(this)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            MealTimerTheme {
                MealTimerApp(
                    onOverlayPermissionRequired = ::openOverlayPermissionSettings,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        overlayController.stopOverlay()
    }

    override fun onStop() {
        super.onStop()
        overlayController.showOverlayIfAllowed()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun openOverlayPermissionSettings() {
        startActivity(OverlayPermissionHelper.createSettingsIntent(this))
    }
}

private enum class AppScreen {
    Timer,
    Settings,
    Photos,
}

@Composable
private fun MealTimerApp(
    onOverlayPermissionRequired: () -> Unit,
    timerViewModel: MealTimerViewModel = viewModel(),
    photoViewModel: MealPhotoViewModel = viewModel(),
) {
    var currentScreen by remember { mutableStateOf(AppScreen.Timer) }
    var showMealTypeDialog by remember { mutableStateOf(false) }
    var pendingMealType by remember { mutableStateOf<MealType?>(null) }
    var selectedPhoto by remember { mutableStateOf<MealPhoto?>(null) }
    val photos by photoViewModel.photos.collectAsState()
    val configuredExerciseDelayMillis by timerViewModel.configuredExerciseDelayMillis.collectAsState()
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        val mealType = pendingMealType
        pendingMealType = null
        if (bitmap != null && mealType != null) {
            photoViewModel.saveCapturedPhoto(bitmap, mealType)
            currentScreen = AppScreen.Photos
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                AppScreen.Timer -> {
                    MealTimerScreen(
                        onPhotoCaptureClick = { showMealTypeDialog = true },
                        onPhotoListClick = { currentScreen = AppScreen.Photos },
                        onOpenSettingsClick = { currentScreen = AppScreen.Settings },
                        onOverlayPermissionRequired = onOverlayPermissionRequired,
                        viewModel = timerViewModel,
                    )
                }

                AppScreen.Settings -> {
                    MealTimerSettingsScreen(
                        selectedDelayMillis = configuredExerciseDelayMillis,
                        onSelectDelayMillis = timerViewModel::setExerciseDelayMillis,
                        onBack = { currentScreen = AppScreen.Timer },
                    )
                }

                AppScreen.Photos -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(vertical = 16.dp),
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = "食事写真",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        TextButton(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onClick = { currentScreen = AppScreen.Timer },
                        ) {
                            Text(text = "タイマーへ戻る")
                        }
                        MealPhotoListScreen(
                            photos = photos,
                            onPhotoClick = { selectedPhoto = it },
                            onRetakeClick = { mealType ->
                                pendingMealType = mealType
                                cameraLauncher.launch(null)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (showMealTypeDialog) {
                MealTypePickerDialog(
                    onMealTypeSelected = { mealType ->
                        showMealTypeDialog = false
                        pendingMealType = mealType
                        cameraLauncher.launch(null)
                    },
                    onDismiss = { showMealTypeDialog = false },
                )
            }

            selectedPhoto?.let { photo ->
                MealPhotoDetailScreen(
                    photo = photo,
                    onDismiss = { selectedPhoto = null },
                )
            }
        }
    }
}

@Composable
private fun MealTypePickerDialog(
    onMealTypeSelected: (MealType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "食事区分を選択")
        },
        text = {
            Column {
                MealType.entries.forEach { mealType ->
                    Button(
                        modifier = Modifier.padding(vertical = 4.dp),
                        onClick = { onMealTypeSelected(mealType) },
                    ) {
                        Text(text = mealType.label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "キャンセル")
            }
        },
    )
}
