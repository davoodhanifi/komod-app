package com.komod.api.presentation.outfits

import com.komod.api.core.error.PlanLimitCategory
import com.komod.api.core.error.PlanLimitExceededException
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// detectPlanLimitError() is extracted out of OutfitViewModel.generateOutfits() specifically so
// this decision — "does this failure warrant a Paywall redirect" — is unit-testable on its own.
// OutfitViewModel itself can't be constructed in this JVM test target: WeatherLocationService/
// WeatherPreferences/AppSettingsOpener are platform `expect class`es with no fake-friendly
// interface (see OutfitViewModelPermissionRecheckTest, which tests shouldRecheckWeatherPermission
// the same way, for the same reason).
class OutfitViewModelPlanLimitTest {

    @Test
    fun `a PlanLimitExceeded error warrants a Paywall redirect`() {
        assertTrue(detectPlanLimitError(PlanLimitExceededException(PlanLimitCategory.DailyGenerationLimit)))
    }

    @Test
    fun `a generic error does not warrant a Paywall redirect`() {
        assertFalse(detectPlanLimitError(RuntimeException("something went wrong")))
    }

    @Test
    fun `a network error does not warrant a Paywall redirect`() {
        assertFalse(detectPlanLimitError(IOException("no connection")))
    }
}
