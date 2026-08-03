package com.komod.api.presentation.uploadreview

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.komod.api.domain.model.WardrobeItemDetail
import com.komod.api.presentation.wardrobe.toWardrobeLabel
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.arrow_left
import komod.shared.generated.resources.arrow_right
import komod.shared.generated.resources.hanger
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val Purple = Color(0xFF7C5CFC)
private val LightLavender = Color(0xFFEDE9FF)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)
private val BorderColor = Color(0xFFE5E7EB)

@Composable
fun UploadReviewScreen(
    imageId: String,
    onNavigateBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onReviewCompleted: () -> Unit,
    onAdjustCrop: (String) -> Unit,
    refreshKey: Int = 0,
    viewModel: UploadReviewViewModel = koinViewModel(parameters = { parametersOf(imageId) }),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) {
            viewModel.load()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UploadReviewEffect.ReviewSucceeded -> {
                    val count = effect.approvedCount
                    onShowSnackbar("$count ${if (count == 1) "item" else "items"} added to your wardrobe.")
                    onReviewCompleted()
                    onNavigateBack()
                }

                UploadReviewEffect.UploadDeleted -> {
                    onShowSnackbar("Upload deleted.")
                    onReviewCompleted()
                    onNavigateBack()
                }

                UploadReviewEffect.ReviewConflict -> {
                    onShowSnackbar("This upload has already been reviewed.")
                    onReviewCompleted()
                    onNavigateBack()
                }

                is UploadReviewEffect.ReviewFailed -> onShowSnackbar(effect.message)
            }
        }
    }

    val isSubmitting = (uiState as? UploadReviewUiState.Ready)?.isSubmitting == true

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Review Extracted Items",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkText,
                        )
                        val readyState = uiState as? UploadReviewUiState.Ready
                        if (readyState != null) {
                            val count = readyState.detail.items.size
                            Text(
                                text = "$count ${if (count == 1) "item" else "items"} detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayText,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isSubmitting) {
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
                UploadReviewUiState.Loading -> UploadReviewLoadingContent()
                is UploadReviewUiState.Error -> UploadReviewErrorContent(
                    message = state.message,
                    onRetry = viewModel::load,
                )
                is UploadReviewUiState.Ready -> UploadReviewReadyContent(
                    state = state,
                    onToggleItem = viewModel::toggleItemSelection,
                    onToggleSelectAll = viewModel::toggleSelectAll,
                    onSubmit = viewModel::submitReview,
                    onDelete = viewModel::deleteUpload,
                    onAdjustCrop = onAdjustCrop,
                )
            }
        }
    }
}

@Composable
private fun UploadReviewReadyContent(
    state: UploadReviewUiState.Ready,
    onToggleItem: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onSubmit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustCrop: (String) -> Unit,
) {
    val interactionsEnabled = !state.isSubmitting
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        UploadedImageHero(imageUrl = state.detail.originalImageUrl)

        if (state.detail.items.isEmpty()) {
            EmptyItemsContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
            )
        } else {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Detected Items",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                )
                Text(
                    text = "Review and select the items you want to add to your wardrobe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayText,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))

                val totalCount = state.detail.items.size
                val selectedCount = state.selectedItemIds.size
                val allSelected = selectedCount == totalCount && totalCount > 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$selectedCount of $totalCount selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (allSelected) "Deselect All" else "Select All",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (interactionsEnabled) Purple else GrayText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = interactionsEnabled, onClick = onToggleSelectAll)
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                state.detail.items.forEach { item ->
                    DetectedItemCard(
                        item = item,
                        isSelected = item.id in state.selectedItemIds,
                        enabled = interactionsEnabled,
                        onToggle = { onToggleItem(item.id) },
                        onImageClick = item.imageUrl?.let { url -> { previewImageUrl = url } },
                        onAdjustCrop = { onAdjustCrop(item.id) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onSubmit,
                    enabled = interactionsEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = "Save $selectedCount ${if (selectedCount == 1) "Item" else "Items"}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                    )
                }

                Text(
                    text = "Delete Upload",
                    color = if (interactionsEnabled) Purple else GrayText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = interactionsEnabled, onClick = onDelete)
                        .padding(vertical = 16.dp),
                )

                ConfidenceInfoCard(modifier = Modifier.padding(bottom = 24.dp))
            }
        }
    }

    previewImageUrl?.let { url ->
        ZoomableImagePreviewDialog(imageUrl = url, onDismiss = { previewImageUrl = null })
    }
}

@Composable
private fun UploadedImageHero(imageUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .background(Color(0xFFF3F4F6)),
    ) {
        Icon(
            painter = painterResource(Res.drawable.hanger),
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp),
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Uploaded image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Uploaded image",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DetectedItemCard(
    item: WardrobeItemDetail,
    isSelected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onImageClick: (() -> Unit)?,
    onAdjustCrop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = Purple, checkmarkColor = Color.White),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6))
                    .clickable(enabled = enabled && onImageClick != null) { onImageClick?.invoke() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.hanger),
                    contentDescription = null,
                    tint = Color(0xFFD1D5DB),
                    modifier = Modifier.size(32.dp),
                )
                if (item.imageUrl != null) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.itemName ?: item.category,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
            }
            Text(
                text = "Adjust Crop",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Purple else GrayText,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = enabled, onClick = onAdjustCrop)
                    .padding(vertical = 2.dp, horizontal = 2.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.itemName ?: item.category.toWardrobeLabel(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConfidenceBadge(confidence = item.confidence)
            }
            val categoryLine = listOfNotNull(
                item.category.toWardrobeLabel(),
                item.subcategory?.takeIf { it.isNotBlank() }?.toWardrobeLabel(),
            ).joinToString(" • ")
            if (categoryLine.isNotBlank()) {
                Text(
                    text = categoryLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item.formality?.takeIf { it.isNotBlank() }?.let { formality ->
                Text(
                    text = formality.toWardrobeLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Icon(
            painter = painterResource(Res.drawable.arrow_right),
            contentDescription = null,
            tint = GrayText,
        )
    }
}

@Composable
private fun ConfidenceBadge(confidence: Double, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "${(confidence * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = GrayText,
        )
    }
}

@Composable
private fun ConfidenceInfoCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
        )
        Text(
            text = "AI confidence indicates how likely it is that the item was correctly identified.",
            style = MaterialTheme.typography.bodySmall,
            color = GrayText,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun ZoomableImagePreviewDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale
                        offset = if (newScale <= 1f) Offset.Zero else offset + pan
                    }
                },
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Item photo preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun EmptyItemsContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(LightLavender),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Checkroom,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "We couldn't confidently detect any clothing items.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = DarkText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Delete Upload",
            color = Purple,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable {
                    // Delete isn't implemented yet.
                }
                .padding(vertical = 8.dp, horizontal = 12.dp),
        )
    }
}

@Composable
private fun UploadReviewLoadingContent() {
    val transition = rememberInfiniteTransition(label = "uploadReviewLoading")
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
                .aspectRatio(1.3f)
                .clip(RoundedCornerShape(20.dp))
                .background(BorderColor.copy(alpha = alpha)),
        )
        Spacer(modifier = Modifier.height(24.dp))
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BorderColor.copy(alpha = alpha)),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun UploadReviewErrorContent(
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
            text = "Couldn't load this upload",
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
