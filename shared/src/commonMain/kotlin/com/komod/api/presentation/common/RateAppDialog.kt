package com.komod.api.presentation.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val RatePurple = Color(0xFF7C5CFC)
private val RateDarkText = Color(0xFF111827)
private val RateGrayText = Color(0xFF6B7280)

private enum class RateAppDialogStep { Ask, Feedback }

/**
 * The custom pre-prompt shown from MainScaffold when ReviewPromptManager decides it's a good
 * moment to ask — see its kdoc for why this two-step "Enjoying Komod?" gate exists instead of
 * going straight to the OS review sheet. [onRateOnStore] is only called for a happy answer;
 * an unhappy one goes to [onSendFeedback] instead, and either dismissal path (the system back
 * gesture, or an explicit "Not now") calls [onDismiss].
 */
@Composable
fun RateAppDialog(
    onRateOnStore: () -> Unit,
    onSendFeedback: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(RateAppDialogStep.Ask) }

    when (step) {
        RateAppDialogStep.Ask -> AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "Enjoying Komod?", fontWeight = FontWeight.Bold, color = RateDarkText)
            },
            text = {
                Text(
                    text = "We'd love to hear what you think — got a minute to share your experience?",
                    color = RateGrayText,
                )
            },
            confirmButton = {
                TextButton(onClick = onRateOnStore) {
                    Text(text = "Yes, I love it!", color = RatePurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { step = RateAppDialogStep.Feedback }) {
                    Text(text = "Not really", color = RateGrayText)
                }
            },
        )

        RateAppDialogStep.Feedback -> AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "Help us improve", fontWeight = FontWeight.Bold, color = RateDarkText)
            },
            text = {
                Text(
                    text = "Sorry to hear that. We'd rather hear it from you than in a review — mind telling us what we could do better?",
                    color = RateGrayText,
                )
            },
            confirmButton = {
                TextButton(onClick = onSendFeedback) {
                    Text(text = "Send Feedback", color = RatePurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = "No thanks", color = RateGrayText)
                }
            },
        )
    }
}
