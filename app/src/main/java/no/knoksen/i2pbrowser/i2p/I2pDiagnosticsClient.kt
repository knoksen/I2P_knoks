package no.knoksen.i2pbrowser.i2p

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.Socket
import kotlin.coroutines.coroutineContext

data class I2pDiagnosticsResult(
    val samReachable: Boolean,
    val httpProxyReachable: Boolean,
    val routerConsoleReachable: Boolean,
    val summary: I2pDiagnosticsSummary,
    val recommendedAction: String,
    val checks: List<DiagnosticCheckResult> = emptyList(),
    val failureCategory: DiagnosticFailureCategory? = null
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

class SocketRouterDiagnosticTransport : RouterDiagnosticTransport {
    override suspend fun diagnose(
        service: DiagnosticService,
        endpoint: RouterEndpoint,
        policy: DiagnosticPolicy
    ): DiagnosticCheckResult {
        return withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            val socket = Socket()
            try {
                socket.soTimeout = policy.readTimeoutMillis.toInt()
                socket.connect(InetSocketAddress(endpoint.host, endpoint.port), policy.connectTimeoutInt)
                DiagnosticCheckResult(
                    service = service,
                    endpoint = endpoint,
                    status = DiagnosticCheckStatus.REACHABLE
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: SocketTimeoutException) {
                DiagnosticCheckResult(
                    service = service,
                    endpoint = endpoint,
                    status = DiagnosticCheckStatus.CONNECTION_TIMEOUT,
                    category = DiagnosticFailureCategory.CONNECTION_TIMEOUT,
                    safeDetail = "Connection timed out."
                )
            } catch (_: ConnectException) {
                DiagnosticCheckResult(
                    service = service,
                    endpoint = endpoint,
                    status = DiagnosticCheckStatus.CONNECTION_REFUSED,
                    category = DiagnosticFailureCategory.CONNECTION_REFUSED,
                    safeDetail = "Connection refused."
                )
            } catch (_: IOException) {
                DiagnosticCheckResult(
                    service = service,
                    endpoint = endpoint,
                    status = DiagnosticCheckStatus.TRANSPORT_CLOSED,
                    category = DiagnosticFailureCategory.TRANSPORT_CLOSED,
                    safeDetail = "Transport unavailable."
                )
            } catch (_: Exception) {
                DiagnosticCheckResult(
                    service = service,
                    endpoint = endpoint,
                    status = DiagnosticCheckStatus.UNEXPECTED_FAILURE,
                    category = DiagnosticFailureCategory.UNEXPECTED_INTERNAL_FAILURE,
                    safeDetail = "Unexpected diagnostic failure."
                )
            } finally {
                try {
                    socket.close()
                } catch (_: Exception) {
                    // Best effort cleanup for diagnostic probes.
                }
            }
        }
    }
}

class PortProbeRouterDiagnosticTransport(
    private val portProbe: PortProbe
) : RouterDiagnosticTransport {
    override suspend fun diagnose(
        service: DiagnosticService,
        endpoint: RouterEndpoint,
        policy: DiagnosticPolicy
    ): DiagnosticCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                coroutineContext.ensureActive()
                val reachable = portProbe.isReachable(endpoint.host, endpoint.port, policy.connectTimeoutInt)
                if (reachable) {
                    DiagnosticCheckResult(service, endpoint, DiagnosticCheckStatus.REACHABLE)
                } else {
                    DiagnosticCheckResult(
                        service = service,
                        endpoint = endpoint,
                        status = DiagnosticCheckStatus.CONNECTION_REFUSED,
                        category = DiagnosticFailureCategory.CONNECTION_REFUSED,
                        safeDetail = "Connection refused."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: SocketTimeoutException) {
                DiagnosticCheckResult(
                    service = service,
                    endpoint = endpoint,
                    status = DiagnosticCheckStatus.CONNECTION_TIMEOUT,
                    category = DiagnosticFailureCategory.CONNECTION_TIMEOUT,
                    safeDetail = "Connection timed out."
                )
            } catch (_: Exception) {
                DiagnosticCheckResult(
                    service = service,
                    endpoint = endpoint,
                    status = DiagnosticCheckStatus.UNEXPECTED_FAILURE,
                    category = DiagnosticFailureCategory.UNEXPECTED_INTERNAL_FAILURE,
                    safeDetail = "Unexpected diagnostic failure."
                )
            }
        }
    }
}

