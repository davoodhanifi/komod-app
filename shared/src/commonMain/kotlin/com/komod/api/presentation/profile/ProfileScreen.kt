package com.komod.api.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.komod.api.BuildKonfig
import com.komod.api.domain.model.CurrentSubscription
import com.komod.api.domain.model.SubscriptionPlan
import com.komod.api.domain.model.User
import com.komod.api.presentation.home.ShimmerBox
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C5CFC)
private val LightPurple = Color(0xFFF2EEFF)
private val Red = Color(0xFFDC2626)
private val LightRed = Color(0xFFFEECEC)
private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun ProfileScreen(
    user: User?,
    onHelpSupportClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSignOutConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    refreshKey: Int = 0,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    // Mirrors HomeScreen/WardrobeScreen's existing "refresh when returning" pattern: a
    // wardrobe or outfit-generation change elsewhere flags this screen dirty, and only
    // then does it re-fetch — not on every recomposition or plain tab switch.
    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) viewModel.loadSubscription()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Manage your account",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProfileCard(user = user, subscriptionState = uiState.subscriptionState)

        Spacer(modifier = Modifier.height(20.dp))

        SubscriptionCard(
            state = uiState.subscriptionState,
            onRetry = viewModel::loadSubscription,
        )

        Spacer(modifier = Modifier.height(20.dp))

        ActionCard(
            title = "Help & Support",
            subtitle = "Get help or contact support",
            titleColor = MaterialTheme.colorScheme.onSurface,
            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconTint = Purple,
            iconContainerColor = LightPurple,
            icon = { tint ->
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = tint,
                )
            },
            onClick = onHelpSupportClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "About Komod",
            subtitle = "App version and information",
            titleColor = MaterialTheme.colorScheme.onSurface,
            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconTint = Purple,
            iconContainerColor = LightPurple,
            trailingText = BuildKonfig.APP_VERSION,
            icon = { tint ->
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = tint,
                )
            },
            onClick = onAboutClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Logout",
            subtitle = "Sign out from your account",
            titleColor = Red,
            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconTint = Red,
            iconContainerColor = LightRed,
            icon = { tint ->
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = tint,
                )
            },
            onClick = {
                showSignOutDialog = true
            },
        )

        Spacer(modifier = Modifier.height(112.dp))
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(text = "Sign out?") },
            text = { Text(text = "Are you sure you want to sign out of Komod?") },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOutConfirmed()
                    },
                ) {
                    Text(
                        text = "Sign Out",
                        color = Red,
                    )
                }
            },
        )
    }
}

@Composable
private fun ProfileCard(user: User?, subscriptionState: SubscriptionUiState) {
    val fullName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Komod User"
    val email = user?.email?.takeIf { it.isNotBlank() } ?: "No email available"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarWithEdit(
                photoUrl = user?.photoUrl,
                initials = buildInitials(fullName, email),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SubscriptionBadge(state = subscriptionState)
            }
        }
    }
}

