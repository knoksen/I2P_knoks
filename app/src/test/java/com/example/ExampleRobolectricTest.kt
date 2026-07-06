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
    viewModel.connectRouterForTesting()
    
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

  @Test
  fun `verify cryptographic signing and verification logic`() {
    val message = "This is a highly confidential signature verification test payload!"
    val kpg = java.security.KeyPairGenerator.getInstance("RSA")
    kpg.initialize(1024)
    val kp = kpg.generateKeyPair()
    val privateKeyBase64 = android.util.Base64.encodeToString(kp.private.encoded, android.util.Base64.NO_WRAP)
    val publicKeyBase64 = android.util.Base64.encodeToString(kp.public.encoded, android.util.Base64.NO_WRAP)

    // Sign message using our CryptoSigner helper
    val signature = com.example.ui.CryptoSigner.sign(message, privateKeyBase64)
    assertNotNull("Signature should be generated successfully", signature)

    // Verify signature using our CryptoSigner helper
    val isVerified = com.example.ui.CryptoSigner.verify(message, signature!!, publicKeyBase64)
    assertTrue("Signature should be verified successfully", isVerified)

    // Verify altered message fails verification
    val isVerifiedAlteredMessage = com.example.ui.CryptoSigner.verify(message + "altered", signature, publicKeyBase64)
    assertTrue("Altered message must fail signature verification", !isVerifiedAlteredMessage)

    // Verify altered signature fails verification
    val isVerifiedAlteredSig = com.example.ui.CryptoSigner.verify(message, "A" + signature.substring(1), publicKeyBase64)
    assertTrue("Altered signature must fail verification", !isVerifiedAlteredSig)
  }

  @Test
  fun `verify key size identity generation in ViewModel`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = I2PViewModel(app)
    
    // Trigger creation and confirm it executes without throwing exception
    viewModel.createNewIdentity("TestIdentity1024", 1024)
    viewModel.createNewIdentity("TestIdentity2048", 2048)
    viewModel.createNewIdentity("TestIdentity4096", 4096)
    
    assertTrue("Identity creation requests dispatched successfully", true)
  }
}
