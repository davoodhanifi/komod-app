package com.komod.api.presentation.savedoutfits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.komod.api.domain.model.OutfitItem
import com.komod.api.domain.model.SavedOutfit
import com.komod.api.presentation.home.ShimmerBox
import com.komod.api.presentation.home.relativeSavedDate
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.arrow_left
import komod.shared.generated.resources.hanger_filled
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C5CFC)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedOutfitsScreen(
    onNavigateBack: () -> Unit,
    onOutfitClick: (SavedOutfit) -> Unit,
    onShowSnackbar: (String) -> Unit = {},
    viewModel: SavedOutfitsViewModel = koinViewModel(),
    lazyGridState: LazyGridState = rememberLazyGridState(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SavedOutfitsEffect.ShowMessage -> onShowSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "Saved Outfits",
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_left),
                            contentDescription = "Back",
                            tint = DarkText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        when (val state = uiState) {
            SavedOutfitsUiState.Loading -> {
                SavedOutfitsGridSkeleton(modifier = Modifier.padding(padding))
            }

            is SavedOutfitsUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    if (state.outfits.isEmpty()) {
                        SavedOutfitsEmptyListState(modifier = Modifier.fillMaxSize())
                    } else {
                        val shouldLoadMore by remember {
                            derivedStateOf {
                                val layoutInfo = lazyGridState.layoutInfo
                                val totalItems = layoutInfo.totalItemsCount
                                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                totalItems > 0 && lastVisibleIndex >= totalItems - 6
                            }
                        }
                        LaunchedEffect(shouldLoadMore, state.outfits) {
                            if (shouldLoadMore) viewModel.loadMoreOutfits()
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = lazyGridState,
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(items = state.outfits, key = { it.id }) { outfit ->
                                SavedOutfitGridCard(
                                    outfit = outfit,
                                    onClick = { onOutfitClick(outfit) },
                                )
                            }

                            if (state.isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    SavedOutfitsLoadMoreIndicator()
                                }
                            }
                        }
                    }
                }
            }

            is SavedOutfitsUiState.Error -> {
                SavedOutfitsErrorState(
                    message = state.message,
                    onRetry = viewModel::load,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
        }
    }
}

@Composable
private fun SavedOutfitsLoadMoreIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = Purple,
        )
    }
}

@Composable
private fun SavedOutfitGridCard(
    outfit: SavedOutfit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            SavedOutfitGridPreview(
                items = outfit.items,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
            )
            if (outfit.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Favorite",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Purple)
                        .padding(4.dp),
                )
            }
        }
        Text(
            text = outfit.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = relativeSavedDate(outfit.createdAt),
            fontSize = 12.sp,
            color = GrayText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SavedOutfitGridPreview(
    items: List<OutfitItem>,
    modifier: Modifier = Modifier,
) {
    val previewItems = items.take(3).filter { !it.imageUrl.isNullOrBlank() }

    Box(modifier = modifier.background(Color(0xFFF3F4F6))) {
        if (previewItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.hanger_filled),
                    contentDescription = null,
                    tint = Color(0xFFD1D5DB),
                    modifier = Modifier.size(36.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                previewItems.forEach { item ->
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedOutfitsGridSkeleton(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(count = 6) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp))
                ShimmerBox(modifier = Modifier.width(60.dp).height(12.dp))
            }
        }
    }
}

@Composable
private fun SavedOutfitsEmptyListState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
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
                painter = painterResource(Res.drawable.hanger_filled),
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No saved outfits yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DarkText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Outfits you save will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SavedOutfitsErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
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
