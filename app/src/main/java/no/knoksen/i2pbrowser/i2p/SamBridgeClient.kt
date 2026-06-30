package no.knoksen.i2pbrowser.i2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

data class SamBridgeResult(
    val destination: String,
    val helloReply: String,
    val sessionReply: String,
    val samVersion: String? = null,
    val compatibilityFallbackUsed: Boolean = false
)

data class SamProtocolReply(
    val raw: String,
    val result: String?,
    val version: String? = null,
    val destination: String? = null,
    val publicDestination: String? = null,
    val privateDestination: String? = null,
    val value: String? = null,
    val message: String? = null
) {
    val isOk: Boolean = result == "OK"
}

interface SamConnection : Closeable {
    fun writeLine(line: String)
    fun readLine(): String?
}

fun interface SamConnectionFactory {
    fun connect(host: String, port: Int, timeoutMs: Int): SamConnection
}

class SocketSamConnection(private val socket: Socket) : SamConnection {
    private val writer: OutputStream = socket.getOutputStream()
    private val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

    override fun writeLine(line: String) {
        writer.write("$line\n".toByteArray())
        writer.flush()
    }

    override fun readLine(): String? = reader.readLine()

    override fun close() {
        socket.close()
    }
}

open class SamBridgeClient(
    private val connectionFactory: SamConnectionFactory = SamConnectionFactory { host, port, timeoutMs ->
        val socket = Socket()
        socket.connect(InetSocketAddress(host, port), timeoutMs)
        SocketSamConnection(socket)
    }
) : Closeable {
    private var activeConnection: SamConnection? = null

    open fun openControlSocket(host: String, port: Int, timeoutMs: Int = 2_000): SamConnection {
        return connectionFactory.connect(host, port, timeoutMs)
    }

    open fun hello(connection: SamConnection): SamProtocolReply {
        connection.writeLine("HELLO VERSION MIN=3.1 MAX=3.1")
        return parseSamReply(connection.readLine().orEmpty())
    }

    open fun generateDestination(connection: SamConnection): SamProtocolReply {
        connection.writeLine("DEST GENERATE SIGNATURE_TYPE=7")
        return parseSamReply(connection.readLine().orEmpty())
    }

    open fun createStreamSession(
        connection: SamConnection,
        sessionId: String,
        destination: String,
        leaseSetEncType: String = "6,4"
    ): SamProtocolReply {
        connection.writeLine(
            "SESSION CREATE STYLE=STREAM ID=$sessionId DESTINATION=$destination i2cp.leaseSetEncType=$leaseSetEncType"
        )
        return parseSamReply(connection.readLine().orEmpty())
    }

    open fun nameLookup(connection: SamConnection, name: String): SamProtocolReply {
        connection.writeLine("NAMING LOOKUP NAME=$name")
        return parseSamReply(connection.readLine().orEmpty())
    }

    open suspend fun connect(host: String, port: Int, timeoutMs: Int = 2_000): SamBridgeResult? {
        return withContext(Dispatchers.IO) {
            var connection: SamConnection? = null
            try {
                connection = openControlSocket(host, port, timeoutMs)
                val helloReply = hello(connection)
                if (!helloReply.isOk) {
                    connection.close()
                    return@withContext null
                }

                val generated = generateDestination(connection)
                val privateDestination = generated.privateDestination ?: generated.destination
                val publicDestination = generated.publicDestination ?: generated.destination
                if (!generated.isOk || privateDestination.isNullOrBlank() || publicDestination.isNullOrBlank()) {
                    connection.close()
                    return@withContext null
                }

                var compatibilityFallbackUsed = false
                var sessionReply = createStreamSession(connection, DEFAULT_SESSION_ID, privateDestination, "6,4")
                if (!sessionReply.isOk) {
                    val fallbackReply = createStreamSession(connection, DEFAULT_SESSION_ID, privateDestination, "4")
                    if (fallbackReply.isOk) {
                        compatibilityFallbackUsed = true
                        sessionReply = fallbackReply
                    }
                }
                if (!sessionReply.isOk) {
                    connection.close()
                    return@withContext null
                }

                activeConnection?.close()
                activeConnection = connection
                SamBridgeResult(
                    destination = publicDestination,
                    helloReply = helloReply.raw,
                    sessionReply = sessionReply.raw,
                    samVersion = helloReply.version,
                    compatibilityFallbackUsed = compatibilityFallbackUsed
                )
            } catch (_: Exception) {
                try {
                    connection?.close()
                } catch (_: Exception) {
                    // Best effort cleanup.
                }
                null
            }
        }
    }

    open suspend fun connect(config: I2pEndpointConfig, timeoutMs: Int = 2_000): SamBridgeResult? {
        return connect(config.host, config.samPort, timeoutMs)
    }

    open suspend fun nameLookup(name: String): String? {
        val connection = activeConnection ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val response = nameLookup(connection, name)
                if (response.isOk) {
                    response.value ?: response.destination
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun close() {
        try {
            activeConnection?.close()
        } catch (_: Exception) {
            // Best effort cleanup.
        } finally {
            activeConnection = null
        }
    }

    companion object {
        const val DEFAULT_SESSION_ID = "I2P_KNOKS_SESSION"

        fun parseSamReply(line: String): SamProtocolReply {
            return SamProtocolReply(
                raw = line,
                result = line.samValue("RESULT"),
                version = line.samValue("VERSION"),
                destination = line.samValue("DESTINATION"),
                publicDestination = line.samValue("PUB"),
                privateDestination = line.samValue("PRIV"),
                value = line.samValue("VALUE"),
                message = line.samMessageValue()
            )
        }
    }
}

internal fun String.isSuccessfulSamReply(prefix: String): Boolean {
    return startsWith(prefix) && samValue("RESULT") == "OK"
}

internal fun String.samValue(key: String): String? {
    val marker = "$key="
    val start = indexOf(marker)
    if (start == -1) return null
    val valueStart = start + marker.length
    val valueEnd = indexOf(' ', valueStart).let { if (it == -1) length else it }
    return substring(valueStart, valueEnd).trim().takeIf { it.isNotEmpty() }
}

internal fun String.samMessageValue(): String? {
    val marker = "MESSAGE="
    val start = indexOf(marker)
    if (start == -1) return null
    return substring(start + marker.length).trim().takeIf { it.isNotEmpty() }
}
