package no.knoksen.i2pbrowser.i2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    val destination: String? = null,
    val publicDestination: String? = null,
    val error: String? = null,
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
    private val samBridgeClient: SamBridgeClient = SamBridgeClient()
) {
    private val mutex = Mutex()
    private var connection: SamConnection? = null

    private val _status = MutableStateFlow(SamSessionStatus(SamSessionState.DISCONNECTED))
    val status: StateFlow<SamSessionStatus> = _status.asStateFlow()

    open suspend fun connect(config: I2pEndpointConfig): SamSessionStatus {
        return mutex.withLock {
            if (_status.value.state == SamSessionState.CONNECTING) return@withLock _status.value

            closeConnection()
            val sessionId = SamBridgeClient.DEFAULT_SESSION_ID
            update(SamSessionStatus(SamSessionState.CONNECTING, sessionId = sessionId))

            withContext(Dispatchers.IO) {
                try {
                    val socket = samBridgeClient.openControlSocket(config.host, config.samPort, 2_000)
                    connection = socket

                    val hello = samBridgeClient.hello(socket)
                    if (!hello.isOk) return@withContext fail("HELLO failed: ${hello.message ?: hello.result ?: "unknown"}")
                    update(_status.value.copy(state = SamSessionState.HELLO_OK, samVersion = hello.version))

                    val generated = samBridgeClient.generateDestination(socket)
                    val privateDestination = generated.privateDestination ?: generated.destination
                    val publicDestination = generated.publicDestination ?: generated.destination
                    if (!generated.isOk || privateDestination.isNullOrBlank() || publicDestination.isNullOrBlank()) {
                        return@withContext fail("DEST GENERATE failed: ${generated.message ?: generated.result ?: "missing destination"}")
                    }
                    update(
                        _status.value.copy(
                            state = SamSessionState.DESTINATION_GENERATED,
                            destination = privateDestination,
                            publicDestination = publicDestination
                        )
                    )

                    var session = samBridgeClient.createStreamSession(socket, sessionId, privateDestination, "6,4")
                    var fallbackMessage: String? = null
                    if (!session.isOk) {
                        val fallback = samBridgeClient.createStreamSession(socket, sessionId, privateDestination, "4")
                        if (fallback.isOk) {
                            session = fallback
                            fallbackMessage = "SESSION CREATE used compatibility fallback i2cp.leaseSetEncType=4."
                        }
                    }
                    if (!session.isOk) {
                        return@withContext fail("SESSION CREATE failed: ${session.message ?: session.result ?: "unknown"}")
                    }

                    val now = System.currentTimeMillis()
                    update(_status.value.copy(state = SamSessionState.SESSION_CREATED, error = fallbackMessage, updatedAtMillis = now))
                    update(_status.value.copy(state = SamSessionState.READY, connectedAtMillis = now, updatedAtMillis = now))
                    _status.value
                } catch (e: Exception) {
                    fail("SAM unavailable: ${e.message ?: e::class.java.simpleName}")
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
                val reply = samBridgeClient.nameLookup(socket, cleanName)
                when {
                    reply.isOk && !reply.value.isNullOrBlank() -> SamNameLookupResult(cleanName, SamNameLookupMode.FOUND, destination = reply.value)
                    reply.result == "KEY_NOT_FOUND" -> SamNameLookupResult(cleanName, SamNameLookupMode.NOT_FOUND, error = reply.message)
                    reply.result == "I2P_ERROR" -> SamNameLookupResult(cleanName, SamNameLookupMode.NOT_FOUND, error = reply.message)
                    else -> SamNameLookupResult(cleanName, SamNameLookupMode.ERROR, error = reply.message ?: reply.result)
                }
            } catch (e: Exception) {
                SamNameLookupResult(cleanName, SamNameLookupMode.SAM_UNAVAILABLE, error = e.message)
            }
        }
    }

    private fun fail(message: String): SamSessionStatus {
        closeConnection()
        update(SamSessionStatus(SamSessionState.FAILED, error = message))
        return _status.value
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
