package no.knoksen.i2pbrowser

import kotlinx.coroutines.test.runTest
import no.knoksen.i2pbrowser.i2p.I2pEndpointConfig
import no.knoksen.i2pbrowser.i2p.SamBridgeClient
import no.knoksen.i2pbrowser.i2p.SamConnection
import no.knoksen.i2pbrowser.i2p.SamNameLookupMode
import no.knoksen.i2pbrowser.i2p.SamProtocolReply
import no.knoksen.i2pbrowser.i2p.SamSessionManager
import no.knoksen.i2pbrowser.i2p.SamSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SamSessionManagerTest {
    private val config = I2pEndpointConfig.LOCAL_ANDROID_ROUTER

    @Test
    fun `successful connect transitions to ready`() = runTest {
        val manager = SamSessionManager(ScriptedSamBridgeClient())

        val status = manager.connect(config)

        assertEquals(SamSessionState.READY, status.state)
        assertEquals("publicDest", status.publicDestination)
        assertEquals("3.1", status.samVersion)
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
    fun `failure during destination generation transitions to failed and closes socket`() = runTest {
        val client = ScriptedSamBridgeClient(destinationResult = SamProtocolReply("DEST REPLY RESULT=I2P_ERROR MESSAGE=No dest", "I2P_ERROR", message = "No dest"))
        val manager = SamSessionManager(client)

        val status = manager.connect(config)

        assertEquals(SamSessionState.FAILED, status.state)
        assertTrue(status.error!!.contains("DEST GENERATE failed"))
        assertTrue(client.connection.closed)
    }

    @Test
    fun `session create retries leaseSet fallback`() = runTest {
        val client = ScriptedSamBridgeClient(
            sessionResults = ArrayDeque(
                listOf(
                    SamProtocolReply("SESSION STATUS RESULT=I2P_ERROR MESSAGE=Unsupported", "I2P_ERROR", message = "Unsupported"),
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
                    SamProtocolReply("SESSION STATUS RESULT=I2P_ERROR MESSAGE=Duplicate ID", "I2P_ERROR", message = "Duplicate ID"),
                    SamProtocolReply("SESSION STATUS RESULT=I2P_ERROR MESSAGE=Duplicate ID", "I2P_ERROR", message = "Duplicate ID")
                )
            )
        )
        val manager = SamSessionManager(client)

        val status = manager.connect(config)

        assertEquals(SamSessionState.FAILED, status.state)
        assertTrue(status.error!!.contains("SESSION CREATE failed"))
        assertTrue(client.connection.closed)
    }

    @Test
    fun `reconnect closes previous socket before creating a new session`() = runTest {
        val firstConnection = FakeSamConnection()
        val secondConnection = FakeSamConnection()
        val client = ReconnectingSamBridgeClient(ArrayDeque(listOf(firstConnection, secondConnection)))
        val manager = SamSessionManager(client)

        manager.connect(config)
        val status = manager.connect(config)

        assertEquals(SamSessionState.READY, status.state)
        assertTrue(firstConnection.closed)
        assertEquals(2, client.openCalls)
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
}

private class ScriptedSamBridgeClient(
    val connection: FakeSamConnection = FakeSamConnection(),
    private val helloResult: SamProtocolReply = SamProtocolReply("HELLO REPLY RESULT=OK VERSION=3.1", "OK", version = "3.1"),
    private val destinationResult: SamProtocolReply = SamProtocolReply("DEST REPLY RESULT=OK PUB=publicDest PRIV=privateDest", "OK", publicDestination = "publicDest", privateDestination = "privateDest"),
    private val sessionResults: ArrayDeque<SamProtocolReply> = ArrayDeque(listOf(SamProtocolReply("SESSION STATUS RESULT=OK", "OK"))),
    private val lookupResult: SamProtocolReply = SamProtocolReply("NAMING REPLY RESULT=OK VALUE=dest", "OK", value = "dest")
) : SamBridgeClient() {
    val leaseSetAttempts = mutableListOf<String>()
    var lookupCalls = 0

    override fun openControlSocket(host: String, port: Int, timeoutMs: Int): SamConnection = connection
    override fun hello(connection: SamConnection): SamProtocolReply = helloResult
    override fun generateDestination(connection: SamConnection): SamProtocolReply = destinationResult
    override fun createStreamSession(connection: SamConnection, sessionId: String, destination: String, leaseSetEncType: String): SamProtocolReply {
        leaseSetAttempts += leaseSetEncType
        return sessionResults.removeFirst()
    }
    override fun nameLookup(connection: SamConnection, name: String): SamProtocolReply {
        lookupCalls += 1
        return lookupResult
    }
}

private class ReconnectingSamBridgeClient(
    private val connections: ArrayDeque<FakeSamConnection>
) : SamBridgeClient() {
    var openCalls = 0

    override fun openControlSocket(host: String, port: Int, timeoutMs: Int): SamConnection {
        openCalls += 1
        return connections.removeFirst()
    }

    override fun hello(connection: SamConnection): SamProtocolReply {
        return SamProtocolReply("HELLO REPLY RESULT=OK VERSION=3.1", "OK", version = "3.1")
    }

    override fun generateDestination(connection: SamConnection): SamProtocolReply {
        return SamProtocolReply("DEST REPLY RESULT=OK PUB=publicDest PRIV=privateDest", "OK", publicDestination = "publicDest", privateDestination = "privateDest")
    }

    override fun createStreamSession(connection: SamConnection, sessionId: String, destination: String, leaseSetEncType: String): SamProtocolReply {
        return SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
    }
}
