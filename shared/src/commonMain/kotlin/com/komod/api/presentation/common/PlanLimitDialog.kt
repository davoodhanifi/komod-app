package com.komod.api.presentation.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.komod.api.core.error.PlanLimitCategory
import com.komod.api.core.error.PlanLimitMessages

private val PlanLimitPurple = Color(0xFF7C5CFC)
private val PlanLimitDarkText = Color(0xFF111827)
private val PlanLimitGrayText = Color(0xFF6B7280)

/**
 * The shared popup for every PlanLimitExceeded situation (wardrobe full, pending upload/
 * analysis capacity full, daily outfit generation limit reached) — same title/message/
 * "Got it" structure everywhere; only the copy varies, driven by [category].
 */
@Composable
fun PlanLimitDialog(
    category: PlanLimitCategory,
    onDismiss: () -> Unit,
) {
    val content = PlanLimitMessages.forCategory(category)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = content.title, fontWeight = FontWeight.Bold, color = PlanLimitDarkText)
        },
        text = {
            Text(text = content.message, color = PlanLimitGrayText)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Got it", color = PlanLimitPurple)
            }
        },
    )
}
