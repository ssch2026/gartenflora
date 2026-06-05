package de.gartenflora

import de.gartenflora.BuildConfig
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Validates API key configuration.
 *
 * - In CI (keys not set): tests are skipped via [assumeTrue].
 * - In local dev (keys set): tests assert keys have plausible format.
 *
 * These tests do NOT make real network calls — that's the job of
 * manual/integration testing with real credentials.
 */
class ApiConfigTest {

    @Test
    fun `PLANTNET_API_KEY BuildConfig field exists`() {
        // Field existence is a compile-time check; if it compiles the field exists.
        assertNotNull(BuildConfig.PLANTNET_API_KEY)
    }

    @Test
    fun `PLANTID_API_KEY BuildConfig field exists`() {
        assertNotNull(BuildConfig.PLANTID_API_KEY)
    }

    @Test
    fun `GEMINI_API_KEY BuildConfig field exists`() {
        assertNotNull(BuildConfig.GEMINI_API_KEY)
    }

    @Test
    fun `PLANTNET_API_KEY has plausible format when configured`() {
        assumeTrue(
            "PLANTNET_API_KEY not configured — skipping format check",
            BuildConfig.PLANTNET_API_KEY.isNotBlank()
        )
        val key = BuildConfig.PLANTNET_API_KEY
        assertTrue("Key must be at least 8 characters", key.length >= 8)
        assertFalse("Key must not contain spaces", key.contains(" "))
    }

    @Test
    fun `PLANTID_API_KEY has plausible format when configured`() {
        assumeTrue(
            "PLANTID_API_KEY not configured — skipping format check",
            BuildConfig.PLANTID_API_KEY.isNotBlank()
        )
        val key = BuildConfig.PLANTID_API_KEY
        assertTrue("Key must be at least 8 characters", key.length >= 8)
        assertFalse("Key must not contain spaces", key.contains(" "))
    }

    @Test
    fun `app version is set`() {
        assertTrue(BuildConfig.VERSION_CODE > 0)
        assertTrue(BuildConfig.VERSION_NAME.isNotBlank())
    }

    @Test
    fun `application id is correct`() {
        assertEquals("de.gartenflora", BuildConfig.APPLICATION_ID)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun assertTrue(message: String, condition: Boolean) {
        if (!condition) fail(message)
    }

    private fun assertFalse(message: String, condition: Boolean) {
        if (condition) fail(message)
    }
}
