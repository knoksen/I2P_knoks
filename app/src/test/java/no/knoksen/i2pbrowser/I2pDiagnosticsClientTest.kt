package no.knoksen.i2pbrowser

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import no.knoksen.i2pbrowser.i2p.DiagnosticCheckResult
import no.knoksen.i2pbrowser.i2p.DiagnosticCheckStatus
import no.knoksen.i2pbrowser.i2p.DiagnosticFailureCategory
import no.knoksen.i2pbrowser.i2p.DiagnosticPolicy
import no.knoksen.i2pbrowser.i2p.DiagnosticService
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsClient
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsSummary
import no.knoksen.i2pbrowser.i2p.I2pEndpointConfig
import no.knoksen.i2pbrowser.i2p.RouterDiagnosticTransport
import no.knoksen.i2pbrowser.i2p.RouterEndpoint
import org.junit.Assert.assertThrows
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

    @Test
    fun `diagnostic transport maps timeout categories without live router`() = runTest {
        val client = I2pDiagnosticsClient(
            diagnosticTransport = ScriptedDiagnosticTransport(
                mapOf(
                    DiagnosticService.SAM_BRIDGE to DiagnosticCheckStatus.REACHABLE,
                    DiagnosticService.HTTP_PROXY to DiagnosticCheckStatus.RESPONSE_TIMEOUT,
                    DiagnosticService.ROUTER_CONSOLE to DiagnosticCheckStatus.REACHABLE
                )
            )
        )

        val result = client.runDiagnostics(I2pEndpointConfig.LOCAL_ANDROID_ROUTER)

        assertEquals(I2pDiagnosticsSummary.HTTP_PROXY_DISABLED, result.summary)
        assertEquals(DiagnosticFailureCategory.RESPONSE_TIMEOUT, result.failureCategory)
        assertTrue(result.recommendedAction.contains("bounded response"))
    }

    @Test
    fun `invalid endpoint returns typed configuration failure`() = runTest {
        val result = I2pDiagnosticsClient().runDiagnostics(
            I2pEndpointConfig.manual("bad host", 7656, 4444, 7657)
        )

        assertEquals(I2pDiagnosticsSummary.UNKNOWN_ERROR, result.summary)
        assertEquals(DiagnosticFailureCategory.INVALID_CONFIGURATION, result.failureCategory)
        assertTrue(result.recommendedAction.contains("Fix I2P endpoint"))
    }

    @Test
    fun `diagnostic policy rejects unbounded values`() {
        assertThrows(IllegalArgumentException::class.java) {
            DiagnosticPolicy(connectTimeoutMillis = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DiagnosticPolicy(readTimeoutMillis = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DiagnosticPolicy(maximumResponseBytes = 0)
        }
    }

    @Test
    fun `diagnostics cancellation propagates and closes fixture resources`() = runTest {
        val transport = BlockingDiagnosticTransport()
        val client = I2pDiagnosticsClient(diagnosticTransport = transport)

        val job = launch { client.runDiagnostics(I2pEndpointConfig.LOCAL_ANDROID_ROUTER) }
        transport.entered.await()
        job.cancelAndJoin()

        assertTrue(transport.closed.isCompleted)
    }
}

private class ScriptedDiagnosticTransport(
    private val statuses: Map<DiagnosticService, DiagnosticCheckStatus>
) : RouterDiagnosticTransport {
    override suspend fun diagnose(
        service: DiagnosticService,
        endpoint: RouterEndpoint,
        policy: DiagnosticPolicy
    ): DiagnosticCheckResult {
        val status = statuses.getValue(service)
        return DiagnosticCheckResult(
            service = service,
            endpoint = endpoint,
            status = status,
            category = when (status) {
                DiagnosticCheckStatus.CONNECTION_REFUSED -> DiagnosticFailureCategory.CONNECTION_REFUSED
                DiagnosticCheckStatus.CONNECTION_TIMEOUT -> DiagnosticFailureCategory.CONNECTION_TIMEOUT
                DiagnosticCheckStatus.RESPONSE_TIMEOUT -> DiagnosticFailureCategory.RESPONSE_TIMEOUT
                DiagnosticCheckStatus.MALFORMED_RESPONSE -> DiagnosticFailureCategory.MALFORMED_RESPONSE
                DiagnosticCheckStatus.UNSUPPORTED_RESPONSE -> DiagnosticFailureCategory.UNSUPPORTED_RESPONSE
                DiagnosticCheckStatus.TRANSPORT_CLOSED -> DiagnosticFailureCategory.TRANSPORT_CLOSED
                DiagnosticCheckStatus.UNEXPECTED_FAILURE -> DiagnosticFailureCategory.UNEXPECTED_INTERNAL_FAILURE
                DiagnosticCheckStatus.REACHABLE,
                DiagnosticCheckStatus.CANCELLED -> null
            }
        )
    }
}

private class BlockingDiagnosticTransport : RouterDiagnosticTransport {
    val entered = CompletableDeferred<Unit>()
    val closed = CompletableDeferred<Unit>()

    override suspend fun diagnose(
        service: DiagnosticService,
        endpoint: RouterEndpoint,
        policy: DiagnosticPolicy
    ): DiagnosticCheckResult {
        return try {
            entered.complete(Unit)
            awaitCancellation()
        } finally {
            closed.complete(Unit)
        }
    }
}
