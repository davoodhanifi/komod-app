package com.komod.api.presentation.outfits

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.komod.api.domain.model.OutfitItem
import com.komod.api.domain.model.OutfitOccasion
import com.komod.api.domain.model.OutfitStyle
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.business
import komod.shared.generated.resources.date
import komod.shared.generated.resources.hanger
import komod.shared.generated.resources.holiday
import komod.shared.generated.resources.office
import komod.shared.generated.resources.party
import komod.shared.generated.resources.shirt
import komod.shared.generated.resources.sport
import komod.shared.generated.resources.travel
import komod.shared.generated.resources.wedding
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

internal val OutfitPurple = Color(0xFF7C5CFC)
internal val OutfitPurpleSoft = Color(0xFFF0ECFF)
internal val OutfitText = Color(0xFF111827)
internal val OutfitMuted = Color(0xFF6B7280)
internal val OutfitSurface = Color(0xFFF7F5F2)
private val OutfitChipSurface = Color(0xFFF3F4F6)
internal val OutfitBorder = Color(0xFFE5E7EB)
private val OutfitSkeleton = Color(0xFFE5E7EB)
private val FilterTileSelectedBackground = Color(0xFFEDE7FF)
private val FilterTileBorderDefault = Color(0xFFE6E8EE)
private val FilterTileLabelDefault = Color(0xFF1F2937)

