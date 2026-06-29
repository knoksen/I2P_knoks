package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.I2PViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("I2P Browser", appName)
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
  fun `verify tactical optimizer functions work correctly when connected`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = I2PViewModel(app)

    // Simulate connection
    viewModel.connectRouter()
    
    // Trigger optimizeLatency and verify fast-path 1-hop activation
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
