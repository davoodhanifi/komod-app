package com.komod.api.presentation.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.komod.api.domain.model.CategoryCount
import com.komod.api.domain.model.RecentItem
import com.komod.api.domain.model.User
import com.komod.api.domain.model.WardrobeSummary
import komod.shared.generated.resources.user_filled
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C5CFC)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)
private val CardBg = Color(0xFFF9F9F9)
private val LightPurple = Color(0xFFF0EDFF)
private val SkeletonColor = Color(0xFFE5E7EB)

@Composable
fun HomeScreen(
    user: User?,
    viewModel: HomeViewModel = koinViewModel(),
    onGenerateOutfit: (occasion: String) -> Unit,
    onViewWardrobe: () -> Unit,
    onItemClick: (String) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
) {
    val uiState by viewModel.uiState.collectAsState()

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
            onGenerateClick = { onGenerateOutfit(uiState.selectedOccasion) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateOutfitCard(
    selectedOccasion: String,
    onOccasionSelected: (String) -> Unit,
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val occasions = listOf("Outdoor", "Office", "Date", "Travel")

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
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Generate an outfit from your wardrobe.",
                fontSize = 14.sp,
                color = GrayText,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                occasions.forEach { occasion ->
                    FilterChip(
                        selected = occasion == selectedOccasion,
                        onClick = { onOccasionSelected(occasion) },
                        label = { Text(occasion) },
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            selectedContainerColor = Purple,
                            selectedLabelColor = Color.White,
                            labelColor = DarkText,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = occasion == selectedOccasion,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onGenerateClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple,
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Text(
                    text = "Generate Outfit ✨",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun WardrobeSummarySection(
    summary: WardrobeSummary,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Your Komod",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )

            TextButton(onClick = onViewAll) {
                Text(
                    text = "View all →",
                    color = Purple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Text(
                    text = "${summary.totalItems} Items",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(summary.categories.sortedByDescending { it.count }) { categoryCount ->
                        CategoryChip(category = categoryCount)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: CategoryCount,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(getCategoryColor(category.category)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = getCategoryIcon(category.category),
                contentDescription = category.category,
                tint = getCategoryIconColor(category.category),
                modifier = Modifier.size(20.dp),
            )
        }

        Column {
            Text(
                text = category.category.capitalize(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DarkText,
            )
            Text(
                text = "${category.count} items",
                fontSize = 12.sp,
                color = GrayText,
            )
        }
    }
}

@Composable
fun RecentItemsSection(
    items: List<RecentItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Recently Added",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
            ) {
                Text(
                    text = "No items yet. Add your first item to get started!",
                    fontSize = 14.sp,
                    color = GrayText,
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    RecentItemCard(
                        item = item,
                        onClick = { onItemClick(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun RecentItemCard(
    item: RecentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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

            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Text(
                    text = item.itemName ?: "Untitled",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatDate(item.createdAt),
                    fontSize = 12.sp,
                    color = GrayText,
                )
            }
        }
    }
}

@Composable
fun WardrobeSummarySkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Your Komod",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(3) {
                        ShimmerBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentItemsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Recently Added",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(3) {
                Card(
                    modifier = Modifier.width(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                ) {
                    Column {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.9f),
                        )
                        Column(modifier = Modifier.padding(12.dp)) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(12.dp),
                            )
                        }
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
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Your Komod",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = GrayText,
                )
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
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Recently Added",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = GrayText,
                )
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
    "shoe", "shoes" -> painterResource(Res.drawable.shoes)
    "jacket", "jackets", "hoodie", "hoodies" -> painterResource(Res.drawable.hoodie)
    "pant", "pants", "trouser", "trousers" -> painterResource(Res.drawable.pants)
    "dress", "dresses" -> painterResource(Res.drawable.dress)
    "hat", "hats", "cap", "caps" -> painterResource(Res.drawable.hat)
    "bag", "bags" -> painterResource(Res.drawable.hand_bag)
    "accessory", "accessories", "necklace", "necklaces" -> painterResource(Res.drawable.necklace)
    "glasses", "sunglasses" -> painterResource(Res.drawable.glasses)
    "belt", "belts" -> painterResource(Res.drawable.belt)
    "boot", "boots" -> painterResource(Res.drawable.boots)
    "heels", "high heels" -> painterResource(Res.drawable.high_heels)
    "sandal", "sandals" -> painterResource(Res.drawable.sandals)
    "sock", "socks" -> painterResource(Res.drawable.socks)
    "vest", "vests" -> painterResource(Res.drawable.vest)
    "suit", "suits" -> painterResource(Res.drawable.suit)
    else -> painterResource(Res.drawable.shirt)
}

fun getCategoryIconColor(category: String): Color {
    return when (category.lowercase()) {
        "watch", "watches" -> Color(0xFF4CAF50)
        "shirt", "shirts" -> Color(0xFF2196F3)
        "shoe", "shoes" -> Color(0xFFFF9800)
        "jacket", "jackets" -> Color(0xFF9C27B0)
        "pant", "pants", "trouser", "trousers" -> Color(0xFF009688)
        "dress", "dresses" -> Color(0xFFE91E63)
        "hat", "hats" -> Color(0xFFFFC107)
        "bag", "bags" -> Color(0xFFFF6F00)
        "accessory", "accessories" -> Color(0xFFF06292)
        "glasses", "sunglasses" -> Color(0xFF03A9F4)
        else -> Color(0xFF7C5CFC)
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "watch", "watches" -> Color(0xFFE8F5E9)
        "shirt", "shirts" -> Color(0xFFE3F2FD)
        "shoe", "shoes" -> Color(0xFFFFF3E0)
        "jacket", "jackets" -> Color(0xFFF3E5F5)
        "pant", "pants", "trouser", "trousers" -> Color(0xFFE0F2F1)
        "dress", "dresses" -> Color(0xFFFCE4EC)
        "hat", "hats" -> Color(0xFFFFF9C4)
        "bag", "bags" -> Color(0xFFFFECB3)
        "accessory", "accessories" -> Color(0xFFF8BBD0)
        "glasses", "sunglasses" -> Color(0xFFE1F5FE)
        else -> Color(0xFFF0EDFF)
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
