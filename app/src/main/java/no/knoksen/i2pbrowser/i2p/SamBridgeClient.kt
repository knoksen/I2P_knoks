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
    val sessionReply: String
)

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

    open suspend fun connect(host: String, port: Int, timeoutMs: Int = 2_000): SamBridgeResult? {
        return withContext(Dispatchers.IO) {
            var connection: SamConnection? = null
            try {
                connection = connectionFactory.connect(host, port, timeoutMs)
                connection.writeLine("HELLO VERSION MIN=3.0 MAX=3.1")
                val helloLine = connection.readLine().orEmpty()
                if (!helloLine.isSuccessfulSamReply("HELLO REPLY")) {
                    connection.close()
                    return@withContext null
                }

                connection.writeLine("SESSION CREATE STYLE=STREAM ID=I2P_KNOKS_SESSION DESTINATION=TRANSIENT")
                val sessionLine = connection.readLine().orEmpty()
                if (!sessionLine.isSuccessfulSamReply("SESSION STATUS")) {
                    connection.close()
                    return@withContext null
                }

                val destination = sessionLine.samValue("DESTINATION") ?: run {
                    connection.close()
                    return@withContext null
                }

                activeConnection?.close()
                activeConnection = connection
                SamBridgeResult(
                    destination = destination,
                    helloReply = helloLine,
                    sessionReply = sessionLine
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

    open suspend fun nameLookup(name: String): String? {
        val connection = activeConnection ?: return null
        return withContext(Dispatchers.IO) {
            try {
                connection.writeLine("NAMELOOKUP NAME=$name")
                val response = connection.readLine().orEmpty()
                if (response.isSuccessfulSamReply("NAMELOOKUP REPLY")) {
                    response.samValue("VALUE")
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