private data class OccasionFilterOption(
    val occasion: OutfitOccasion,
    val icon: DrawableResource,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitScreen(
    initialOccasion: String? = null,
    viewModel: OutfitViewModel = koinViewModel(),
    onOpenDetails: (Outfit) -> Unit,
    onShowSnackbar: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStyleSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OutfitEffect.ShowSnackbar -> onShowSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(initialOccasion) {
        if (initialOccasion != null) {
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
                onOccasionSelected = viewModel::selectOccasion,
                weatherUiState = uiState.weatherUiState,
                onWeatherToggle = viewModel::setWeatherEnabled,
                onWeatherRetry = viewModel::retryWeather,
                onOpenWeatherSettings = viewModel::openWeatherSettings,
                onWeatherPermissionDenied = viewModel::markWeatherPermissionRequired,
            )
        }

        item {
            StyleSelector(
                selectedStyle = uiState.selectedStyle,
                onClick = { showStyleSheet = true },
            )
        }

        item { GenerateButton(isGenerating = uiState.isGenerating, onGenerate = viewModel::generateOutfits) }

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
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
            else -> {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(
                    items = uiState.outfits,
                    key = { outfit -> outfit.id },
                ) { outfit ->
                    OutfitCard(
                        outfit = outfit,
                        onTryThis = { onOpenDetails(outfit) },
                        isSaving = outfit.id in uiState.savingOutfitIds,
                        isUnsaving = outfit.id in uiState.unsavingOutfitIds,
                        isSaved = outfit.id in uiState.savedOutfitIds,
                        onSaveOutfit = { viewModel.toggleSaveOutfit(outfit) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OutfitHeader(
    selectedOccasion: OutfitOccasion,
    onOccasionSelected: (OutfitOccasion) -> Unit,
    weatherUiState: WeatherUiState,
    onWeatherToggle: (Boolean) -> Unit,
    onWeatherRetry: () -> Unit,
    onOpenWeatherSettings: () -> Unit,
    onWeatherPermissionDenied: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Outfits",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your personal stylist, Komoda",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutfitFilters(
            selectedOccasion = selectedOccasion,
            onOccasionSelected = onOccasionSelected,
        )
        Spacer(modifier = Modifier.height(12.dp))
        WeatherSection(
            uiState = weatherUiState,
            onToggleWeather = onWeatherToggle,
            onRetry = onWeatherRetry,
            onOpenSettings = onOpenWeatherSettings,
            onPermissionDenied = onWeatherPermissionDenied,
        )

    }
}

@Composable
private fun StyleSelector(
    selectedStyle: OutfitStyle?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp),
    ) {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, OutfitBorder),
        ) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = null,
                tint = OutfitMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Style: ${selectedStyle?.label ?: "Any"}",
                color = OutfitText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun OutfitFilters(
    selectedOccasion: OutfitOccasion,
    onOccasionSelected: (OutfitOccasion) -> Unit,
) {
    val filterOptions = remember {
        val iconMap = mapOf(
            OutfitOccasion.Outdoor to Res.drawable.sport,
            OutfitOccasion.Office to Res.drawable.office,
            OutfitOccasion.Date to Res.drawable.date,
            OutfitOccasion.Travel to Res.drawable.travel,
            OutfitOccasion.Casual to Res.drawable.shirt,
            OutfitOccasion.Sport to Res.drawable.sport,
            OutfitOccasion.Party to Res.drawable.party,
            OutfitOccasion.Business to Res.drawable.business,
            OutfitOccasion.Wedding to Res.drawable.wedding,
            OutfitOccasion.Holiday to Res.drawable.holiday,
        )
        val preferredOrder = listOf(
            OutfitOccasion.Outdoor,
            OutfitOccasion.Office,
            OutfitOccasion.Date,
            OutfitOccasion.Travel,
            OutfitOccasion.Casual,
            OutfitOccasion.Sport,
            OutfitOccasion.Party,
            OutfitOccasion.Business,
            OutfitOccasion.Wedding,
            OutfitOccasion.Holiday,
        )
        preferredOrder
            .filter { it in OutfitOccasion.entries }
            .map { occasion ->
                OccasionFilterOption(
                    occasion = occasion,
                    icon = iconMap.getValue(occasion),
                )
            }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filterOptions.forEach { option ->
            OccasionFilterTile(
                option = option,
                selected = option.occasion == selectedOccasion,
                onClick = { onOccasionSelected(option.occasion) },
            )
        }
    }
}

@Composable
private fun OccasionFilterTile(
    option: OccasionFilterOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) FilterTileSelectedBackground else Color.White,
        animationSpec = tween(durationMillis = 200),
        label = "outfit-filter-bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) OutfitPurple else FilterTileBorderDefault,
        animationSpec = tween(durationMillis = 200),
        label = "outfit-filter-border",
    )
    val iconColor by animateColorAsState(
        targetValue = OutfitPurple,
        animationSpec = tween(durationMillis = 200),
        label = "outfit-filter-icon",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) OutfitPurple else FilterTileLabelDefault,
        animationSpec = tween(durationMillis = 200),
        label = "outfit-filter-label",
    )

    Column(
        modifier = Modifier
            .size(width = 80.dp, height = 80.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(option.icon),
            contentDescription = "${option.occasion.label}, ${if (selected) "selected" else "not selected"}",
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = option.occasion.label,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
private fun GenerateButton(
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
    isSaving: Boolean = false,
    isUnsaving: Boolean = false,
    isSaved: Boolean = false,
    onSaveOutfit: () -> Unit = {},
) {
    val shape = RoundedCornerShape(28.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .shadow(14.dp, shape, ambientColor = Color(0x14111827), spotColor = Color(0x14111827))
            .clickable(onClick = onTryThis),
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
                SaveOutfitButton(
                    isSaving = isSaving,
                    isUnsaving = isUnsaving,
                    isSaved = isSaved,
                    onClick = onSaveOutfit,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onTryThis,
                    enabled = !isSaving && !isUnsaving,
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
                    Text(text = "Try This", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SaveOutfitButton(
    isSaving: Boolean,
    isUnsaving: Boolean,
    isSaved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBusy = isSaving || isUnsaving
    OutlinedButton(
        onClick = onClick,
        enabled = !isBusy,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OutfitBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = OutfitText,
            disabledContentColor = if (isSaved) OutfitPurple else OutfitMuted,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        modifier = modifier,
    ) {
        when {
            isBusy -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = OutfitMuted,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isSaving) "Saving..." else "Removing...", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            isSaved -> {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Saved Outfit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            else -> {
                Icon(
                    painter = painterResource(Res.drawable.hanger),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Save Outfit", fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
        val visibleItems = wardrobeItems.take(6).filter { !it.imageUrl.isNullOrBlank() }
        if (visibleItems.isEmpty()) {
            EmptyCollagePlaceholder()
            return@BoxWithConstraints
        }
        val arrangedItems = arrangeItemsForComposition(visibleItems).take(6)
        val slots = collageSlots(items = arrangedItems, width = maxWidth, height = maxHeight)
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
            CollageSlot(width * 0.62f, height * 0.86f, width * 0.02f, height * 0.02f, -2f, 2f),
            CollageSlot(width * 0.58f, height * 0.86f, width * 0.40f, height * 0.06f, 2f, 1f),
        )
    }

    val slots = mutableListOf(
        CollageSlot(width * 0.60f, height * 0.78f, width * 0.00f, height * 0.00f, -1.5f, 3f),
        CollageSlot(width * 0.54f, height * 0.86f, width * 0.42f, height * 0.03f, 1.8f, 2f),
        CollageSlot(width * 0.44f, height * 0.30f, width * 0.16f, height * 0.62f, -4f, 4f),
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
    val value = listOfNotNull(category, subcategory).joinToString(" ").lowercase()
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
    val value = listOfNotNull(category, subcategory).joinToString(" ").lowercase()
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
    selectedStyle: OutfitStyle?,
    onStyleSelected: (OutfitStyle?) -> Unit,
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
        StyleRow(
            label = "Any style",
            selected = selectedStyle == null,
            onClick = { onStyleSelected(null) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutfitStyle.entries.forEach { style ->
            StyleRow(
                label = style.label,
                selected = style == selectedStyle,
                onClick = { onStyleSelected(style) },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StyleRow(
    label: String,
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
            text = label,
            color = OutfitText,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
