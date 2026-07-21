package com.komod.api.presentation.outfits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komod.api.domain.model.WeatherCurrent
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.arrow_right
import komod.shared.generated.resources.cloud_fog_stroke_rounded
import komod.shared.generated.resources.cloud_lightning_stroke_rounded
import komod.shared.generated.resources.cloud_stroke_rounded
import komod.shared.generated.resources.rain_stroke_rounded
import komod.shared.generated.resources.slow_winds_stroke_rounded
import komod.shared.generated.resources.snow_stroke_rounded
import komod.shared.generated.resources.sun_cloud_02_stroke_rounded
import komod.shared.generated.resources.sun_01_stroke_rounded
import komod.shared.generated.resources.uv_02_stroke_rounded
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private val WeatherPurple = Color(0xFF6E56FF)
private val WeatherPurpleSoft = Color(0xFFF2EDFF)
private val WeatherText = Color(0xFF121826)
private val WeatherMuted = Color(0xFF6B7280)
private val WeatherBorder = Color(0xFFF0EAFD)
private val WeatherDisabledBg = Color(0xFFF3F4F6)
private val WeatherDisabledText = Color(0xFF9CA3AF)
private val WeatherDisabledIcon = Color(0xFFD1D5DB)

@Composable
fun WeatherSection(
    uiState: WeatherUiState,
    onToggleWeather: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onPermissionDenied: () -> Unit,
) {
    val permissionController = rememberWeatherPermissionController(
        onGranted = { onToggleWeather(true) },
        onDenied = {
            onPermissionDenied()
            onToggleWeather(false)
        },
    )
    var isExpanded by remember { mutableStateOf(true) }

    // Cache last known weather so we can show it even when weather is disabled
    var lastKnownWeather by remember { mutableStateOf<WeatherCurrent?>(null) }
    if (uiState is WeatherUiState.Loaded) {
        lastKnownWeather = uiState.weather
    }

    val weatherEnabled = uiState != WeatherUiState.WeatherDisabled && uiState != WeatherUiState.PermissionRequired

    val onToggle = { checked: Boolean ->
        if (checked) {
            if (permissionController.hasPermission) {
                onToggleWeather(true)
            } else {
                permissionController.requestPermission()
            }
        } else {
            onToggleWeather(false)
        }
    }

    when (uiState) {
        WeatherUiState.PermissionRequired -> PermissionRequiredBanner(onOpenSettings = onOpenSettings)

        WeatherUiState.WeatherDisabled -> WeatherHeroCard(
            checked = false,
            isEnabled = false,
            expanded = isExpanded,
            onToggleChecked = onToggle,
            onExpandToggle = { isExpanded = !isExpanded },
            bodyContent = {
                val cached = lastKnownWeather
                if (cached != null) {
                    WeatherBody(
                        weather = cached,
                        expanded = isExpanded,
                        isEnabled = false,
                    )
                }
            },
        )

        WeatherUiState.Loading -> WeatherHeroCard(
            checked = true,
            isEnabled = true,
            expanded = true,
            onToggleChecked = onToggle,
            onExpandToggle = { isExpanded = !isExpanded },
            bodyContent = { WeatherSkeleton() },
        )

        is WeatherUiState.Error -> WeatherHeroCard(
            checked = true,
            isEnabled = true,
            expanded = isExpanded,
            onToggleChecked = onToggle,
            onExpandToggle = { isExpanded = !isExpanded },
            bodyContent = {
                WeatherErrorBody(
                    message = uiState.message,
                    onRetry = onRetry,
                )
            },
        )

        is WeatherUiState.Loaded -> WeatherHeroCard(
            checked = true,
            isEnabled = true,
            expanded = isExpanded,
            onToggleChecked = onToggle,
            onExpandToggle = { isExpanded = !isExpanded },
            bodyContent = {
                WeatherBody(
                    weather = uiState.weather,
                    expanded = isExpanded,
                    isEnabled = true,
                )
            },
        )
    }
}

