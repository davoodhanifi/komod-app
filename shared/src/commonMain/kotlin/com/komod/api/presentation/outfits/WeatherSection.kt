package com.komod.api.presentation.outfits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komod.api.domain.model.WeatherCurrent
import com.komod.api.domain.model.WeatherLocation
import komod.shared.generated.resources.Res
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
    editedTemperatureC: Double? = null,
    onTemperatureChange: (Double) -> Unit = {},
    onResetTemperature: () -> Unit = {},
) {
    val permissionController = rememberWeatherPermissionController(
        onGranted = { onToggleWeather(true) },
        onDenied = {
            onPermissionDenied()
            onToggleWeather(false)
        },
    )
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
            location = lastKnownWeather?.location,
            onToggleChecked = onToggle,
            bodyContent = {
                val cached = lastKnownWeather
                if (cached != null) {
                    WeatherBody(
                        weather = cached,
                        isEnabled = false,
                    )
                }
            },
        )

        WeatherUiState.Loading -> WeatherHeroCard(
            checked = true,
            isEnabled = true,
            location = null,
            onToggleChecked = onToggle,
            bodyContent = { WeatherSkeleton() },
        )

        is WeatherUiState.Error -> WeatherHeroCard(
            checked = true,
            isEnabled = true,
            location = null,
            onToggleChecked = onToggle,
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
            location = uiState.weather.location,
            onToggleChecked = onToggle,
            bodyContent = {
                WeatherBody(
                    weather = uiState.weather,
                    isEnabled = true,
                    editedTemperatureC = editedTemperatureC,
                    onTemperatureChange = onTemperatureChange,
                    onResetTemperature = onResetTemperature,
                )
            },
        )
    }
}

@Composable
private fun WeatherHeroCard(
    checked: Boolean,
    isEnabled: Boolean,
    location: WeatherLocation?,
    onToggleChecked: (Boolean) -> Unit,
    bodyContent: @Composable () -> Unit,
) {
    val cardBg = if (isEnabled) Color.White else WeatherDisabledBg
    val contentAlpha = if (isEnabled) 1f else 0.65f

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
            // Header row — location on the left, "Use weather" toggle on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                if (location != null) {
                    WeatherLocationLabel(
                        location = location,
                        isEnabled = isEnabled,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 12.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Use weather",
                        color = if (isEnabled) WeatherMuted else WeatherDisabledText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    androidx.compose.material3.Switch(
                        checked = checked,
                        onCheckedChange = onToggleChecked,
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WeatherPurple,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = WeatherDisabledIcon,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main weather body
            bodyContent()
        }
    }
}

