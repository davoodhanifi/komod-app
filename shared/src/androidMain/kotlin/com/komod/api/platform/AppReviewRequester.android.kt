package com.komod.api.platform

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

actual class AppReviewRequester {
    // The Play In-App Review API needs a foreground Activity (not just an Application
    // Context) to attach its bottom sheet to — see AndroidCurrentActivityHolder. Silently
    // no-ops with no Activity available, matching iOS's "just don't show anything" fallback.
    actual fun requestReview() {
        val activity = AndroidCurrentActivityHolder.activity ?: return
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Google's own throttling decides whether anything actually appears here —
                // this call can silently no-op just like the request above can fail.
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }

    actual fun openStoreListing() {
        val context = AndroidAppContextHolder.context
        val packageName = context.packageName
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(playStoreIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        }
    }
}
