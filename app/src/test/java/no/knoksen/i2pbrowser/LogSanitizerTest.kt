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
    fun `redacts endpoint identity session and query parameter fixtures`() {
        val sanitized = LogSanitizer.sanitize(
            """
            endpointHost=127.0.0.1 samPort=7656 httpProxyPort=4444
            destination=example-test-destination.invalid
            i2pAddress=test-contact.i2p fingerprint=AA-BB-CC-DD
            sessionId=i2p_knoks_test_session
            proxy exception url=http://site.i2p/?token=secret-token
            encoded query token%3DurlEncodedSecret
            """.trimIndent()
        )

        assertFalse(sanitized.contains("127.0.0.1"))
        assertFalse(sanitized.contains("7656"))
        assertFalse(sanitized.contains("4444"))
        assertFalse(sanitized.contains("example-test-destination.invalid"))
        assertFalse(sanitized.contains("test-contact.i2p"))
        assertFalse(sanitized.contains("AA-BB-CC-DD"))
        assertFalse(sanitized.contains("i2p_knoks_test_session"))
        assertFalse(sanitized.contains("secret-token"))
        assertFalse(sanitized.contains("urlEncodedSecret"))
        assertTrue(sanitized.contains("endpointHost=[redacted]"))
        assertTrue(sanitized.contains("token%3D[redacted]"))
    }

    @Test
    fun `sanitization is repeatable and keeps ordinary diagnostic structure`() {
        val input = "DIAGNOSTIC level=INFO samReachable=true messageBody=\"sensitive body\""

        val once = LogSanitizer.sanitize(input)
        val twice = LogSanitizer.sanitize(once)

        assertEquals(once, twice)
        assertTrue(once.contains("DIAGNOSTIC"))
        assertTrue(once.contains("samReachable=true"))
        assertTrue(once.contains("messageBody=[redacted]"))
    }

    @Test
    fun `redacts diagnostic exception fixtures while keeping categories useful`() {
        val sanitized = LogSanitizer.sanitize(
            """
            diagnosticCategory=CONNECTION_TIMEOUT endpoint=[::1]:7656 HOST=192.168.1.44
            sessionId=i2p_knoks_test_session publicDestination=TEST_PUBLIC_IDENTITY_NOT_REAL
            uri=http://example.i2p/path?token=querySecret cause="nested endpointHost=10.0.2.2 destination=hidden.i2p"
            malformedResponse=${"x".repeat(700)}
            """.trimIndent()
        )

        assertTrue(sanitized.contains("diagnosticCategory=CONNECTION_TIMEOUT"))
        assertFalse(sanitized.contains("[::1]:7656"))
        assertFalse(sanitized.contains("192.168.1.44"))
        assertFalse(sanitized.contains("i2p_knoks_test_session"))
        assertFalse(sanitized.contains("TEST_PUBLIC_IDENTITY_NOT_REAL"))
        assertFalse(sanitized.contains("querySecret"))
        assertFalse(sanitized.contains("10.0.2.2"))
        assertFalse(sanitized.contains("hidden.i2p"))
        assertTrue(sanitized.contains("malformedResponse=[redacted]"))
    }

    @Test
    fun `caps long log messages`() {
        val sanitized = LogSanitizer.sanitize("x".repeat(800))

        assertTrue(sanitized.length < 530)
        assertTrue(sanitized.endsWith("... [truncated]"))
    }
}
