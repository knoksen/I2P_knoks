package no.knoksen.i2pbrowser

import kotlinx.coroutines.test.runTest
import no.knoksen.i2pbrowser.i2p.HttpTransportResponse
import no.knoksen.i2pbrowser.i2p.I2pFetchMode
import no.knoksen.i2pbrowser.i2p.I2pHttpClient
import no.knoksen.i2pbrowser.i2p.SafePreviewSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class I2pHttpClientTest {
    @Test
    fun `classifies i2p urls`() {
        assertTrue(I2pHttpClient.isI2pUrl("i2p-project.i2p"))
        assertTrue(I2pHttpClient.isI2pUrl("http://postman.i2p/inbox"))
    }

    @Test
    fun `normalize url keeps uppercase scheme`() {
        assertEquals("HTTP://site.i2p", I2pHttpClient.normalizeUrl("HTTP://site.i2p"))
        assertEquals("HTTPS://site.i2p", I2pHttpClient.normalizeUrl("HTTPS://site.i2p"))
    }

    @Test
    fun `redirect target parser accepts only i2p hosts`() {
        assertTrue(I2pHttpClient.isI2pRedirectTarget("http://target.i2p"))
        assertTrue(I2pHttpClient.isI2pRedirectTarget("http://target.i2p/"))
        assertTrue(I2pHttpClient.isI2pRedirectTarget("http://target.i2p/path"))
        assertTrue(I2pHttpClient.isI2pRedirectTarget("http://target.i2p?x=1"))

        assertEquals(false, I2pHttpClient.isI2pRedirectTarget("http://evil.com/?next=target.i2p"))
        assertEquals(false, I2pHttpClient.isI2pRedirectTarget("http://noti2p.example"))
    }

    @Test
    fun `non i2p url returns simulated preview without transport`() = runTest {
        val client = I2pHttpClient(
            transport = { _ -> error("Transport should not run for non-.i2p URLs") }
        )

        val result = client.fetch("https://example.com")

        assertEquals(I2pFetchMode.NON_I2P_URL, result.mode)
        assertNull(result.statusCode)
    }

    @Test
    fun `invalid url returns invalid url without transport`() = runTest {
        val client = I2pHttpClient(
            transport = { _ -> error("Transport should not run for invalid URLs") }
        )

        val result = client.fetch("http://bad host.i2p")

        assertEquals(I2pFetchMode.INVALID_URL, result.mode)
        assertTrue(result.error!!.contains("Invalid URL"))
    }

    @Test
    fun `proxy connection failure returns proxy unavailable`() = runTest {
        val client = I2pHttpClient(
            transport = { _ -> throw ConnectException("Connection refused") }
        )

        val result = client.fetch("http://i2p-project.i2p")

        assertEquals(I2pFetchMode.PROXY_UNAVAILABLE, result.mode)
        assertEquals("http://i2p-project.i2p", result.url)
        assertTrue(result.error!!.contains("Connection refused"))
    }

    @Test
    fun `successful proxy response returns status title and preview`() = runTest {
        val client = I2pHttpClient(
            transport = { _ ->
                HttpTransportResponse(
                    statusCode = 200,
                    statusMessage = "OK",
                    finalUrl = "http://i2p-project.i2p",
                    headers = mapOf("Server" to "i2pd", "Content-Type" to "text/html"),
                    contentType = "text/html",
                    contentLength = 86,
                    elapsedMs = 42,
                    body = "<html><head><title>I2P Project</title></head><body>Hello from eepsite</body></html>"
                )
            }
        )

        val result = client.fetch("i2p-project.i2p")

        assertEquals(I2pFetchMode.REAL_PROXY_OK, result.mode)
        assertEquals(200, result.statusCode)
        assertEquals("OK", result.statusMessage)
        assertEquals("text/html", result.contentType)
        assertEquals(86L, result.contentLength)
        assertEquals("i2pd", result.responseHeaders["Server"])
        assertEquals(42L, result.elapsedMs)
        assertEquals("I2P Project", result.title)
        assertTrue(result.bodyPreview!!.contains("Hello from eepsite"))
    }

    @Test
    fun `redirect response captures location and does not become success`() = runTest {
        val client = I2pHttpClient(
            transport = { _ ->
                HttpTransportResponse(
                    statusCode = 302,
                    statusMessage = "Found",
                    headers = mapOf("Location" to "http://target.i2p/"),
                    contentType = "text/html",
                    redirectLocation = "http://target.i2p/",
                    body = "<html>Moved</html>"
                )
            }
        )

        val result = client.fetch("source.i2p")

        assertEquals(I2pFetchMode.REDIRECT, result.mode)
        assertEquals("http://target.i2p/", result.redirectLocation)
        assertEquals(302, result.statusCode)
    }

    @Test
    fun `http error response maps to http error`() = runTest {
        val client = I2pHttpClient(
            transport = { _ ->
                HttpTransportResponse(
                    statusCode = 500,
                    statusMessage = "Server Error",
                    contentType = "text/plain",
                    body = "broken"
                )
            }
        )

        val result = client.fetch("broken.i2p")

        assertEquals(I2pFetchMode.HTTP_ERROR, result.mode)
        assertEquals(500, result.statusCode)
        assertTrue(result.error!!.contains("HTTP error"))
    }

    @Test
    fun `unsupported content type skips body preview`() = runTest {
        val client = I2pHttpClient(
            transport = { _ ->
                HttpTransportResponse(
                    statusCode = 200,
                    contentType = "image/png",
                    contentLength = 10,
                    body = "raw-bytes"
                )
            }
        )

        val result = client.fetch("image.i2p")

        assertEquals(I2pFetchMode.UNSUPPORTED_CONTENT_TYPE, result.mode)
        assertTrue(result.bodyPreview!!.contains("Preview skipped"))
    }

    @Test
    fun `unknown content length remains absent instead of negative`() = runTest {
        assertNull(I2pHttpClient.normalizeContentLength(-1))
        assertNull(I2pHttpClient.normalizeContentLength(null))
        assertEquals(128L, I2pHttpClient.normalizeContentLength(128))

        val client = I2pHttpClient(
            transport = { _ ->
                HttpTransportResponse(
                    statusCode = 200,
                    contentType = "text/plain",
                    contentLength = null,
                    body = "no length header"
                )
            }
        )

        val result = client.fetch("unknown-length.i2p")

        assertEquals(I2pFetchMode.REAL_PROXY_OK, result.mode)
        assertNull(result.contentLength)
    }

    @Test
    fun `unknown host maps host lookup failed`() = runTest {
        val client = I2pHttpClient(
            transport = { _ -> throw UnknownHostException("host not found") }
        )

        val result = client.fetch("missing.i2p")

        assertEquals(I2pFetchMode.HOST_LOOKUP_FAILED, result.mode)
    }

    @Test
    fun `timeout maps timeout`() = runTest {
        val client = I2pHttpClient(
            transport = { _ -> throw SocketTimeoutException("timeout") }
        )

        val result = client.fetch("slow.i2p")

        assertEquals(I2pFetchMode.TIMEOUT, result.mode)
    }

    @Test
    fun `safe preview removes scripts styles and html tags`() {
        val preview = SafePreviewSanitizer.sanitize(
            contentType = "text/html",
            body = "<style>body{}</style><script>alert(1)</script><h1 onclick=\"x()\">Hello</h1>"
        )

        assertEquals("Hello", preview)
    }

    @Test
    fun `safe preview caps long text and preserves json`() {
        val preview = SafePreviewSanitizer.sanitize(
            contentType = "application/json",
            body = """{"name":"eepsite","items":[1,2,3]}""",
            maxChars = 12
        )

        assertEquals("""{"name":"eep""", preview)
    }

    @Test
    fun `safe preview skips binary content`() {
        val preview = SafePreviewSanitizer.sanitize(
            contentType = "application/octet-stream",
            body = "binary"
        )

        assertTrue(preview.contains("Preview skipped"))
    }

    @Test
    fun `safe preview skips unknown content type`() {
        val preview = SafePreviewSanitizer.sanitize(
            contentType = null,
            body = "unknown"
        )

        assertTrue(preview.contains("Preview skipped"))
    }
}
