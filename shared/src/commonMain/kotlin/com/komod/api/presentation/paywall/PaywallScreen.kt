package com.komod.api.presentation.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.komod.api.domain.model.BillingPeriod
import com.komod.api.domain.model.PaywallPlan
import com.komod.api.domain.model.SubscriptionPlan
import com.komod.api.presentation.home.ShimmerBox
import com.komod.api.presentation.profile.displayName
import com.komod.api.presentation.profile.wardrobeLimitDescription
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C5CFC)
private val LightLavender = Color(0xFFEDE9FF)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)
private val BorderColor = Color(0xFFE5E7EB)
private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun PaywallScreen(
    onNavigateBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    viewModel: PaywallViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PaywallEffect.ShowSnackbar -> onShowSnackbar(effect.message)
                PaywallEffect.PurchaseCompleted -> {
                    onShowSnackbar("You're all set! Your plan has been updated.")
                    onNavigateBack()
                }
            }
        }
    }

    // Refreshes offerings/entitlements whenever the paywall regains focus — e.g. after the
    // user finishes (or backs out of) the system purchase sheet, or returns here having
    // changed something in Settings. Mirrors the ON_RESUME pattern used on OutfitScreen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadPlans()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "Choose Your Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkText,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = DarkText)
                    }
                },
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (val plansState = uiState.plansState) {
                    PaywallPlansState.Loading -> PaywallLoadingContent()
                    is PaywallPlansState.Error -> PaywallErrorContent(
                        message = plansState.message,
                        onRetry = viewModel::loadPlans,
                    )
                    is PaywallPlansState.Success -> PaywallPlansContent(
                        plans = plansState.plans,
                        billingPeriod = uiState.billingPeriod,
                        selectedPlan = uiState.selectedPlan,
                        activeEntitlementPlan = uiState.activeEntitlementPlan,
                        onSelectBillingPeriod = viewModel::selectBillingPeriod,
                        onSelectPlan = viewModel::selectPlan,
                    )
                }
            }

            if (uiState.plansState is PaywallPlansState.Success) {
                PaywallBottomBar(
                    isPurchasing = uiState.isPurchasing,
                    isRestoring = uiState.isRestoring,
                    isCurrentPlanSelected = uiState.selectedPlan != null && uiState.selectedPlan == uiState.activeEntitlementPlan,
                    canPurchase = uiState.selectedPlan != null,
                    onContinueClick = viewModel::purchaseSelectedPlan,
                    onRestoreClick = viewModel::restorePurchases,
                )
            }
        }
    }
}

@Composable
private fun PaywallPlansContent(
    plans: List<PaywallPlan>,
    billingPeriod: BillingPeriod,
    selectedPlan: SubscriptionPlan?,
    activeEntitlementPlan: SubscriptionPlan?,
    onSelectBillingPeriod: (BillingPeriod) -> Unit,
    onSelectPlan: (SubscriptionPlan) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Unlock more wardrobe space and outfit generations.",
            style = MaterialTheme.typography.bodyLarge,
            color = GrayText,
        )

        Spacer(modifier = Modifier.height(20.dp))

        BillingPeriodToggle(selected = billingPeriod, onSelect = onSelectBillingPeriod)

        Spacer(modifier = Modifier.height(20.dp))

        plans.forEach { plan ->
            PlanCard(
                plan = plan,
                billingPeriod = billingPeriod,
                isSelected = plan.plan == selectedPlan,
                isCurrent = plan.plan == activeEntitlementPlan,
                onClick = { onSelectPlan(plan.plan) },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BillingPeriodToggle(
    selected: BillingPeriod,
    onSelect: (BillingPeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightLavender)
            .padding(4.dp),
    ) {
        BillingPeriodOption(
            label = "Monthly",
            selected = selected == BillingPeriod.Monthly,
            onClick = { onSelect(BillingPeriod.Monthly) },
            modifier = Modifier.weight(1f),
        )
        BillingPeriodOption(
            label = "Yearly",
            selected = selected == BillingPeriod.Yearly,
            onClick = { onSelect(BillingPeriod.Yearly) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BillingPeriodOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Purple else GrayText,
        )
    }
}

@Composable
private fun PlanCard(
    plan: PaywallPlan,
    billingPeriod: BillingPeriod,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val rcPackage = if (billingPeriod == BillingPeriod.Monthly) plan.monthlyPackage else plan.yearlyPackage
    // Localized price straight from StoreKit via RevenueCat — never formatted/hard-coded here.
    val priceFormatted = rcPackage.storeProduct.price.formatted
    val periodSuffix = if (billingPeriod == BillingPeriod.Monthly) "/month" else "/year"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = if (isSelected) LightLavender else Color.White),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Purple else BorderColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = plan.plan.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkText,
                    )
                    if (isCurrent) {
                        Surface(shape = RoundedCornerShape(100.dp), color = Purple) {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plan.plan.wardrobeLimitDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayText,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = priceFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Purple else DarkText,
                )
                Text(
                    text = periodSuffix,
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText,
                )
            }
        }
    }
}

@Composable
private fun PaywallBottomBar(
    isPurchasing: Boolean,
    isRestoring: Boolean,
    isCurrentPlanSelected: Boolean,
    canPurchase: Boolean,
    onContinueClick: () -> Unit,
    onRestoreClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Button(
            onClick = onContinueClick,
            enabled = canPurchase && !isPurchasing && !isCurrentPlanSelected,
            colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            if (isPurchasing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = if (isCurrentPlanSelected) "Current Plan" else "Continue",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onRestoreClick,
            enabled = !isRestoring,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isRestoring) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Purple)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = "Restore Purchases", color = Purple, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PaywallLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)))
        Spacer(modifier = Modifier.height(20.dp))
        repeat(4) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(84.dp).clip(CardShape))
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PaywallErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = GrayText,
            textAlign = TextAlign.Center,
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
