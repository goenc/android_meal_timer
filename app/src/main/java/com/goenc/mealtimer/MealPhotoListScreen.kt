package com.goenc.mealtimer

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MealPhotoListScreen(
    photos: List<MealPhoto>,
    onPhotoClick: (MealPhoto) -> Unit,
    onRetakeClick: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupedPhotos = photos.groupBy { it.date }.toSortedMap(compareByDescending { it })
    val dates = groupedPhotos.keys.toList().ifEmpty { listOf(currentDateText()) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(dates) { date ->
            DatePhotoSection(
                date = date,
                photos = groupedPhotos[date].orEmpty(),
                onPhotoClick = onPhotoClick,
                onRetakeClick = onRetakeClick,
            )
        }
    }
}

@Composable
private fun DatePhotoSection(
    date: String,
    photos: List<MealPhoto>,
    onPhotoClick: (MealPhoto) -> Unit,
    onRetakeClick: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MealType.entries.forEach { mealType ->
                val photo = photos.firstOrNull { it.mealType == mealType }
                MealPhotoSlot(
                    mealType = mealType,
                    photo = photo,
                    onPhotoClick = onPhotoClick,
                    onRetakeClick = onRetakeClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MealPhotoSlot(
    mealType: MealType,
    photo: MealPhoto?,
    onPhotoClick: (MealPhoto) -> Unit,
    onRetakeClick: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = mealType.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            if (photo == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color(0xFFE5E7EB)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "未撮影",
                        color = Color(0xFF475467),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                PhotoThumbnail(
                    photo = photo,
                    onPhotoClick = onPhotoClick,
                )
            }

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                onClick = { onRetakeClick(mealType) },
            ) {
                Text(text = if (photo == null) "撮影" else "撮り直し")
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(
    photo: MealPhoto,
    onPhotoClick: (MealPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(photo.imagePath) {
        BitmapFactory.decodeFile(photo.imagePath)?.asImageBitmap()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color(0xFFE5E7EB))
            .clickable { onPhotoClick(photo) },
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap == null) {
            Text(text = "読込失敗")
        } else {
            Image(
                bitmap = imageBitmap,
                contentDescription = "${photo.mealType.label}の写真",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun currentDateText(): String = java.time.LocalDate.now().toString()
