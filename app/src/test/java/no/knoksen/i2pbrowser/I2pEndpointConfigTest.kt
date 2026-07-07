package no.knoksen.i2pbrowser

import kotlinx.coroutines.test.runTest
import no.knoksen.i2pbrowser.data.AppSettingsEntity
import no.knoksen.i2pbrowser.data.EndpointConfigLoadStatus
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsClient
import no.knoksen.i2pbrowser.i2p.I2pEndpointConfig
import no.knoksen.i2pbrowser.i2p.I2pHttpClient
import no.knoksen.i2pbrowser.i2p.SamBridgeClient
import no.knoksen.i2pbrowser.i2p.SamBridgeResult
import no.knoksen.i2pbrowser.data.toEndpointConfig
import no.knoksen.i2pbrowser.data.toEndpointConfigLoadState
import no.knoksen.i2pbrowser.data.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `endpoint validation accepts localhost ipv4 lan and dns hosts`() {
        val validHosts = listOf("127.0.0.1", "localhost", "192.168.1.44", "router.lan", "i2p-router.local", "::1", "[::1]")

        validHosts.forEach { host ->
            val result = I2pEndpointConfig.manual(host, 7656, 4444, 7657).validate()
            assertTrue("Expected $host to be valid", result.isValid)
        }
    }

    @Test
    fun `endpoint validation rejects blank spaced malformed hosts and invalid ports`() {
        val invalidConfigs = listOf(
            I2pEndpointConfig.manual("", 7656, 4444, 7657),
            I2pEndpointConfig.manual("router host", 7656, 4444, 7657),
            I2pEndpointConfig(label = "Manual", host = "router\u0000host", samPort = 7656, httpProxyPort = 4444, routerConsolePort = 7657),
            I2pEndpointConfig.manual("256.1.1.1", 7656, 4444, 7657),
            I2pEndpointConfig.manual("999.1.1.1", 7656, 4444, 7657),
            I2pEndpointConfig.manual("router_lan", 7656, 4444, 7657),
            I2pEndpointConfig.manual("127.0.0.1", 0, 4444, 7657),
            I2pEndpointConfig.manual("127.0.0.1", 7656, 65536, 7657),
            I2pEndpointConfig.manual("127.0.0.1", 7656, 70000, 7657),
            I2pEndpointConfig.manual("127.0.0.1", 7656, 4444, -1)
        )

        invalidConfigs.forEach { config ->
            val result = config.validate()
            assertFalse("Expected $config to be invalid", result.isValid)
        }
    }

    @Test
    fun `endpoint persistence entity round trip preserves configured values`() {
        val config = I2pEndpointConfig.manual("10.0.2.2", 17656, 14444, 17657)

        val restored = config.toEntity().toEndpointConfig()

        assertEquals(config, restored)
    }

    @Test
    fun `endpoint validation reports whitespace without silently trimming to another host`() {
        val result = I2pEndpointConfig.manual(" 127.0.0.1 ", 7656, 4444, 7657).validate()

        assertTrue(result.isValid)
        assertEquals("127.0.0.1", result.normalizedConfig?.host)
    }

    @Test
    fun `malformed persisted endpoint falls back with explicit status`() {
        val state = AppSettingsEntity(endpointHost = "bad host").toEndpointConfigLoadState()

        assertEquals(EndpointConfigLoadStatus.PERSISTED_INVALID_FALLBACK, state.status)
        assertEquals(I2pEndpointConfig.LOCAL_ANDROID_ROUTER, state.config)
        assertTrue(state.safeMessage.orEmpty().contains("malformed"))
    }

    @Test
    fun `missing persisted endpoint uses documented default status`() {
        val missing: AppSettingsEntity? = null
        val state = missing.toEndpointConfigLoadState()

        assertEquals(EndpointConfigLoadStatus.DEFAULT_MISSING, state.status)
        assertEquals(I2pEndpointConfig.LOCAL_ANDROID_ROUTER, state.config)
    }

    @Test
    fun `invalid persisted port falls back without preserving malformed value`() {
        val state = AppSettingsEntity(endpointHost = "127.0.0.1", samPort = 0).toEndpointConfigLoadState()

        assertEquals(EndpointConfigLoadStatus.PERSISTED_INVALID_FALLBACK, state.status)
        assertEquals(7656, state.config.samPort)
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
