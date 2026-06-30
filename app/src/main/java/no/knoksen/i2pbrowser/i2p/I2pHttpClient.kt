package no.knoksen.i2pbrowser.i2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.TimeUnit

data class I2pFetchResult(
    val mode: I2pFetchMode,
    val url: String,
    val statusCode: Int? = null,
    val title: String? = null,
    val bodyPreview: String? = null,
    val error: String? = null
)

enum class I2pFetchMode {
    REAL_PROXY_OK,
    PROXY_UNAVAILABLE,
    HOST_LOOKUP_FAILED,
    SIMULATED_PREVIEW
}

data class HttpTransportResponse(
    val statusCode: Int,
    val body: String
)

fun interface I2pHttpTransport {
    @Throws(IOException::class)
    fun execute(url: String, proxyHost: String, proxyPort: Int, timeoutMs: Long): HttpTransportResponse
}

class OkHttpI2pTransport : I2pHttpTransport {
    override fun execute(url: String, proxyHost: String, proxyPort: Int, timeoutMs: Long): HttpTransportResponse {
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
        val client = OkHttpClient.Builder()
            .proxy(proxy)
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .followRedirects(false)
            .build()
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            return HttpTransportResponse(
                statusCode = response.code,
                body = response.body?.string().orEmpty()
            )
        }
    }
}

open class I2pHttpClient(
    private val transport: I2pHttpTransport = OkHttpI2pTransport(),
    private val proxyHost: String = "127.0.0.1",
    private val proxyPort: Int = 4444,
    private val timeoutMs: Long = 2_500
) {
    open suspend fun fetch(url: String): I2pFetchResult {
        val normalizedUrl = normalizeUrl(url)
        if (!isI2pUrl(normalizedUrl)) {
            return I2pFetchResult(
                mode = I2pFetchMode.SIMULATED_PREVIEW,
                url = normalizedUrl,
                error = "Non-.i2p URL uses local preview renderer."
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = transport.execute(normalizedUrl, proxyHost, proxyPort, timeoutMs)
                I2pFetchResult(
                    mode = I2pFetchMode.REAL_PROXY_OK,
                    url = normalizedUrl,
                    statusCode = response.statusCode,
                    title = extractTitle(response.body),
                    bodyPreview = response.body.stripHtml().take(700)
                )
            } catch (e: ConnectException) {
                proxyUnavailable(normalizedUrl, e)
            } catch (e: UnknownHostException) {
                hostLookupFailed(normalizedUrl, e)
            } catch (e: IOException) {
                if (e.message.orEmpty().contains("proxy", ignoreCase = true) ||
                    e.message.orEmpty().contains("connect", ignoreCase = true)
                ) {
                    proxyUnavailable(normalizedUrl, e)
                } else {
                    hostLookupFailed(normalizedUrl, e)
                }
            }
        }
    }

    companion object {
        fun normalizeUrl(url: String): String {
            val trimmed = url.trim()
            return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
        }

        fun isI2pUrl(url: String): Boolean {
            val normalizedUrl = normalizeUrl(url)
            val host = runCatching { URI(normalizedUrl).host }.getOrNull()
                ?: normalizedUrl.substringAfter("://").substringBefore("/").substringBefore(":")
            return host.lowercase(Locale.US).endsWith(".i2p")
        }

        fun extractTitle(body: String): String? {
            val match = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(body)
            return match?.groupValues?.getOrNull(1)?.stripHtml()?.trim()?.takeIf { it.isNotBlank() }
        }
    }
}

private fun proxyUnavailable(url: String, exception: IOException): I2pFetchResult {
    return I2pFetchResult(
        mode = I2pFetchMode.PROXY_UNAVAILABLE,
        url = url,
        error = exception.message ?: "Local I2P HTTP proxy is unavailable."
    )
}

private fun hostLookupFailed(url: String, exception: IOException): I2pFetchResult {
    return I2pFetchResult(
        mode = I2pFetchMode.HOST_LOOKUP_FAILED,
        url = url,
        error = exception.message ?: "I2P host lookup failed through the local proxy."
    )
}

private fun String.stripHtml(): String {
    return replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
}
