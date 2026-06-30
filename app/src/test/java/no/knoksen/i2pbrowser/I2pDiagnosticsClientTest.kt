package no.knoksen.i2pbrowser

import kotlinx.coroutines.test.runTest
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsClient
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class I2pDiagnosticsClientTest {
    @Test
    fun `all local services reachable maps to ready`() {
        val result = I2pDiagnosticsClient.fromPortStates(
            samReachable = true,
            httpProxyReachable = true,
            routerConsoleReachable = true
        )

        assertEquals(I2pDiagnosticsSummary.READY, result.summary)
        assertTrue(result.recommendedAction.contains("Retry"))
    }

    @Test
    fun `no local services reachable maps to router not running`() {
        val result = I2pDiagnosticsClient.fromPortStates(
            samReachable = false,
            httpProxyReachable = false,
            routerConsoleReachable = false
        )

        assertEquals(I2pDiagnosticsSummary.ROUTER_NOT_RUNNING, result.summary)
        assertTrue(result.recommendedAction.contains("Start I2P"))
    }

    @Test
    fun `router console without SAM maps to SAM disabled`() {
        val result = I2pDiagnosticsClient.fromPortStates(
            samReachable = false,
            httpProxyReachable = true,
            routerConsoleReachable = true
        )

        assertEquals(I2pDiagnosticsSummary.SAM_DISABLED, result.summary)
        assertTrue(result.recommendedAction.contains("enable SAM"))
    }

    @Test
    fun `router console only maps to SAM disabled`() {
        val result = I2pDiagnosticsClient.fromPortStates(
            samReachable = false,
            httpProxyReachable = false,
            routerConsoleReachable = true
        )

        assertEquals(I2pDiagnosticsSummary.SAM_DISABLED, result.summary)
        assertTrue(result.recommendedAction.contains("enable SAM"))
    }

    @Test
    fun `SAM without HTTP proxy maps to HTTP proxy disabled`() {
        val result = I2pDiagnosticsClient.fromPortStates(
            samReachable = true,
            httpProxyReachable = false,
            routerConsoleReachable = true
        )

        assertEquals(I2pDiagnosticsSummary.HTTP_PROXY_DISABLED, result.summary)
        assertTrue(result.recommendedAction.contains("HTTP proxy"))
    }

    @Test
    fun `diagnostics uses injected port probe`() = runTest {
        val client = I2pDiagnosticsClient(
            portProbe = { _, port, _ -> port == 7656 }
        )

        val result = client.runDiagnostics()

        assertTrue(result.samReachable)
        assertFalse(result.httpProxyReachable)
        assertFalse(result.routerConsoleReachable)
        assertEquals(I2pDiagnosticsSummary.HTTP_PROXY_DISABLED, result.summary)
    }
}