@Composable
private fun AvatarWithEdit(photoUrl: String?, initials: String) {
    Box(
        modifier = Modifier.size(72.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
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
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Purple,
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(24.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit profile photo",
                    tint = Purple,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun SubscriptionBadge(state: SubscriptionUiState) {
    when (state) {
        // Nothing to show yet — a shimmer placeholder rather than any plan name, so
        // nothing incorrect is ever shown even briefly.
        SubscriptionUiState.Loading -> {
            ShimmerBox(
                modifier = Modifier
                    .width(72.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(100.dp)),
            )
        }
        // The plan couldn't be loaded — omit the badge entirely (the SubscriptionCard
        // below already shows a proper error + retry) rather than guessing.
        is SubscriptionUiState.Error -> Unit
        is SubscriptionUiState.Success -> {
            // Rack is the free tier, so it keeps the old "Free" badge's light/outlined
            // look; every paid plan (door counts, Walk-in) gets the solid "Pro" look.
            val isFreeTier = state.subscription.plan == SubscriptionPlan.Rack
            val backgroundColor = if (isFreeTier) LightPurple else Purple
            val textColor = if (isFreeTier) Purple else Color.White

            Surface(
                shape = RoundedCornerShape(100.dp),
                color = backgroundColor,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .then(
                                if (isFreeTier) {
                                    Modifier.border(width = 1.dp, color = Purple, shape = CircleShape)
                                } else {
                                    Modifier
                                }
                            ),
                    )
                    Text(
                        text = state.subscription.plan.displayName(),
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    titleColor: Color,
    subtitleColor: Color,
    iconTint: Color,
    iconContainerColor: Color,
    trailingText: String? = null,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                icon(iconTint)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                trailingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun buildInitials(fullName: String, email: String): String {
    val normalized = fullName.trim()
    if (normalized.isNotEmpty() && normalized != "Komod User") {
        val parts = normalized.split(" ").filter { it.isNotBlank() }
        if (parts.isNotEmpty()) {
            val first = parts.first().first().uppercaseChar()
            val second = parts.getOrNull(1)?.first()?.uppercaseChar()
            return if (second == null) first.toString() else "$first$second"
        }
    }
    return email.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
}

// null limit means unlimited (the backend's own signal) — never "0" or any other
// stand-in value invented locally.
internal fun formatUsageText(current: Int, limit: Int?, unitSuffix: String): String =
    "$current / ${limit?.toString() ?: "Unlimited"} $unitSuffix"

// Only meaningful for a finite limit — callers must not show a progress bar at all when
// limit is null (unlimited), so this never needs to represent that case.
internal fun usageProgress(current: Int, limit: Int): Float =
    (current.toFloat() / limit.toFloat()).coerceIn(0f, 1f)

@Composable
private fun SubscriptionCard(
    state: SubscriptionUiState,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            when (state) {
                SubscriptionUiState.Loading -> SubscriptionLoadingContent()
                is SubscriptionUiState.Success -> SubscriptionSuccessContent(state.subscription)
                is SubscriptionUiState.Error -> SubscriptionErrorContent(message = state.message, onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun SubscriptionSuccessContent(subscription: CurrentSubscription) {
    Column {
        Text(
            text = subscription.plan.displayName(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(20.dp))

        UsageRow(
            label = "Wardrobe",
            current = subscription.currentWardrobeItemCount,
            limit = subscription.wardrobeItemLimit,
            unitSuffix = "items",
        )

        Spacer(modifier = Modifier.height(16.dp))

        UsageRow(
            label = "Outfit generations",
            current = subscription.todayOutfitGenerationCount,
            limit = subscription.dailyOutfitGenerationLimit,
            unitSuffix = "today",
        )
    }
}

// limit == null means unlimited (the backend's own signal — never inferred from the plan
// locally): shown as plain "current / Unlimited" text with no progress bar, since a bar
// implies a ceiling that doesn't exist here.
@Composable
private fun UsageRow(
    label: String,
    current: Int,
    limit: Int?,
    unitSuffix: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formatUsageText(current, limit, unitSuffix),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (limit != null && limit > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { usageProgress(current, limit) },
                color = Purple,
                trackColor = LightPurple,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
            )
        }
    }
}

@Composable
private fun SubscriptionLoadingContent() {
    Column {
        ShimmerBox(modifier = Modifier.width(150.dp).height(22.dp).clip(RoundedCornerShape(6.dp)))

        Spacer(modifier = Modifier.height(24.dp))

        ShimmerBox(modifier = Modifier.width(90.dp).height(14.dp).clip(RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(6.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)))

        Spacer(modifier = Modifier.height(16.dp))

        ShimmerBox(modifier = Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(6.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)))
    }
}

@Composable
private fun SubscriptionErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Column {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
        ) {
            Text("Retry")
        }
    }
}
