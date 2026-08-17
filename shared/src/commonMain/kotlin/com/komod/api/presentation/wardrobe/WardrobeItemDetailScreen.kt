package com.komod.api.presentation.wardrobe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.arrow_left
import komod.shared.generated.resources.delete
import komod.shared.generated.resources.hanger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val AppBg = Color(0xFFF7F8FC)
private val Primary = Color(0xFF7456FF)
private val Dark = Color(0xFF101828)
private val Muted = Color(0xFF667085)
private val LightDivider = Color(0xFFF1F3F7)
private val ChipBg = Color(0xFFF5F7FB)
private val Skeleton = Color(0xFFE9EDF4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeItemDetailScreen(
    wardrobeItemId: String,
    onNavigateBack: () -> Unit,
    onNavigateToWardrobeList: () -> Unit,
    onEditItem: () -> Unit,
    onAdjustCrop: (imageId: String) -> Unit,
    onRefreshWardrobe: () -> Unit,
    onRefreshHome: () -> Unit,
    refreshKey: Int = 0,
    onShowSnackbar: (String) -> Unit,
    viewModel: WardrobeItemDetailViewModel = koinViewModel(parameters = { parametersOf(wardrobeItemId) }),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val successState = uiState as? WardrobeItemDetailUiState.Success
    val isFavorite = successState?.item?.isFavorite == true
    val favoriteEnabled = successState?.let { !it.isDeleting && !it.isUpdatingFavorite } == true
    val deleteEnabled = successState?.isDeleting?.not() == true

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                WardrobeItemDetailEffect.FavoriteUpdated -> {
                    onRefreshWardrobe()
                    onRefreshHome()
                }
                is WardrobeItemDetailEffect.FavoriteUpdateFailed -> {
                    onShowSnackbar(effect.message)
                }
                WardrobeItemDetailEffect.DeleteSucceeded -> {
                    onShowSnackbar("Item deleted.")
                    onRefreshWardrobe()
                    onRefreshHome()
                    onNavigateBack()
                }
                WardrobeItemDetailEffect.ItemMissing -> {
                    onShowSnackbar("This item no longer exists.")
                    onRefreshWardrobe()
                    onRefreshHome()
                    onNavigateToWardrobeList()
                }
                is WardrobeItemDetailEffect.DeleteFailed -> {
                    onShowSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_left),
                            contentDescription = "Back",
                            tint = Dark,
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = favoriteEnabled,
                        onClick = { viewModel.onEvent(WardrobeItemDetailEvent.FavoriteClicked) },
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Unfavorite item" else "Favorite item",
                            tint = if (isFavorite) Color(0xFFD92D20) else Dark,
                        )
                    }
                    IconButton(onClick = onEditItem) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit item",
                            tint = Dark,
                        )
                    }
                    IconButton(
                        enabled = deleteEnabled,
                        onClick = { viewModel.onEvent(WardrobeItemDetailEvent.DeleteClicked) },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.delete),
                            contentDescription = "Delete item",
                            tint = Color(0xFFD92D20),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBg),
        ) {
            when (val state = uiState) {
                WardrobeItemDetailUiState.Loading -> LoadingContent()
                is WardrobeItemDetailUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = viewModel::load,
                )
                is WardrobeItemDetailUiState.Success -> DetailContent(
                    item = state.item,
                    showDeleteDialog = state.isDeleteDialogVisible,
                    isDeleting = state.isDeleting,
                    onDeleteDismiss = { viewModel.onEvent(WardrobeItemDetailEvent.DeleteDismissed) },
                    onDeleteConfirm = { viewModel.onEvent(WardrobeItemDetailEvent.DeleteConfirmed) },
                    onAdjustCrop = { onAdjustCrop(state.item.imageId) },
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    item: WardrobeItemDetail,
    showDeleteDialog: Boolean,
    isDeleting: Boolean,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onAdjustCrop: () -> Unit,
) {
    var showImageViewer by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            HeroImage(
                imageUrl = item.imageUrl,
                contentDescription = item.itemName ?: item.category,
                onClick = { showImageViewer = true },
                onAdjustCrop = onAdjustCrop,
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-26).dp)
                    .padding(horizontal = 0.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(34.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFD9DDE5)),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = itemTitle(item),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 37.sp,
                        lineHeight = 42.sp,
                        color = Dark,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = itemSubTitle(item),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = LightDivider)
                    Spacer(Modifier.height(16.dp))
                    QuickFactsRow(item = item)
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = LightDivider)
                    Spacer(Modifier.height(16.dp))
                    SectionTitle("Perfect for")
                    Spacer(Modifier.height(12.dp))
                    PerfectForChips(item = item)
                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = LightDivider)
                    Spacer(Modifier.height(16.dp))
                    SectionTitle("Details")
                    Spacer(Modifier.height(12.dp))
                    AddedOnRow(createdAt = item.createdAt)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            isDeleting = isDeleting,
            onDismiss = onDeleteDismiss,
            onConfirm = onDeleteConfirm,
        )
    }

    if (showImageViewer && !item.imageUrl.isNullOrBlank()) {
        FullScreenImageViewer(
            imageUrl = item.imageUrl,
            contentDescription = item.itemName ?: item.category,
            onDismiss = { showImageViewer = false },
        )
    }
}

