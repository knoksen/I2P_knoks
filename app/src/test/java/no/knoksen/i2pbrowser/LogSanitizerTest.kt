package no.knoksen.i2pbrowser

import no.knoksen.i2pbrowser.data.LogSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {

    @Test
    fun `redacts SAM private destination material`() {
        val sanitized = LogSanitizer.sanitize("DEST REPLY PUB=publicDest PRIV=privateSecretKey")

        assertTrue(sanitized.contains("PUB=publicDest"))
        assertTrue(sanitized.contains("PRIV=[redacted]"))
        assertFalse(sanitized.contains("privateSecretKey"))
    }

    @Test
    fun `redacts credentials and sensitive headers`() {
        val sanitized = LogSanitizer.sanitize(
            "Authorization=Bearer abc123 Cookie=session=secret Set-Cookie=id=secret apiKey=my-key password=hunter2"
        )

        assertFalse(sanitized.contains("abc123"))
        assertFalse(sanitized.contains("session=secret"))
        assertFalse(sanitized.contains("id=secret"))
        assertFalse(sanitized.contains("my-key"))
        assertFalse(sanitized.contains("hunter2"))
        assertEquals(5, Regex("\\[redacted]").findAll(sanitized).count())
    }

    @Test
    fun `redacts message bodies and plaintext fields`() {
        val sanitized = LogSanitizer.sanitize("body=hello world plaintext='secret note' decryptedBody=\"cached message\"")

        assertFalse(sanitized.contains("hello world"))
        assertFalse(sanitized.contains("secret note"))
        assertFalse(sanitized.contains("cached message"))
        assertTrue(sanitized.contains("body=[redacted]"))
        assertTrue(sanitized.contains("plaintext=[redacted]"))
        assertTrue(sanitized.contains("decryptedBody=[redacted]"))
    }

    @Test
    fun `redacts connect identity private material references`() {
        val sanitized = LogSanitizer.sanitize(
            "privateMaterialRef=local-only-ref:abc123 privateAppKey='app-secret' privateDestination=hidden"
        )

        assertFalse(sanitized.contains("local-only-ref:abc123"))
        assertFalse(sanitized.contains("app-secret"))
        assertFalse(sanitized.contains("hidden"))
        assertTrue(sanitized.contains("privateMaterialRef=[redacted]"))
        assertTrue(sanitized.contains("privateAppKey=[redacted]"))
        assertTrue(sanitized.contains("privateDestination=[redacted]"))
    }

    @Test
    fun `caps long log messages`() {
        val sanitized = LogSanitizer.sanitize("x".repeat(800))

        assertTrue(sanitized.length < 530)
        assertTrue(sanitized.endsWith("... [truncated]"))
    }
}
