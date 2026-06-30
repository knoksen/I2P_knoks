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
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.TimeUnit

const val MAX_BODY_PREVIEW_CHARS = 8_000

data class I2pFetchResult(
    val mode: I2pFetchMode,
    val url: String,
    val finalUrl: String? = null,
    val statusCode: Int? = null,
    val statusMessage: String? = null,
    val contentType: String? = null,
    val contentLength: Long? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val redirectLocation: String? = null,
    val elapsedMs: Long? = null,
    val fetchedAtMillis: Long = System.currentTimeMillis(),
    val title: String? = null,
    val bodyPreview: String? = null,
    val error: String? = null
)

enum class I2pFetchMode {
    REAL_PROXY_OK,
    REDIRECT,
    HTTP_ERROR,
    PROXY_UNAVAILABLE,
    HOST_LOOKUP_FAILED,
    TIMEOUT,
    UNSUPPORTED_CONTENT_TYPE,
    INVALID_URL,
    NON_I2P_URL,
    SIMULATED_PREVIEW
}

data class HttpTransportResponse(
    val statusCode: Int,
    val statusMessage: String = "",
    val finalUrl: String = "",
    val headers: Map<String, String> = emptyMap(),
    val contentType: String? = null,
    val contentLength: Long? = null,
    val redirectLocation: String? = null,
    val elapsedMs: Long = 0,
    val body: String
)

fun interface I2pHttpTransport {
    @Throws(IOException::class)
    fun execute(url: String): HttpTransportResponse
}

class OkHttpI2pTransport(
    proxyHost: String = "127.0.0.1",
    proxyPort: Int = 4444,
    timeoutMs: Long = 2_500
) : I2pHttpTransport {
    private val client = OkHttpClient.Builder()
        .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .followRedirects(false)
        .build()

    override fun execute(url: String): HttpTransportResponse {
        val request = Request.Builder().url(url).get().build()
        val startedAt = System.nanoTime()
        client.newCall(request).execute().use { response ->
            return HttpTransportResponse(
                statusCode = response.code,
                statusMessage = response.message,
                finalUrl = response.request.url.toString(),
                headers = response.headers.toMap(),
                contentType = response.body?.contentType()?.toString(),
                contentLength = response.body?.contentLength(),
                redirectLocation = response.header("Location"),
                elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt),
                body = response.peekBody((MAX_BODY_PREVIEW_CHARS * 4).toLong()).string()
            )
        }
    }
}

open class I2pHttpClient(
    val proxyHost: String = "127.0.0.1",
    val proxyPort: Int = 4444,
    private val timeoutMs: Long = 2_500,
    private val transport: I2pHttpTransport = OkHttpI2pTransport(proxyHost, proxyPort, timeoutMs)
) {
    open suspend fun fetch(url: String): I2pFetchResult {
        val normalizedUrl = normalizeUrl(url)
        if (!isValidHttpUrl(normalizedUrl)) {
            return I2pFetchResult(
                mode = I2pFetchMode.INVALID_URL,
                url = normalizedUrl,
                error = "Invalid URL. Enter a valid http:// or https:// URL."
            )
        }
        if (!isI2pUrl(normalizedUrl)) {
            return I2pFetchResult(
                mode = I2pFetchMode.NON_I2P_URL,
                url = normalizedUrl,
                finalUrl = normalizedUrl,
                bodyPreview = "Non-.i2p URL uses local preview renderer.",
                error = "Non-.i2p URL is not fetched through the I2P HTTP proxy."
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = transport.execute(normalizedUrl)
                val preview = SafePreviewSanitizer.sanitize(response.contentType, response.body)
                val mode = when {
                    response.statusCode in 300..399 -> I2pFetchMode.REDIRECT
                    response.statusCode >= 400 -> I2pFetchMode.HTTP_ERROR
                    !SafePreviewSanitizer.isPreviewSupported(response.contentType) -> I2pFetchMode.UNSUPPORTED_CONTENT_TYPE
                    else -> I2pFetchMode.REAL_PROXY_OK
                }
                I2pFetchResult(
                    mode = mode,
                    url = normalizedUrl,
                    finalUrl = response.finalUrl.ifBlank { normalizedUrl },
                    statusCode = response.statusCode,
                    statusMessage = response.statusMessage,
                    contentType = response.contentType,
                    contentLength = response.contentLength,
                    responseHeaders = response.headers,
                    redirectLocation = response.redirectLocation,
                    elapsedMs = response.elapsedMs,
                    title = extractTitle(response.body),
                    bodyPreview = preview,
                    error = when (mode) {
                        I2pFetchMode.REDIRECT -> "Redirect returned by proxy response."
                        I2pFetchMode.HTTP_ERROR -> "HTTP error returned by proxy response."
                        I2pFetchMode.UNSUPPORTED_CONTENT_TYPE -> "Preview skipped for unsupported or binary content type."
                        else -> null
                    }
                )
            } catch (e: SocketTimeoutException) {
                timeout(normalizedUrl, e)
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
        fun fromEndpointConfig(config: I2pEndpointConfig, timeoutMs: Long = 2_500): I2pHttpClient {
            return I2pHttpClient(
                proxyHost = config.host,
                proxyPort = config.httpProxyPort,
                timeoutMs = timeoutMs
            )
        }

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

        fun isValidHttpUrl(url: String): Boolean {
            val uri = runCatching { URI(url) }.getOrNull() ?: return false
            val scheme = uri.scheme?.lowercase(Locale.US) ?: return false
            return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
        }

        fun extractTitle(body: String): String? {
            val match = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(body)
            return match?.groupValues?.getOrNull(1)
                ?.let { SafePreviewSanitizer.sanitize("text/html", it, maxChars = 160) }
                ?.takeIf { it.isNotBlank() }
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

private fun timeout(url: String, exception: IOException): I2pFetchResult {
    return I2pFetchResult(
        mode = I2pFetchMode.TIMEOUT,
        url = url,
        error = exception.message ?: "I2P HTTP proxy request timed out."
    )
}

private fun hostLookupFailed(url: String, exception: IOException): I2pFetchResult {
    return I2pFetchResult(
        mode = I2pFetchMode.HOST_LOOKUP_FAILED,
        url = url,
        error = exception.message ?: "I2P host lookup failed through the local proxy."
    )
}
