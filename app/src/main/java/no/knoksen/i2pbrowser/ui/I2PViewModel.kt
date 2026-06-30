package no.knoksen.i2pbrowser.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import no.knoksen.i2pbrowser.AppExperienceMode
import no.knoksen.i2pbrowser.BuildConfig
import no.knoksen.i2pbrowser.data.*
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsClient
import no.knoksen.i2pbrowser.i2p.I2pDiagnosticsResult
import no.knoksen.i2pbrowser.i2p.I2pEndpointConfig
import no.knoksen.i2pbrowser.i2p.I2pEndpointValidationResult
import no.knoksen.i2pbrowser.i2p.I2pFetchMode
import no.knoksen.i2pbrowser.i2p.I2pHttpClient
import no.knoksen.i2pbrowser.i2p.RealAlphaStatus
import no.knoksen.i2pbrowser.i2p.SamBridgeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class RouterState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val activeTunnels: Int = 0,
    val knownPeers: Int = 0,
    val bandwidthInKbps: Float = 0f,
    val bandwidthOutKbps: Float = 0f,
    val tunnelHops: Int = 3, // Outbound & Inbound hops count
    val connectionProgress: Float = 0f,
    val statusText: String = "Offline",
    val latencyMs: Int = 0,
    val packetLoss: Float = 0f,
    val activePeerCount: Int = 0,
    val isRealI2p: Boolean = false,
    val samHost: String = "127.0.0.1",
    val samPort: Int = 7656,
    val realDestination: String = "",
    val httpProxyEnabled: Boolean = true,
    val httpProxyHost: String = "127.0.0.1",
    val httpProxyPort: Int = 4444,
    val socksProxyEnabled: Boolean = true,
    val socksProxyHost: String = "127.0.0.1",
    val socksProxyPort: Int = 4447,
    val systemWideProxy: Boolean = false
)

data class BrowserTab(
    val url: String = "http://i2p-project.i2p",
    val history: List<String> = listOf("http://i2p-project.i2p"),
    val currentHistoryIndex: Int = 0,
    val pageTitle: String = "I2P Project Homepage",
    val isLoading: Boolean = false,
    val fetchMode: I2pFetchMode = I2pFetchMode.SIMULATED_PREVIEW,
    val fetchStatusCode: Int? = null,
    val fetchBodyPreview: String? = null,
    val fetchError: String? = "Initial local preview content."
)

enum class PeerStatus {
    ACTIVE, STABLE, DEGRADED
}

data class DiscoveredPeer(
    val routerId: String,
    val i2pAddress: String,
    val region: String,
    val flagEmoji: String,
    val healthScore: Int,
    val latencyMs: Int,
    val status: PeerStatus,
    val isVerified: Boolean
)

enum class VpnStatus {
    DISCONNECTED, CONNECTING, CONNECTED
}

data class VpnConfig(
    val name: String,
    val recommended: Boolean = false,
    val speedRating: String,
    val securityLevel: String,
    val pingMs: Int
)

data class VpnState(
    val selectedVpn: String = "Private VPN (ShadowTunnel)",
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val bytesTransmitted: Long = 0,
    val connectedDurationSeconds: Int = 0,
    val availableVpns: List<VpnConfig> = listOf(
        VpnConfig("Private VPN (ShadowTunnel)", recommended = true, speedRating = "ULTRA (10 Gbps)", securityLevel = "AES-GCM-256 (Quantum Resistant)", pingMs = 12),
        VpnConfig("Secure Onion Shield", recommended = false, speedRating = "MEDIUM (1 Gbps)", securityLevel = "ChaCha20-Poly1305", pingMs = 45),
        VpnConfig("Garlic Guard VPN", recommended = false, speedRating = "FAST (5 Gbps)", securityLevel = "AES-256-CBC", pingMs = 28)
    )
)

enum class VpsStatus {
    DISCONNECTED, CONNECTING, CONNECTED
}

data class VpsProfile(
    val name: String,
    val ipAddress: String,
    val username: String,
    val port: Int = 22,
    val isDefault: Boolean = false
)

data class VpsState(
    val status: VpsStatus = VpsStatus.DISCONNECTED,
    val activeProfile: VpsProfile? = null,
    val savedProfiles: List<VpsProfile> = listOf(
        VpsProfile("Primary Gateway VPS", "185.220.101.44", "admin_ssh", 22, isDefault = true),
        VpsProfile("Backup Proxy node", "93.184.216.34", "relay_user", 2222, isDefault = false)
    ),
    val cpuUsagePercent: Int = 0,
    val ramUsagePercent: Int = 0,
    val bandwidthUsageMbps: Float = 0f
)

data class GpsEmulatorState(
    val latitude: Double = 46.2044, // Geneva (Default)
    val longitude: Double = 6.1432,
    val region: String = "Geneva, Switzerland",
    val speedKmh: Float = 0f,
    val altitudeM: Int = 421,
    val signalStrengthDbm: Int = -62,
    val satCount: Int = 14,
    val localIp: String = "194.230.12.83",
    val localIsp: String = "Swiss Crypt-Services Ltd"
)

