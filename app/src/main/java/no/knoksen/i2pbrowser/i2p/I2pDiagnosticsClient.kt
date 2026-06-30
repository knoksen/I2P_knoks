package no.knoksen.i2pbrowser.i2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class I2pDiagnosticsResult(
    val samReachable: Boolean,
    val httpProxyReachable: Boolean,
    val routerConsoleReachable: Boolean,
    val summary: I2pDiagnosticsSummary,
    val recommendedAction: String
)

enum class I2pDiagnosticsSummary {
    READY,
    ROUTER_NOT_RUNNING,
    SAM_DISABLED,
    HTTP_PROXY_DISABLED,
    PARTIAL_READY,
    UNKNOWN_ERROR
}

fun interface PortProbe {
    fun isReachable(host: String, port: Int, timeoutMs: Int): Boolean
}

class SocketPortProbe : PortProbe {
    override fun isReachable(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

open class I2pDiagnosticsClient(
    val host: String = "127.0.0.1",
    val samPort: Int = 7656,
    val httpProxyPort: Int = 4444,
    val routerConsolePort: Int = 7657,
    private val timeoutMs: Int = 700,
    private val portProbe: PortProbe = SocketPortProbe()
) {
    open suspend fun runDiagnostics(): I2pDiagnosticsResult {
        return withContext(Dispatchers.IO) {
            val samReachable = portProbe.isReachable(host, samPort, timeoutMs)
            val httpProxyReachable = portProbe.isReachable(host, httpProxyPort, timeoutMs)
            val routerConsoleReachable = portProbe.isReachable(host, routerConsolePort, timeoutMs)
            fromPortStates(samReachable, httpProxyReachable, routerConsoleReachable)
        }
    }

    open suspend fun runDiagnostics(config: I2pEndpointConfig): I2pDiagnosticsResult {
        val client = I2pDiagnosticsClient(
            host = config.host,
            samPort = config.samPort,
            httpProxyPort = config.httpProxyPort,
            routerConsolePort = config.routerConsolePort,
            timeoutMs = timeoutMs,
            portProbe = portProbe
        )
        return client.runDiagnostics()
    }

    companion object {
        fun fromPortStates(
            samReachable: Boolean,
            httpProxyReachable: Boolean,
            routerConsoleReachable: Boolean
        ): I2pDiagnosticsResult {
            val summary = when {
                samReachable && httpProxyReachable && routerConsoleReachable -> I2pDiagnosticsSummary.READY
                !samReachable && !httpProxyReachable && !routerConsoleReachable -> I2pDiagnosticsSummary.ROUTER_NOT_RUNNING
                !samReachable && (httpProxyReachable || routerConsoleReachable) -> I2pDiagnosticsSummary.SAM_DISABLED
                samReachable && !httpProxyReachable -> I2pDiagnosticsSummary.HTTP_PROXY_DISABLED
                samReachable || httpProxyReachable || routerConsoleReachable -> I2pDiagnosticsSummary.PARTIAL_READY
                else -> I2pDiagnosticsSummary.UNKNOWN_ERROR
            }
            return I2pDiagnosticsResult(
                samReachable = samReachable,
                httpProxyReachable = httpProxyReachable,
                routerConsoleReachable = routerConsoleReachable,
                summary = summary,
                recommendedAction = recommendedAction(summary)
            )
        }

        fun recommendedAction(summary: I2pDiagnosticsSummary): String {
            return when (summary) {
                I2pDiagnosticsSummary.READY -> "Local I2P services are reachable. Retry the .i2p request."
                I2pDiagnosticsSummary.ROUTER_NOT_RUNNING -> "Start I2P or i2pd locally, then open the router console and retry."
                I2pDiagnosticsSummary.SAM_DISABLED -> "Open router console and enable SAM bridge on the configured SAM port."
                I2pDiagnosticsSummary.HTTP_PROXY_DISABLED -> "Enable the HTTP proxy / I2PTunnel client on the configured HTTP proxy port, then retry this request."
                I2pDiagnosticsSummary.PARTIAL_READY -> "Some local I2P services are reachable. Check SAM and HTTP proxy settings in router console."
                I2pDiagnosticsSummary.UNKNOWN_ERROR -> "Check local firewall, router logs, and I2P/i2pd service status."
            }
        }
    }
}
