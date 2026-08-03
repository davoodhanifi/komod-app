package com.komod.api.presentation.cropeditor

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.komod.api.domain.model.BoundingBox
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.arrow_left
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

private val Purple = Color(0xFF7C5CFC)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)
private val BorderColor = Color(0xFFE5E7EB)

private val HandleTouchSize = 32.dp
private val HandleVisualSize = 16.dp

@Composable
fun CropEditorScreen(
    imageId: String,
    wardrobeItemId: String,
    onNavigateBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onCropSaved: () -> Unit,
    viewModel: CropEditorViewModel = koinViewModel(parameters = { parametersOf(imageId, wardrobeItemId) }),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CropEditorEffect.CropSaved -> {
                    onShowSnackbar("Crop updated.")
                    onCropSaved()
                    onNavigateBack()
                }

                is CropEditorEffect.SaveFailed -> onShowSnackbar(effect.message)
            }
        }
    }

    val readyState = uiState as? CropEditorUiState.Ready
    val isSaving = readyState?.isSaving == true

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Adjust Crop",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkText,
                        )
                        if (readyState != null) {
                            Text(
                                text = readyState.itemTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isSaving) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val state = uiState) {
                CropEditorUiState.Loading -> CropEditorLoadingContent()
                is CropEditorUiState.Error -> CropEditorErrorContent(
                    message = state.message,
                    onRetry = viewModel::load,
                )

                is CropEditorUiState.Ready -> CropEditorReadyContent(
                    state = state,
                    onBoundingBoxChange = viewModel::updateBoundingBox,
                    onReset = viewModel::resetCrop,
                    onSave = viewModel::saveCrop,
                )
            }
        }
    }
}

