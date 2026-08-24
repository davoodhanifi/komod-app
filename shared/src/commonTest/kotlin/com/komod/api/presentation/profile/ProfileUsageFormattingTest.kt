package com.komod.api.presentation.profile

import kotlin.test.Test
import kotlin.test.assertEquals

// 3 & 4. Finite-limit rendering ("X / Y unit") and the "Unlimited" case for a null limit.
class ProfileUsageFormattingTest {

    @Test
    fun `a finite limit renders as the exact spec example`() {
        assertEquals("87 / 150 items", formatUsageText(current = 87, limit = 150, unitSuffix = "items"))
        assertEquals("12 / 30 today", formatUsageText(current = 12, limit = 30, unitSuffix = "today"))
    }

    @Test
    fun `a null limit renders as Unlimited never a fabricated number`() {
        assertEquals("87 / Unlimited items", formatUsageText(current = 87, limit = null, unitSuffix = "items"))
        assertEquals("12 / Unlimited today", formatUsageText(current = 12, limit = null, unitSuffix = "today"))
    }

    @Test
    fun `progress is the current-over-limit ratio`() {
        assertEquals(0.58f, usageProgress(current = 87, limit = 150), absoluteTolerance = 0.001f)
        assertEquals(0.4f, usageProgress(current = 12, limit = 30), absoluteTolerance = 0.001f)
    }

    @Test
    fun `progress never exceeds 1 even if usage is reported over the limit`() {
        assertEquals(1f, usageProgress(current = 160, limit = 150))
    }

    @Test
    fun `usage under 70 percent is Normal severity`() {
        assertEquals(UsageSeverity.Normal, usageSeverity(current = 0, limit = 150))
        assertEquals(UsageSeverity.Normal, usageSeverity(current = 100, limit = 150)) // ~66.7%
    }

    @Test
    fun `usage from 70 up to 90 percent is Warning severity`() {
        assertEquals(UsageSeverity.Warning, usageSeverity(current = 105, limit = 150)) // exactly 70%
        assertEquals(UsageSeverity.Warning, usageSeverity(current = 130, limit = 150)) // ~86.7%
    }

    @Test
    fun `usage at 90 percent or above is Critical severity`() {
        assertEquals(UsageSeverity.Critical, usageSeverity(current = 135, limit = 150)) // exactly 90%
        assertEquals(UsageSeverity.Critical, usageSeverity(current = 150, limit = 150)) // 100%
        assertEquals(UsageSeverity.Critical, usageSeverity(current = 160, limit = 150)) // over limit
    }
}