@Composable
private fun WeatherHeroCard(
    checked: Boolean,
    isEnabled: Boolean,
    expanded: Boolean,
    onToggleChecked: (Boolean) -> Unit,
    onExpandToggle: () -> Unit,
    bodyContent: @Composable () -> Unit,
) {
    val cardBg = if (isEnabled) Color.White else WeatherDisabledBg
    val contentAlpha = if (isEnabled) 1f else 0.65f
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(200),
        label = "weather-arrow",
    )

    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .graphicsLayer { alpha = contentAlpha },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Header row — "Use weather" toggle right-aligned
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Use weather",
                    color = if (isEnabled) WeatherMuted else WeatherDisabledText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Switch(
                    checked = checked,
                    onCheckedChange = onToggleChecked,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WeatherPurple,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = WeatherDisabledIcon,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main weather body
            bodyContent()
        }
    }
}

@Composable
private fun WeatherBody(
    weather: WeatherCurrent,
    expanded: Boolean,
    isEnabled: Boolean,
) {
    val textColor = if (isEnabled) WeatherText else WeatherDisabledText
    val mutedColor = if (isEnabled) WeatherMuted else WeatherDisabledText
    val iconTint = if (isEnabled) WeatherPurple else WeatherDisabledIcon
    val iconTintUnspecified = if (isEnabled) Color.Unspecified else WeatherDisabledIcon

    Column(modifier = Modifier.fillMaxWidth()) {
        // Main info row: icon | temp + condition | rain/wind stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Weather illustration
            Icon(
                painter = painterResource(weatherIcon(weather)),
                contentDescription = weather.condition,
                tint = iconTintUnspecified,
                modifier = Modifier.size(70.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Temperature + condition
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Text(
                            text = "${weather.temperatureC.roundToInt()}°",
                            color = textColor,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 46.sp,
                        )
                        Text(
                            text = weather.condition,
                            color = mutedColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                Spacer(modifier = Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Feels like ${weather.feelsLikeC.roundToInt()}°",
                        color = mutedColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "|", color = Color(0xFFD0D5DD), fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        painter = painterResource(Res.drawable.slow_winds_stroke_rounded),
                        contentDescription = "Wind",
                        tint = iconTint,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${weather.windSpeedKmh.roundToInt()} km/h",
                        color = mutedColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

    }
}

@Composable
private fun WeatherErrorBody(
    message: String,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            color = WeatherMuted,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = WeatherPurple),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(text = "Retry", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PermissionRequiredBanner(
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier
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
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Use weather",
                    color = WeatherMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Switch(
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(
                        disabledUncheckedThumbColor = Color.White,
                        disabledUncheckedTrackColor = WeatherDisabledIcon,
                        disabledUncheckedBorderColor = Color.Transparent,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Location permission is required to use weather-aware outfit generation.",
                color = WeatherMuted,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = WeatherPurple),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(text = "Open app settings", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WeatherSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(WeatherPurpleSoft),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonLine(width = 96.dp, height = 48.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonLine(width = 120.dp, height = 18.dp)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        SkeletonLine(width = 0.dp, height = 16.dp, fill = true)
    }
}

@Composable
private fun SkeletonLine(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    fill: Boolean = false,
) {
    Box(
        modifier = Modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier.width(width))
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(WeatherBorder),
    )
}

private fun weatherIcon(weather: WeatherCurrent): DrawableResource {
    return when (normalizeCondition(weather.condition)) {
        "clear" -> Res.drawable.sun_01_stroke_rounded
        "partlycloudy" -> Res.drawable.sun_cloud_02_stroke_rounded
        "cloudy" -> Res.drawable.cloud_stroke_rounded
        "rain" -> Res.drawable.rain_stroke_rounded
        "snow" -> Res.drawable.snow_stroke_rounded
        "thunderstorm" -> Res.drawable.cloud_lightning_stroke_rounded
        "fog" -> Res.drawable.cloud_fog_stroke_rounded
        else -> when (weather.weatherCode) {
            0 -> Res.drawable.sun_01_stroke_rounded
            1 -> Res.drawable.sun_cloud_02_stroke_rounded
            2 -> Res.drawable.cloud_stroke_rounded
            3 -> Res.drawable.rain_stroke_rounded
            4 -> Res.drawable.snow_stroke_rounded
            5 -> Res.drawable.cloud_lightning_stroke_rounded
            6 -> Res.drawable.cloud_fog_stroke_rounded
            else -> when {
                weather.isSnowing -> Res.drawable.snow_stroke_rounded
                weather.isRaining -> Res.drawable.rain_stroke_rounded
                else -> Res.drawable.cloud_stroke_rounded
            }
        }
    }
}

private fun normalizeCondition(value: String): String {
    return value.lowercase().replace(" ", "").replace("_", "")
}

