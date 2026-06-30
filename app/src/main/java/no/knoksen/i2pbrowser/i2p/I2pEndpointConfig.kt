package no.knoksen.i2pbrowser.i2p

data class I2pEndpointConfig(
    val label: String,
    val host: String,
    val samPort: Int = 7656,
    val httpProxyPort: Int = 4444,
    val routerConsolePort: Int = 7657
) {
    fun validate(): I2pEndpointValidationResult {
        val errors = buildList {
            validateHost(host)?.let { add(it) }
            validatePort("SAM port", samPort)?.let { add(it) }
            validatePort("HTTP proxy port", httpProxyPort)?.let { add(it) }
            validatePort("Router console port", routerConsolePort)?.let { add(it) }
        }
        return I2pEndpointValidationResult(errors)
    }

    companion object {
        val LOCAL_ANDROID_ROUTER = I2pEndpointConfig(
            label = "Local Android Router",
            host = "127.0.0.1"
        )

        fun desktopLanRouter(host: String): I2pEndpointConfig {
            return I2pEndpointConfig(
                label = "Desktop / LAN Router",
                host = host
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
                host = host,
                samPort = samPort,
                httpProxyPort = httpProxyPort,
                routerConsolePort = routerConsolePort
            )
        }

        private val HOSTNAME_PATTERN = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$")

        fun validateHost(host: String): String? {
            val value = host.trim()
            if (value.isBlank()) return "Router host is required."
            if (value != host || value.any { it.isWhitespace() }) return "Router host cannot contain spaces."
            if (value == "localhost") return null
            if (isValidIpv4(value)) return null
            if (looksLikeIpv4(value)) return "IPv4 address octets must be in range 0..255."
            if (HOSTNAME_PATTERN.matches(value)) return null
            return "Router host must be localhost, IPv4, or a DNS hostname."
        }

        fun validatePort(label: String, port: Int): String? {
            return if (port in 1..65535) null else "$label must be in range 1..65535."
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
    }
}

data class I2pEndpointValidationResult(
    val errors: List<String>
) {
    val isValid: Boolean = errors.isEmpty()
    val errorText: String = errors.joinToString("\n")
}
