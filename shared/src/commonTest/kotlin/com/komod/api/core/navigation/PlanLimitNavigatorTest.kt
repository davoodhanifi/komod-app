package com.komod.api.core.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanLimitNavigatorTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no request is pending initially`() {
        val navigator = PlanLimitNavigatorImpl()
        assertEquals(false, navigator.pendingPaywallRequest.value)
    }

    @Test
    fun `requestPaywall marks a request pending`() {
        val navigator = PlanLimitNavigatorImpl()
        navigator.requestPaywall()
        assertEquals(true, navigator.pendingPaywallRequest.value)
    }

    @Test
    fun `onPaywallShown clears the pending request`() {
        val navigator = PlanLimitNavigatorImpl()
        navigator.requestPaywall()
        navigator.onPaywallShown()
        assertEquals(false, navigator.pendingPaywallRequest.value)
    }

    // Requirement: several plan-limit errors firing in close succession (e.g. two photos in
    // the same batch, or two different screens) must coalesce into exactly one Paywall
    // navigation rather than pushing the Paywall route once per error.
    @Test
    fun `multiple requestPaywall calls before the request is shown coalesce into a single pending request`() =
        runTest(UnconfinedTestDispatcher()) {
        val navigator = PlanLimitNavigatorImpl()
        var pendingTrueCount = 0
        val job = launch {
            navigator.pendingPaywallRequest.collect { if (it) pendingTrueCount++ }
        }

        navigator.requestPaywall()
        navigator.requestPaywall()
        navigator.requestPaywall()

        assertEquals(true, navigator.pendingPaywallRequest.value)
        assertEquals(1, pendingTrueCount)
        job.cancel()
    }

    // Requirement: returning from the Paywall (without upgrading) must not leave a stale
    // request around, but a *new* limit error afterward must still be able to trigger a fresh
    // navigation — the mechanism re-arms rather than being a one-shot latch.
    @Test
    fun `a fresh request after the Paywall was shown triggers a second pending request`() =
        runTest(UnconfinedTestDispatcher()) {
        val navigator = PlanLimitNavigatorImpl()
        var pendingTrueCount = 0
        val job = launch {
            navigator.pendingPaywallRequest.collect { if (it) pendingTrueCount++ }
        }

        navigator.requestPaywall()
        navigator.onPaywallShown() // simulates the user returning from the Paywall
        assertEquals(false, navigator.pendingPaywallRequest.value)

        navigator.requestPaywall() // a later, unrelated limit error
        assertEquals(true, navigator.pendingPaywallRequest.value)
        assertEquals(2, pendingTrueCount)
        job.cancel()
    }

    @Test
    fun `onPaywallShown without a pending request is a no-op`() {
        val navigator = PlanLimitNavigatorImpl()
        navigator.onPaywallShown()
        assertEquals(false, navigator.pendingPaywallRequest.value)
    }
}
