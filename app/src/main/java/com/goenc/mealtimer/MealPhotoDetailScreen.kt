package com.goenc.mealtimer

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MealPhotoDetailScreen(
    photo: MealPhoto,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(photo.imagePath) {
        BitmapFactory.decodeFile(photo.imagePath)?.asImageBitmap()
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "${photo.date} ${photo.mealType.label}",
                    fontWeight = FontWeight.Bold,
                )
                Text(text = "記録写真")
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center,
            ) {
                if (imageBitmap == null) {
                    Text(
                        modifier = Modifier.padding(24.dp),
                        text = "画像を読み込めませんでした",
                    )
                } else {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "${photo.mealType.label}の拡大写真",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "閉じる")
            }
        },
    )
}
