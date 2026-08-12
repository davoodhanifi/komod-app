package com.komod.api.presentation.additem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komod.api.platform.createThumbnail
import com.komod.api.platform.toImageBitmap
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.arrow_left
import komod.shared.generated.resources.delete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource

private val Purple = Color(0xFF7C5CFC)
private val LightLavender = Color(0xFFEDE9FF)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)
private val Green = Color(0xFF4ADE80)
private val Red = Color(0xFFD92D20)

/**
 * Step 1 of the multi-photo flow: an Instagram-style grid of every selected photo, shown before
 * any upload happens, so the user can pick which ones (if any) to crop — or drop from the batch
 * entirely. Nothing is uploaded until [onContinueUploading].
 */
@Composable
fun ReviewPhotosScreen(
    photos: List<ReviewPhoto>,
    onCropClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onContinueUploading: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            text = "Review Photos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkText,
                        )
                        Text(
                            text = "${photos.size} ${if (photos.size == 1) "photo" else "photos"} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrayText,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_left),
                            contentDescription = "Back",
                            tint = DarkText,
                        )
                    }
                },
            )
        },
        containerColor = Color.White,
        bottomBar = {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LightLavender.copy(alpha = 0.7f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Crop around the clothing you want to add for better detection " +
                            "and more accurate wardrobe results from Komoda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkText,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Button(
                    onClick = onContinueUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(text = "Continue Uploading", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(photos, key = { it.id }) { photo ->
                ReviewPhotoTile(
                    photo = photo,
                    onCropClick = { onCropClick(photo.id) },
                    onDeleteClick = { onDeleteClick(photo.id) },
                )
            }
        }
    }
}

@Composable
private fun ReviewPhotoTile(
    photo: ReviewPhoto,
    onCropClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var thumbnail by remember(photo.id) { mutableStateOf<ImageBitmap?>(null) }

    // Keyed on croppedBytes too: once a crop is confirmed, this reruns to decode the actual
    // cropped result — the previous thumbnail stays visible until it's ready, so there's no
    // flash back to a spinner for what the user just finished cropping.
    LaunchedEffect(photo.id, photo.croppedBytes) {
        val croppedBytes = photo.croppedBytes
        thumbnail = withContext(Dispatchers.Default) {
            if (croppedBytes != null) croppedBytes.toImageBitmap() else createThumbnail(photo.original.bytes, maxDimension = 384).toImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF3F4F6)),
    ) {
        val bitmap = thumbnail
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Selected photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp),
                strokeWidth = 2.dp,
                color = Purple,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                    ),
                )
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCropClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (photo.isCropped) Icons.Filled.Check else Icons.Filled.Crop,
                    contentDescription = if (photo.isCropped) "Cropped — tap to adjust" else "Crop",
                    tint = if (photo.isCropped) Green else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.delete),
                    contentDescription = "Remove photo",
                    tint = Red,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
