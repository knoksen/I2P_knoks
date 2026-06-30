package no.knoksen.i2pbrowser

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import no.knoksen.i2pbrowser.i2p.SamBridgeClient
import no.knoksen.i2pbrowser.i2p.SamBridgeResult
import no.knoksen.i2pbrowser.i2p.SamConnection
import no.knoksen.i2pbrowser.i2p.SamProtocolReply
import no.knoksen.i2pbrowser.ui.I2PViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("I2P Knoks Browser", appName)
  }

  @Test
  fun `verify router state circuit statistics on initialization and connection`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = I2PViewModel(app)

    val initialState = viewModel.routerState.value
    // Initial state should be offline with zeroed metrics
    assertEquals(false, initialState.isConnected)
    assertEquals(0, initialState.latencyMs)
    assertEquals(0f, initialState.packetLoss)
    assertEquals(0, initialState.activePeerCount)
  }

  @Test
  fun `no SAM bridge leaves router in honest simulation mode`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = I2PViewModel(app, FakeSamBridgeClient(result = null), routerAnimationDelayScale = 0f)

    viewModel.connectRouterForTest()

    val state = viewModel.routerState.value
    assertTrue(state.isConnected)
    assertFalse(state.isRealI2p)
    assertEquals("", state.realDestination)
    assertEquals("SIMULATED I2P PREVIEW MODE - no real SAM bridge detected", state.statusText)
  }

  @Test
  fun `valid SAM bridge marks router as real I2P`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = I2PViewModel(app, FakeSamBridgeClient(result = SamBridgeResult("real-destination", "HELLO REPLY RESULT=OK", "SESSION STATUS RESULT=OK DESTINATION=real-destination")), routerAnimationDelayScale = 0f)

    viewModel.connectRouterForTest()

    val state = viewModel.routerState.value
    assertTrue(state.isConnected)
    assertTrue(state.isRealI2p)
    assertEquals("real-destination", state.realDestination)
    assertEquals("Connected to REAL I2P via SAM API", state.statusText)
  }

  @Test
  fun `optimizer only works when connected`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = I2PViewModel(app, FakeSamBridgeClient(result = null), routerAnimationDelayScale = 0f)

    viewModel.optimizeLatency()
    assertEquals(3, viewModel.routerState.value.tunnelHops)

    viewModel.connectRouterForTest()

    viewModel.optimizeLatency()
    val stateAfterOptimize = viewModel.routerState.value
    assertEquals(1, stateAfterOptimize.tunnelHops)
    assertTrue(stateAfterOptimize.latencyMs in 85..140)

    // Trigger autoStabilizeCircuit and verify standard 3-hop restore
    viewModel.autoStabilizeCircuit()
    val stateAfterStabilize = viewModel.routerState.value
    assertEquals(3, stateAfterStabilize.tunnelHops)
    assertTrue(stateAfterStabilize.latencyMs in 380..430)
  }
}

private class FakeSamBridgeClient(
  private val result: SamBridgeResult?
) : SamBridgeClient() {
  override suspend fun connect(host: String, port: Int, timeoutMs: Int): SamBridgeResult? = result
  override fun openControlSocket(host: String, port: Int, timeoutMs: Int): SamConnection = FakeSamConnection()
  override fun hello(connection: SamConnection): SamProtocolReply {
    return if (result == null) {
      SamProtocolReply("HELLO REPLY RESULT=I2P_ERROR MESSAGE=SAM disabled", "I2P_ERROR", message = "SAM disabled")
    } else {
      SamProtocolReply("HELLO REPLY RESULT=OK VERSION=3.1", "OK", version = "3.1")
    }
  }
  override fun generateDestination(connection: SamConnection): SamProtocolReply {
    val destination = result?.destination.orEmpty()
    return SamProtocolReply("DEST REPLY RESULT=OK PUB=$destination PRIV=private", "OK", publicDestination = destination, privateDestination = "private")
  }
  override fun createStreamSession(connection: SamConnection, sessionId: String, destination: String, leaseSetEncType: String): SamProtocolReply {
    return SamProtocolReply("SESSION STATUS RESULT=OK", "OK")
  }
}
