package no.knoksen.i2pbrowser

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import no.knoksen.i2pbrowser.data.Bookmark
import no.knoksen.i2pbrowser.data.Contact
import no.knoksen.i2pbrowser.data.ConnectIdentityImportResult
import no.knoksen.i2pbrowser.data.EndpointConfigLoadState
import no.knoksen.i2pbrowser.data.EndpointConfigLoadStatus
import no.knoksen.i2pbrowser.data.I2PRepositoryContract
import no.knoksen.i2pbrowser.data.Identity
import no.knoksen.i2pbrowser.data.LogEntry
import no.knoksen.i2pbrowser.data.SecureMessage
import no.knoksen.i2pbrowser.data.TrustedKey
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsClient
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsResult
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsSummary
import no.knoksen.i2pbrowser.i2p.I2pEndpointConfig
import no.knoksen.i2pbrowser.i2p.RealAlphaReadiness
import no.knoksen.i2pbrowser.ui.ConnectIdentityImportUiCategory
import no.knoksen.i2pbrowser.ui.RepositoryInitializationFailureCategory
import no.knoksen.i2pbrowser.ui.I2PViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class I2PViewModelCoreFlowTest {
    @Test
    fun `startup default state is release real unchecked and offline`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = I2PViewModel(
            application = app,
            routerAnimationDelayScale = 0f,
            repositoryFactory = { FakeRepository() }
        )

        assertEquals(AppExperienceMode.RELEASE_REAL, viewModel.appExperienceMode.value)
        assertEquals(I2pEndpointConfig.LOCAL_ANDROID_ROUTER, viewModel.endpointConfig.value)
        assertEquals(RealAlphaReadiness.UNCHECKED, viewModel.realAlphaStatus.value.state)
        assertFalse(viewModel.routerState.value.isConnected)
        assertFalse(viewModel.routerState.value.isRealI2p)
        assertNull(viewModel.diagnosticsResult.value)
        viewModel.cancelForTest()
    }

    @Test
    fun `repeated diagnostics replace stale result and reset running flag`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val first = I2pDiagnosticsClient.fromPortStates(
            samReachable = false,
            httpProxyReachable = false,
            routerConsoleReachable = false
        )
        val second = I2pDiagnosticsClient.fromPortStates(
            samReachable = true,
            httpProxyReachable = true,
            routerConsoleReachable = true
        )
        val viewModel = I2PViewModel(
            application = app,
            diagnosticsClient = ScriptedDiagnosticsClient(first, second),
            routerAnimationDelayScale = 0f,
            repositoryFactory = { FakeRepository() }
        )

        viewModel.runI2pDiagnostics()
        drainMain()
        assertEquals(I2pDiagnosticsSummary.ROUTER_NOT_RUNNING, viewModel.diagnosticsResult.value?.summary)
        assertFalse(viewModel.isRunningDiagnostics.value)

        viewModel.runI2pDiagnostics()
        drainMain()
        assertEquals(I2pDiagnosticsSummary.READY, viewModel.diagnosticsResult.value?.summary)
        assertFalse(viewModel.isRunningDiagnostics.value)
        viewModel.cancelForTest()
    }

    @Test
    fun `latest diagnostic request wins when older request completes late`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val firstGate = CompletableDeferred<Unit>()
        val client = RacingDiagnosticsClient(firstGate)
        val viewModel = I2PViewModel(
            application = app,
            diagnosticsClient = client,
            routerAnimationDelayScale = 0f,
            repositoryFactory = { FakeRepository() }
        )

        viewModel.runI2pDiagnostics()
        drainMain()
        assertEquals(1, client.calls)
        assertFalse(viewModel.diagnosticsResult.value?.summary == I2pDiagnosticsSummary.ROUTER_NOT_RUNNING)

        viewModel.runI2pDiagnostics()
        drainMain()
        assertEquals(I2pDiagnosticsSummary.READY, viewModel.diagnosticsResult.value?.summary)

        firstGate.complete(Unit)
        drainMain()
        assertEquals(I2pDiagnosticsSummary.READY, viewModel.diagnosticsResult.value?.summary)
        assertFalse(viewModel.isRunningDiagnostics.value)
        viewModel.cancelForTest()
    }

    @Test
    fun `repository initialization failure is bounded and non-crashing`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val viewModel = I2PViewModel(
            application = app,
            repositoryFactory = { error("raw database path C:/private/example.db") },
            routerAnimationDelayScale = 0f
        )
        drainMain()

        val error = viewModel.repositoryInitializationError.value
        assertEquals(RepositoryInitializationFailureCategory.STORED_ENDPOINT_UNAVAILABLE, error?.category)
        assertFalse(error?.safeMessage.orEmpty().contains("private/example"))
        assertEquals(I2pEndpointConfig.LOCAL_ANDROID_ROUTER, viewModel.endpointConfig.value)
        viewModel.cancelForTest()
    }

    @Test
    fun `public identity duplicate import result is exposed as bounded UI state`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = I2PViewModel(
            application = app,
            repositoryFactory = {
                FakeRepository(
                    importResult = ConnectIdentityImportResult.AlreadyExists(
                        identityId = 42L,
                        fingerprint = "AA-BB-CC-DD",
                        warnings = emptyList()
                    )
                )
            },
            routerAnimationDelayScale = 0f
        )

        viewModel.importConnectIdentityPublic("raw export text")
        drainMain()

        val state = viewModel.connectIdentityImportState.value
        assertEquals(ConnectIdentityImportUiCategory.ALREADY_EXISTS, state?.category)
        assertFalse(state?.safeMessage.orEmpty().contains("AA-BB-CC-DD"))
        assertFalse(state?.safeMessage.orEmpty().contains("raw export"))
        viewModel.cancelForTest()
    }
}