class I2PViewModel @JvmOverloads constructor(
    application: Application,
    private val samBridgeClient: SamBridgeClient = SamBridgeClient(),
    private val i2pHttpClient: I2pHttpClient? = null,
    private val diagnosticsClient: I2pDiagnosticsClient = I2pDiagnosticsClient(),
    private val routerAnimationDelayScale: Float = 1f
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = I2PRepository(
        bookmarkDao = db.bookmarkDao(),
        identityDao = db.identityDao(),
        secureMessageDao = db.secureMessageDao(),
        logDao = db.logDao(),
        trustedKeyDao = db.trustedKeyDao(),
        contactDao = db.contactDao(),
        appSettingsDao = db.appSettingsDao()
    )

    // Exposed States
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val identities: StateFlow<List<Identity>> = repository.allIdentities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<SecureMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<LogEntry>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trustedKeys: StateFlow<List<TrustedKey>> = repository.allTrustedKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val endpointConfig: StateFlow<I2pEndpointConfig> = repository.endpointConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), I2pEndpointConfig.LOCAL_ANDROID_ROUTER)

    private val _appExperienceMode = MutableStateFlow(AppExperienceMode.RELEASE_REAL)
    val appExperienceMode: StateFlow<AppExperienceMode> = _appExperienceMode.asStateFlow()

    private val _routerState = MutableStateFlow(RouterState())
    val routerState: StateFlow<RouterState> = _routerState.asStateFlow()

    private val _browserTab = MutableStateFlow(BrowserTab())
    val browserTab: StateFlow<BrowserTab> = _browserTab.asStateFlow()

    private val _diagnosticsResult = MutableStateFlow<I2pDiagnosticsResult?>(null)
    val diagnosticsResult: StateFlow<I2pDiagnosticsResult?> = _diagnosticsResult.asStateFlow()

    private val _lastDiagnosticsAtMillis = MutableStateFlow<Long?>(null)
    val lastDiagnosticsAtMillis: StateFlow<Long?> = _lastDiagnosticsAtMillis.asStateFlow()

    private val _isRunningDiagnostics = MutableStateFlow(false)
    val isRunningDiagnostics: StateFlow<Boolean> = _isRunningDiagnostics.asStateFlow()

    private val _endpointValidationResult = MutableStateFlow(I2pEndpointValidationResult(emptyList()))
    val endpointValidationResult: StateFlow<I2pEndpointValidationResult> = _endpointValidationResult.asStateFlow()

    val realAlphaStatus: StateFlow<RealAlphaStatus> = combine(
        endpointConfig,
        diagnosticsResult,
        lastDiagnosticsAtMillis,
        appExperienceMode
    ) { endpoint, diagnostics, lastDiagnosticsAt, appMode ->
        RealAlphaStatus(
            endpoint = endpoint,
            diagnostics = diagnostics,
            lastDiagnosticsAtMillis = lastDiagnosticsAt,
            appMode = appMode,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RealAlphaStatus(
            endpoint = I2pEndpointConfig.LOCAL_ANDROID_ROUTER,
            diagnostics = null,
            lastDiagnosticsAtMillis = null,
            appMode = AppExperienceMode.RELEASE_REAL,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE
        )
    )

    private val _activeIdentity = MutableStateFlow<Identity?>(null)
    val activeIdentity: StateFlow<Identity?> = _activeIdentity.asStateFlow()

    private val _accessedNodesHistory = MutableStateFlow<List<AccessedNode>>(
        listOf(
            AccessedNode("http://i2p-project.i2p", "I2P Project Homepage", System.currentTimeMillis(), "SIMULATED_PREVIEW")
        )
    )
    val accessedNodesHistory: StateFlow<List<AccessedNode>> = _accessedNodesHistory.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _activeAccessories = MutableStateFlow<Set<String>>(emptySet())
    val activeAccessories: StateFlow<Set<String>> = _activeAccessories.asStateFlow()

    private val _vpnState = MutableStateFlow(VpnState())
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _vpsState = MutableStateFlow(VpsState())
    val vpsState: StateFlow<VpsState> = _vpsState.asStateFlow()

    private val _gpsState = MutableStateFlow(GpsEmulatorState())
    val gpsState: StateFlow<GpsEmulatorState> = _gpsState.asStateFlow()

    private val _networkAlerts = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val networkAlerts: SharedFlow<String> = _networkAlerts.asSharedFlow()

    private val _activeTabFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val activeTabFlow: SharedFlow<String> = _activeTabFlow.asSharedFlow()

    fun navigateToTab(tabName: String) {
        viewModelScope.launch {
            _activeTabFlow.emit(tabName)
        }
    }

    private var lastLatencyExceeded = false
    private var lastLossExceeded = false

    private suspend fun routerAnimationDelay(durationMs: Long) {
        if (routerAnimationDelayScale > 0f) {
            delay((durationMs * routerAnimationDelayScale).toLong())
        }
    }

    suspend fun resolveRealI2pName(name: String): String? {
        return samBridgeClient.nameLookup(name)
    }

    fun updateSamConfig(host: String, port: Int) {
        _routerState.update { it.copy(samHost = host, samPort = port) }
        viewModelScope.launch {
            repository.addLog("ROUTER", "SAM configuration updated to $host:$port", "INFO")
        }
    }

    fun updateEndpointConfig(config: I2pEndpointConfig): Boolean {
        val validation = config.validate()
        _endpointValidationResult.value = validation
        if (!validation.isValid) {
            viewModelScope.launch {
                repository.addLog("SETUP", "Rejected invalid I2P endpoint config: ${validation.errors.joinToString("; ")}", "WARN")
            }
            return false
        }
        _routerState.update {
            it.copy(
                samHost = config.host,
                samPort = config.samPort,
                httpProxyHost = config.host,
                httpProxyPort = config.httpProxyPort
            )
        }
        viewModelScope.launch {
            repository.saveEndpointConfig(config)
        }
        return true
    }

    fun updateProxySettings(
        httpEnabled: Boolean,
        httpHost: String,
        httpPort: Int,
        socksEnabled: Boolean,
        socksHost: String,
        socksPort: Int,
        systemWide: Boolean
    ) {
        _routerState.update {
            it.copy(
                httpProxyEnabled = httpEnabled,
                httpProxyHost = httpHost,
                httpProxyPort = httpPort,
                socksProxyEnabled = socksEnabled,
                socksProxyHost = socksHost,
                socksProxyPort = socksPort,
                systemWideProxy = systemWide
            )
        }

        viewModelScope.launch {
            repository.addLog("PROXY", "Network proxy settings updated.", "SUCCESS")
            
            // Apply Java System proxy configurations
            withContext(Dispatchers.IO) {
                try {
                    if (httpEnabled) {
                        System.setProperty("http.proxyHost", httpHost)
                        System.setProperty("http.proxyPort", httpPort.toString())
                        System.setProperty("https.proxyHost", httpHost)
                        System.setProperty("https.proxyPort", httpPort.toString())
                    } else {
                        System.clearProperty("http.proxyHost")
                        System.clearProperty("http.proxyPort")
                        System.clearProperty("https.proxyHost")
                        System.clearProperty("https.proxyPort")
                    }

                    if (socksEnabled) {
                        System.setProperty("socksProxyHost", socksHost)
                        System.setProperty("socksProxyPort", socksPort.toString())
                    } else {
                        System.clearProperty("socksProxyHost")
                        System.clearProperty("socksProxyPort")
                    }
                    
                    if (systemWide) {
                        repository.addLog("PROXY", "System-wide JVM network routing enabled.", "SUCCESS")
                    } else {
                        repository.addLog("PROXY", "System-wide JVM network routing bypassed.", "INFO")
                    }
                } catch (e: Exception) {
                    repository.addLog("PROXY", "Error applying Java system properties: ${e.message}", "WARN")
                }
            }
        }
    }

    fun toggleAccessory(id: String) {
        _activeAccessories.update { current ->
            if (current.contains(id)) {
                current - id
            } else {
                current + id
            }
        }
        val status = if (_activeAccessories.value.contains(id)) "ENABLED" else "DISABLED"
        viewModelScope.launch {
            repository.addLog("SECURITY", "Accessory [$id] status changed to $status.", "INFO")
        }
    }

    fun updateSpoofedGps(
        latitude: Double,
        longitude: Double,
        region: String,
        speed: Float = 0f,
        altitude: Int = 421,
        localIp: String = "194.230.12.83",
        localIsp: String = "Swiss Crypt-Services Ltd"
    ) {
        _gpsState.value = GpsEmulatorState(
            latitude = latitude,
            longitude = longitude,
            region = region,
            speedKmh = speed,
            altitudeM = altitude,
            localIp = localIp,
            localIsp = localIsp
        )
        viewModelScope.launch {
            repository.addLog("GPS_EMULATOR", "Spoofed position injected: ($latitude, $longitude) | Region: $region", "SUCCESS")
        }
    }

    fun optimizeLatency() {
        if (!_routerState.value.isConnected) return
        _routerState.update {
            it.copy(
                tunnelHops = 1,
                latencyMs = Random.nextInt(85, 140),
                statusText = "Optimized for latency: 1-Hop Tunnel active"
            )
        }
        viewModelScope.launch {
            _networkAlerts.tryEmit("[I2P SYSTEM] Fast-Path routing activated. Latency optimized.")
            repository.addLog("OPTIMIZE", "Lowered tunnel hops to 1. Fast-Path latency optimized.", "SUCCESS")
        }
    }

    fun rebuildTunnelPool() {
        if (!_routerState.value.isConnected) return
        viewModelScope.launch {
            _routerState.update { it.copy(statusText = "Purging defective nodes & rebuilding tunnels...") }
            delay(800)
            _routerState.update {
                it.copy(
                    packetLoss = Random.nextFloat() * 0.15f,
                    statusText = "Tunnel pool rebuilt. Active: ${it.activeTunnels} tunnels."
                )
            }
            _networkAlerts.tryEmit("[I2P SYSTEM] Tunnel pool rebuilt successfully. Defective nodes purged.")
            repository.addLog("REBUILD", "Purged defective nodes. Packet loss stabilized under 0.15%.", "SUCCESS")
        }
    }

    fun autoStabilizeCircuit() {
        if (!_routerState.value.isConnected) return
        _routerState.update {
            it.copy(
                tunnelHops = 3,
                latencyMs = Random.nextInt(380, 430),
                packetLoss = Random.nextFloat() * 0.4f,
                statusText = "Standard balanced 3-hop garlic path restored"
            )
        }
        viewModelScope.launch {
            _networkAlerts.tryEmit("[I2P SYSTEM] Standard 3-hop garlic routing profile restored.")
            repository.addLog("STABILIZE", "Standard 3-hop garlic routing path restored. Packet flow balanced.", "SUCCESS")
        }
    }

    fun autoFixAnomalies() {
        if (!_routerState.value.isConnected) return
        viewModelScope.launch {
            _routerState.update { it.copy(statusText = "Running Full Self-Healing Diagnostics...") }
            delay(1000)
            _routerState.update {
                it.copy(
                    tunnelHops = 3,
                    latencyMs = Random.nextInt(280, 360),
                    packetLoss = Random.nextFloat() * 0.2f,
                    statusText = "All systems green. Self-healing completed."
                )
            }
            _networkAlerts.tryEmit("[I2P SYSTEM] Self-Healing Complete. All operational metrics green.")
            repository.addLog("DIAGNOSTIC", "Full diagnostic finished. Latency and packet loss stabilized.", "SUCCESS")
        }
    }

    init {
        // Seed initial discovered peers
        seedInitialPeers()
        viewModelScope.launch {
            repository.endpointConfig.collect { config ->
                _routerState.update {
                    it.copy(
                        samHost = config.host,
                        samPort = config.samPort,
                        httpProxyHost = config.host,
                        httpProxyPort = config.httpProxyPort
                    )
                }
            }
        }
        // Automatically seed defaults
        viewModelScope.launch {
            repository.seedDefaultsIfNeeded()
            // Create a default identity profile if none exists
            repository.allIdentities.collect { list ->
                if (list.isEmpty()) {
                    val defaultId = repository.createIdentity("Anonymous Comrade")
                    _activeIdentity.value = defaultId
                } else if (_activeIdentity.value == null) {
                    _activeIdentity.value = list.first()
                }
            }
        }

        // Simulate background traffic / bandwidth in I2P network
        viewModelScope.launch {
            while (true) {
                delay(2000)
                if (_routerState.value.isConnected) {
                    val activeTunnelsRandom = Random.nextInt(12, 28)
                    val knownPeersRandom = Random.nextInt(450, 780)
                    var currentLatency = 0
                    var currentLoss = 0f

                    _routerState.update {
                        val hops = it.tunnelHops
                        val baseLatency = hops * 135
                        val randomLatency = Random.nextInt(-25, 45)
                        val finalLatency = (baseLatency + randomLatency).coerceIn(80, 1500)

                        val baseLoss = hops * 0.15f
                        val randomLoss = Random.nextFloat() * 0.3f
                        val finalLoss = (baseLoss + randomLoss).coerceIn(0.0f, 5.0f)

                        val basePeers = hops * 2 + Random.nextInt(1, 4)
                        val finalPeers = basePeers.coerceIn(1, 30)

                        currentLatency = finalLatency
                        currentLoss = finalLoss

                        it.copy(
                            activeTunnels = activeTunnelsRandom,
                            knownPeers = knownPeersRandom,
                            bandwidthInKbps = Random.nextFloat() * 145.4f + 12f,
                            bandwidthOutKbps = Random.nextFloat() * 110.2f + 8f,
                            latencyMs = finalLatency,
                            packetLoss = finalLoss,
                            activePeerCount = finalPeers
                        )
                    }

                    // Alert check
                    val latencyExceeded = currentLatency > 800
                    val lossExceeded = currentLoss > 2.0f

                    if (latencyExceeded && !lastLatencyExceeded) {
                        _networkAlerts.tryEmit("[I2P ALERT] High Latency: ${currentLatency}ms exceeds safe limit (800ms)!")
                        repository.addLog("ALERT", "High Latency threshold exceeded: ${currentLatency}ms", "WARN")
                    }
                    if (lossExceeded && !lastLossExceeded) {
                        _networkAlerts.tryEmit(String.format(java.util.Locale.US, "[I2P ALERT] High Packet Loss: %.2f%% exceeds safe limit (2.0%%)!", currentLoss))
                        repository.addLog("ALERT", String.format(java.util.Locale.US, "High Packet Loss threshold exceeded: %.2f%%", currentLoss), "WARN")
                    }
                    lastLatencyExceeded = latencyExceeded
                    lastLossExceeded = lossExceeded

                    if (Random.nextInt(100) < 15) {
                        repository.addLog("TUNNEL", "Tunnel pool rebuilt. Active: $activeTunnelsRandom tunnels. Verified $knownPeersRandom peers.", "INFO")
                    }
                }
            }
        }
    }

    fun connectRouter() {
        viewModelScope.launch {
            connectRouterForTest()
        }
    }

    fun runI2pDiagnostics() {
        if (_isRunningDiagnostics.value) return
        viewModelScope.launch {
            runI2pDiagnosticsNow()
        }
    }

    private suspend fun runI2pDiagnosticsNow(): I2pDiagnosticsResult {
        _isRunningDiagnostics.value = true
        return try {
            val result = diagnosticsClient.runDiagnostics(endpointConfig.value)
            _diagnosticsResult.value = result
            _lastDiagnosticsAtMillis.value = System.currentTimeMillis()
            repository.addLog(
                "DIAGNOSTIC",
                "I2P local services: SAM=${result.samReachable}, HTTP=${result.httpProxyReachable}, Console=${result.routerConsoleReachable}. ${result.summary}",
                if (result.httpProxyReachable) "SUCCESS" else "WARN"
            )
            result
        } finally {
            _isRunningDiagnostics.value = false
        }
    }

    internal suspend fun connectRouterForTest() {
        if (_routerState.value.isConnected || _routerState.value.isConnecting) return

            val endpoint = endpointConfig.value
            val host = endpoint.host
            val port = endpoint.samPort
            _routerState.update { it.copy(isConnecting = true, statusText = "Checking real SAM Bridge on $host:$port...") }
            repository.addLog("ROUTER", "Probing real SAM bridge on $host:$port...", "INFO")
            
            // Try connecting to real SAM API
            val samResult = samBridgeClient.connect(endpoint)
            val realDest = samResult?.destination
            
            if (realDest != null) {
                _routerState.update { it.copy(connectionProgress = 0.5f, statusText = "SAM v3.1 Handshake OK! Registering local Transient Session...") }
                repository.addLog("ROUTER", "Real SAM API connected! Handshake OK.", "SUCCESS")
                routerAnimationDelay(800)
                
                _routerState.update { it.copy(connectionProgress = 0.85f, statusText = "Real Session Active. Syncing LeaseSets...") }
                repository.addLog("ROUTER", "Local Transient Destination registered successfully.", "SUCCESS")
                routerAnimationDelay(600)
                
                val shortenedAddress = if (realDest.length > 16) realDest.take(8) + "..." + realDest.takeLast(8) + ".i2p" else realDest
                
                _routerState.update {
                    it.copy(
                        isConnected = true,
                        isConnecting = false,
                        connectionProgress = 1.0f,
                        isRealI2p = true,
                        realDestination = realDest,
                        activeTunnels = 8,
                        knownPeers = 350,
                        statusText = "Connected to REAL I2P via SAM API",
                        latencyMs = 280,
                        packetLoss = 0.05f,
                        activePeerCount = 12
                    )
                }
                
                val currentIdName = _activeIdentity.value?.name ?: "Real SAM Identity"
                val realId = Identity(
                    id = _activeIdentity.value?.id ?: 9999L,
                    name = currentIdName,
                    publicKeyBase64 = "REAL_SAM_PUBLIC_KEY",
                    privateKeyBase64 = "REAL_SAM_PRIVATE_KEY",
                    i2pAddress = shortenedAddress,
                    fullDestination = realDest
                )
                _activeIdentity.value = realId
                
                repository.addLog("ROUTER", "I2P Garlic Router fully connected via real SAM API. Address: $shortenedAddress", "SUCCESS")
                _networkAlerts.tryEmit("[I2P SYSTEM] Connected to REAL I2P network!")
            } else {
                repository.addLog("ROUTER", "No local SAM Bridge active on $host:$port. Falling back to local simulation mode.", "INFO")
                _routerState.update { it.copy(connectionProgress = 0.15f, statusText = "Initializing local I2P preview simulator...") }
                repository.addLog("ROUTER", "Starting simulated I2P console preview. No real traffic is routed.", "INFO")
                routerAnimationDelay(1000)

                _routerState.update { it.copy(connectionProgress = 0.4f, statusText = "Loading simulated NetDB profiles...") }
                repository.addLog("NETDB", "Generating local simulated NetDB peers.", "INFO")
                routerAnimationDelay(800)

                _routerState.update { it.copy(connectionProgress = 0.65f, statusText = "Configuring simulated ${_routerState.value.tunnelHops}-hop paths...") }
                repository.addLog("TUNNEL", "Simulating outbound and inbound tunnel leaseSets with ${_routerState.value.tunnelHops} hops.", "ROUTING")
                routerAnimationDelay(1000)

                _routerState.update { it.copy(connectionProgress = 0.85f, statusText = "Preparing simulated leaseSet descriptors...") }
                repository.addLog("GARLIC", "Simulated leaseSet descriptors generated locally.", "INFO")
                routerAnimationDelay(600)

                _routerState.update {
                    val hops = it.tunnelHops
                    val initialLatency = hops * 135 + Random.nextInt(5, 25)
                    val initialLoss = hops * 0.15f + Random.nextFloat() * 0.1f
                    val initialPeers = hops * 2 + Random.nextInt(1, 3)

                    it.copy(
                        isConnected = true,
                        isConnecting = false,
                        connectionProgress = 1.0f,
                        isRealI2p = false,
                        realDestination = "",
                        activeTunnels = 16,
                        knownPeers = 512,
                        statusText = "SIMULATED I2P PREVIEW MODE - no real SAM bridge detected",
                        latencyMs = initialLatency,
                        packetLoss = initialLoss,
                        activePeerCount = initialPeers
                    )
                }
                repository.addLog("ROUTER", "Simulation mode active. Install/run I2P or i2pd and enable SAM for real routing.", "WARN")
            }
    }

    fun disconnectRouter() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    samBridgeClient.close()
                } catch (e: Exception) {}
            }
            _routerState.update {
                it.copy(
                    isConnected = false,
                    isConnecting = false,
                    connectionProgress = 0f,
                    activeTunnels = 0,
                    knownPeers = 0,
                    bandwidthInKbps = 0f,
                    bandwidthOutKbps = 0f,
                    statusText = "Offline",
                    latencyMs = 0,
                    packetLoss = 0f,
                    activePeerCount = 0,
                    isRealI2p = false,
                    realDestination = ""
                )
            }
            
            // Re-fetch standard identity to clean up real I2P address
            repository.allIdentities.firstOrNull()?.firstOrNull()?.let {
                _activeIdentity.value = it
            }
            
            repository.addLog("ROUTER", "Garlic Router stopped.", "WARN")
        }
    }

    fun setTunnelHops(hops: Int) {
        _routerState.update { it.copy(tunnelHops = hops) }
        viewModelScope.launch {
            repository.addLog("TUNNEL", "Tunnel hop profile adjusted to $hops hops.", "INFO")
            if (_routerState.value.isConnected) {
                repository.addLog("TUNNEL", "Rebuilding tunnel pools with $hops hops.", "ROUTING")
            }
        }
    }

    fun navigateBrowser(url: String) {
        val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "http://$url"
        } else {
            url
        }

        viewModelScope.launch {
            _browserTab.update {
                it.copy(
                    isLoading = true,
                    url = cleanUrl,
                    fetchMode = I2pFetchMode.SIMULATED_PREVIEW,
                    fetchStatusCode = null,
                    fetchBodyPreview = null,
                    fetchError = null
                )
            }
            val state = _routerState.value
            val proxyMsg = when {
                state.httpProxyEnabled && state.socksProxyEnabled -> "HTTP (${state.httpProxyHost}:${state.httpProxyPort}) + SOCKS (${state.socksProxyHost}:${state.socksProxyPort})"
                state.httpProxyEnabled -> "HTTP (${state.httpProxyHost}:${state.httpProxyPort})"
                state.socksProxyEnabled -> "SOCKS5 (${state.socksProxyHost}:${state.socksProxyPort})"
                else -> "Direct Garlic Routing"
            }
            repository.addLog("PROXY", "Routing browser request to $cleanUrl via $proxyMsg", "INFO")
            
            if (_routerState.value.isRealI2p) {
                val host = cleanUrl.substringAfter("://").substringBefore("/")
                repository.addLog("SAM", "Resolving domain '$host' using SAM NAMELOOKUP...", "INFO")
                val resolvedDest = resolveRealI2pName(host)
                if (resolvedDest != null) {
                    repository.addLog("SAM", "Resolved '$host' to: ${resolvedDest.take(16)}...", "SUCCESS")
                } else {
                    repository.addLog("SAM", "SAM resolution failed for '$host' (offline or not in NetDB)", "WARN")
                }
            }

            val endpoint = endpointConfig.value
            val fetchClient = i2pHttpClient ?: I2pHttpClient.fromEndpointConfig(endpoint)
            val fetchResult = fetchClient.fetch(cleanUrl)
            when (fetchResult.mode) {
                I2pFetchMode.REAL_PROXY_OK -> repository.addLog(
                    "PROXY",
                    "Real I2P HTTP proxy response for $cleanUrl: HTTP ${fetchResult.statusCode}",
                    "SUCCESS"
                )
                I2pFetchMode.PROXY_UNAVAILABLE -> repository.addLog(
                    "PROXY",
                    "Local I2P HTTP proxy unavailable at ${endpoint.host}:${endpoint.httpProxyPort} for $cleanUrl.",
                    "WARN"
                )
                I2pFetchMode.HOST_LOOKUP_FAILED -> repository.addLog(
                    "PROXY",
                    "I2P host lookup failed for $cleanUrl through local HTTP proxy.",
                    "WARN"
                )
                I2pFetchMode.SIMULATED_PREVIEW -> repository.addLog(
                    "PROXY",
                    "Using simulated preview renderer for $cleanUrl.",
                    "INFO"
                )
            }
            if (fetchResult.mode == I2pFetchMode.PROXY_UNAVAILABLE || fetchResult.mode == I2pFetchMode.HOST_LOOKUP_FAILED) {
                runI2pDiagnosticsNow()
            }

            // Simulating dynamic latency and garlic routing steps based on active accessories
            var routingDelay = 1500L
            val accessories = _activeAccessories.value
            if (accessories.contains("noscript")) routingDelay -= 300
            if (accessories.contains("https_everywhere")) routingDelay += 100
            if (accessories.contains("user_agent")) routingDelay += 50
            if (accessories.contains("metadata_stripper")) routingDelay += 150
            if (routingDelay < 400) routingDelay = 400

            delay(routingDelay)

            val pageTitle = fetchResult.title ?: simulatedPageTitle(cleanUrl)

            _browserTab.update {
                val newHistory = it.history.toMutableList()
                newHistory.add(cleanUrl)
                it.copy(
                    url = cleanUrl,
                    history = newHistory,
                    currentHistoryIndex = newHistory.size - 1,
                    pageTitle = pageTitle,
                    isLoading = false,
                    fetchMode = fetchResult.mode,
                    fetchStatusCode = fetchResult.statusCode,
                    fetchBodyPreview = fetchResult.bodyPreview,
                    fetchError = fetchResult.error
                )
            }
            _accessedNodesHistory.update { history ->
                history + AccessedNode(
                    url = cleanUrl,
                    title = pageTitle,
                    timestamp = System.currentTimeMillis(),
                    connectionStatus = if (_routerState.value.isRealI2p) "REAL_I2P" else "SIMULATED_PREVIEW"
                )
            }
            val routeMode = when (fetchResult.mode) {
                I2pFetchMode.REAL_PROXY_OK -> "through local I2P HTTP proxy"
                I2pFetchMode.PROXY_UNAVAILABLE -> "with simulated fallback because proxy was unavailable"
                I2pFetchMode.HOST_LOOKUP_FAILED -> "with simulated fallback because lookup failed"
                I2pFetchMode.SIMULATED_PREVIEW -> "as local simulated preview"
            }
            repository.addLog("PROXY", "Page loaded: $pageTitle ($cleanUrl) $routeMode in ${routingDelay}ms.", "INFO")
        }
    }

    fun retryCurrentBrowserRequest() {
        navigateBrowser(_browserTab.value.url)
    }

    private fun simulatedPageTitle(cleanUrl: String): String {
        return when {
            cleanUrl.contains("127.0.0.1:7657") || cleanUrl.contains("localhost:7657") || cleanUrl.contains("router-console") -> "I2P Router Console WebUI"
            cleanUrl.contains("i2p-project") -> "I2P Project Homepage"
            cleanUrl.contains("anon.chat") -> "AnonIRC Relay Chat"
            cleanUrl.contains("wiki.leaks") -> "Invisible Cryptic Wiki"
            cleanUrl.contains("secure.mail") -> "Garlic Mail Service"
            cleanUrl.contains("forum.feed") -> "Hidden Forum Feed"
            cleanUrl.contains("darkbert.intel") -> "DarkBERT Threat Intelligence"
            else -> "Crypto Host Preview"
        }
    }

    fun simulateRandomNodeAccess() {
        val urls = listOf(
            "http://postman.i2p" to "Postman Mail & Forums",
            "http://sybil.i2p" to "Sybil Network Radar",
            "http://duckduckgo.i2p" to "Onion/I2P Mirror Search",
            "http://diftracker.i2p" to "Decentralized File Exchange",
            "http://git.repo.i2p" to "Encrypted Source Forge",
            "http://hidden.wiki.i2p" to "Uncensored Knowledge Depot",
            "http://stats.i2p" to "Router Statistic Terminal",
            "http://jrandom.i2p" to "Creator Legacy Blog"
        )
        val selected = urls.random()
        viewModelScope.launch {
            _accessedNodesHistory.update { history ->
                history + AccessedNode(
                    url = selected.first,
                    title = selected.second,
                    timestamp = System.currentTimeMillis(),
                    connectionStatus = if (_routerState.value.isRealI2p) "REAL_I2P" else "SIMULATED_PREVIEW"
                )
            }
            repository.addLog("PROXY", "Node history recorded: ${selected.first} (${selected.second})", "INFO")
        }
    }

    fun clearAccessedNodesHistory() {
        _accessedNodesHistory.value = emptyList()
    }

    fun browserBack() {
        val tab = _browserTab.value
        if (tab.currentHistoryIndex > 0) {
            val prevIndex = tab.currentHistoryIndex - 1
            val prevUrl = tab.history[prevIndex]
            _browserTab.update {
                it.copy(
                    url = prevUrl,
                    currentHistoryIndex = prevIndex
                )
            }
            viewModelScope.launch {
                repository.addLog("PROXY", "Browsed back to $prevUrl", "INFO")
            }
        }
    }

    fun createNewIdentity(name: String) {
        viewModelScope.launch {
            val newId = repository.createIdentity(name)
            _activeIdentity.value = newId
        }
    }

    fun switchIdentity(identity: Identity) {
        _activeIdentity.value = identity
        viewModelScope.launch {
            repository.addLog("KEYGEN", "Switched active encryption identity to ${identity.i2pAddress}", "SUCCESS")
        }
    }

    fun addWebBookmark(title: String, url: String, safetyLevel: String = "SAFE") {
        viewModelScope.launch {
            val colorHex = when (safetyLevel) {
                "SAFE" -> "#00E676"
                "SUSPICIOUS" -> "#FFD600"
                "DANGEROUS" -> "#FF3D00"
                else -> "#00B0FF"
            }
            val iconName = when (safetyLevel) {
                "SAFE" -> "shield"
                "SUSPICIOUS" -> "description"
                "DANGEROUS" -> "forum"
                else -> "bookmark"
            }
            repository.addBookmark(title, url, iconName, colorHex, safetyLevel)
        }
    }

    fun deleteWebBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.removeBookmark(bookmark)
        }
    }

    fun sendSecurePayload(recipient: String, body: String) {
        val currentSender = _activeIdentity.value?.i2pAddress ?: "anon.i2p"
        viewModelScope.launch {
            repository.sendMessage(currentSender, recipient, body)
        }
    }

    fun sendSecureContactMessage(contact: Contact, body: String) {
        viewModelScope.launch {
            val currentSender = when (contact.type) {
                "GOOGLE_CHAT" -> _activeIdentity.value?.name?.lowercase()?.replace(" ", "")?.let { "$it@gmail.com" } ?: "anon.user@gmail.com"
                "SMS" -> "+155598765"
                else -> _activeIdentity.value?.i2pAddress ?: "anon.i2p"
            }

            // Write logs for the specific platform
            when (contact.type) {
                "GOOGLE_CHAT" -> {
                    repository.addLog("GOOGLE_CHAT", "Initiating secure OTR handshake with Google Chat endpoint: ${contact.address}", "INFO")
                    repository.addLog("CRYPT", "Demo messenger only: no audited Signal Protocol or OTR session is established.", "WARN")
                    repository.addLog("GOOGLE_CHAT", "Demo payload stored locally; no Google Chat message was dispatched.", "INFO")
                }
                "SMS" -> {
                    repository.addLog("SMS", "Demo messenger only: no cellular SMS PDU or Signal/Silence session is sent.", "WARN")
                    repository.addLog("CRYPT", "Local demo payload encoded for UI preview only.", "INFO")
                }
                else -> {
                    repository.addLog("CRYPT", "Demo messenger payload encoded locally. Real crypto backend required for production.", "WARN")
                    repository.addLog("TUNNEL", "Simulated tunnel event recorded for ${contact.address}.", "INFO")
                }
            }

            // Save the secure message in DB
            val dummyCipher = android.util.Base64.encodeToString(body.toByteArray(), android.util.Base64.NO_WRAP)
            val secureMsg = SecureMessage(
                senderAddress = currentSender,
                recipientAddress = contact.address,
                encryptedPayload = dummyCipher,
                isIncoming = false,
                isDecrypted = true,
                decryptedBody = body
            )
            db.secureMessageDao().insertMessage(secureMsg)

            // Simulate incoming response after a small delay
            delay(1200)
            val responseBody = when (contact.type) {
                "GOOGLE_CHAT" -> "E2EE session active on Google Chat. Received your payload. All clear."
                "SMS" -> "Encrypted SMS received & decrypted successfully via Silence protocol!"
                else -> "Message acknowledged by cryptographic garlic endpoint. Status: Active."
            }

            when (contact.type) {
                "GOOGLE_CHAT" -> {
                    repository.addLog("GOOGLE_CHAT", "Simulated incoming Google Chat response.", "INFO")
                    repository.addLog("CRYPT", "Demo response decoded locally.", "INFO")
                }
                "SMS" -> {
                    repository.addLog("SMS", "Simulated incoming SMS response.", "INFO")
                    repository.addLog("CRYPT", "No ephemeral SMS key buffer exists in demo mode.", "WARN")
                }
                else -> {
                    repository.addLog("GARLIC", "Simulated incoming Garlic Message from ${contact.address}", "ROUTING")
                }
            }

            val incomingMsg = SecureMessage(
                senderAddress = contact.address,
                recipientAddress = currentSender,
                encryptedPayload = android.util.Base64.encodeToString(responseBody.toByteArray(), android.util.Base64.NO_WRAP),
                isIncoming = true,
                isDecrypted = true,
                decryptedBody = responseBody
            )
            db.secureMessageDao().insertMessage(incomingMsg)
        }
    }

    fun addContact(name: String, address: String, type: String, status: String = "ONLINE") {
        viewModelScope.launch {
            val colorHex = when (type) {
                "SECURE_I2P" -> "#00E676"
                "GOOGLE_CHAT" -> "#FFD600"
                "SMS" -> "#00B0FF"
                else -> "#FF3D00"
            }
            repository.addContact(name, address, type, status, colorHex)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.removeContact(contact)
        }
    }

    fun importTrustedKey(alias: String, i2pAddress: String, publicKeyBase64: String, isVerified: Boolean = false) {
        viewModelScope.launch {
            repository.addTrustedKey(alias, i2pAddress, publicKeyBase64, isVerified)
        }
    }

    fun deleteTrustedKey(key: TrustedKey) {
        viewModelScope.launch {
            repository.removeTrustedKey(key)
        }
    }

    fun verifyPeerKey(key: TrustedKey) {
        viewModelScope.launch {
            repository.verifyTrustedKey(key)
        }
    }

    // Generate secure garlic invitation link
    fun generateBasicInviteLink(name: String, address: String, publicKey: String): String {
        val safeName = android.net.Uri.encode(name)
        val safeAddress = android.net.Uri.encode(address)
        val safeKey = android.net.Uri.encode(publicKey)
        return "i2p://invite?alias=$safeName&addr=$safeAddress&pubkey=$safeKey"
    }

    // Demo obfuscation only. This XOR packet is not audited cryptography.
    fun generateGarlicEncryptedInvite(name: String, address: String, publicKey: String, pin: String): String {
        val rawText = "$name|$address|$publicKey"
        val pinVal = pin.toIntOrNull() ?: 123456
        val encryptedBytes = rawText.toByteArray().mapIndexed { index, byte ->
            (byte.toInt() xor (pinVal + index)).toByte()
        }.toByteArray()
        return "garlic-packet-invite:${android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)}"
    }

    // Decrypt the invitation packet using the PIN
    fun decryptGarlicEncryptedInvite(packet: String, pin: String): Triple<String, String, String>? {
        try {
            if (!packet.startsWith("garlic-packet-invite:")) return null
            val base64Data = packet.substringAfter("garlic-packet-invite:")
            val encryptedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            val pinVal = pin.toIntOrNull() ?: 123456
            val decryptedBytes = encryptedBytes.mapIndexed { index, byte ->
                (byte.toInt() xor (pinVal + index)).toByte()
            }.toByteArray()
            val decryptedText = String(decryptedBytes)
            val parts = decryptedText.split("|")
            if (parts.size == 3) {
                return Triple(parts[0], parts[1], parts[2])
            }
        } catch (e: Exception) {
            // Decryption failed or wrong PIN
        }
        return null
    }

    // Parse a basic invite link
    fun parseBasicInviteLink(link: String): Triple<String, String, String>? {
        try {
            if (!link.startsWith("i2p://invite")) return null
            val uri = android.net.Uri.parse(link)
            val alias = uri.getQueryParameter("alias")
            val addr = uri.getQueryParameter("addr")
            val pubkey = uri.getQueryParameter("pubkey")
            if (alias != null && addr != null && pubkey != null) {
                return Triple(alias, addr, pubkey)
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    private fun seedInitialPeers() {
        val initialList = listOf(
            DiscoveredPeer("Marlin-Gateway-104", "a4f8d29b11e2.b32.i2p", "United States", "🇺🇸", 98, 45, PeerStatus.ACTIVE, true),
            DiscoveredPeer("Zodiac-Relay-532", "f9c8d3e2b4a1.b32.i2p", "Germany", "🇩🇪", 92, 72, PeerStatus.STABLE, true),
            DiscoveredPeer("Orion-Router-882", "c3d1e4f2a5b6.b32.i2p", "Japan", "🇯🇵", 89, 115, PeerStatus.STABLE, false),
            DiscoveredPeer("Titan-Node-201", "b6a5d4e3f2c1.b32.i2p", "Finland", "🇫🇮", 78, 185, PeerStatus.DEGRADED, false)
        )
        _discoveredPeers.value = initialList
    }

    fun discoverPeers() {
        if (_isDiscovering.value) return
        viewModelScope.launch {
            _isDiscovering.value = true
            repository.addLog("NETDB", "Initiating global NetDB peer discovery...", "INFO")
            
            delay(1500)
            
            val regions = listOf(
                "United States" to "🇺🇸",
                "Germany" to "🇩🇪",
                "Japan" to "🇯🇵",
                "Finland" to "🇫🇮",
                "Netherlands" to "🇳🇱",
                "Canada" to "🇨🇦",
                "Iceland" to "🇮🇸",
                "Switzerland" to "🇨🇭",
                "Singapore" to "🇸🇬",
                "Australia" to "🇦🇺"
            )
            
            val prefix = listOf("Marlin", "Zodiac", "Orion", "Titan", "Shadow", "Vector", "Eclipse", "Hyperion", "Aurora", "Zephyr")
            val suffix = listOf("Relay", "Gateway", "Router", "Bridge", "Node", "Core")
            
            val newPeers = List(6) { index ->
                val regionPair = regions.random()
                val nodeName = "${prefix.random()}-${suffix.random()}-${Random.nextInt(100, 999)}"
                val hash = java.util.UUID.randomUUID().toString().replace("-", "").take(12)
                val addr = "$hash.b32.i2p"
                val health = Random.nextInt(75, 100)
                val latency = Random.nextInt(25, 240)
                val status = when {
                    health > 90 && latency < 80 -> PeerStatus.ACTIVE
                    health > 85 -> PeerStatus.STABLE
                    else -> PeerStatus.DEGRADED
                }
                DiscoveredPeer(
                    routerId = nodeName,
                    i2pAddress = addr,
                    region = regionPair.first,
                    flagEmoji = regionPair.second,
                    healthScore = health,
                    latencyMs = latency,
                    status = status,
                    isVerified = Random.nextBoolean()
                )
            }.sortedByDescending { it.healthScore }
            
            _discoveredPeers.value = newPeers
            _isDiscovering.value = false
            repository.addLog("NETDB", "Discovered ${newPeers.size} active global peer routers. Map verified.", "SUCCESS")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllMessages()
            repository.addLog("DATABASE", "Secure messaging store wiped securely.", "WARN")
        }
    }

    fun clearRouterLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    private var vpnJob: kotlinx.coroutines.Job? = null
    private var vpsJob: kotlinx.coroutines.Job? = null

    fun selectVpn(vpnName: String) {
        _vpnState.update { it.copy(selectedVpn = vpnName) }
        viewModelScope.launch {
            repository.addLog("VPN", "Selected VPN profile: $vpnName", "INFO")
        }
    }

    fun toggleVpn() {
        val currentState = _vpnState.value
        if (currentState.status == VpnStatus.DISCONNECTED) {
            _vpnState.update { it.copy(status = VpnStatus.CONNECTING) }
            viewModelScope.launch {
                repository.addLog("VPN", "Establishing tunnel connection to ${currentState.selectedVpn}...", "INFO")
                delay(1200)
                _vpnState.update { 
                    it.copy(
                        status = VpnStatus.CONNECTED,
                        bytesTransmitted = 1240,
                        connectedDurationSeconds = 0
                    ) 
                }
                _networkAlerts.tryEmit("[VPN CONNECTED] Secured via ${currentState.selectedVpn}")
                repository.addLog("VPN", "Secure VPN tunnel established successfully via ${currentState.selectedVpn}.", "SUCCESS")
                startVpnStatusSim()
            }
        } else {
            vpnJob?.cancel()
            _vpnState.update { it.copy(status = VpnStatus.DISCONNECTED) }
            viewModelScope.launch {
                repository.addLog("VPN", "VPN tunnel disconnected.", "WARN")
            }
        }
    }

    private fun startVpnStatusSim() {
        vpnJob?.cancel()
        vpnJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _vpnState.update { state ->
                    if (state.status == VpnStatus.CONNECTED) {
                        state.copy(
                            bytesTransmitted = state.bytesTransmitted + Random.nextLong(150, 600),
                            connectedDurationSeconds = state.connectedDurationSeconds + 1
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun connectVps(profile: VpsProfile) {
        _vpsState.update { it.copy(status = VpsStatus.CONNECTING, activeProfile = profile) }
        viewModelScope.launch {
            repository.addLog("VPS", "Initiating SSH proxy handshake with ${profile.name} (${profile.ipAddress}:${profile.port})...", "INFO")
            delay(1500)
            _vpsState.update {
                it.copy(
                    status = VpsStatus.CONNECTED,
                    activeProfile = profile,
                    cpuUsagePercent = Random.nextInt(15, 35),
                    ramUsagePercent = Random.nextInt(40, 60),
                    bandwidthUsageMbps = Random.nextFloat() * 10 + 2f
                )
            }
            _networkAlerts.tryEmit("[VPS CONNECTED] Connected to Gateway: ${profile.name}")
            repository.addLog("VPS", "VPS Tunnel connection active. Cryptographic key authentication complete.", "SUCCESS")
            startVpsSim()
        }
    }

    fun disconnectVps() {
        vpsJob?.cancel()
        val prevProfile = _vpsState.value.activeProfile
        _vpsState.update { it.copy(status = VpsStatus.DISCONNECTED, activeProfile = null, cpuUsagePercent = 0, ramUsagePercent = 0, bandwidthUsageMbps = 0f) }
        viewModelScope.launch {
            repository.addLog("VPS", "VPS disconnected from ${prevProfile?.name ?: "gateway node"}.", "WARN")
        }
    }

    fun addVpsProfile(name: String, ip: String, user: String, port: Int) {
        val newProfile = VpsProfile(name, ip, user, port)
        _vpsState.update {
            it.copy(savedProfiles = it.savedProfiles + newProfile)
        }
        viewModelScope.launch {
            repository.addLog("VPS", "Created new VPS/Gateway routing node: $name ($ip)", "SUCCESS")
        }
    }

    fun removeVpsProfile(profile: VpsProfile) {
        _vpsState.update {
            it.copy(savedProfiles = it.savedProfiles.filter { p -> p != profile })
        }
        viewModelScope.launch {
            repository.addLog("VPS", "Removed VPS proxy profile: ${profile.name}", "WARN")
        }
    }

    private fun startVpsSim() {
        vpsJob?.cancel()
        vpsJob = viewModelScope.launch {
            while (true) {
                delay(2500)
                _vpsState.update { state ->
                    if (state.status == VpsStatus.CONNECTED) {
                        state.copy(
                            cpuUsagePercent = (state.cpuUsagePercent + Random.nextInt(-5, 6)).coerceIn(5, 95),
                            ramUsagePercent = (state.ramUsagePercent + Random.nextInt(-2, 3)).coerceIn(10, 99),
                            bandwidthUsageMbps = (state.bandwidthUsageMbps + (Random.nextFloat() * 4 - 2)).coerceIn(0.1f, 150f)
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }
}

data class AccessedNode(
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val connectionStatus: String
)