@Composable
private fun WeatherLocationLabel(
    location: WeatherLocation,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val neighborhood = location.neighborhood?.takeIf { it.isNotBlank() }
    val city = cleanCityName(location.city)
    val primary = neighborhood ?: city ?: return
    val secondary = if (neighborhood != null) city else null

    Column(modifier = modifier) {
        Text(
            text = primary,
            color = if (isEnabled) WeatherText else WeatherDisabledText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (secondary != null) {
            Text(
                text = secondary,
                color = if (isEnabled) WeatherMuted else WeatherDisabledText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeatherBody(
    weather: WeatherCurrent,
    isEnabled: Boolean,
    editedTemperatureC: Double? = null,
    onTemperatureChange: (Double) -> Unit = {},
    onResetTemperature: () -> Unit = {},
) {
    val textColor = if (isEnabled) WeatherText else WeatherDisabledText
    val mutedColor = if (isEnabled) WeatherMuted else WeatherDisabledText
    val iconTint = if (isEnabled) WeatherPurple else WeatherDisabledIcon
    val iconTintUnspecified = if (isEnabled) Color.Unspecified else WeatherDisabledIcon

    val displayTemperatureC = editedTemperatureC ?: weather.temperatureC
    var isEditingTemperature by remember { mutableStateOf(false) }
    var editUnit by remember { mutableStateOf(TemperatureUnit.CELSIUS) }
    var editText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Main info row: icon | temp + condition
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Weather illustration
            Icon(
                painter = painterResource(
                    weatherIcon(
                        condition = weather.condition,
                        weatherCode = weather.weatherCode,
                        isRaining = weather.isRaining,
                        isSnowing = weather.isSnowing,
                    ),
                ),
                contentDescription = weather.condition,
                tint = iconTintUnspecified,
                modifier = Modifier.size(70.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Temperature (+ Fahrenheit) and condition
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${displayTemperatureC.roundToInt()}°",
                        color = textColor,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 46.sp,
                    )
                    Text(
                        text = fahrenheitLabel(displayTemperatureC),
                        color = mutedColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    if (isEnabled) {
                        IconButton(
                            onClick = {
                                editUnit = TemperatureUnit.CELSIUS
                                editText = displayTemperatureC.roundToInt().toString()
                                isEditingTemperature = true
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .padding(bottom = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit temperature",
                                tint = iconTint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                Text(
                    text = weather.condition,
                    color = mutedColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        AnimatedVisibility(visible = isEditingTemperature) {
            TemperatureEditor(
                text = editText,
                unit = editUnit,
                onTextChange = { newText -> editText = newText.filter { it.isDigit() || it == '-' } },
                onUnitChange = { newUnit ->
                    editText.toDoubleOrNull()?.let { current ->
                        editText = convertTemperature(current, editUnit, newUnit).roundToInt().toString()
                    }
                    editUnit = newUnit
                },
                onConfirm = {
                    editText.toDoubleOrNull()?.let { value ->
                        onTemperatureChange(convertTemperature(value, editUnit, TemperatureUnit.CELSIUS))
                    }
                    isEditingTemperature = false
                },
                onCancel = { isEditingTemperature = false },
            )
        }

        if (editedTemperatureC != null && !isEditingTemperature) {
            TextButton(
                onClick = onResetTemperature,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = "Reset to current weather",
                    color = WeatherPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(WeatherBorder),
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Feels like (left wing) | Wind (right wing)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = "Feels like ${weather.feelsLikeC.roundToInt()}° ${fahrenheitLabel(weather.feelsLikeC)}",
                    color = mutedColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(16.dp)
                    .background(WeatherBorder),
            )

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

private enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

private fun convertTemperature(value: Double, from: TemperatureUnit, to: TemperatureUnit): Double {
    if (from == to) return value
    return if (to == TemperatureUnit.FAHRENHEIT) value * 9.0 / 5.0 + 32.0 else (value - 32.0) * 5.0 / 9.0
}

@Composable
private fun TemperatureEditor(
    text: String,
    unit: TemperatureUnit,
    onTextChange: (String) -> Unit,
    onUnitChange: (TemperatureUnit) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TemperatureInputPill(value = text, onValueChange = onTextChange)
        UnitToggle(selected = unit, onSelect = onUnitChange)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cancel",
                tint = WeatherMuted,
                modifier = Modifier.size(16.dp),
            )
        }
        IconButton(onClick = onConfirm, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Save temperature",
                tint = WeatherPurple,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// Matches the app's compact "pill" chip language (see UnitChip/OccasionFilterTile) rather
// than Material3's OutlinedTextField, whose ~56dp min height reads as oversized next to the
// 28dp icon buttons and chips around it in this card.
@Composable
private fun TemperatureInputPill(
    value: String,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, WeatherPurple, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = WeatherText,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(WeatherPurple),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun UnitToggle(
    selected: TemperatureUnit,
    onSelect: (TemperatureUnit) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(WeatherPurpleSoft),
    ) {
        UnitChip(label = "°C", selected = selected == TemperatureUnit.CELSIUS) {
            onSelect(TemperatureUnit.CELSIUS)
        }
        UnitChip(label = "°F", selected = selected == TemperatureUnit.FAHRENHEIT) {
            onSelect(TemperatureUnit.FAHRENHEIT)
        }
    }
}

@Composable
private fun UnitChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) WeatherPurple else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else WeatherPurple,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
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
                Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.material3.Switch(
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    modifier = Modifier.scale(0.8f),
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

internal fun weatherIcon(
    condition: String,
    weatherCode: Int?,
    isRaining: Boolean,
    isSnowing: Boolean,
): DrawableResource {
    return when (normalizeCondition(condition)) {
        "clear" -> Res.drawable.sun_01_stroke_rounded
        "partlycloudy" -> Res.drawable.sun_cloud_02_stroke_rounded
        "cloudy" -> Res.drawable.cloud_stroke_rounded
        "rain" -> Res.drawable.rain_stroke_rounded
        "snow" -> Res.drawable.snow_stroke_rounded
        "thunderstorm" -> Res.drawable.cloud_lightning_stroke_rounded
        "fog" -> Res.drawable.cloud_fog_stroke_rounded
        else -> when (weatherCode) {
            0 -> Res.drawable.sun_01_stroke_rounded
            1 -> Res.drawable.sun_cloud_02_stroke_rounded
            2 -> Res.drawable.cloud_stroke_rounded
            3 -> Res.drawable.rain_stroke_rounded
            4 -> Res.drawable.snow_stroke_rounded
            5 -> Res.drawable.cloud_lightning_stroke_rounded
            6 -> Res.drawable.cloud_fog_stroke_rounded
            else -> when {
                isSnowing -> Res.drawable.snow_stroke_rounded
                isRaining -> Res.drawable.rain_stroke_rounded
                else -> Res.drawable.cloud_stroke_rounded
            }
        }
    }
}

private fun fahrenheitLabel(celsius: Double): String {
    val fahrenheit = convertTemperature(celsius, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT).roundToInt()
    return "($fahrenheit°F)"
}

private fun cleanCityName(city: String?): String? {
    return city
        ?.trim()
        ?.removePrefix("Gemeente ")
        ?.removePrefix("gemeente ")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

private fun normalizeCondition(value: String): String {
    return value.lowercase().replace(" ", "").replace("_", "")
}