@Composable
private fun CropEditorReadyContent(
    state: CropEditorUiState.Ready,
    onBoundingBoxChange: (BoundingBox) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    val interactionsEnabled = !state.isSaving

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            CropCanvas(
                imageUrl = state.originalImageUrl,
                boundingBox = state.cropState.currentBoundingBox,
                enabled = interactionsEnabled,
                onBoundingBoxChange = onBoundingBoxChange,
            )
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
                onClick = onReset,
                enabled = interactionsEnabled && state.cropState.isModified,
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
                onClick = onSave,
                enabled = interactionsEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(text = "Save Crop", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun CropCanvas(
    imageUrl: String?,
    boundingBox: BoundingBox,
    enabled: Boolean,
    onBoundingBoxChange: (BoundingBox) -> Unit,
) {
    var imageAspectRatio by remember { mutableStateOf(1f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // The rounded clip must stay on the image layer only. Corner handles are
    // centered exactly on the box's corners (half in, half out by design), so
    // clipping the outer container along with them would cut the handles away
    // entirely — they'd still be laid out correctly, just invisible.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(imageAspectRatio)
            .onSizeChanged { containerSize = it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Original uploaded image",
                    contentScale = ContentScale.Fit,
                    onSuccess = { success ->
                        val width = success.result.image.width
                        val height = success.result.image.height
                        if (width > 0 && height > 0) {
                            imageAspectRatio = width.toFloat() / height.toFloat()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
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
private fun CropBoxOverlay(
    boundingBox: BoundingBox,
    containerSize: IntSize,
    enabled: Boolean,
    onBoundingBoxChange: (BoundingBox) -> Unit,
) {
    val density = LocalDensity.current

    // pointerInput below is only keyed on (enabled, containerSize), so its
    // detectDragGestures block is launched once and never restarted as the box
    // moves. Reading through rememberUpdatedState (instead of closing over the
    // boundingBox/onBoundingBoxChange parameters directly) keeps every drag delta
    // anchored to the box's current position instead of the position from when
    // the gesture detector was first launched.
    val latestBoundingBox = rememberUpdatedState(boundingBox)
    val latestOnBoundingBoxChange = rememberUpdatedState(onBoundingBoxChange)

    val rectLeftPx = boundingBox.x * containerSize.width
    val rectTopPx = boundingBox.y * containerSize.height
    val rectWidthPx = boundingBox.width * containerSize.width
    val rectHeightPx = boundingBox.height * containerSize.height

    fun dragToNormalized(drag: Offset): Offset = Offset(
        x = drag.x / containerSize.width,
        y = drag.y / containerSize.height,
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scrim = Color.Black.copy(alpha = 0.55f)
        drawRect(color = scrim, topLeft = Offset.Zero, size = Size(size.width, rectTopPx))
        drawRect(
            color = scrim,
            topLeft = Offset(0f, rectTopPx + rectHeightPx),
            size = Size(size.width, size.height - (rectTopPx + rectHeightPx)),
        )
        drawRect(
            color = scrim,
            topLeft = Offset(0f, rectTopPx),
            size = Size(rectLeftPx, rectHeightPx),
        )
        drawRect(
            color = scrim,
            topLeft = Offset(rectLeftPx + rectWidthPx, rectTopPx),
            size = Size(size.width - (rectLeftPx + rectWidthPx), rectHeightPx),
        )
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(rectLeftPx.roundToInt(), rectTopPx.roundToInt()) }
            .size(
                width = pxToDp(rectWidthPx, density),
                height = pxToDp(rectHeightPx, density),
            )
            .border(BorderStroke(2.dp, Purple))
            .pointerInput(enabled, containerSize) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val normalized = dragToNormalized(dragAmount)
                    latestOnBoundingBoxChange.value(latestBoundingBox.value.moved(normalized.x, normalized.y))
                }
            },
    )

    CropHandle(
        enabled = enabled,
        offsetPx = Offset(rectLeftPx, rectTopPx),
        onDrag = { drag ->
            val normalized = dragToNormalized(drag)
            latestOnBoundingBoxChange.value(
                latestBoundingBox.value.resized(BoundingBox.Corner.TopLeft, normalized.x, normalized.y),
            )
        },
    )
    CropHandle(
        enabled = enabled,
        offsetPx = Offset(rectLeftPx + rectWidthPx, rectTopPx),
        onDrag = { drag ->
            val normalized = dragToNormalized(drag)
            latestOnBoundingBoxChange.value(
                latestBoundingBox.value.resized(BoundingBox.Corner.TopRight, normalized.x, normalized.y),
            )
        },
    )
    CropHandle(
        enabled = enabled,
        offsetPx = Offset(rectLeftPx, rectTopPx + rectHeightPx),
        onDrag = { drag ->
            val normalized = dragToNormalized(drag)
            latestOnBoundingBoxChange.value(
                latestBoundingBox.value.resized(BoundingBox.Corner.BottomLeft, normalized.x, normalized.y),
            )
        },
    )
    CropHandle(
        enabled = enabled,
        offsetPx = Offset(rectLeftPx + rectWidthPx, rectTopPx + rectHeightPx),
        onDrag = { drag ->
            val normalized = dragToNormalized(drag)
            latestOnBoundingBoxChange.value(
                latestBoundingBox.value.resized(BoundingBox.Corner.BottomRight, normalized.x, normalized.y),
            )
        },
    )
}

@Composable
private fun CropHandle(
    enabled: Boolean,
    offsetPx: Offset,
    onDrag: (Offset) -> Unit,
) {
    val density = LocalDensity.current
    val touchSizePx = with(density) { HandleTouchSize.toPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (offsetPx.x - touchSizePx / 2).roundToInt(),
                    y = (offsetPx.y - touchSizePx / 2).roundToInt(),
                )
            }
            .size(HandleTouchSize)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(HandleVisualSize)
                .background(Color.White, CircleShape)
                .border(BorderStroke(2.dp, Purple), CircleShape),
        )
    }
}

private fun pxToDp(px: Float, density: androidx.compose.ui.unit.Density): Dp = with(density) { px.toDp() }

@Composable
private fun CropEditorLoadingContent() {
    val transition = rememberInfiniteTransition(label = "cropEditorLoading")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(BorderColor.copy(alpha = alpha)),
        )
    }
}

@Composable
private fun CropEditorErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Couldn't load this item",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DarkText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = GrayText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
        ) {
            Text(
                text = "Try Again",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}
