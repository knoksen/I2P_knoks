package no.knoksen.i2pbrowser

import kotlinx.coroutines.test.runTest
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsClient
import no.knoksen.i2pbrowser.i2p.I2pEndpointConfig
import no.knoksen.i2pbrowser.i2p.I2pHttpClient
import no.knoksen.i2pbrowser.i2p.SamBridgeClient
import no.knoksen.i2pbrowser.i2p.SamBridgeResult
import org.junit.Assert.assertEquals
import org.junit.Test

class I2pEndpointConfigTest {
    @Test
    fun `endpoint config maps into diagnostics client`() = runTest {
        val config = I2pEndpointConfig.manual("192.168.1.44", 17656, 14444, 17657)
        val checkedPorts = mutableListOf<Int>()
        val client = I2pDiagnosticsClient(
            portProbe = { _, port, _ ->
                checkedPorts += port
                true
            }
        )

        client.runDiagnostics(config)

        assertEquals(listOf(17656, 14444, 17657), checkedPorts)
    }

    @Test
    fun `endpoint config maps into HTTP proxy client`() {
        val config = I2pEndpointConfig.manual("10.0.2.2", 7656, 4445, 7657)

        val client = I2pHttpClient.fromEndpointConfig(config)

        assertEquals("10.0.2.2", client.proxyHost)
        assertEquals(4445, client.proxyPort)
    }

    @Test
    fun `endpoint config maps into SAM client`() = runTest {
        val config = I2pEndpointConfig.manual("10.0.2.2", 17656, 4444, 7657)
        val client = CapturingSamBridgeClient()

        client.connect(config)

        assertEquals("10.0.2.2", client.host)
        assertEquals(17656, client.port)
    }
}

private class CapturingSamBridgeClient : SamBridgeClient() {
    var host: String? = null
    var port: Int? = null

    override suspend fun connect(host: String, port: Int, timeoutMs: Int): SamBridgeResult? {
        this.host = host
        this.port = port
        return null
    }
}
