package com.komod.api.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import org.jetbrains.compose.resources.painterResource
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.watch
import komod.shared.generated.resources.shirt
import komod.shared.generated.resources.shoes
import komod.shared.generated.resources.hoodie
import komod.shared.generated.resources.pants
import komod.shared.generated.resources.dress
import komod.shared.generated.resources.hat
import komod.shared.generated.resources.hand_bag
import komod.shared.generated.resources.necklace
import komod.shared.generated.resources.glasses
import komod.shared.generated.resources.belt
import komod.shared.generated.resources.boots
import komod.shared.generated.resources.high_heels
import komod.shared.generated.resources.sandals
import komod.shared.generated.resources.socks
import komod.shared.generated.resources.vest
import komod.shared.generated.resources.suit
import komod.shared.generated.resources.sport
import komod.shared.generated.resources.office
import komod.shared.generated.resources.date
import komod.shared.generated.resources.travel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.komod.api.domain.model.RecentItem
import com.komod.api.domain.model.User
import com.komod.api.domain.model.WardrobeSummary
import androidx.compose.ui.graphics.painter.Painter
import komod.shared.generated.resources.hanger_filled
import komod.shared.generated.resources.long_sleeve_shirt
import komod.shared.generated.resources.shorts_pants
import komod.shared.generated.resources.t_shirt
import komod.shared.generated.resources.tank_top
import komod.shared.generated.resources.user_filled
import org.jetbrains.compose.resources.DrawableResource
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C5CFC)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)
private val LightPurple = Color(0xFFF0EDFF)
private val SkeletonColor = Color(0xFFE5E7EB)
private val TileSelectedBackground = Color(0xFFEDE7FF)
private val TileBorderDefault = Color(0xFFE6E8EE)
private val TileIconDefault = Purple
private val TileLabelDefault = Color(0xFF1F2937)

private val Res.drawable.tree: DrawableResource
    get() = Res.drawable.sport

private val Res.drawable.briefcase: DrawableResource
    get() = Res.drawable.office

private val Res.drawable.heart: DrawableResource
    get() = Res.drawable.date

private val Res.drawable.airplane: DrawableResource
    get() = Res.drawable.travel

private data class OccasionOption(
    val label: String,
    val icon: DrawableResource,
)