@Composable
private fun HeroImage(
    imageUrl: String?,
    contentDescription: String,
    onClick: () -> Unit,
    onAdjustCrop: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .background(AppBg)
            .then(
                if (!imageUrl.isNullOrBlank()) Modifier.clickable(onClick = onClick) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Checkroom,
            contentDescription = null,
            tint = Color(0xFFD0D7E3),
            modifier = Modifier.size(84.dp),
        )
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            // Temporarily disabled: AI catalog image generation now handles
            // item extraction/cropping automatically.
            // TODO: Re-enable if manual crop adjustment is needed again.
            // Row(
            //     verticalAlignment = Alignment.CenterVertically,
            //     modifier = Modifier
            //         .align(Alignment.TopEnd)
            //         .padding(16.dp)
            //         .clip(RoundedCornerShape(50))
            //         .background(Color.Black.copy(alpha = 0.45f))
            //         .clickable(onClick = onAdjustCrop)
            //         .padding(horizontal = 12.dp, vertical = 8.dp),
            // ) {
            //     Icon(
            //         imageVector = Icons.Outlined.Crop,
            //         contentDescription = null,
            //         tint = Color.White,
            //         modifier = Modifier.size(14.dp),
            //     )
            //     Spacer(Modifier.width(6.dp))
            //     Text(
            //         text = "Adjust Crop",
            //         color = Color.White,
            //         fontSize = 12.sp,
            //         fontWeight = FontWeight.Medium,
            //     )
            // }
        }
    }
}

@Composable
private fun FullScreenImageViewer(
    imageUrl: String,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    var contentVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { contentVisible = true }

    val requestClose = {
        if (contentVisible) {
            contentVisible = false
            scope.launch {
                delay(200)
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = requestClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (scale <= 1f) requestClose() },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 3f
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale <= 1f) Offset.Zero else offset + pan
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
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
                    onClick = requestClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
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
}

@Composable
private fun QuickFactsRow(item: WardrobeItemDetail) {
    Row(modifier = Modifier.fillMaxWidth()) {
        FactItem(
            modifier = Modifier.weight(1f),
            icon = { Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp)) },
            label = "Color",
            value = item.primaryColor?.toLabel() ?: "—",
        )
        FactItem(
            modifier = Modifier.weight(1f),
            icon = { Icon(painter = painterResource(Res.drawable.hanger), contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp)) },
            label = "Category",
            value = item.category.toLabel(),
        )
        FactItem(
            modifier = Modifier.weight(1f),
            icon = { Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp)) },
            label = "Brand",
            value = item.brand?.takeIf { it.isNotBlank() }?.toLabel() ?: "—",
        )
        FactItem(
            modifier = Modifier.weight(1f),
            icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp)) },
            label = "Material",
            value = item.material?.toLabel() ?: "—",
        )
    }
}

