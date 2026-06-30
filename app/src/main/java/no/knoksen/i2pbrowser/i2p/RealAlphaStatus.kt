package no.knoksen.i2pbrowser.i2p

import no.knoksen.i2pbrowser.AppExperienceMode

data class RealAlphaStatus(
    val endpoint: I2pEndpointConfig,
    val diagnostics: I2pDiagnosticsResult?,
    val lastDiagnosticsAtMillis: Long?,
    val appMode: AppExperienceMode,
    val versionName: String,
    val versionCode: Int
) {
    val state: RealAlphaReadiness = when (diagnostics?.summary) {
        I2pDiagnosticsSummary.READY -> RealAlphaReadiness.READY
        I2pDiagnosticsSummary.ROUTER_NOT_RUNNING -> RealAlphaReadiness.OFFLINE
        I2pDiagnosticsSummary.SAM_DISABLED,
        I2pDiagnosticsSummary.HTTP_PROXY_DISABLED,
        I2pDiagnosticsSummary.PARTIAL_READY,
        I2pDiagnosticsSummary.UNKNOWN_ERROR -> RealAlphaReadiness.PARTIAL
        null -> RealAlphaReadiness.UNCHECKED
    }

    val isReadyForI2pInspection: Boolean = state == RealAlphaReadiness.READY

    val summaryText: String = when (state) {
        RealAlphaReadiness.READY -> "Ready for real .i2p inspection"
        RealAlphaReadiness.PARTIAL -> "I2P services need attention"
        RealAlphaReadiness.OFFLINE -> "I2P router is not reachable"
        RealAlphaReadiness.UNCHECKED -> "Run diagnostics to verify readiness"
    }
}

enum class RealAlphaReadiness {
    READY,
    PARTIAL,
    OFFLINE,
    UNCHECKED
}
