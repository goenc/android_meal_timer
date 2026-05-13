package com.goenc.mealtimer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val MetadataFileName = "meal_photos.json"
private const val PhotoDirectoryName = "meal_photos"

class MealPhotoRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val metadataFile = File(appContext.filesDir, MetadataFileName)

    val photoDirectory: File
        get() = File(appContext.filesDir, PhotoDirectoryName)

    suspend fun loadPhotos(): List<MealPhoto> = withContext(Dispatchers.IO) {
        if (!metadataFile.exists()) {
            return@withContext emptyList()
        }

        val array = JSONArray(metadataFile.readText())
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    MealPhoto(
                        id = item.getString("id"),
                        date = item.getString("date"),
                        mealType = MealType.valueOf(item.getString("mealType")),
                        capturedAt = item.getLong("capturedAt"),
                        imagePath = item.getString("imagePath"),
                    ),
                )
            }
        }.filter { File(it.imagePath).exists() }
    }

    suspend fun upsertPhoto(photo: MealPhoto) = withContext(Dispatchers.IO) {
        val currentPhotos = loadPhotos()
        currentPhotos
            .filter { it.date == photo.date && it.mealType == photo.mealType && it.imagePath != photo.imagePath }
            .forEach { File(it.imagePath).delete() }

        val updated = currentPhotos
            .filterNot { it.date == photo.date && it.mealType == photo.mealType }
            .plus(photo)
            .sortedWith(compareByDescending<MealPhoto> { it.date }.thenBy { it.mealType.ordinal })

        savePhotos(updated)
    }

    private fun savePhotos(photos: List<MealPhoto>) {
        val array = JSONArray()
        photos.forEach { photo ->
            array.put(
                JSONObject()
                    .put("id", photo.id)
                    .put("date", photo.date)
                    .put("mealType", photo.mealType.name)
                    .put("capturedAt", photo.capturedAt)
                    .put("imagePath", photo.imagePath),
            )
        }
        metadataFile.writeText(array.toString())
    }
}
