package com.goenc.mealtimer

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.time.LocalDate
import java.util.UUID

class MealPhotoCaptureManager(
    context: Context,
    private val repository: MealPhotoRepository = MealPhotoRepository(context),
) {
    suspend fun saveCapturedPhoto(
        bitmap: Bitmap,
        mealType: MealType,
    ): MealPhoto {
        val now = System.currentTimeMillis()
        val date = LocalDate.now().toString()
        val id = UUID.randomUUID().toString()
        val destination = File(repository.photoDirectory, "${date}_${mealType.name}_$id.jpg")

        ImageResizeUtil.saveResizedJpeg(bitmap, destination)

        val photo = MealPhoto(
            id = id,
            date = date,
            mealType = mealType,
            capturedAt = now,
            imagePath = destination.absolutePath,
        )
        repository.upsertPhoto(photo)
        return photo
    }
}
