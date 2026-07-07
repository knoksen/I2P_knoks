package no.knoksen.i2pbrowser.i2p

import java.net.Inet6Address
import java.net.InetAddress

data class I2pEndpointConfig(
    val label: String,
    val host: String,
    val samPort: Int = 7656,
    val httpProxyPort: Int = 4444,
    val routerConsolePort: Int = 7657
) {
    fun validate(): I2pEndpointValidationResult {
        val normalizedHost = normalizeHostInput(host)
        val errors = buildList {
            validateHostError(host)?.let { add(it) }
            validatePortError("SAM port", samPort)?.let { add(it) }
            validatePortError("HTTP proxy port", httpProxyPort)?.let { add(it) }
            validatePortError("Router console port", routerConsolePort)?.let { add(it) }
        }
        val normalizedConfig = if (errors.isEmpty()) {
            copy(host = normalizedHost)
        } else {
            null
        }
        return I2pEndpointValidationResult(errors, normalizedConfig)
    }

    fun normalizedOrNull(): I2pEndpointConfig? {
        return validate().normalizedConfig
    }

    fun routerEndpoint(service: DiagnosticService): RouterEndpoint {
        val normalized = normalizedOrNull() ?: this
        val port = when (service) {
            DiagnosticService.SAM_BRIDGE -> normalized.samPort
            DiagnosticService.HTTP_PROXY -> normalized.httpProxyPort
            DiagnosticService.ROUTER_CONSOLE -> normalized.routerConsolePort
        }
        return RouterEndpoint(normalized.host, port)
    }

    companion object {
        val LOCAL_ANDROID_ROUTER = I2pEndpointConfig(
            label = "Local Android Router",
            host = "127.0.0.1"
        )

        fun desktopLanRouter(host: String): I2pEndpointConfig {
            return I2pEndpointConfig(
                label = "Desktop / LAN Router",
                host = normalizeHostInput(host)
            )
        }

        fun manual(
            host: String,
            samPort: Int,
            httpProxyPort: Int,
            routerConsolePort: Int
        ): I2pEndpointConfig {
            return I2pEndpointConfig(
                label = "Manual",
                host = normalizeHostInput(host),
                samPort = samPort,
                httpProxyPort = httpProxyPort,
                routerConsolePort = routerConsolePort
            )
        }

        private val HOSTNAME_PATTERN = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$")

        fun normalizeHostInput(host: String): String {
            val trimmed = host.trim()
            return if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed.removePrefix("[").removeSuffix("]")
            } else {
                trimmed
            }
        }

        fun validateHost(host: String): String? {
            return validateHostError(host)?.message
        }

        fun validateHostError(host: String): I2pEndpointValidationError? {
            if (host.any { it.isISOControl() }) {
                return I2pEndpointValidationError(
                    I2pEndpointValidationErrorCode.HOST_CONTROL_CHARACTER,
                    "Router host cannot contain control characters."
                )
            }
            val value = normalizeHostInput(host)
            if (value.isBlank()) {
                return I2pEndpointValidationError(
                    I2pEndpointValidationErrorCode.HOST_REQUIRED,
                    "Router host is required."
                )
            }
            if (value.any { it.isWhitespace() }) {
                return I2pEndpointValidationError(
                    I2pEndpointValidationErrorCode.HOST_WHITESPACE,
                    "Router host cannot contain spaces."
                )
            }
            if (value == "localhost") return null
            if (isValidIpv4(value)) return null
            if (looksLikeIpv4(value)) {
                return I2pEndpointValidationError(
                    I2pEndpointValidationErrorCode.HOST_INVALID_IPV4,
                    "IPv4 address octets must be in range 0..255."
                )
            }
            if (isValidIpv6(value)) return null
            if (HOSTNAME_PATTERN.matches(value)) return null
            return I2pEndpointValidationError(
                I2pEndpointValidationErrorCode.HOST_MALFORMED,
                "Router host must be localhost, IPv4, IPv6, or a DNS hostname."
            )
        }

        fun validatePort(label: String, port: Int): String? {
            return validatePortError(label, port)?.message
        }

        fun validatePortError(label: String, port: Int): I2pEndpointValidationError? {
            return if (port in 1..65535) {
                null
            } else {
                I2pEndpointValidationError(
                    I2pEndpointValidationErrorCode.PORT_OUT_OF_RANGE,
                    "$label must be in range 1..65535."
                )
            }
        }

        private fun isValidIpv4(host: String): Boolean {
            val parts = host.split(".")
            return parts.size == 4 && parts.all { part ->
                part.isNotEmpty() &&
                    part.length <= 3 &&
                    part.all { it.isDigit() } &&
                    part.toIntOrNull() in 0..255
            }
        }

        private fun looksLikeIpv4(host: String): Boolean {
            val parts = host.split(".")
            return parts.size == 4 && parts.all { part -> part.isNotEmpty() && part.all { it.isDigit() } }
        }

        private fun isValidIpv6(host: String): Boolean {
            if (!host.contains(":")) return false
            return runCatching { InetAddress.getByName(host) }
                .getOrNull() is Inet6Address
        }
    }
}

data class I2pEndpointValidationResult(
    val reasons: List<I2pEndpointValidationError>,
    val normalizedConfig: I2pEndpointConfig? = null
) {
    val isValid: Boolean = reasons.isEmpty()
    val errors: List<String> = reasons.map { it.message }
    val errorText: String = errors.joinToString("\n")
}

data class I2pEndpointValidationError(
    val code: I2pEndpointValidationErrorCode,
    val message: String
)

enum class I2pEndpointValidationErrorCode {
    HOST_REQUIRED,
    HOST_WHITESPACE,
    HOST_CONTROL_CHARACTER,
    HOST_INVALID_IPV4,
    HOST_MALFORMED,
    PORT_OUT_OF_RANGE
}
