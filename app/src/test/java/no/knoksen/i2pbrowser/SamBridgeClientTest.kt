package no.knoksen.i2pbrowser

import kotlinx.coroutines.test.runTest
import no.knoksen.i2pbrowser.i2p.SamBridgeClient
import no.knoksen.i2pbrowser.i2p.SamConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamBridgeClientTest {
    @Test
    fun `valid hello destination and session response returns public destination`() = runTest {
        val connection = FakeSamConnection(
            "HELLO REPLY RESULT=OK VERSION=3.1",
            "DEST REPLY PUB=publicDest PRIV=privateDest",
            "SESSION STATUS RESULT=OK"
        )
        val client = SamBridgeClient(connectionFactory = { _, _, _ -> connection })

        val result = client.connect("127.0.0.1", 7656)

        assertEquals("publicDest", result?.destination)
        assertEquals("3.1", result?.samVersion)
        assertEquals(
            "SESSION CREATE STYLE=STREAM ID=${connection.writes.last().substringAfter("ID=").substringBefore(" ")} DESTINATION=privateDest i2cp.leaseSetEncType=6,4",
            connection.writes.last()
        )
    }

    @Test
    fun `hello OK parser extracts version`() {
        val reply = SamBridgeClient.parseSamReply("HELLO REPLY RESULT=OK VERSION=3.1")

        assertTrue(reply.isOk)
        assertEquals("3.1", reply.version)
    }

    @Test
    fun `hello NOVERSION parser maps failure details`() {
        val reply = SamBridgeClient.parseSamReply("HELLO REPLY RESULT=NOVERSION MESSAGE=Version not supported")

        assertEquals("NOVERSION", reply.result)
        assertEquals("Version not supported", reply.message)
    }

    @Test
    fun `I2P error parser extracts message`() {
        val reply = SamBridgeClient.parseSamReply("SESSION STATUS RESULT=I2P_ERROR MESSAGE=Duplicated ID")

        assertEquals("I2P_ERROR", reply.result)
        assertEquals("Duplicated ID", reply.message)
    }

    @Test
    fun `destination parser extracts public and private material`() {
        val reply = SamBridgeClient.parseSamReply("DEST REPLY PUB=publicDest PRIV=privateDest")

        assertEquals("publicDest", reply.publicDestination)
        assertEquals("privateDest", reply.privateDestination)
        assertEquals(true, reply.isDestinationReplyOk)
    }

    @Test
    fun `destination parser accepts optional RESULT OK`() {
        val reply = SamBridgeClient.parseSamReply("DEST REPLY RESULT=OK PUB=publicDest PRIV=privateDest")

        assertEquals(true, reply.isDestinationReplyOk)
    }

    @Test
    fun `destination parser rejects missing private material`() {
        val reply = SamBridgeClient.parseSamReply("DEST REPLY PUB=publicDest")

        assertEquals(false, reply.isDestinationReplyOk)
    }

    @Test
    fun `destination parser rejects missing public material`() {
        val reply = SamBridgeClient.parseSamReply("DEST REPLY PRIV=privateDest")

        assertEquals(false, reply.isDestinationReplyOk)
    }

    @Test
    fun `invalid hello response falls back to null and closes socket`() = runTest {
        val connection = FakeSamConnection("HELLO REPLY RESULT=I2P_ERROR MESSAGE=SAM disabled")
        val client = SamBridgeClient(connectionFactory = { _, _, _ -> connection })

        assertNull(client.connect("127.0.0.1", 7656))
        assertTrue(connection.closed)
    }

    @Test
    fun `missing destination falls back to null`() = runTest {
        val client = SamBridgeClient(
            connectionFactory = { _, _, _ ->
                FakeSamConnection(
                    "HELLO REPLY RESULT=OK VERSION=3.1",
                    "DEST REPLY RESULT=OK"
                )
            }
        )

        assertNull(client.connect("127.0.0.1", 7656))
    }

    @Test
    fun `session create fallback to leaseSetEncType 4 is explicit`() = runTest {
        val connection = FakeSamConnection(
            "HELLO REPLY RESULT=OK VERSION=3.1",
            "DEST REPLY PUB=publicDest PRIV=privateDest",
            "SESSION STATUS RESULT=I2P_ERROR MESSAGE=Unsupported leaseSetEncType",
            "SESSION STATUS RESULT=OK"
        )
        val client = SamBridgeClient(connectionFactory = { _, _, _ -> connection })

        val result = client.connect("127.0.0.1", 7656)

        assertEquals("publicDest", result?.destination)
        assertEquals(true, result?.compatibilityFallbackUsed)
        assertEquals(
            "SESSION CREATE STYLE=STREAM ID=${connection.writes.last().substringAfter("ID=").substringBefore(" ")} DESTINATION=privateDest i2cp.leaseSetEncType=4",
            connection.writes.last()
        )
    }

    @Test
    fun `generated session id contains no whitespace`() {
        val sessionId = SamBridgeClient.newSessionId()

        assertTrue(sessionId.startsWith(SamBridgeClient.SESSION_ID_PREFIX))
        assertEquals(false, sessionId.any { it.isWhitespace() })
    }
}

class FakeSamConnection(vararg replies: String) : SamConnection {
    private val remainingReplies = ArrayDeque(replies.toList())
    val writes = mutableListOf<String>()
    var closed = false

    override fun writeLine(line: String) {
        writes += line
    }

    override fun readLine(): String? = remainingReplies.removeFirstOrNull()

    override fun close() {
        closed = true
    }
}
