package no.knoksen.i2pbrowser.i2p

data class RouterEndpoint(
    val host: String,
    val port: Int
) {
    val displayValue: String
        get() = if (host.contains(":")) "[$host]:$port" else "$host:$port"
}

enum class DiagnosticService {
    SAM_BRIDGE,
    HTTP_PROXY,
    ROUTER_CONSOLE
}

data class DiagnosticPolicy(
    val connectTimeoutMillis: Long = 700,
    val readTimeoutMillis: Long = 700,
    val maximumResponseBytes: Int = 512
) {
    init {
        require(connectTimeoutMillis in 1..30_000) { "connectTimeoutMillis must be bounded." }
        require(readTimeoutMillis in 1..30_000) { "readTimeoutMillis must be bounded." }
        require(maximumResponseBytes in 1..64_000) { "maximumResponseBytes must be bounded." }
    }

    val connectTimeoutInt: Int = connectTimeoutMillis.toInt()
}

enum class DiagnosticCheckStatus {
    REACHABLE,
    CONNECTION_REFUSED,
    CONNECTION_TIMEOUT,
    RESPONSE_TIMEOUT,
    MALFORMED_RESPONSE,
    UNSUPPORTED_RESPONSE,
    TRANSPORT_CLOSED,
    CANCELLED,
    UNEXPECTED_FAILURE
}

enum class DiagnosticFailureCategory {
    INVALID_CONFIGURATION,
    CONNECTION_REFUSED,
    CONNECTION_TIMEOUT,
    RESPONSE_TIMEOUT,
    MALFORMED_RESPONSE,
    UNSUPPORTED_RESPONSE,
    TRANSPORT_CLOSED,
    REPOSITORY_UNAVAILABLE,
    UNEXPECTED_INTERNAL_FAILURE
}

data class DiagnosticCheckResult(
    val service: DiagnosticService,
    val endpoint: RouterEndpoint,
    val status: DiagnosticCheckStatus,
    val category: DiagnosticFailureCategory? = null,
    val safeDetail: String? = null
) {
    val isReachable: Boolean = status == DiagnosticCheckStatus.REACHABLE
}

sealed interface RouterDiagnosticResult {
    val checks: List<DiagnosticCheckResult>

    data class Success(
        override val checks: List<DiagnosticCheckResult>
    ) : RouterDiagnosticResult

    data class Partial(
        override val checks: List<DiagnosticCheckResult>,
        val category: DiagnosticFailureCategory? = null
    ) : RouterDiagnosticResult

    data class Failure(
        val category: DiagnosticFailureCategory,
        val safeMessage: String,
        override val checks: List<DiagnosticCheckResult> = emptyList()
    ) : RouterDiagnosticResult

    data object Cancelled : RouterDiagnosticResult {
        override val checks: List<DiagnosticCheckResult> = emptyList()
    }
}

fun interface RouterDiagnosticTransport {
    suspend fun diagnose(
        service: DiagnosticService,
        endpoint: RouterEndpoint,
        policy: DiagnosticPolicy
    ): DiagnosticCheckResult
}
