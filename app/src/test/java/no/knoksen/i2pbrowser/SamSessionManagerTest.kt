package no.knoksen.i2pbrowser

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import no.knoksen.i2pbrowser.i2p.I2pEndpointConfig
import no.knoksen.i2pbrowser.i2p.SamBridgeClient
import no.knoksen.i2pbrowser.i2p.SamConnection
import no.knoksen.i2pbrowser.i2p.SamNameLookupMode
import no.knoksen.i2pbrowser.i2p.SamProtocolStep
import no.knoksen.i2pbrowser.i2p.SamProtocolReply
import no.knoksen.i2pbrowser.i2p.SamSessionManager
import no.knoksen.i2pbrowser.i2p.SamSessionState
import no.knoksen.i2pbrowser.i2p.SamTimeoutPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SamSessionManagerTest {
    private val config = I2pEndpointConfig.LOCAL_ANDROID_ROUTER

    @Test
    fun `successful connect transitions to ready`() = runTest {
        val manager = SamSessionManager(ScriptedSamBridgeClient())

        val status = manager.connect(config)

        assertEquals(SamSessionState.READY, status.state)
        assertEquals("publicDest", status.publicDestination)
        assertEquals(true, status.privateDestinationPresent)
        assertEquals("3.1", status.samVersion)
        assertEquals(false, status.toString().contains("privateSecretKey"))
    }

    @Test
    fun `connect applies step specific read timeouts`() = runTest {
        val client = ScriptedSamBridgeClient()
        val manager = SamSessionManager(
            client,
            SamTimeoutPolicy(
                connectTimeoutMs = 101,
                helloReadTimeoutMs = 202,
                destinationReadTimeoutMs = 303,
                sessionCreateReadTimeoutMs = 404
            )
        )

        manager.connect(config)

        assertEquals(101, client.openTimeouts.single())
        assertEquals(listOf(202, 303, 404), client.connection.readTimeouts)
    }

    @Test
    fun `failure during hello transitions to failed and closes socket`() = runTest {
        val client = ScriptedSamBridgeClient(helloResult = SamProtocolReply("HELLO REPLY RESULT=NOVERSION MESSAGE=No version", "NOVERSION", message = "No version"))
        val manager = SamSessionManager(client)

        val status = manager.connect(config)

        assertEquals(SamSessionState.FAILED, status.state)
        assertTrue(status.error!!.contains("HELLO failed"))
        assertTrue(client.connection.closed)
    }

    @Test
    fun `hello timeout transitions to failed and closes socket`() = runTest {
        val client = TimeoutSamBridgeClient(SamProtocolStep.HELLO)
        val manager = SamSessionManager(client, SamTimeoutPolicy(helloReadTimeoutMs = 123))

        val status = manager.connect(config)

        assertEquals(SamSessionState.FAILED, status.state)
        assertEquals(SamProtocolStep.HELLO, status.lastFailedStep)
        assertTrue(status.error!!.contains("HELLO timed out after 123ms"))
        assertTrue(client.connection.closed)
    }

    @Test
    fun `failure during destination generation transitions to failed and closes socket`() = runTest {
        val client = ScriptedSamBridgeClient(destinationResult = SamProtocolReply("DEST REPLY RESULT=I2P_ERROR MESSAGE=No dest", "I2P_ERROR", message = "No dest"))
        val manager = SamSessionManager(client)

        val status = manager.connect(config)

        assertEquals(SamSessionState.FAILED, status.state)
        assertTrue(status.error!!.contains("DEST GENERATE failed"))
        assertTrue(client.connection.closed)
    }

    @Test
    fun `destination timeout transitions to failed and closes socket`() = runTest {
        val client = TimeoutSamBridgeClient(SamProtocolStep.DEST_GENERATE)
        val manager = SamSessionManager(client, SamTimeoutPolicy(destinationReadTimeoutMs = 456))

        val status = manager.connect(config)

        assertEquals(SamSessionState.FAILED, status.state)
        assertEquals(SamProtocolStep.DEST_GENERATE, status.lastFailedStep)
        assertTrue(status.error!!.contains("DEST GENERATE timed out after 456ms"))
        assertTrue(client.connection.closed)
    }

    @Test
    fun `session create retries leaseSet fallback`() = runTest {
        val client = ScriptedSamBridgeClient(
            sessionResults = ArrayDeque(
                listOf(
                    SamProtocolReply("SESSION STATUS RESULT=I2P_ERROR MESSAGE=Unsupported leaseSet encryption", "I2P_ERROR", message = "Unsupported leaseSet encryption"),
                    SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
                )
            )
        )
        val manager = SamSessionManager(client)

        val status = manager.connect(config)

        assertEquals(SamSessionState.READY, status.state)
        assertEquals("SESSION CREATE used compatibility fallback i2cp.leaseSetEncType=4.", status.error)
        assertEquals(listOf("6,4", "4"), client.leaseSetAttempts)
    }

    @Test
    fun `session create failure transitions to failed and closes socket`() = runTest {
        val client = ScriptedSamBridgeClient(
            sessionResults = ArrayDeque(
                listOf(
                    SamProtocolReply("SESSION STATUS RESULT=DUPLICATED_ID MESSAGE=Duplicate ID", "DUPLICATED_ID", message = "Duplicate ID"),
                    SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
                )
            )
        )
        val manager = SamSessionManager(client)

        val status = manager.connect(config)

        assertEquals(SamSessionState.FAILED, status.state)
        assertTrue(status.error!!.contains("SESSION CREATE failed"))
        assertTrue(client.connection.closed)
        assertEquals(listOf("6,4"), client.leaseSetAttempts)
    }

    @Test
    fun `session create timeout transitions to failed and closes socket`() = runTest {
        val client = TimeoutSamBridgeClient(SamProtocolStep.SESSION_CREATE)
        val manager = SamSessionManager(client, SamTimeoutPolicy(sessionCreateReadTimeoutMs = 789))

        val status = manager.connect(config)

        assertEquals(SamSessionState.FAILED, status.state)
        assertEquals(SamProtocolStep.SESSION_CREATE, status.lastFailedStep)
        assertTrue(status.error!!.contains("SESSION CREATE timed out after 789ms"))
        assertTrue(client.connection.closed)
    }

    @Test
    fun `cancellation during hello closes socket and leaves failed state`() = runTest {
        val client = CancellingSamBridgeClient(SamProtocolStep.HELLO)
        val manager = SamSessionManager(client)
        var cancelled = false

        try {
            manager.connect(config)
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
        assertTrue(client.connection.closed)
        assertEquals(SamSessionState.FAILED, manager.status.value.state)
        assertEquals(SamProtocolStep.HELLO, manager.status.value.lastFailedStep)
        assertTrue(manager.status.value.error!!.contains("cancelled"))
    }

    @Test
    fun `cancellation during session create closes socket and leaves failed state`() = runTest {
        val client = CancellingSamBridgeClient(SamProtocolStep.SESSION_CREATE)
        val manager = SamSessionManager(client)
        var cancelled = false

        try {
            manager.connect(config)
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
        assertTrue(client.connection.closed)
        assertEquals(SamSessionState.FAILED, manager.status.value.state)
        assertEquals(SamProtocolStep.SESSION_CREATE, manager.status.value.lastFailedStep)
    }

    @Test
    fun `invalid key does not trigger leaseSet fallback`() = runTest {
        val client = ScriptedSamBridgeClient(
            sessionResults = ArrayDeque(
                listOf(
                    SamProtocolReply("SESSION STATUS RESULT=INVALID_KEY MESSAGE=Invalid private key", "INVALID_KEY", message = "Invalid private key"),
                    SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
                )
            )
        )
        val manager = SamSessionManager(client)

        val status = manager.connect(config)

        assertEquals(SamSessionState.FAILED, status.state)
        assertEquals(listOf("6,4"), client.leaseSetAttempts)
    }

    @Test
    fun `generic I2P error without leaseSet hint does not trigger fallback`() = runTest {
        val client = ScriptedSamBridgeClient(
            sessionResults = ArrayDeque(
                listOf(
                    SamProtocolReply("SESSION STATUS RESULT=I2P_ERROR MESSAGE=Router busy", "I2P_ERROR", message = "Router busy"),
                    SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
                )
            )
        )
        val manager = SamSessionManager(client)

        val status = manager.connect(config)

        assertEquals(SamSessionState.FAILED, status.state)
        assertEquals(listOf("6,4"), client.leaseSetAttempts)
    }

    @Test
    fun `ready same endpoint returns existing session`() = runTest {
        val firstConnection = ManagerFakeSamConnection()
        val secondConnection = ManagerFakeSamConnection()
        val client = ReconnectingSamBridgeClient(ArrayDeque(listOf(firstConnection, secondConnection)))
        val manager = SamSessionManager(client)

        val first = manager.connect(config)
        val second = manager.connect(config)

        assertEquals(SamSessionState.READY, second.state)
        assertEquals(first.sessionId, second.sessionId)
        assertEquals(false, firstConnection.closed)
        assertEquals(1, client.openCalls)
    }

    @Test
    fun `endpoint change closes previous socket before creating a new session`() = runTest {
        val firstConnection = ManagerFakeSamConnection()
        val secondConnection = ManagerFakeSamConnection()
        val client = ReconnectingSamBridgeClient(ArrayDeque(listOf(firstConnection, secondConnection)))
        val manager = SamSessionManager(client)
        val secondConfig = I2pEndpointConfig.manual("127.0.0.2", 7656, 4444, 7657)

        manager.connect(config)
        val status = manager.connect(secondConfig)

        assertEquals(SamSessionState.READY, status.state)
        assertTrue(firstConnection.closed)
        assertEquals(2, client.openCalls)
        assertEquals(2, client.sessionIds.distinct().size)
        assertEquals(false, client.sessionIds.any { sessionId -> sessionId.any { it.isWhitespace() } })
    }

    @Test
    fun `duplicate rapid connect does not open a second socket`() = runTest {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val client = BlockingSamBridgeClient(started, release)
        val manager = SamSessionManager(client)

        val first = async(Dispatchers.Default) { manager.connect(config) }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        val second = async(Dispatchers.Default) { manager.connect(config) }
        release.countDown()

        assertEquals(SamSessionState.READY, first.await().state)
        assertEquals(SamSessionState.READY, second.await().state)
        assertEquals(1, client.openCalls)
    }

    @Test
    fun `close transitions to closed`() = runTest {
        val client = ScriptedSamBridgeClient()
        val manager = SamSessionManager(client)

        manager.connect(config)
        val status = manager.close()

        assertEquals(SamSessionState.CLOSED, status.state)
        assertTrue(client.connection.closed)
    }

    @Test
    fun `valid i2p lookup found`() = runTest {
        val client = ScriptedSamBridgeClient(lookupResult = SamProtocolReply("NAMING REPLY RESULT=OK VALUE=dest", "OK", value = "dest"))
        val manager = SamSessionManager(client)
        manager.connect(config)

        val result = manager.nameLookup("site.i2p")

        assertEquals(SamNameLookupMode.FOUND, result.mode)
        assertEquals("dest", result.destination)
        assertEquals(listOf(5_000, 10_000, 120_000, 15_000), client.connection.readTimeouts)
    }

    @Test
    fun `valid i2p lookup not found`() = runTest {
        val client = ScriptedSamBridgeClient(lookupResult = SamProtocolReply("NAMING REPLY RESULT=KEY_NOT_FOUND MESSAGE=missing", "KEY_NOT_FOUND", message = "missing"))
        val manager = SamSessionManager(client)
        manager.connect(config)

        val result = manager.nameLookup("missing.i2p")

        assertEquals(SamNameLookupMode.NOT_FOUND, result.mode)
    }

    @Test
    fun `invalid non-i2p lookup rejected before SAM`() = runTest {
        val client = ScriptedSamBridgeClient()
        val manager = SamSessionManager(client)

        val result = manager.nameLookup("example.com")

        assertEquals(SamNameLookupMode.INVALID_NAME, result.mode)
        assertEquals(0, client.lookupCalls)
    }

    @Test
    fun `SAM unavailable maps lookup correctly`() = runTest {
        val manager = SamSessionManager(ScriptedSamBridgeClient())

        val result = manager.nameLookup("site.i2p")

        assertEquals(SamNameLookupMode.SAM_UNAVAILABLE, result.mode)
    }

    @Test
    fun `lookup timeout maps to SAM unavailable with clear message`() = runTest {
        val client = TimeoutSamBridgeClient(SamProtocolStep.NAME_LOOKUP)
        val manager = SamSessionManager(client, SamTimeoutPolicy(nameLookupReadTimeoutMs = 321))
        manager.connect(config)

        val result = manager.nameLookup("site.i2p")

        assertEquals(SamNameLookupMode.SAM_UNAVAILABLE, result.mode)
        assertTrue(result.error!!.contains("NAME LOOKUP timed out after 321ms"))
    }

    @Test
    fun `name lookup cancellation rethrows without mutating session state`() = runTest {
        val client = CancellingLookupSamBridgeClient()
        val manager = SamSessionManager(client)
        manager.connect(config)
        val before = manager.status.value
        var cancelled = false

        try {
            manager.nameLookup("site.i2p")
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
        assertEquals(before, manager.status.value)
    }
}

private class ManagerFakeSamConnection(vararg replies: String) : SamConnection {
    private val remainingReplies = ArrayDeque(replies.toList())
    val writes = mutableListOf<String>()
    val readTimeouts = mutableListOf<Int>()
    var closed = false

    override fun writeLine(line: String) {
        writes += line
    }

    override fun readLine(): String? = remainingReplies.removeFirstOrNull()

    override fun setReadTimeout(timeoutMs: Int) {
        readTimeouts += timeoutMs
    }

    override fun close() {
        closed = true
    }
}

private class ScriptedSamBridgeClient(
    val connection: ManagerFakeSamConnection = ManagerFakeSamConnection(),
    private val helloResult: SamProtocolReply = SamProtocolReply("HELLO REPLY RESULT=OK VERSION=3.1", "OK", version = "3.1"),
    private val destinationResult: SamProtocolReply = SamProtocolReply("DEST REPLY PUB=publicDest PRIV=privateSecretKey", null, publicDestination = "publicDest", privateDestination = "privateSecretKey"),
    private val sessionResults: ArrayDeque<SamProtocolReply> = ArrayDeque(listOf(SamProtocolReply("SESSION STATUS RESULT=OK", "OK"))),
    private val lookupResult: SamProtocolReply = SamProtocolReply("NAMING REPLY RESULT=OK VALUE=dest", "OK", value = "dest")
) : SamBridgeClient() {
    val leaseSetAttempts = mutableListOf<String>()
    val sessionIds = mutableListOf<String>()
    val openTimeouts = mutableListOf<Int>()
    var lookupCalls = 0

    override fun openControlSocket(host: String, port: Int, timeoutMs: Int): SamConnection {
        openTimeouts += timeoutMs
        return connection
    }
    override fun hello(connection: SamConnection): SamProtocolReply = helloResult
    override fun generateDestination(connection: SamConnection): SamProtocolReply = destinationResult
    override fun createStreamSession(connection: SamConnection, sessionId: String, destination: String, leaseSetEncType: String): SamProtocolReply {
        sessionIds += sessionId
        leaseSetAttempts += leaseSetEncType
        return sessionResults.removeFirst()
    }
    override fun nameLookup(connection: SamConnection, name: String): SamProtocolReply {
        lookupCalls += 1
        return lookupResult
    }
}

private class TimeoutSamBridgeClient(
    private val timeoutStep: SamProtocolStep,
    val connection: ManagerFakeSamConnection = ManagerFakeSamConnection()
) : SamBridgeClient() {
    override fun openControlSocket(host: String, port: Int, timeoutMs: Int): SamConnection = connection

    override fun hello(connection: SamConnection): SamProtocolReply {
        if (timeoutStep == SamProtocolStep.HELLO) throw SocketTimeoutException("hello hung")
        return SamProtocolReply("HELLO REPLY RESULT=OK VERSION=3.1", "OK", version = "3.1")
    }

    override fun generateDestination(connection: SamConnection): SamProtocolReply {
        if (timeoutStep == SamProtocolStep.DEST_GENERATE) throw SocketTimeoutException("destination hung")
        return SamProtocolReply("DEST REPLY PUB=publicDest PRIV=privateSecretKey", null, publicDestination = "publicDest", privateDestination = "privateSecretKey")
    }

    override fun createStreamSession(connection: SamConnection, sessionId: String, destination: String, leaseSetEncType: String): SamProtocolReply {
        if (timeoutStep == SamProtocolStep.SESSION_CREATE) throw SocketTimeoutException("session create hung")
        return SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
    }

    override fun nameLookup(connection: SamConnection, name: String): SamProtocolReply {
        if (timeoutStep == SamProtocolStep.NAME_LOOKUP) throw SocketTimeoutException("lookup hung")
        return SamProtocolReply("NAMING REPLY RESULT=OK VALUE=dest", "OK", value = "dest")
    }
}

private class CancellingSamBridgeClient(
    private val cancellationStep: SamProtocolStep,
    val connection: ManagerFakeSamConnection = ManagerFakeSamConnection()
) : SamBridgeClient() {
    override fun openControlSocket(host: String, port: Int, timeoutMs: Int): SamConnection = connection

    override fun hello(connection: SamConnection): SamProtocolReply {
        if (cancellationStep == SamProtocolStep.HELLO) throw CancellationException("cancel hello")
        return SamProtocolReply("HELLO REPLY RESULT=OK VERSION=3.1", "OK", version = "3.1")
    }

    override fun generateDestination(connection: SamConnection): SamProtocolReply {
        return SamProtocolReply("DEST REPLY PUB=publicDest PRIV=privateSecretKey", null, publicDestination = "publicDest", privateDestination = "privateSecretKey")
    }

    override fun createStreamSession(connection: SamConnection, sessionId: String, destination: String, leaseSetEncType: String): SamProtocolReply {
        if (cancellationStep == SamProtocolStep.SESSION_CREATE) throw CancellationException("cancel session")
        return SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
    }
}

private class BlockingSamBridgeClient(
    private val started: CountDownLatch,
    private val release: CountDownLatch,
    private val connection: ManagerFakeSamConnection = ManagerFakeSamConnection()
) : SamBridgeClient() {
    var openCalls = 0

    override fun openControlSocket(host: String, port: Int, timeoutMs: Int): SamConnection {
        openCalls += 1
        return connection
    }

    override fun hello(connection: SamConnection): SamProtocolReply {
        started.countDown()
        release.await(5, TimeUnit.SECONDS)
        return SamProtocolReply("HELLO REPLY RESULT=OK VERSION=3.1", "OK", version = "3.1")
    }

    override fun generateDestination(connection: SamConnection): SamProtocolReply {
        return SamProtocolReply("DEST REPLY PUB=publicDest PRIV=privateSecretKey", null, publicDestination = "publicDest", privateDestination = "privateSecretKey")
    }

    override fun createStreamSession(connection: SamConnection, sessionId: String, destination: String, leaseSetEncType: String): SamProtocolReply {
        return SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
    }
}

private class CancellingLookupSamBridgeClient(
    val connection: ManagerFakeSamConnection = ManagerFakeSamConnection()
) : SamBridgeClient() {
    override fun openControlSocket(host: String, port: Int, timeoutMs: Int): SamConnection = connection

    override fun hello(connection: SamConnection): SamProtocolReply {
        return SamProtocolReply("HELLO REPLY RESULT=OK VERSION=3.1", "OK", version = "3.1")
    }

    override fun generateDestination(connection: SamConnection): SamProtocolReply {
        return SamProtocolReply("DEST REPLY PUB=publicDest PRIV=privateSecretKey", null, publicDestination = "publicDest", privateDestination = "privateSecretKey")
    }

    override fun createStreamSession(connection: SamConnection, sessionId: String, destination: String, leaseSetEncType: String): SamProtocolReply {
        return SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
    }

    override fun nameLookup(connection: SamConnection, name: String): SamProtocolReply {
        throw CancellationException("lookup cancelled")
    }
}

private class ReconnectingSamBridgeClient(
    private val connections: ArrayDeque<ManagerFakeSamConnection>
) : SamBridgeClient() {
    var openCalls = 0
    val sessionIds = mutableListOf<String>()

    override fun openControlSocket(host: String, port: Int, timeoutMs: Int): SamConnection {
        openCalls += 1
        return connections.removeFirst()
    }

    override fun hello(connection: SamConnection): SamProtocolReply {
        return SamProtocolReply("HELLO REPLY RESULT=OK VERSION=3.1", "OK", version = "3.1")
    }

    override fun generateDestination(connection: SamConnection): SamProtocolReply {
        return SamProtocolReply("DEST REPLY PUB=publicDest PRIV=privateDest", null, publicDestination = "publicDest", privateDestination = "privateDest")
    }

    override fun createStreamSession(connection: SamConnection, sessionId: String, destination: String, leaseSetEncType: String): SamProtocolReply {
        sessionIds += sessionId
        return SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
    }
}
