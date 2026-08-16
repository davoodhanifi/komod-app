package com.komod.api.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.komod.api.domain.model.Outfit
import com.komod.api.domain.model.OutfitItem
import com.komod.api.domain.model.WardrobeSummary
import com.komod.api.presentation.outfits.weatherIcon
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.hanger_filled
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource

private val OutfitPurple = Color(0xFF7C5CFC)
private val OutfitLightPurple = Color(0xFFF0EDFF)
private val OutfitDarkText = Color(0xFF111827)
private val OutfitGrayText = Color(0xFF6B7280)
private val OutfitThumbnailBackground = Color(0xFFF3F4F6)
private val OutfitDirectionCardBackground = Color(0xFFFAFAFA)
private val OutfitDirectionCardBorder = Color(0xFFF0F0F0)

// No minimum-item rule exists server-side yet; this mirrors the "10 items across
// tops, bottoms and shoes" recommendation from the design reference.
const val MIN_WARDROBE_ITEMS_FOR_OUTFIT = 10

fun hasEnoughItemsForOutfit(summary: WardrobeSummary): Boolean =
    summary.totalItems >= MIN_WARDROBE_ITEMS_FOR_OUTFIT

sealed interface OutfitOfTheDayState {
    data object Loading : OutfitOfTheDayState
    data object NotEnoughItems : OutfitOfTheDayState

    // "outfits" is 1-5 entries — GET /outfits/today returns fewer than 5 whenever the
    // wardrobe can't support every styling direction well, which is normal and expected.
    data class Available(
        val outfits: List<Outfit>,
        val weather: OutfitOfTheDayWeather,
    ) : OutfitOfTheDayState
}

data class OutfitOfTheDayWeather(
    val temperatureC: Double,
    val condition: String,
    val isRaining: Boolean,
    val isSnowing: Boolean,
    val next6Hours: OutfitOfTheDayWeatherRange? = null,
)

data class OutfitOfTheDayWeatherRange(
    val minTemperatureC: Double,
    val maxTemperatureC: Double,
)

// "21° (70°F)" when the next-6h range is unknown, or "18–25° (64–77°F)" when it is — matching
// the Celsius-plus-Fahrenheit-in-parentheses format used on the outfit generation screen.
internal fun outfitTemperatureHeadline(weather: OutfitOfTheDayWeather): String {
    val range = weather.next6Hours
        ?: return "${weather.temperatureC.roundToInt()}° (${fahrenheit(weather.temperatureC)}°F)"
    return "${range.minTemperatureC.roundToInt()}–${range.maxTemperatureC.roundToInt()}° " +
        "(${fahrenheit(range.minTemperatureC)}–${fahrenheit(range.maxTemperatureC)}°F)"
}

private fun fahrenheit(celsius: Double): Int = (celsius * 9.0 / 5.0 + 32.0).roundToInt()

@Composable
fun OutfitOfTheDayCard(
    state: OutfitOfTheDayState,
    onViewOutfit: (Outfit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weather = (state as? OutfitOfTheDayState.Available)?.weather

    OutfitOfTheDayShell(modifier = modifier, weather = weather) {
        when (state) {
            is OutfitOfTheDayState.Loading -> LoadingBody()
            is OutfitOfTheDayState.NotEnoughItems -> NotEnoughItemsBody()
            is OutfitOfTheDayState.Available -> AvailableBody(state = state, onViewOutfit = onViewOutfit)
        }
    }
}

@Composable
private fun OutfitOfTheDayShell(
    modifier: Modifier = Modifier,
    weather: OutfitOfTheDayWeather? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .padding(horizontal = 20.dp)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Outfit of the Day ✨",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OutfitDarkText,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Styled by Komoda",
                        fontSize = 13.sp,
                        color = OutfitGrayText,
                    )
                }

                if (weather != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    WeatherBadge(weather = weather)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun NotEnoughItemsBody() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OutfitLightPurple),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.hanger_filled),
                contentDescription = null,
                tint = OutfitPurple,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "Not enough items yet",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = OutfitDarkText,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Add a few more items to your wardrobe and we'll create your daily outfit.",
                fontSize = 13.sp,
                color = OutfitGrayText,
            )
        }
    }
}

@Composable
private fun LoadingBody() {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ShimmerBox(modifier = Modifier.width(140.dp).height(16.dp))

        Spacer(modifier = Modifier.height(6.dp))

        ShimmerBox(modifier = Modifier.width(200.dp).height(12.dp))
    }
}

@Composable
private fun AvailableBody(
    state: OutfitOfTheDayState.Available,
    onViewOutfit: (Outfit) -> Unit,
) {
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.outfits, key = { it.id }) { outfit ->
                OutfitDirectionCard(
                    outfit = outfit,
                    onClick = { onViewOutfit(outfit) },
                )
            }
        }
    }
}

@Composable
private fun OutfitDirectionCard(
    outfit: Outfit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(OutfitDirectionCardBackground)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(OutfitLightPurple)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = outfit.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = OutfitPurple,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Reason is intentionally not shown here — it only appears once the user taps
        // through to Outfit Details for this outfit.
        if (outfit.items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                outfit.items.take(4).forEach { item ->
                    OutfitItemThumbnail(item = item, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OutfitItemThumbnail(
    item: OutfitItem,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(OutfitThumbnailBackground),
        contentAlignment = Alignment.Center,
    ) {
        if (item.imageUrl != null) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.category,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.hanger_filled),
                contentDescription = null,
                tint = Color(0xFFD1D5DB),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun WeatherBadge(weather: OutfitOfTheDayWeather) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(
                weatherIcon(
                    condition = weather.condition,
                    weatherCode = null,
                    isRaining = weather.isRaining,
                    isSnowing = weather.isSnowing,
                ),
            ),
            contentDescription = weather.condition,
            tint = OutfitPurple,
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.width(6.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = outfitTemperatureHeadline(weather),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OutfitDarkText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = weather.condition,
                fontSize = 11.sp,
                color = OutfitGrayText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
