package no.knoksen.i2pbrowser

import kotlinx.coroutines.test.runTest
import no.knoksen.i2pbrowser.i2p.HttpTransportResponse
import no.knoksen.i2pbrowser.i2p.I2pFetchMode
import no.knoksen.i2pbrowser.i2p.I2pHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException

class I2pHttpClientTest {
    @Test
    fun `classifies i2p urls`() {
        assertTrue(I2pHttpClient.isI2pUrl("i2p-project.i2p"))
        assertTrue(I2pHttpClient.isI2pUrl("http://postman.i2p/inbox"))
    }

    @Test
    fun `non i2p url returns simulated preview without transport`() = runTest {
        val client = I2pHttpClient(
            transport = { _, _, _, _ -> error("Transport should not run for non-.i2p URLs") }
        )

        val result = client.fetch("https://example.com")

        assertEquals(I2pFetchMode.SIMULATED_PREVIEW, result.mode)
        assertNull(result.statusCode)
    }

    @Test
    fun `proxy connection failure returns proxy unavailable`() = runTest {
        val client = I2pHttpClient(
            transport = { _, _, _, _ -> throw ConnectException("Connection refused") }
        )

        val result = client.fetch("http://i2p-project.i2p")

        assertEquals(I2pFetchMode.PROXY_UNAVAILABLE, result.mode)
        assertEquals("http://i2p-project.i2p", result.url)
        assertTrue(result.error!!.contains("Connection refused"))
    }

    @Test
    fun `successful proxy response returns status title and preview`() = runTest {
        val client = I2pHttpClient(
            transport = { _, _, _, _ ->
                HttpTransportResponse(
                    statusCode = 200,
                    body = "<html><head><title>I2P Project</title></head><body>Hello from eepsite</body></html>"
                )
            }
        )

        val result = client.fetch("i2p-project.i2p")

        assertEquals(I2pFetchMode.REAL_PROXY_OK, result.mode)
        assertEquals(200, result.statusCode)
        assertEquals("I2P Project", result.title)
        assertTrue(result.bodyPreview!!.contains("Hello from eepsite"))
    }
}
