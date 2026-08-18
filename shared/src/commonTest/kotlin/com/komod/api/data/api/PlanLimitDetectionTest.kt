package com.komod.api.data.api

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanLimitDetectionTest {

    @Test
    fun `a ProblemDetails body with the PlanLimitExceeded code is detected`() {
        val body = """{"type":"about:blank","title":"Payment Required","status":402,"detail":"Your Komod is full.","code":"PlanLimitExceeded"}"""

        assertTrue(isPlanLimitExceeded(body))
    }

    @Test
    fun `a code field with a different value is not detected as PlanLimitExceeded`() {
        val body = """{"type":"about:blank","title":"Not Found","status":404,"code":"NotFound"}"""

        assertFalse(isPlanLimitExceeded(body))
    }

    @Test
    fun `a ProblemDetails body with no code field at all is not detected`() {
        val body = """{"type":"about:blank","title":"Internal Server Error","status":500}"""

        assertFalse(isPlanLimitExceeded(body))
    }

    @Test
    fun `a non-JSON body does not crash detection`() {
        assertFalse(isPlanLimitExceeded("not json at all"))
    }

    @Test
    fun `an empty body does not crash detection`() {
        assertFalse(isPlanLimitExceeded(""))
    }

    @Test
    fun `an errorCode field is also recognized, since the exact backend field name is unconfirmed`() {
        val body = """{"type":"about:blank","title":"Payment Required","status":402,"errorCode":"PlanLimitExceeded"}"""

        assertTrue(isPlanLimitExceeded(body))
    }

    @Test
    fun `the literal code appearing anywhere in the body is recognized as a last resort`() {
        // Covers a shape neither "code" nor "errorCode" — e.g. the code baked into a
        // "type" URI — without waiting on another round trip to confirm the real field name.
        val body = """{"type":"https://api.komod.app/errors/PlanLimitExceeded","title":"Payment Required","status":402}"""

        assertTrue(isPlanLimitExceeded(body))
    }

    @Test
    fun `the code match is case-sensitive, not a loose contains check`() {
        // Guards against a naive "contains" implementation matching unrelated text that
        // happens to mention the phrase, e.g. inside a human-readable "detail" message.
        val body = """{"detail":"This mentions planlimitexceeded in passing.","code":"SomethingElse"}"""

        assertFalse(isPlanLimitExceeded(body))
    }
}
