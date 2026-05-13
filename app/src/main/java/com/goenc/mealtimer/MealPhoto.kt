package com.goenc.mealtimer

data class MealPhoto(
    val id: String,
    val date: String,
    val mealType: MealType,
    val capturedAt: Long,
    val imagePath: String,
)