open class I2pDiagnosticsClient(
    val host: String = "127.0.0.1",
    val samPort: Int = 7656,
    val httpProxyPort: Int = 4444,
    val routerConsolePort: Int = 7657,
    private val timeoutMs: Int = 700,
    private val portProbe: PortProbe = SocketPortProbe(),
    private val diagnosticTransport: RouterDiagnosticTransport = PortProbeRouterDiagnosticTransport(portProbe),
    private val diagnosticPolicy: DiagnosticPolicy = DiagnosticPolicy(
        connectTimeoutMillis = timeoutMs.toLong(),
        readTimeoutMillis = timeoutMs.toLong()
    )
) {
    open suspend fun runDiagnostics(): I2pDiagnosticsResult {
        return runDiagnostics(I2pEndpointConfig.manual(host, samPort, httpProxyPort, routerConsolePort))
    }

    open suspend fun runDiagnostics(config: I2pEndpointConfig): I2pDiagnosticsResult {
        val validation = config.validate()
        val normalized = validation.normalizedConfig
            ?: return invalidConfiguration(validation.errorText)
        val checks = mutableListOf<DiagnosticCheckResult>()
        for (service in DiagnosticService.entries) {
            coroutineContext.ensureActive()
            checks += diagnosticTransport.diagnose(
                service = service,
                endpoint = normalized.routerEndpoint(service),
                policy = diagnosticPolicy
            )
        }
        return fromCheckResults(checks)
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

        fun fromCheckResults(checks: List<DiagnosticCheckResult>): I2pDiagnosticsResult {
            val samReachable = checks.any { it.service == DiagnosticService.SAM_BRIDGE && it.isReachable }
            val httpProxyReachable = checks.any { it.service == DiagnosticService.HTTP_PROXY && it.isReachable }
            val routerConsoleReachable = checks.any { it.service == DiagnosticService.ROUTER_CONSOLE && it.isReachable }
            val category = checks.firstNotNullOfOrNull { it.category }
            val base = fromPortStates(samReachable, httpProxyReachable, routerConsoleReachable)
            return base.copy(
                checks = checks,
                failureCategory = category,
                recommendedAction = recommendedAction(base.summary, category)
            )
        }

        fun invalidConfiguration(errorText: String): I2pDiagnosticsResult {
            return I2pDiagnosticsResult(
                samReachable = false,
                httpProxyReachable = false,
                routerConsoleReachable = false,
                summary = I2pDiagnosticsSummary.UNKNOWN_ERROR,
                recommendedAction = "Fix I2P endpoint settings before running diagnostics.",
                failureCategory = DiagnosticFailureCategory.INVALID_CONFIGURATION,
                checks = emptyList()
            )
        }

        fun unexpectedInternalFailure(): I2pDiagnosticsResult {
            return I2pDiagnosticsResult(
                samReachable = false,
                httpProxyReachable = false,
                routerConsoleReachable = false,
                summary = I2pDiagnosticsSummary.UNKNOWN_ERROR,
                recommendedAction = "Diagnostics failed internally. Retry, then review local app logs if the problem continues.",
                failureCategory = DiagnosticFailureCategory.UNEXPECTED_INTERNAL_FAILURE,
                checks = emptyList()
            )
        }

        fun recommendedAction(summary: I2pDiagnosticsSummary): String {
            return recommendedAction(summary, null)
        }

        fun recommendedAction(
            summary: I2pDiagnosticsSummary,
            failureCategory: DiagnosticFailureCategory?
        ): String {
            if (failureCategory == DiagnosticFailureCategory.CONNECTION_TIMEOUT) {
                return "The configured endpoint did not answer before the connection timeout. Check router host and ports; a timeout does not prove the router is offline."
            }
            if (failureCategory == DiagnosticFailureCategory.RESPONSE_TIMEOUT) {
                return "The diagnostic transport connected but did not receive a bounded response in time. Retry or check the local router service."
            }
            if (failureCategory == DiagnosticFailureCategory.INVALID_CONFIGURATION) {
                return "Fix I2P endpoint settings before running diagnostics."
            }
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