@Composable
fun HomeScreen(
    user: User?,
    viewModel: HomeViewModel = koinViewModel(),
    onGenerateOutfit: (occasion: String) -> Unit,
    onViewWardrobe: () -> Unit,
    onItemClick: (String) -> Unit,
    refreshKey: Int = 0,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(bottom = 80.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        GreetingSection(user = user)

        Spacer(modifier = Modifier.height(24.dp))

        GenerateOutfitCard(
            selectedOccasion = uiState.selectedOccasion,
            onOccasionSelected = viewModel::selectOccasion,
            onGenerateClick = onGenerateOutfit,
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (val state = uiState.summaryState) {
            is WardrobeSummaryState.Loading -> WardrobeSummarySkeleton()
            is WardrobeSummaryState.Success -> WardrobeSummarySection(
                summary = state.summary,
                onViewAll = onViewWardrobe,
            )
            is WardrobeSummaryState.Error -> WardrobeSummaryError(
                message = state.message,
                onRetry = viewModel::retryLoadSummary,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (val state = uiState.recentItemsState) {
            is RecentItemsState.Loading -> RecentItemsSkeleton()
            is RecentItemsState.Success -> RecentItemsSection(
                items = state.items,
                onItemClick = onItemClick,
                onViewAll = onViewWardrobe,
            )
            is RecentItemsState.Error -> RecentItemsError(
                message = state.message,
                onRetry = viewModel::retryLoadRecentItems,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun GreetingSection(
    user: User?,
    modifier: Modifier = Modifier,
) {
    val greeting = "Hi" // Simplified for now - can be enhanced later

    val firstName = user?.displayName?.split(" ")?.firstOrNull() ?: "there"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$greeting, $firstName",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Let's find your perfect outfit",
                fontSize = 16.sp,
                color = GrayText,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        UserAvatar(photoUrl = user?.photoUrl)
    }
}

@Composable
private fun UserAvatar(photoUrl: String?) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(LightPurple),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = painterResource(Res.drawable.user_filled),
                fallback = painterResource(Res.drawable.user_filled),
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.user_filled),
                contentDescription = "Profile",
                tint = Purple,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
fun GenerateOutfitCard(
    selectedOccasion: String?,
    onOccasionSelected: (String) -> Unit,
    onGenerateClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val occasions = listOf(
        OccasionOption(label = "Outdoor", icon = Res.drawable.tree),
        OccasionOption(label = "Office", icon = Res.drawable.briefcase),
        OccasionOption(label = "Date", icon = Res.drawable.heart),
        OccasionOption(label = "Travel", icon = Res.drawable.airplane),
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = LightPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(
                text = "What are you dressing for today?",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Generate AI-powered outfit recommendations.",
                fontSize = 13.sp,
                color = GrayText,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                occasions.forEach { occasion ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        OccasionTile(
                            option = occasion,
                            selected = occasion.label == selectedOccasion,
                            onClick = { onOccasionSelected(occasion.label) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .sizeIn(minWidth = 64.dp, minHeight = 64.dp, maxWidth = 80.dp, maxHeight = 80.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { selectedOccasion?.let(onGenerateClick) },
                enabled = selectedOccasion != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple,
                    disabledContainerColor = Purple.copy(alpha = 0.45f),
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Generate Outfit",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "✨",
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun OccasionTile(
    option: OccasionOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) TileSelectedBackground else Color.White,
        animationSpec = tween(durationMillis = 200),
        label = "occasion-tile-bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Purple else TileBorderDefault,
        animationSpec = tween(durationMillis = 200),
        label = "occasion-tile-border",
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) Purple else TileIconDefault,
        animationSpec = tween(durationMillis = 200),
        label = "occasion-tile-icon",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) Purple else TileLabelDefault,
        animationSpec = tween(durationMillis = 200),
        label = "occasion-tile-label",
    )

    Surface(
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription = "${option.label}, ${if (selected) "selected" else "not selected"}"
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp)),
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(option.icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = option.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun WardrobeSummarySection(
    summary: WardrobeSummary,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Total + top 3 categories = 4 columns (never scrollable)
    val topCategories = summary.categories
        .sortedByDescending { it.count }
        .take(3)

    Card(
        modifier = modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewAll)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Header row inside the card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "My Komod",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                )
                Text(
                    text = "View all →",
                    color = Purple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // First column: total items
                WardrobeSummaryItem(
                    icon = painterResource(Res.drawable.hanger_filled),
                    iconColor = Purple,
                    count = summary.totalItems,
                    label = "Items",
                    modifier = Modifier.weight(1f),
                )

                topCategories.forEach { category ->
                    WardrobeSummaryDivider()
                    WardrobeSummaryItem(
                        icon = getCategoryIcon(category.category),
                        iconColor = getCategoryIconColor(category.category),
                        count = category.count,
                        label = category.category.capitalize(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WardrobeSummaryItem(
    icon: Painter,
    iconColor: Color,
    count: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText,
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = GrayText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WardrobeSummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(56.dp)
            .background(Color(0xFFE5E7EB)),
    )
}

@Composable
fun RecentItemsSection(
    items: List<RecentItem>,
    onItemClick: (String) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayItems = items.take(5)

    Card(
        modifier = modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewAll),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recently Added",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                )
                Text(
                    text = "View all →",
                    color = Purple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (displayItems.isEmpty()) {
                Text(
                    text = "No items yet. Add your first item to get started!",
                    fontSize = 14.sp,
                    color = GrayText,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    displayItems.forEach { item ->
                        RecentItemTile(
                            item = item,
                            onClick = { onItemClick(item.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentItemTile(
    item: RecentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center,
        ) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.itemName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = formatDate(item.createdAt),
            fontSize = 10.sp,
            color = GrayText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun WardrobeSummarySkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerBox(modifier = Modifier.width(100.dp).height(18.dp))
                ShimmerBox(modifier = Modifier.width(60.dp).height(14.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(4) { index ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(56.dp)
                                .background(Color(0xFFE5E7EB)),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ShimmerBox(modifier = Modifier.size(28.dp))
                        ShimmerBox(modifier = Modifier.width(32.dp).height(22.dp))
                        ShimmerBox(modifier = Modifier.width(40.dp).height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RecentItemsSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerBox(modifier = Modifier.width(120.dp).height(18.dp))
                ShimmerBox(modifier = Modifier.width(60.dp).height(14.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(4) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        ShimmerBox(modifier = Modifier.width(40.dp).height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WardrobeSummaryError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "My Komod",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = message, fontSize = 14.sp, color = GrayText)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun RecentItemsError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Recently Added",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = message, fontSize = 14.sp, color = GrayText)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer",
    )

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SkeletonColor.copy(alpha = alpha),
                        SkeletonColor.copy(alpha = alpha),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape = RoundedCornerShape(8.dp),
            ),
    )
}

@Composable
fun getCategoryIcon(category: String) = when (category.lowercase()) {
    "watch", "watches" -> painterResource(Res.drawable.watch)
    "shirt", "shirts" -> painterResource(Res.drawable.shirt)
    "t-shirt", "t-shirts", "tshirt", "tshirts", "tee", "tees" -> painterResource(Res.drawable.t_shirt)
    "long sleeve", "long-sleeve", "long sleeve shirt" -> painterResource(Res.drawable.long_sleeve_shirt)
    "top", "tops" -> painterResource(Res.drawable.tank_top)
    "shoe", "shoes", "sneaker", "sneakers" -> painterResource(Res.drawable.shoes)
    "jacket", "jackets", "blazer", "blazers", "outerwear", "coat", "coats" -> painterResource(Res.drawable.suit)
    "hoodie", "hoodies", "sweatshirt", "sweatshirts", "sweater", "sweaters" -> painterResource(Res.drawable.hoodie)
    "pant", "pants", "trouser", "trousers" -> painterResource(Res.drawable.pants)
    "short", "shorts" -> painterResource(Res.drawable.shorts_pants)
    "dress", "dresses" -> painterResource(Res.drawable.dress)
    "hat", "hats", "cap", "caps" -> painterResource(Res.drawable.hat)
    "bag", "bags", "handbag", "handbags" -> painterResource(Res.drawable.hand_bag)
    "accessory", "accessories", "necklace", "necklaces" -> painterResource(Res.drawable.necklace)
    "glasses", "sunglasses" -> painterResource(Res.drawable.glasses)
    "belt", "belts" -> painterResource(Res.drawable.belt)
    "boot", "boots" -> painterResource(Res.drawable.boots)
    "heels", "high heels", "high-heels" -> painterResource(Res.drawable.high_heels)
    "sandal", "sandals" -> painterResource(Res.drawable.sandals)
    "sock", "socks" -> painterResource(Res.drawable.socks)
    "vest", "vests" -> painterResource(Res.drawable.vest)
    "suit", "suits" -> painterResource(Res.drawable.suit)
    else -> painterResource(Res.drawable.shirt)
}

fun getCategoryIconColor(category: String): Color {
    return when (category.lowercase()) {
        "watch", "watches" -> Color(0xFF4CAF50)
        "shirt", "shirts", "t-shirt", "t-shirts", "tshirt", "tshirts", "tee", "tees",
        "top", "tops", "long sleeve", "long-sleeve" -> Color(0xFF2196F3)
        "shoe", "shoes", "sneaker", "sneakers", "boot", "boots",
        "heels", "high heels", "sandal", "sandals" -> Color(0xFFFF9800)
        "jacket", "jackets", "blazer", "blazers", "outerwear", "coat", "coats",
        "suit", "suits" -> Color(0xFF4CAF50)
        "hoodie", "hoodies", "sweatshirt", "sweatshirts", "sweater", "sweaters" -> Color(0xFF9C27B0)
        "pant", "pants", "trouser", "trousers", "short", "shorts" -> Color(0xFF009688)
        "dress", "dresses" -> Color(0xFFE91E63)
        "hat", "hats", "cap", "caps" -> Color(0xFFFFC107)
        "bag", "bags", "handbag", "handbags" -> Color(0xFFFF6F00)
        "accessory", "accessories", "necklace", "necklaces" -> Color(0xFFF06292)
        "glasses", "sunglasses" -> Color(0xFF03A9F4)
        "belt", "belts" -> Color(0xFF795548)
        "sock", "socks" -> Color(0xFF607D8B)
        else -> Color(0xFF7C5CFC)
    }
}

fun formatDate(isoDate: String): String {
    // Simple formatting for multiplatform compatibility
    // Format: "2024-01-15T10:30:00Z" -> "Jan 15, 2024"
    return try {
        val parts = isoDate.split("T").firstOrNull()?.split("-") ?: return isoDate
        if (parts.size < 3) return isoDate
        
        val year = parts[0]
        val month = parts[1].toIntOrNull() ?: return isoDate
        val day = parts[2].toIntOrNull() ?: return isoDate
        
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
            else -> return isoDate
        }
        
        "$monthName $day, $year"
    } catch (_: Exception) {
        isoDate
    }
}

fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
