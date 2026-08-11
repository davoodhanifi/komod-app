package com.komod.api.presentation.wardrobe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.komod.api.data.api.model.ImageStatus
import com.komod.api.domain.model.WardrobeItem
import com.komod.api.presentation.home.getCategoryIcon
import com.komod.api.presentation.home.getCategoryIconColor
import com.komod.api.presentation.main.LocalBottomNavBarHeight
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.delete
import komod.shared.generated.resources.hanger
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C5CFC)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)
private val CardBg = Color(0xFFF9F9F9)
private val SkeletonColor = Color(0xFFE5E7EB)
private val CategoryFilterItemWidth = 72.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeScreen(
    viewModel: WardrobeViewModel = koinViewModel(),
    refreshKey: Int = 0,
    initialCategory: String? = null,
    onAddItem: () -> Unit,
    onItemClick: (String) -> Unit,
    onUploadClick: (String) -> Unit,
    onShowSnackbar: (String) -> Unit = {},
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState = rememberLazyGridState(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val recentUploads by viewModel.recentUploads.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val pendingDeleteUploadId by viewModel.pendingDeleteUploadId.collectAsState()
    val deletingUploadIds by viewModel.deletingUploadIds.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val isBulkDeleting by viewModel.isBulkDeleting.collectAsState()
    val bulkDeleteConfirmVisible by viewModel.bulkDeleteConfirmVisible.collectAsState()

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WardrobeEffect.ShowMessage -> onShowSnackbar(effect.message)
            }
        }
    }

    // Belt-and-suspenders: fires every time this composable (re)enters composition —
    // cold start and every return from a screen pushed on top of it (e.g. Add Item) —
    // independent of the nav-level savedStateHandle signal above. Recent Uploads must
    // never depend on that signal alone to catch up after an upload.
    LaunchedEffect(Unit) {
        viewModel.refreshRecentUploads()
    }

    LaunchedEffect(initialCategory) {
        if (initialCategory != null) {
            viewModel.selectCategory(initialCategory)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = onAddItem,
                    containerColor = Purple,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 75.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Item")
                }
            }
        },
        containerColor = Color.White,
    ) { padding ->
        // Recent Uploads is driven by UploadedImageStore, not by the wardrobe-items
        // fetch below — it must render regardless of whether that fetch is Loading,
        // Success, or Error, or a same-session upload silently disappears from view
        // whenever an unrelated grid refresh is mid-flight or fails.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            WardrobeHeader(
                selectionMode = selectionMode,
                canSelect = uiState.let { it is WardrobeUiState.Success && it.items.isNotEmpty() },
                onEnterSelectionMode = viewModel::enterSelectionMode,
                onExitSelectionMode = viewModel::exitSelectionMode,
            )
            RecentUploadsSection(
                uploads = recentUploads,
                onUploadClick = onUploadClick,
                onUploadLongPress = viewModel::requestDeleteUpload,
            )

            when (val state = uiState) {
                WardrobeUiState.Loading -> {
                    LoadingState(modifier = Modifier.weight(1f))
                }

                is WardrobeUiState.Success -> {
                    val categories = remember(state.items) {
                        listOf(AllCategoriesLabel) + state.items.map { it.category }.distinct()
                    }
                    val filteredItems = remember(state.items, selectedCategory) {
                        if (selectedCategory == AllCategoriesLabel) {
                            state.items
                        } else {
                            state.items.filter { it.category == selectedCategory }
                        }
                    }

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (state.items.isNotEmpty()) {
                                CategoryFilterRow(
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    onCategorySelected = viewModel::selectCategory,
                                )
                            }

                            if (state.items.isEmpty()) {
                                EmptyState(
                                    onAddItem = onAddItem,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else if (filteredItems.isEmpty()) {
                                NoCategoryItemsState(
                                    category = selectedCategory,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    state = lazyGridState,
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 0.dp,
                                        bottom = LocalBottomNavBarHeight.current + if (selectionMode) 96.dp else 16.dp,
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    items(
                                        items = filteredItems,
                                        key = { item -> item.id },
                                    ) { item ->
                                        WardrobeCard(
                                            item = item,
                                            onClick = {
                                                if (selectionMode) {
                                                    viewModel.toggleItemSelection(item.id)
                                                } else {
                                                    onItemClick(item.id)
                                                }
                                            },
                                            selectionMode = selectionMode,
                                            isSelected = item.id in selectedItemIds,
                                            modifier = Modifier.animateItem(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is WardrobeUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = viewModel::loadItems,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (selectionMode) {
        SelectionActionBar(
            selectedCount = selectedItemIds.size,
            isDeleting = isBulkDeleting,
            onCancel = viewModel::exitSelectionMode,
            onDelete = viewModel::requestBulkDelete,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
    } // Box

    if (pendingDeleteUploadId != null) {
        DeleteUploadConfirmDialog(
            isDeleting = pendingDeleteUploadId in deletingUploadIds,
            onDismiss = viewModel::dismissDeleteUpload,
            onConfirm = viewModel::confirmDeleteUpload,
        )
    }

    if (bulkDeleteConfirmVisible) {
        val count = selectedItemIds.size
        ConfirmDeleteDialog(
            title = if (count == 1) "Delete this item?" else "Delete $count items?",
            message = "This action cannot be undone.",
            isDeleting = isBulkDeleting,
            onDismiss = viewModel::dismissBulkDeleteConfirm,
            onConfirm = viewModel::confirmBulkDelete,
        )
    }
}

@Composable
private fun WardrobeHeader(
    selectionMode: Boolean,
    canSelect: Boolean,
    onEnterSelectionMode: () -> Unit,
    onExitSelectionMode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "My Wardrobe",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (selectionMode) {
                Text(
                    text = "Cancel",
                    color = Purple,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onExitSelectionMode),
                )
            } else if (canSelect) {
                Text(
                    text = "Select",
                    color = Purple,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onEnterSelectionMode),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Everything you own, beautifully organized",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = categories, key = { category -> category }) { category ->
            CategoryFilterItem(
                category = category,
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun CategoryFilterItem(
    category: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = if (category == AllCategoriesLabel) painterResource(Res.drawable.hanger) else getCategoryIcon(category)
    val iconColor = if (category == AllCategoriesLabel) Purple else getCategoryIconColor(category)

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Purple.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(200),
        label = "categoryFilterBackground",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) Purple else GrayText,
        animationSpec = tween(200),
        label = "categoryFilterLabel",
    )

    Column(
        modifier = modifier
            .width(CategoryFilterItemWidth)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = category.toWardrobeLabel(),
            tint = iconColor,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = category.toWardrobeLabel(),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NoCategoryItemsState(category: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No items in ${category.toWardrobeLabel()} yet.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = DarkText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try adding more photos or choose another category.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RecentUploadsSection(
    uploads: List<RecentUploadUi>,
    onUploadClick: (String) -> Unit,
    onUploadLongPress: (String) -> Unit,
) {
    if (uploads.isEmpty()) return

    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Recent Uploads",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uploads.forEach { upload ->
                    UploadQueueCard(
                        upload = upload,
                        onClick = { onUploadClick(upload.imageId) },
                        onLongPress = { onUploadLongPress(upload.imageId) },
                    )
                }
            }

            if (uploads.any { it.status != ImageStatus.Analyzed }) {
                Text(
                    text = "Long press an upload to cancel.",
                    fontSize = 11.sp,
                    color = GrayText,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            AnimatedContent(
                targetState = uploadStatusMessage(uploads),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "uploadStatusMessage",
            ) { message ->
                if (message != null) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = message.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GrayText,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message.subtitle,
                            fontSize = 12.sp,
                            color = GrayText,
                        )
                    }
                }
            }
        }
    }
}

private sealed interface UploadStatusMessage {
    val title: String
    val subtitle: String

    data class Processing(val count: Int) : UploadStatusMessage {
        override val title: String
            get() = "$count ${if (count == 1) "photo is" else "photos are"} being processed ✨"
        override val subtitle: String
            get() = "Our AI is carefully finding your wardrobe pieces. This usually takes only a few seconds."
    }

    data object Ready : UploadStatusMessage {
        override val title: String = "Your wardrobe is ready! 🎉"
        override val subtitle: String =
            "Review your extracted items, keep what you like, and we'll save them to your wardrobe."
    }
}

private fun uploadStatusMessage(uploads: List<RecentUploadUi>): UploadStatusMessage? {
    val processingCount = uploads.count {
        it.status == ImageStatus.Pending || it.status == ImageStatus.Processing
    }
    if (processingCount > 0) return UploadStatusMessage.Processing(processingCount)

    val hasAnalyzed = uploads.any { it.status == ImageStatus.Analyzed }
    return if (hasAnalyzed) UploadStatusMessage.Ready else null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UploadQueueCard(upload: RecentUploadUi, onClick: () -> Unit, onLongPress: () -> Unit) {
    val isActive = upload.status == ImageStatus.Pending || upload.status == ImageStatus.Processing
    val isAnalyzed = upload.status == ImageStatus.Analyzed

    Card(
        modifier = Modifier
            .size(76.dp)
            .combinedClickable(
                onClick = { if (isAnalyzed) onClick() },
                // Matches the backend's own precondition: only images still Pending,
                // Processing, or Failed can be deleted this way — once analyzed, its
                // items must be removed individually instead.
                onLongClick = { if (!isAnalyzed) onLongPress() },
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ClothingPlaceholder(size = 28.dp)
            }
            if (upload.thumbnailUrl != null) {
                AsyncImage(
                    model = upload.thumbnailUrl,
                    contentDescription = "Uploaded photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (isActive) {
                UploadShimmerOverlay(modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            ),
                        )
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (upload.status == ImageStatus.Pending) "Uploading" else "Extracting items...",
                        color = Color.White,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isAnalyzed) {
                AnalyzedBadge(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp),
                )
            }

            if (upload.status == ImageStatus.Failed) {
                StatusBadge(
                    text = "Failed",
                    containerColor = Color(0xFFDC2626),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DeleteUploadConfirmDialog(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDeleteDialog(
        title = "Delete this upload?",
        message = "This upload will be removed from your queue.",
        isDeleting = isDeleting,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
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
                text = title,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )
        },
        text = {
            Text(
                text = message,
                color = GrayText,
            )
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
    )
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    isDeleting: Boolean,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 20.dp)
            .padding(bottom = LocalBottomNavBarHeight.current / 2),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$selectedCount selected",
                fontWeight = FontWeight.SemiBold,
                color = DarkText,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isDeleting,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onDelete,
                    enabled = !isDeleting && selectedCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD92D20),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFD92D20).copy(alpha = 0.4f),
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
            }
        }
    }
}

@Composable
private fun UploadShimmerOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "uploadShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "uploadShimmerAlpha",
    )

    Box(modifier = modifier.background(Purple.copy(alpha = alpha))) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(12.dp),
        )
    }
}

