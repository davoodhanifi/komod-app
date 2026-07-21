package com.komod.api.presentation.outfits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private val WeatherPurple = Color(0xFF6E56FF)
private val WeatherPurpleSoft = Color(0xFFF2EDFF)
private val WeatherText = Color(0xFF121826)
private val WeatherMuted = Color(0xFF6B7280)
private val WeatherBorder = Color(0xFFF0EAFD)

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
    val weatherEnabled = uiState != WeatherUiState.WeatherDisabled && uiState != WeatherUiState.PermissionRequired

    when (uiState) {
        WeatherUiState.PermissionRequired -> PermissionRequiredBanner(onOpenSettings = onOpenSettings)
        WeatherUiState.WeatherDisabled -> CollapsedWeatherCard(
            checked = false,
            enabled = true,
            onToggleChecked = {
                if (permissionController.hasPermission) {
                    onToggleWeather(true)
                } else {
                    permissionController.requestPermission()
                }
            },
        )
        WeatherUiState.Loading -> WeatherCard(
            checked = weatherEnabled,
            expanded = true,
            onToggleChecked = { checked ->
                if (checked) {
                    if (permissionController.hasPermission) {
                        onToggleWeather(true)
                    } else {
                        permissionController.requestPermission()
                    }
                } else {
                    onToggleWeather(false)
                }
            },
            onExpandToggle = { isExpanded = !isExpanded },
            bodyContent = { WeatherSkeleton() },
        )
        is WeatherUiState.Error -> WeatherCard(
            checked = weatherEnabled,
            expanded = isExpanded,
            onToggleChecked = { checked ->
                if (checked) {
                    if (permissionController.hasPermission) {
                        onToggleWeather(true)
                    } else {
                        permissionController.requestPermission()
                    }
                } else {
                    onToggleWeather(false)
                }
            },
            onExpandToggle = { isExpanded = !isExpanded },
            bodyContent = {
                WeatherErrorBody(
                    message = uiState.message,
                    onRetry = onRetry,
                )
            },
        )
        is WeatherUiState.Loaded -> WeatherCard(
            checked = weatherEnabled,
            expanded = isExpanded,
            onToggleChecked = { checked ->
                if (checked) {
                    if (permissionController.hasPermission) {
                        onToggleWeather(true)
                    } else {
                        permissionController.requestPermission()
                    }
                } else {
                    onToggleWeather(false)
                }
            },
            onExpandToggle = { isExpanded = !isExpanded },
            bodyContent = {
                WeatherBody(
                    weather = uiState.weather,
                    expanded = isExpanded,
                )
            },
        )
    }
}

@Composable
private fun WeatherCard(
    checked: Boolean,
    expanded: Boolean,
    onToggleChecked: (Boolean) -> Unit,
    onExpandToggle: () -> Unit,
    bodyContent: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x142A1454), spotColor = Color(0x142A1454)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .border(1.dp, WeatherBorder, RoundedCornerShape(28.dp))
                .padding(18.dp),
        ) {
            WeatherHeader(
                checked = checked,
                enabled = true,
                onToggleChecked = onToggleChecked,
            )
            Spacer(modifier = Modifier.height(16.dp))
            bodyContent()
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 90f else 0f,
                    animationSpec = tween(200),
                    label = "weather-arrow",
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WeatherPurpleSoft)
                        .clickable(onClick = onExpandToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_right),
                        contentDescription = null,
                        tint = WeatherPurple,
                        modifier = Modifier.size(18.dp).rotate(rotation),
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsedWeatherCard(
    checked: Boolean,
    enabled: Boolean,
    onToggleChecked: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x142A1454), spotColor = Color(0x142A1454)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .border(1.dp, WeatherBorder, RoundedCornerShape(28.dp))
                .padding(18.dp),
        ) {
            WeatherHeader(
                checked = checked,
                enabled = enabled,
                onToggleChecked = onToggleChecked,
            )
        }
    }
}

@Composable
private fun WeatherHeader(
    checked: Boolean,
    enabled: Boolean,
    onToggleChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Use weather",
            color = WeatherText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onToggleChecked,
        )
    }
}

@Composable
private fun WeatherBody(
    weather: WeatherCurrent,
    expanded: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(weatherIcon(weather)),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(110.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${weather.temperatureC.roundToInt()}°",
                    color = WeatherText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 52.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = weather.condition,
                        color = WeatherMuted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (weather.isRaining) {
                        Spacer(modifier = Modifier.width(12.dp))
                        WeatherIndicator(icon = Res.drawable.rain_stroke_rounded, label = "Rain")
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 5 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it / 5 },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WeatherDetail(text = "Feels like ${weather.feelsLikeC.roundToInt()}°")
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "|", color = Color(0xFFD0D5DD))
                Spacer(modifier = Modifier.width(12.dp))
                WeatherDetail(icon = Res.drawable.slow_winds_stroke_rounded, text = "${weather.windSpeedKmh.roundToInt()} km/h")
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
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x142A1454), spotColor = Color(0x142A1454)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .border(1.dp, WeatherBorder, RoundedCornerShape(28.dp))
                .padding(18.dp),
        ) {
            WeatherHeader(
                checked = false,
                enabled = false,
                onToggleChecked = {},
            )
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
private fun WeatherIndicator(
    icon: DrawableResource,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = WeatherPurple,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = WeatherMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun WeatherDetail(
    text: String,
    icon: DrawableResource? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = WeatherPurple,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = WeatherMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun WeatherSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(WeatherPurpleSoft),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonLine(width = 96.dp, height = 44.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonLine(width = 132.dp, height = 20.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonLine(width = 84.dp, height = 14.dp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
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