@Composable
private fun FactItem(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    label: String,
    value: String,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = Muted,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = value,
            color = Dark,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Dark,
        fontWeight = FontWeight.Bold,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerfectForChips(item: WardrobeItemDetail) {
    val chips = when {
        !item.occasion.isNullOrEmpty() -> item.occasion.map { it.toLabel() }
        !item.style.isNullOrEmpty() -> item.style.map { it.toLabel() }
        else -> listOf("Casual")
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        chips.forEach { chip ->
            Surface(
                color = ChipBg,
                shape = RoundedCornerShape(100.dp),
            ) {
                Text(
                    text = chip,
                    color = Dark,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AddedOnRow(createdAt: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Added on",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = createdAt.toDisplayDate(),
            color = Dark,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LoadingContent() {
    val transition = rememberInfiniteTransition(label = "loading")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(370.dp)
                    .background(Skeleton.copy(alpha = alpha)),
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-26).dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonLine(width = 34.dp, height = 5.dp, alpha = alpha, centered = true)
                    SkeletonLine(width = 220.dp, height = 32.dp, alpha = alpha)
                    SkeletonLine(width = 190.dp, height = 18.dp, alpha = alpha)
                    SkeletonLine(width = 1.dp, height = 1.dp, alpha = alpha, fill = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        repeat(4) {
                            SkeletonLine(
                                width = 1.dp,
                                height = 58.dp,
                                alpha = alpha,
                                fill = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    SkeletonLine(width = 120.dp, height = 24.dp, alpha = alpha)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        repeat(3) {
                            SkeletonLine(width = 90.dp, height = 32.dp, alpha = alpha)
                        }
                    }
                    SkeletonLine(width = 1.dp, height = 52.dp, alpha = alpha, fill = true)
                }
            }
        }
    }
}

@Composable
private fun SkeletonLine(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    alpha: Float,
    centered: Boolean = false,
    fill: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .then(
                if (fill) Modifier.fillMaxWidth()
                else Modifier.width(width),
            )
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(Skeleton.copy(alpha = alpha))
            .then(if (centered) Modifier else Modifier),
    )
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
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
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(62.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Couldn't load item",
            style = MaterialTheme.typography.titleLarge,
            color = Dark,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
        ) {
            Text("Retry", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(
                painter = painterResource(Res.drawable.delete),
                contentDescription = null,
                tint = Color(0xFFD92D20),
            )
        },
        title = {
            Text(
                text = "Delete item?",
                fontWeight = FontWeight.Bold,
                color = Dark,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "This item will be permanently removed from your wardrobe.",
                    color = Muted,
                )
                Text(
                    text = "This action cannot be undone.",
                    color = Muted,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD92D20),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFD92D20).copy(alpha = 0.5f),
                    disabledContentColor = Color.White,
                ),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(24.dp),
    )
}

private fun itemTitle(item: WardrobeItemDetail): String {
    item.itemName?.takeIf { it.isNotBlank() }?.let { return it.toLabel() }
    val name = listOfNotNull(item.primaryColor, item.material, item.category).joinToString(" ")
    return name.toLabel().ifBlank { "Wardrobe Item" }
}

private fun itemSubTitle(item: WardrobeItemDetail): String {
    val left = item.formality?.toLabel()
    val right = item.season?.takeIf { it.isNotEmpty() }?.joinToString(" / ") { it.toLabel() }
    return listOfNotNull(left, right).joinToString("  •  ").ifBlank { "Smart Casual  •  All Season" }
}

private fun String.toLabel(): String = split('_', '-', ' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { part ->
        part.replaceFirstChar { c -> c.uppercase() }
    }

private fun String.toDisplayDate(): String {
    val onlyDate = take(10)
    val parts = onlyDate.split("-")
    if (parts.size != 3) return this
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return this
    val day = parts[2].toIntOrNull() ?: return this
    val monthName = when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> return this
    }
    return "$monthName $day, $year"
}
