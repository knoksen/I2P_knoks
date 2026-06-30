package no.knoksen.i2pbrowser.i2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException

enum class SamSessionState {
    DISCONNECTED,
    CONNECTING,
    HELLO_OK,
    DESTINATION_GENERATED,
    SESSION_CREATED,
    READY,
    FAILED,
    CLOSED
}

data class SamSessionStatus(
    val state: SamSessionState,
    val sessionId: String? = null,
    val publicDestination: String? = null,
    val privateDestinationPresent: Boolean = false,
    val error: String? = null,
    val lastFailedStep: SamProtocolStep? = null,
    val endpointHost: String? = null,
    val endpointPort: Int? = null,
    val samVersion: String? = null,
    val connectedAtMillis: Long? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class SamNameLookupResult(
    val name: String,
    val mode: SamNameLookupMode,
    val destination: String? = null,
    val error: String? = null
)

enum class SamNameLookupMode {
    FOUND,
    NOT_FOUND,
    INVALID_NAME,
    SAM_UNAVAILABLE,
    ERROR
}

open class SamSessionManager(
    private val samBridgeClient: SamBridgeClient = SamBridgeClient(),
    private val timeoutPolicy: SamTimeoutPolicy = SamTimeoutPolicy()
) {
    private val mutex = Mutex()
    private var connection: SamConnection? = null

    private val _status = MutableStateFlow(SamSessionStatus(SamSessionState.DISCONNECTED))
    val status: StateFlow<SamSessionStatus> = _status.asStateFlow()

    open suspend fun connect(config: I2pEndpointConfig): SamSessionStatus {
        return mutex.withLock {
            if (_status.value.state == SamSessionState.CONNECTING) return@withLock _status.value
            if (_status.value.state == SamSessionState.READY &&
                _status.value.endpointHost == config.host &&
                _status.value.endpointPort == config.samPort
            ) {
                return@withLock _status.value
            }

            closeConnection()
            val sessionId = SamBridgeClient.newSessionId()
            update(
                SamSessionStatus(
                    state = SamSessionState.CONNECTING,
                    sessionId = sessionId,
                    endpointHost = config.host,
                    endpointPort = config.samPort
                )
            )

            withContext(Dispatchers.IO) {
                try {
                    val socket = samBridgeClient.openControlSocket(config.host, config.samPort, timeoutPolicy.connectTimeoutMs)
                    connection = socket

                    socket.setReadTimeout(timeoutPolicy.helloReadTimeoutMs)
                    val hello = readSamStep(SamProtocolStep.HELLO, timeoutPolicy.helloReadTimeoutMs) {
                        samBridgeClient.hello(socket)
                    } ?: return@withContext _status.value
                    if (!hello.isOk) return@withContext fail("HELLO failed: ${hello.message ?: hello.result ?: "unknown"}", SamProtocolStep.HELLO)
                    update(_status.value.copy(state = SamSessionState.HELLO_OK, samVersion = hello.version))

                    socket.setReadTimeout(timeoutPolicy.destinationReadTimeoutMs)
                    val generated = readSamStep(SamProtocolStep.DEST_GENERATE, timeoutPolicy.destinationReadTimeoutMs) {
                        samBridgeClient.generateDestination(socket)
                    } ?: return@withContext _status.value
                    val privateDestination = generated.privateDestination ?: generated.destination
                    val publicDestination = generated.publicDestination ?: generated.destination
                    if (!generated.isDestinationReplyOk || privateDestination.isNullOrBlank() || publicDestination.isNullOrBlank()) {
                        return@withContext fail("DEST GENERATE failed: ${generated.message ?: generated.result ?: "missing destination"}", SamProtocolStep.DEST_GENERATE)
                    }
                    update(
                        _status.value.copy(
                            state = SamSessionState.DESTINATION_GENERATED,
                            publicDestination = publicDestination,
                            privateDestinationPresent = true
                        )
                    )

                    socket.setReadTimeout(timeoutPolicy.sessionCreateReadTimeoutMs)
                    var session = readSamStep(SamProtocolStep.SESSION_CREATE, timeoutPolicy.sessionCreateReadTimeoutMs) {
                        samBridgeClient.createStreamSession(socket, sessionId, privateDestination, "6,4")
                    } ?: return@withContext _status.value
                    var fallbackMessage: String? = null
                    if (!session.isOk && SamBridgeClient.shouldRetryLeaseSetFallback(session)) {
                        socket.setReadTimeout(timeoutPolicy.sessionCreateReadTimeoutMs)
                        val fallback = readSamStep(SamProtocolStep.SESSION_CREATE, timeoutPolicy.sessionCreateReadTimeoutMs) {
                            samBridgeClient.createStreamSession(socket, sessionId, privateDestination, "4")
                        } ?: return@withContext _status.value
                        if (fallback.isOk) {
                            session = fallback
                            fallbackMessage = "SESSION CREATE used compatibility fallback i2cp.leaseSetEncType=4."
                        }
                    }
                    if (!session.isOk) {
                        return@withContext fail("SESSION CREATE failed: ${session.message ?: session.result ?: "unknown"}", SamProtocolStep.SESSION_CREATE)
                    }

                    val now = System.currentTimeMillis()
                    update(_status.value.copy(state = SamSessionState.SESSION_CREATED, error = fallbackMessage, updatedAtMillis = now))
                    update(_status.value.copy(state = SamSessionState.READY, connectedAtMillis = now, updatedAtMillis = now))
                    _status.value
                } catch (e: CancellationException) {
                    if (_status.value.state != SamSessionState.FAILED) {
                        fail("SAM connect cancelled. Socket closed.", SamProtocolStep.CONNECT)
                    }
                    throw e
                } catch (e: SocketTimeoutException) {
                    fail("SAM CONNECT timed out after ${timeoutPolicy.connectTimeoutMs}ms.", SamProtocolStep.CONNECT)
                } catch (e: Exception) {
                    fail("SAM unavailable: ${e.message ?: e::class.java.simpleName}", SamProtocolStep.CONNECT)
                }
            }
        }
    }

    open suspend fun close(): SamSessionStatus {
        return mutex.withLock {
            closeConnection()
            update(SamSessionStatus(SamSessionState.CLOSED))
            _status.value
        }
    }

    open suspend fun nameLookup(name: String): SamNameLookupResult {
        val cleanName = name.trim()
        if (!cleanName.endsWith(".i2p")) {
            return SamNameLookupResult(cleanName, SamNameLookupMode.INVALID_NAME, error = "Only .i2p names can be looked up through SAM.")
        }
        val socket = connection ?: return SamNameLookupResult(cleanName, SamNameLookupMode.SAM_UNAVAILABLE, error = "SAM session is not connected.")

        return withContext(Dispatchers.IO) {
            try {
                socket.setReadTimeout(timeoutPolicy.nameLookupReadTimeoutMs)
                val reply = samBridgeClient.nameLookup(socket, cleanName)
                when {
                    reply.isOk && !reply.value.isNullOrBlank() -> SamNameLookupResult(cleanName, SamNameLookupMode.FOUND, destination = reply.value)
                    reply.result == "KEY_NOT_FOUND" -> SamNameLookupResult(cleanName, SamNameLookupMode.NOT_FOUND, error = reply.message)
                    reply.result == "I2P_ERROR" -> SamNameLookupResult(cleanName, SamNameLookupMode.NOT_FOUND, error = reply.message)
                    else -> SamNameLookupResult(cleanName, SamNameLookupMode.ERROR, error = reply.message ?: reply.result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SocketTimeoutException) {
                SamNameLookupResult(cleanName, SamNameLookupMode.SAM_UNAVAILABLE, error = "NAME LOOKUP timed out after ${timeoutPolicy.nameLookupReadTimeoutMs}ms.")
            } catch (e: Exception) {
                SamNameLookupResult(cleanName, SamNameLookupMode.SAM_UNAVAILABLE, error = e.message)
            }
        }
    }

    private fun fail(message: String, step: SamProtocolStep? = null): SamSessionStatus {
        closeConnection()
        update(
            _status.value.copy(
                state = SamSessionState.FAILED,
                error = message,
                lastFailedStep = step
            )
        )
        return _status.value
    }

    private fun readTimeoutMessage(step: SamProtocolStep, timeoutMs: Int): String {
        return when (step) {
            SamProtocolStep.HELLO -> "HELLO timed out after ${timeoutMs}ms."
            SamProtocolStep.DEST_GENERATE -> "DEST GENERATE timed out after ${timeoutMs}ms."
            SamProtocolStep.SESSION_CREATE -> "SESSION CREATE timed out after ${timeoutMs}ms."
            SamProtocolStep.NAME_LOOKUP -> "NAME LOOKUP timed out after ${timeoutMs}ms."
            SamProtocolStep.CONNECT -> "SAM CONNECT timed out after ${timeoutMs}ms."
            SamProtocolStep.CLOSE -> "SAM CLOSE timed out after ${timeoutMs}ms."
        }
    }

    private inline fun readSamStep(
        step: SamProtocolStep,
        timeoutMs: Int,
        block: () -> SamProtocolReply
    ): SamProtocolReply? {
        return try {
            block()
        } catch (e: CancellationException) {
            fail("SAM ${step.name} cancelled. Socket closed.", step)
            throw e
        } catch (e: SocketTimeoutException) {
            fail(readTimeoutMessage(step, timeoutMs), step)
            null
        }
    }

    private fun update(status: SamSessionStatus) {
        _status.value = status.copy(updatedAtMillis = System.currentTimeMillis())
    }

    private fun closeConnection() {
        try {
            connection?.close()
        } catch (_: Exception) {
            // Best effort cleanup.
        } finally {
            connection = null
        }
    }
}
