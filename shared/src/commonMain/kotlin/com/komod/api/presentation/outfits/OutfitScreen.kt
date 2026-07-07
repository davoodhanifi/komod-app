package com.komod.api.presentation.outfits

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.komod.api.domain.model.Outfit
import com.komod.api.domain.model.OutfitOccasion
import com.komod.api.domain.model.OutfitStyle
import com.komod.api.domain.model.OutfitItem
import org.koin.compose.viewmodel.koinViewModel

private val OutfitPurple = Color(0xFF7C5CFC)
private val OutfitPurpleSoft = Color(0xFFF0ECFF)
private val OutfitText = Color(0xFF111827)
private val OutfitMuted = Color(0xFF6B7280)
private val OutfitSurface = Color(0xFFF7F5F2)
private val OutfitChipSurface = Color(0xFFF3F4F6)
private val OutfitBorder = Color(0xFFE5E7EB)
private val OutfitSkeleton = Color(0xFFE5E7EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitScreen(
    initialOccasion: String? = null,
    viewModel: OutfitViewModel = koinViewModel(),
    onOpenDetails: (Outfit) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStyleSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle initial occasion and auto-generate (only once)
    LaunchedEffect(initialOccasion) {
        if (initialOccasion != null) {
            // Find the matching OutfitOccasion enum
            val occasion = OutfitOccasion.entries.find { 
                it.label.equals(initialOccasion, ignoreCase = true) || 
                it.apiValue.equals(initialOccasion, ignoreCase = true)
            }
            if (occasion != null) {
                viewModel.autoGenerateOutfits(occasion)
            }
        }
    }

    if (showStyleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStyleSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            StyleSheetContent(
                selectedStyle = uiState.selectedStyle,
                onStyleSelected = { style ->
                    viewModel.selectStyle(style)
                    showStyleSheet = false
                },
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 104.dp),
    ) {
        item {
            OutfitHeader(
                selectedOccasion = uiState.selectedOccasion,
                onSettingsClick = { showStyleSheet = true },
                onOccasionSelected = viewModel::selectOccasion,
            )
        }

        when {
            uiState.isGenerating -> {
                item { GeneratingTitle() }
                items(2) { SkeletonOutfitCard() }
            }

            uiState.errorMessage != null && uiState.outfits.isEmpty() -> {
                item {
                    ErrorState(
                        message = uiState.errorMessage ?: "Something went wrong. Please try again.",
                        onRetry = viewModel::generateOutfits,
                    )
                }
            }

            uiState.outfits.isEmpty() -> {
                item { EmptyState(isGenerating = uiState.isGenerating, onGenerate = viewModel::generateOutfits) }
            }

            else -> {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(
                    items = uiState.outfits,
                    key = { outfit -> outfit.name + outfit.wardrobeItemIds.joinToString() },
                ) { outfit ->
                    OutfitCard(
                        outfit = outfit,
                        onTryThis = { onOpenDetails(outfit) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OutfitHeader(
    selectedOccasion: OutfitOccasion,
    onSettingsClick: () -> Unit,
    onOccasionSelected: (OutfitOccasion) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Outfits",
                    color = OutfitText,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 38.sp,
                )
                Text(
                    text = "AI-powered recommendations\njust for you",
                    color = OutfitMuted,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Filters",
                    tint = OutfitMuted,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        OutfitFilters(
            selectedOccasion = selectedOccasion,
            onOccasionSelected = onOccasionSelected,
        )
    }
}

@Composable
fun OutfitFilters(
    selectedOccasion: OutfitOccasion,
    onOccasionSelected: (OutfitOccasion) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutfitOccasion.entries.forEach { occasion ->
            val selected = occasion == selectedOccasion
            FilterChip(
                label = occasion.label,
                icon = if (occasion == OutfitOccasion.All) Icons.Filled.AutoAwesome else Icons.Outlined.Checkroom,
                selected = selected,
                onClick = { onOccasionSelected(occasion) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(width = 70.dp, height = 88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF121212) else OutfitChipSurface)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else OutfitMuted,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            color = if (selected) Color.White else OutfitText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun GeneratingTitle() {
    Text(
        text = "Creating outfit recommendations...",
        color = OutfitMuted,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
    )
}

@Composable
private fun EmptyState(
    isGenerating: Boolean = false,
    onGenerate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFF8F4FF), Color(0xFFF5F1EC)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .clip(CircleShape)
                    .background(OutfitPurpleSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = OutfitPurple,
                    modifier = Modifier.size(54.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Generate AI outfits from your wardrobe.",
            color = OutfitText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onGenerate,
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = OutfitPurple),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (isGenerating) "Generating..." else "Generate Outfits",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun OutfitCard(
    outfit: Outfit,
    onTryThis: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clickable(onClick = onTryThis)
            .shadow(14.dp, shape, ambientColor = Color(0x14111827), spotColor = Color(0x14111827)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = OutfitSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(0.42f)) {
                    Text(
                        text = outfit.name,
                        color = OutfitText,
                        fontSize = 24.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MatchBadge(score = outfit.matchScore)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = outfit.reason,
                        color = OutfitMuted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutfitImageCollage(
                    wardrobeItems = outfit.items,
                    modifier = Modifier
                        .weight(0.58f)
                        .height(218.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, OutfitBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OutfitText),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checkroom,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Outfit",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Button(
                    onClick = onTryThis,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OutfitPurple),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Try This",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchBadge(score: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(OutfitPurpleSoft)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = OutfitPurple,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$score% match",
            color = OutfitPurple,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun OutfitImageCollage(
    wardrobeItems: List<OutfitItem>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xFFF4F0EA)),
    ) {
        val visibleItems = wardrobeItems
            .take(6)
            .filter { !it.imageUrl.isNullOrBlank() }

        if (visibleItems.isEmpty()) {
            EmptyCollagePlaceholder()
            return@BoxWithConstraints
        }

        val arrangedItems = arrangeItemsForComposition(visibleItems).take(6)
        val slots = collageSlots(
            items = arrangedItems,
            width = maxWidth,
            height = maxHeight,
        )
        arrangedItems.forEachIndexed { index, item ->
            CollageItem(item = item, slot = slots[index])
        }
    }
}

@Composable
private fun CollageItem(
    item: OutfitItem,
    slot: CollageSlot,
) {
    Box(
        modifier = Modifier
            .offset(x = slot.x, y = slot.y)
            .size(slot.width, slot.height)
            .zIndex(slot.zIndex)
            .shadow(
                elevation = if (item.isAccessoryLike()) 8.dp else 14.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x1F111827),
                spotColor = Color(0x1F111827),
            )
            .graphicsLayer { rotationZ = slot.rotation }
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)), RoundedCornerShape(18.dp))
            .padding(2.dp),
    ) {
        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = null,
                    tint = Color(0xFFD1D5DB),
                )
            }
        }
    }
}

private data class CollageSlot(
    val width: Dp,
    val height: Dp,
    val x: Dp,
    val y: Dp,
    val rotation: Float,
    val zIndex: Float,
)

private fun collageSlots(
    items: List<OutfitItem>,
    width: Dp,
    height: Dp,
): List<CollageSlot> {
    val total = items.size.coerceAtMost(6)
    if (total == 1) {
        val item = items.first()
        return listOf(
            CollageSlot(
                width = if (item.isAccessoryLike()) width * 0.48f else width * 0.90f,
                height = if (item.isAccessoryLike()) height * 0.42f else height * 0.94f,
                x = if (item.isAccessoryLike()) width * 0.26f else width * 0.04f,
                y = if (item.isAccessoryLike()) height * 0.26f else height * 0.02f,
                rotation = if (item.isAccessoryLike()) -3f else 0f,
                zIndex = 3f,
            ),
        )
    }

    if (total == 2) {
        return listOf(
            CollageSlot(
                width = width * 0.62f,
                height = height * 0.86f,
                x = width * 0.02f,
                y = height * 0.02f,
                rotation = -2f,
                zIndex = 2f,
            ),
            CollageSlot(
                width = width * 0.58f,
                height = height * 0.86f,
                x = width * 0.40f,
                y = height * 0.06f,
                rotation = 2f,
                zIndex = 1f,
            ),
        )
    }

    val slots = mutableListOf(
        CollageSlot(
            width = width * 0.60f,
            height = height * 0.78f,
            x = width * 0.00f,
            y = height * 0.00f,
            rotation = -1.5f,
            zIndex = 3f,
        ),
        CollageSlot(
            width = width * 0.54f,
            height = height * 0.86f,
            x = width * 0.42f,
            y = height * 0.03f,
            rotation = 1.8f,
            zIndex = 2f,
        ),
        CollageSlot(
            width = width * 0.44f,
            height = height * 0.30f,
            x = width * 0.16f,
            y = height * 0.62f,
            rotation = -4f,
            zIndex = 4f,
        ),
    )

    val accessoryAnchors = listOf(
        Triple(width * 0.66f, height * 0.62f, 4.5f),
        Triple(width * 0.02f, height * 0.64f, -6f),
        Triple(width * 0.72f, height * 0.24f, 6f),
    )

    items.drop(3).forEachIndexed { index, item ->
        val anchor = accessoryAnchors[index % accessoryAnchors.size]
        val baseW = if (item.isAccessoryLike()) width * 0.22f else width * 0.34f
        val baseH = if (item.isAccessoryLike()) height * 0.18f else height * 0.28f
        slots += CollageSlot(
            width = baseW,
            height = baseH,
            x = anchor.first,
            y = anchor.second,
            rotation = anchor.third,
            zIndex = if (item.isAccessoryLike()) 5f else 2.5f,
        )
    }
    return slots.take(total)
}

@Composable
private fun EmptyCollagePlaceholder() {
    val placeholders = listOf(
        CollageSlot(80.dp, 64.dp, 8.dp, 8.dp, 0f, 1f),
        CollageSlot(80.dp, 64.dp, 86.dp, 12.dp, 0f, 1f),
        CollageSlot(52.dp, 44.dp, 22.dp, 82.dp, 0f, 2f),
        CollageSlot(52.dp, 44.dp, 84.dp, 88.dp, 0f, 2f),
    )

    placeholders.forEach { slot ->
        Box(
            modifier = Modifier
                .offset(x = slot.x, y = slot.y)
                .size(slot.width, slot.height)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Checkroom,
                contentDescription = null,
                tint = Color(0xFFD1D5DB),
            )
        }
    }
}

private fun OutfitItem.isAccessoryLike(): Boolean {
    val value = listOfNotNull(category, subcategory)
        .joinToString(" ")
        .lowercase()
    if (value.isBlank()) return false
    return value.contains("accessor") ||
        value.contains("jewel") ||
        value.contains("hat") ||
        value.contains("bag") ||
        value.contains("belt") ||
        value.contains("watch") ||
        value.contains("sock") ||
        value.contains("scarf") ||
        value.contains("glove") ||
        value.contains("sunglass")
}

private enum class OutfitPieceType {
    Top,
    Bottom,
    Footwear,
    Accessory,
    Other,
}

private fun arrangeItemsForComposition(items: List<OutfitItem>): List<OutfitItem> {
    return items.sortedBy { item ->
        when (item.pieceType()) {
            OutfitPieceType.Top -> 0
            OutfitPieceType.Bottom -> 1
            OutfitPieceType.Footwear -> 2
            OutfitPieceType.Other -> 3
            OutfitPieceType.Accessory -> 4
        }
    }
}

private fun OutfitItem.pieceType(): OutfitPieceType {
    val value = listOfNotNull(category, subcategory)
        .joinToString(" ")
        .lowercase()
    return when {
        value.contains("shoe") || value.contains("sneaker") || value.contains("boot") || value.contains("footwear") -> OutfitPieceType.Footwear
        value.contains("pant") || value.contains("jean") || value.contains("trouser") || value.contains("short") || value.contains("skirt") -> OutfitPieceType.Bottom
        value.contains("shirt") || value.contains("t-shirt") || value.contains("tee") || value.contains("top") || value.contains("jacket") || value.contains("coat") || value.contains("hoodie") || value.contains("sweater") || value.contains("blazer") -> OutfitPieceType.Top
        isAccessoryLike() -> OutfitPieceType.Accessory
        else -> OutfitPieceType.Other
    }
}

@Composable
private fun SkeletonOutfitCard() {
    val transition = rememberInfiniteTransition(label = "outfit-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F1EA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonLine(width = 150.dp, height = 30.dp, alpha = alpha)
                    Spacer(modifier = Modifier.height(12.dp))
                    SkeletonLine(width = 88.dp, height = 26.dp, alpha = alpha)
                    Spacer(modifier = Modifier.height(12.dp))
                    SkeletonLine(width = 110.dp, height = 12.dp, alpha = alpha)
                    Spacer(modifier = Modifier.height(8.dp))
                    repeat(3) {
                        SkeletonLine(width = 130.dp, height = 12.dp, alpha = alpha)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(196.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = alpha)),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SkeletonLine(width = 0.dp, height = 52.dp, alpha = alpha, fill = true, modifier = Modifier.weight(1f))
                SkeletonLine(width = 0.dp, height = 52.dp, alpha = alpha, fill = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SkeletonLine(
    width: Dp,
    height: Dp,
    alpha: Float,
    fill: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier.width(width))
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(OutfitSkeleton.copy(alpha = alpha)),
    )
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(58.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = OutfitMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = OutfitPurple),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        ) {
            Text(text = "Retry", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StyleSheetContent(
    selectedStyle: OutfitStyle,
    onStyleSelected: (OutfitStyle) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = "Style",
            color = OutfitText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutfitStyle.entries.forEach { style ->
            StyleRow(
                style = style,
                selected = style == selectedStyle,
                onClick = { onStyleSelected(style) },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StyleRow(
    style: OutfitStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) OutfitPurpleSoft else OutfitChipSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = style.label,
            color = OutfitText,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
