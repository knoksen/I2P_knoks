package no.knoksen.i2pbrowser.i2p

data class I2pEndpointConfig(
    val label: String,
    val host: String,
    val samPort: Int = 7656,
    val httpProxyPort: Int = 4444,
    val routerConsolePort: Int = 7657
) {
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
    }
}
