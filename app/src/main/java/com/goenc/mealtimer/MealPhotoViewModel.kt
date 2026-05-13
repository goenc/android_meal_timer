package com.goenc.mealtimer

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MealPhotoViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = MealPhotoRepository(application)
    private val captureManager = MealPhotoCaptureManager(application, repository)

    private val _photos = MutableStateFlow<List<MealPhoto>>(emptyList())
    val photos: StateFlow<List<MealPhoto>> = _photos.asStateFlow()

    init {
        refreshPhotos()
    }

    fun saveCapturedPhoto(
        bitmap: Bitmap,
        mealType: MealType,
    ) {
        viewModelScope.launch {
            captureManager.saveCapturedPhoto(bitmap, mealType)
            refreshPhotos()
        }
    }

    private fun refreshPhotos() {
        viewModelScope.launch {
            _photos.value = repository.loadPhotos()
        }
    }
}
