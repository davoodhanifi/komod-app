package com.komod.api.presentation.cropeditor

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komod.api.domain.model.BoundingBox
import com.komod.api.domain.model.CropEditorState
import com.komod.api.platform.PickedImage
import com.komod.api.platform.toImageBitmap
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.arrow_left
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource

private val Purple = Color(0xFF7C5CFC)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)
private val BorderColor = Color(0xFFE5E7EB)

/**
 * A pre-upload crop editor for a freshly-picked local photo. Unlike [CropEditorScreen], this
 * operates entirely on in-memory bytes — no network fetch, no server-known image/item id — since
 * the photo hasn't been uploaded yet. Confirming only reports the chosen [BoundingBox] back to
 * the caller; the caller decides when (and whether) to actually rasterize the crop.
 */
@Composable
fun PreUploadCropScreen(
    photo: PickedImage,
    initialBoundingBox: BoundingBox,
    title: String,
    subtitle: String? = null,
    primaryButtonLabel: String,
    isSaving: Boolean = false,
    onBack: () -> Unit,
    onConfirm: (BoundingBox) -> Unit,
) {
    var cropState by remember(photo) {
        mutableStateOf(CropEditorState(originalBoundingBox = initialBoundingBox, currentBoundingBox = initialBoundingBox))
    }
    var imageBitmap by remember(photo) { mutableStateOf<ImageBitmap?>(null) }
    val interactionsEnabled = !isSaving

    LaunchedEffect(photo) {
        imageBitmap = runCatching {
            withContext(Dispatchers.Default) { photo.bytes().toImageBitmap() }
        }.getOrNull()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkText,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = interactionsEnabled) {
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val bitmap = imageBitmap
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    PreUploadCropCanvas(
                        imageBitmap = bitmap,
                        boundingBox = cropState.currentBoundingBox,
                        enabled = interactionsEnabled,
                        onBoundingBoxChange = { cropState = cropState.withBoundingBox(it) },
                    )
                } else {
                    PreUploadCropLoadingContent()
                }
            }

            Text(
                text = "Drag inside the box to move it, or drag a corner to resize.",
                style = MaterialTheme.typography.bodySmall,
                color = GrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { cropState = cropState.reset() },
                    enabled = interactionsEnabled && cropState.isModified,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkText),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text(text = "Reset", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                Button(
                    onClick = { onConfirm(cropState.currentBoundingBox) },
                    enabled = interactionsEnabled && bitmap != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(text = primaryButtonLabel, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun PreUploadCropCanvas(
    imageBitmap: ImageBitmap,
    boundingBox: BoundingBox,
    enabled: Boolean,
    onBoundingBoxChange: (BoundingBox) -> Unit,
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val aspectRatio = imageBitmap.width.toFloat() / imageBitmap.height.toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (aspectRatio.isFinite() && aspectRatio > 0f) aspectRatio else 1f)
            .onSizeChanged { containerSize = it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Selected photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (containerSize.width > 0 && containerSize.height > 0) {
            CropBoxOverlay(
                boundingBox = boundingBox,
                containerSize = containerSize,
                enabled = enabled,
                onBoundingBoxChange = onBoundingBoxChange,
            )
        }
    }
}

@Composable
private fun PreUploadCropLoadingContent() {
    val transition = rememberInfiniteTransition(label = "preUploadCropLoading")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(BorderColor.copy(alpha = alpha)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Purple)
    }
}
