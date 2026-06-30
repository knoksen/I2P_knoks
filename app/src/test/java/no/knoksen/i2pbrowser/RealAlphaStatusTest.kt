package no.knoksen.i2pbrowser

import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsClient
import no.knoksen.i2pbrowser.i2p.I2pEndpointConfig
import no.knoksen.i2pbrowser.i2p.RealAlphaReadiness
import no.knoksen.i2pbrowser.i2p.RealAlphaStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealAlphaStatusTest {
    @Test
    fun `status model maps ready diagnostics correctly`() {
        val status = RealAlphaStatus(
            endpoint = I2pEndpointConfig.LOCAL_ANDROID_ROUTER,
            diagnostics = I2pDiagnosticsClient.fromPortStates(
                samReachable = true,
                httpProxyReachable = true,
                routerConsoleReachable = true
            ),
            lastDiagnosticsAtMillis = 1_000L,
            appMode = AppExperienceMode.RELEASE_REAL,
            versionName = "0.3.1-dev",
            versionCode = 4
        )

        assertEquals(RealAlphaReadiness.READY, status.state)
        assertTrue(status.isReadyForI2pInspection)
        assertEquals("Ready for real .i2p inspection", status.summaryText)
    }

    @Test
    fun `status model maps partial and offline diagnostics correctly`() {
        val partial = RealAlphaStatus(
            endpoint = I2pEndpointConfig.LOCAL_ANDROID_ROUTER,
            diagnostics = I2pDiagnosticsClient.fromPortStates(
                samReachable = true,
                httpProxyReachable = false,
                routerConsoleReachable = true
            ),
            lastDiagnosticsAtMillis = 1_000L,
            appMode = AppExperienceMode.RELEASE_REAL,
            versionName = "0.3.1-dev",
            versionCode = 4
        )
        val offline = partial.copy(
            diagnostics = I2pDiagnosticsClient.fromPortStates(
                samReachable = false,
                httpProxyReachable = false,
                routerConsoleReachable = false
            )
        )

        assertEquals(RealAlphaReadiness.PARTIAL, partial.state)
        assertFalse(partial.isReadyForI2pInspection)
        assertEquals(RealAlphaReadiness.OFFLINE, offline.state)
        assertFalse(offline.isReadyForI2pInspection)
    }
}
