package no.knoksen.i2pbrowser

import kotlinx.coroutines.test.runTest
import no.knoksen.i2pbrowser.i2p.SamBridgeClient
import no.knoksen.i2pbrowser.i2p.SamConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SamBridgeClientTest {
    @Test
    fun `valid hello and session response returns destination`() = runTest {
        val client = SamBridgeClient(
            connectionFactory = { _, _, _ ->
                FakeSamConnection(
                    "HELLO REPLY RESULT=OK VERSION=3.1",
                    "SESSION STATUS RESULT=OK DESTINATION=abc123"
                )
            }
        )

        val result = client.connect("127.0.0.1", 7656)

        assertEquals("abc123", result?.destination)
    }

    @Test
    fun `invalid hello response falls back to null`() = runTest {
        val client = SamBridgeClient(
            connectionFactory = { _, _, _ ->
                FakeSamConnection(
                    "HELLO REPLY RESULT=I2P_ERROR MESSAGE=SAM disabled"
                )
            }
        )

        assertNull(client.connect("127.0.0.1", 7656))
    }

    @Test
    fun `missing destination falls back to null`() = runTest {
        val client = SamBridgeClient(
            connectionFactory = { _, _, _ ->
                FakeSamConnection(
                    "HELLO REPLY RESULT=OK VERSION=3.1",
                    "SESSION STATUS RESULT=OK"
                )
            }
        )

        assertNull(client.connect("127.0.0.1", 7656))
    }
}

private class FakeSamConnection(vararg replies: String) : SamConnection {
    private val remainingReplies = ArrayDeque(replies.toList())

    override fun writeLine(line: String) = Unit

    override fun readLine(): String? = remainingReplies.removeFirstOrNull()

    override fun close() = Unit
}