@Composable
private fun AnalyzedBadge(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.6f, animationSpec = tween(250)),
        modifier = modifier,
    ) {
        StatusBadge(text = "Review", containerColor = Color(0xFF16A34A))
    }
}

@Composable
private fun StatusBadge(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun WardrobeCard(
    item: WardrobeItem,
    onClick: () -> Unit,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, Purple, RoundedCornerShape(16.dp)) else Modifier,
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center,
            ) {
                ClothingPlaceholder()
                if (item.imageUrl != null) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.itemName ?: item.category,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (selectionMode) {
                    SelectionIndicator(
                        isSelected = isSelected,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = item.itemName ?: item.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = GrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.isFavorite || item.primaryColor != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item.primaryColor != null) {
                            Text(
                                text = item.primaryColor,
                                style = MaterialTheme.typography.labelSmall,
                                color = Purple,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (item.isFavorite) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Favorite item",
                                tint = Purple,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun ClothingPlaceholder(size: Dp = 48.dp) {
    Icon(
        painter = painterResource(Res.drawable.hanger),
        contentDescription = null,
        tint = Color(0xFFD1D5DB),
        modifier = Modifier.size(size),
    )
}

@Composable
private fun SelectionIndicator(isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (isSelected) Purple else Color.White)
            .border(1.5.dp, if (isSelected) Purple else Color(0xFFD1D5DB), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = LocalBottomNavBarHeight.current + 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(6) {
            SkeletonCard()
        }
    }
}

@Composable
private fun SkeletonCard() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(SkeletonColor.copy(alpha = alpha)),
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SkeletonColor.copy(alpha = alpha)),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SkeletonColor.copy(alpha = alpha)),
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun EmptyState(
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFEDE9FF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Checkroom,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No wardrobe items yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DarkText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your first outfit to start building your AI wardrobe.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddItem,
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
        ) {
            Text(
                text = "Add Item",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
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
            text = "Something went wrong",
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