private fun I2PViewModel.cancelForTest() {
    viewModelScope.cancel()
}

private fun drainMain() {
    shadowOf(Looper.getMainLooper()).idle()
}

private class FakeRepository(
    private val importResult: ConnectIdentityImportResult = ConnectIdentityImportResult.Imported(
        identityId = 1L,
        fingerprint = "TEST-FINGERPRINT",
        warnings = emptyList()
    )
) : I2PRepositoryContract {
    override val allBookmarks: Flow<List<Bookmark>> = flowOf(emptyList())
    override val allIdentities: Flow<List<Identity>> = flowOf(emptyList())
    override val allMessages: Flow<List<SecureMessage>> = flowOf(emptyList())
    override val recentLogs: Flow<List<LogEntry>> = flowOf(emptyList())
    override val allTrustedKeys: Flow<List<TrustedKey>> = flowOf(emptyList())
    override val allContacts: Flow<List<Contact>> = flowOf(emptyList())
    private val endpointState = MutableStateFlow(
        EndpointConfigLoadState(
            config = I2pEndpointConfig.LOCAL_ANDROID_ROUTER,
            status = EndpointConfigLoadStatus.DEFAULT_MISSING
        )
    )
    override val endpointConfigState: Flow<EndpointConfigLoadState> = endpointState
    override val endpointConfig: Flow<I2pEndpointConfig> = flowOf(I2pEndpointConfig.LOCAL_ANDROID_ROUTER)

    override suspend fun saveEndpointConfig(config: I2pEndpointConfig) {
        endpointState.value = EndpointConfigLoadState(config, EndpointConfigLoadStatus.PERSISTED_VALID)
    }
    override suspend fun addContact(name: String, address: String, type: String, status: String, avatarColorHex: String) = Unit
    override suspend fun removeContact(contact: Contact) = Unit
    override suspend fun addBookmark(title: String, url: String, iconName: String, colorHex: String, safetyLevel: String) = Unit
    override suspend fun removeBookmark(bookmark: Bookmark) = Unit
    override suspend fun addLog(tag: String, message: String, level: String) = Unit
    override suspend fun clearLogs() = Unit
    override suspend fun clearAllMessages() = Unit
    override suspend fun importConnectIdentityPublic(exportText: String): ConnectIdentityImportResult = importResult
    override suspend fun createIdentity(name: String): Identity {
        return Identity(
            name = name,
            publicKeyBase64 = "",
            privateKeyBase64 = "",
            i2pAddress = "local-profile.i2p",
            fullDestination = ""
        )
    }
    override suspend fun insertSecureMessage(message: SecureMessage) = Unit
    override suspend fun sendMessage(sender: String, recipient: String, body: String) = Unit
    override suspend fun addTrustedKey(alias: String, i2pAddress: String, publicKeyBase64: String, isVerified: Boolean) = Unit
    override suspend fun removeTrustedKey(key: TrustedKey) = Unit
    override suspend fun verifyTrustedKey(key: TrustedKey) = Unit
    override suspend fun seedDefaultsIfNeeded() = Unit
}

private class ScriptedDiagnosticsClient(
    vararg results: I2pDiagnosticsResult
) : I2pDiagnosticsClient() {
    private val remaining = ArrayDeque(results.toList())

    override suspend fun runDiagnostics(config: I2pEndpointConfig): I2pDiagnosticsResult {
        return remaining.removeFirst()
    }
}

private class RacingDiagnosticsClient(
    private val firstGate: CompletableDeferred<Unit>
) : I2pDiagnosticsClient() {
    var calls = 0

    override suspend fun runDiagnostics(config: I2pEndpointConfig): I2pDiagnosticsResult {
        calls += 1
        return if (calls == 1) {
            try {
                firstGate.await()
            } catch (_: CancellationException) {
                withContext(NonCancellable) {
                    firstGate.await()
                }
            }
            I2pDiagnosticsClient.fromPortStates(
                samReachable = false,
                httpProxyReachable = false,
                routerConsoleReachable = false
            )
        } else {
            I2pDiagnosticsClient.fromPortStates(
                samReachable = true,
                httpProxyReachable = true,
                routerConsoleReachable = true
            )
        }
    }
}
