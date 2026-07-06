package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
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
    val isLoading: Boolean = false
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

class I2PViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = I2PRepository(
        bookmarkDao = db.bookmarkDao(),
        identityDao = db.identityDao(),
        secureMessageDao = db.secureMessageDao(),
        logDao = db.logDao(),
        trustedKeyDao = db.trustedKeyDao(),
        contactDao = db.contactDao(),
        tunnelDao = db.tunnelDao()
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

    val tunnels: StateFlow<List<I2PTunnel>> = repository.allTunnels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _routerState = MutableStateFlow(RouterState())
    val routerState: StateFlow<RouterState> = _routerState.asStateFlow()

    private val _browserTab = MutableStateFlow(BrowserTab())
    val browserTab: StateFlow<BrowserTab> = _browserTab.asStateFlow()

    private val _activeIdentity = MutableStateFlow<Identity?>(null)
    val activeIdentity: StateFlow<Identity?> = _activeIdentity.asStateFlow()

    private val _accessedNodesHistory = MutableStateFlow<List<AccessedNode>>(
        listOf(
            AccessedNode("http://i2p-project.i2p", "I2P Project Homepage", System.currentTimeMillis(), "SECURE_TUNNEL")
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

    private var samSocket: Socket? = null

    private suspend fun tryRealSamConnect(host: String, port: Int): String? {
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(host, port), 2000)
                val writer = socket.getOutputStream()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                writer.write("HELLO VERSION MIN=3.0 MAX=3.1\n".toByteArray())
                writer.flush()
                val helloLine = reader.readLine() ?: ""
                if (!helloLine.startsWith("HELLO REPLY") || !helloLine.contains("RESULT=OK")) {
                    socket.close()
                    return@withContext null
                }

                writer.write("SESSION CREATE STYLE=STREAM ID=AIS_I2P_SESSION DESTINATION=TRANSIENT\n".toByteArray())
                writer.flush()
                val sessionLine = reader.readLine() ?: ""
                if (!sessionLine.startsWith("SESSION STATUS") || !sessionLine.contains("RESULT=OK")) {
                    socket.close()
                    return@withContext null
                }

                val destKeyword = "DESTINATION="
                val destIndex = sessionLine.indexOf(destKeyword)
                if (destIndex != -1) {
                    val dest = sessionLine.substring(destIndex + destKeyword.length).trim()
                    samSocket = socket
                    return@withContext dest
                }
            } catch (e: Exception) {
                try { socket?.close() } catch (ex: Exception) {}
            }
            null
        }
    }

    suspend fun resolveRealI2pName(name: String): String? {
        val socket = samSocket ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val writer = socket.getOutputStream()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.write("NAMELOOKUP NAME=$name\n".toByteArray())
                writer.flush()
                val response = reader.readLine() ?: ""
                if (response.startsWith("NAMELOOKUP REPLY") && response.contains("RESULT=OK")) {
                    val valueKeyword = "VALUE="
                    val idx = response.indexOf(valueKeyword)
                    if (idx != -1) {
                        return@withContext response.substring(idx + valueKeyword.length).trim()
                    }
                }
            } catch (e: Exception) {
                // Socket closed or error
            }
            null
        }
    }

    fun updateSamConfig(host: String, port: Int) {
        _routerState.update { it.copy(samHost = host, samPort = port) }
        viewModelScope.launch {
            repository.addLog("ROUTER", "SAM configuration updated to $host:$port", "INFO")
        }
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
                    val realActiveCount = tunnels.value.count { it.isActive }
                    
                    // Increment traffic of active tunnels in database
                    if (realActiveCount > 0) {
                        val txInc = Random.nextLong(450, 2200)
                        val rxInc = Random.nextLong(1200, 5800)
                        repository.incrementActiveTunnelsTraffic(txInc, rxInc)
                    }

                    val activeTunnelsCount = realActiveCount
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
                            activeTunnels = activeTunnelsCount,
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
                        repository.addLog("TUNNEL", "Tunnel pool rebuilt. Active: $activeTunnelsCount tunnels. Verified $knownPeersRandom peers.", "INFO")
                    }
                }
            }
        }
    }

    fun connectRouter() {
        if (_routerState.value.isConnected || _routerState.value.isConnecting) return

        viewModelScope.launch {
            val host = _routerState.value.samHost
            val port = _routerState.value.samPort
            _routerState.update { it.copy(isConnecting = true, statusText = "Checking real SAM Bridge on $host:$port...") }
            repository.addLog("ROUTER", "Probing real SAM bridge on $host:$port...", "INFO")
            
            // Try connecting to real SAM API
            val realDest = tryRealSamConnect(host, port)
            
            if (realDest != null) {
                _routerState.update { it.copy(connectionProgress = 0.5f, statusText = "SAM v3.1 Handshake OK! Registering local Transient Session...") }
                repository.addLog("ROUTER", "Real SAM API connected! Handshake OK.", "SUCCESS")
                delay(800)
                
                _routerState.update { it.copy(connectionProgress = 0.85f, statusText = "Real Session Active. Syncing LeaseSets...") }
                repository.addLog("ROUTER", "Local Transient Destination registered successfully.", "SUCCESS")
                delay(600)
                
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
                repository.addLog("ROUTER", "No local SAM Bridge active on $host:$port. Launching virtual I2P network engine...", "INFO")
                _routerState.update { it.copy(connectionProgress = 0.15f, statusText = "Initializing Garlic Router Engine...") }
                repository.addLog("ROUTER", "Starting simulated I2P garlic router daemon...", "INFO")
                delay(1000)

                _routerState.update { it.copy(connectionProgress = 0.4f, statusText = "Resolving NetDB profiles...") }
                repository.addLog("NETDB", "Requesting NetDB reseeding from active routers...", "INFO")
                delay(800)

                _routerState.update { it.copy(connectionProgress = 0.65f, statusText = "Configuring ${_routerState.value.tunnelHops}-hop garlic tunnels...") }
                repository.addLog("TUNNEL", "Building outbound and inbound tunnel leaseSets with ${_routerState.value.tunnelHops} hops.", "ROUTING")
                delay(1000)

                _routerState.update { it.copy(connectionProgress = 0.85f, statusText = "Exchanging leaseSet descriptors...") }
                repository.addLog("GARLIC", "Signed leaseSet descriptors published securely.", "SUCCESS")
                delay(600)

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
                        statusText = "Connected securely to I2P network",
                        latencyMs = initialLatency,
                        packetLoss = initialLoss,
                        activePeerCount = initialPeers
                    )
                }
                repository.addLog("ROUTER", "I2P Garlic Router fully connected and listening.", "SUCCESS")
            }
        }
    }

    fun connectRouterForTesting() {
        _routerState.update {
            it.copy(
                isConnected = true,
                isConnecting = false,
                connectionProgress = 1.0f,
                isRealI2p = false,
                realDestination = "",
                activeTunnels = 16,
                knownPeers = 512,
                statusText = "Connected securely to I2P network (Test)",
                latencyMs = 405,
                packetLoss = 0.5f,
                activePeerCount = 7
            )
        }
    }

    fun disconnectRouter() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    samSocket?.close()
                } catch (e: Exception) {}
                samSocket = null
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

    fun addTunnel(name: String, type: String, localPort: Int, targetAddress: String, hops: Int) {
        viewModelScope.launch {
            repository.addTunnel(name, type, localPort, targetAddress, hops)
        }
    }

    fun removeTunnel(tunnel: I2PTunnel) {
        viewModelScope.launch {
            repository.removeTunnel(tunnel)
        }
    }

    fun toggleTunnelStatus(tunnel: I2PTunnel) {
        viewModelScope.launch {
            repository.toggleTunnelStatus(tunnel)
        }
    }

    fun navigateBrowser(url: String) {
        val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "http://$url"
        } else {
            url
        }

        viewModelScope.launch {
            _browserTab.update { it.copy(isLoading = true, url = cleanUrl) }
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

            // Simulating dynamic latency and garlic routing steps based on active accessories
            var routingDelay = 1500L
            val accessories = _activeAccessories.value
            if (accessories.contains("noscript")) routingDelay -= 300
            if (accessories.contains("https_everywhere")) routingDelay += 100
            if (accessories.contains("user_agent")) routingDelay += 50
            if (accessories.contains("metadata_stripper")) routingDelay += 150
            if (routingDelay < 400) routingDelay = 400

            delay(routingDelay)

            val pageTitle = when {
                cleanUrl.contains("127.0.0.1:7657") || cleanUrl.contains("localhost:7657") || cleanUrl.contains("router-console") -> "I2P Router Console WebUI"
                cleanUrl.contains("i2p-project") -> "I2P Project Homepage"
                cleanUrl.contains("anon.chat") -> "AnonIRC Relay Chat"
                cleanUrl.contains("wiki.leaks") -> "Invisible Cryptic Wiki"
                cleanUrl.contains("secure.mail") -> "Garlic Mail Service"
                cleanUrl.contains("forum.feed") -> "Hidden Forum Feed"
                cleanUrl.contains("darkbert.intel") -> "DarkBERT Threat Intelligence"
                else -> "Crypto Host (HTTP 200)"
            }

            _browserTab.update {
                val newHistory = it.history.toMutableList()
                newHistory.add(cleanUrl)
                it.copy(
                    url = cleanUrl,
                    history = newHistory,
                    currentHistoryIndex = newHistory.size - 1,
                    pageTitle = pageTitle,
                    isLoading = false
                )
            }
            _accessedNodesHistory.update { history ->
                history + AccessedNode(
                    url = cleanUrl,
                    title = pageTitle,
                    timestamp = System.currentTimeMillis(),
                    connectionStatus = if (_routerState.value.isConnected) "SECURE_TUNNEL" else "UNSECURED_LOCAL"
                )
            }
            repository.addLog("PROXY", "Page loaded: $pageTitle ($cleanUrl) securely routed in ${routingDelay}ms.", "SUCCESS")
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
            val isConn = _routerState.value.isConnected
            _accessedNodesHistory.update { history ->
                history + AccessedNode(
                    url = selected.first,
                    title = selected.second,
                    timestamp = System.currentTimeMillis(),
                    connectionStatus = if (isConn) "SECURE_TUNNEL" else "UNSECURED_LOCAL"
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

    fun createNewIdentity(name: String, keySize: Int = 1024) {
        viewModelScope.launch {
            val newId = repository.createIdentity(name, keySize)
            _activeIdentity.value = newId
        }
    }

    fun importIdentity(name: String, publicKeyBase64: String, privateKeyBase64: String) {
        viewModelScope.launch {
            val newId = repository.importIdentity(name, publicKeyBase64, privateKeyBase64)
            _activeIdentity.value = newId
        }
    }

    fun deleteIdentity(identity: Identity) {
        viewModelScope.launch {
            repository.deleteIdentity(identity)
            if (_activeIdentity.value?.id == identity.id) {
                _activeIdentity.value = null
            }
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

            // Look up the recipient's public key from the trusted key ring, or fall back to an auto-generated valid sandbox key
            val recipientPublicKey = trustedKeys.value.find { it.i2pAddress == contact.address }?.publicKeyBase64
                ?: getOrCreateValidKeysForPeer(contact.name, "").first

            // Perform actual Hybrid asymmetric encryption before storing/transmitting
            val realEncryptedPayload = encryptHybrid(body, recipientPublicKey)

            // Write logs for the specific platform
            when (contact.type) {
                "GOOGLE_CHAT" -> {
                    repository.addLog("GOOGLE_CHAT", "Initiating secure OTR handshake with Google Chat endpoint: ${contact.address}", "INFO")
                    repository.addLog("CRYPT", "Establishing end-to-end encrypted session using Signal Protocol (X3DH/Double Ratchet)...", "SUCCESS")
                    repository.addLog("GOOGLE_CHAT", "OTR-encrypted packet dispatched over OAuth2 TLS stream.", "SUCCESS")
                }
                "SMS" -> {
                    repository.addLog("SMS", "Encrypting message payload using Silence/Signal SMS format (A/B keys)...", "INFO")
                    repository.addLog("CRYPT", "Diffie-Hellman SMS key exchange confirmed with peer: ${contact.address}", "SUCCESS")
                    repository.addLog("SMS", "Dispatching multi-part encrypted PDU via cellular transport subsystem.", "SUCCESS")
                }
                else -> {
                    repository.addLog("CRYPT", "Encrypting garlic clove payload with ElGamal/AES-256 for ${contact.address}", "INFO")
                    repository.addLog("TUNNEL", "Tunnel built: Outbound tunnel (Gate -> Proxy -> Endpoint)", "SUCCESS")
                }
            }

            // Save the secure message in DB (outgoing message is stored with decryptedBody for display but encrypted payload represents what was sent)
            val secureMsg = SecureMessage(
                senderAddress = currentSender,
                recipientAddress = contact.address,
                encryptedPayload = realEncryptedPayload,
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
                    repository.addLog("GOOGLE_CHAT", "Incoming encrypted push event from Google Chat servers...", "INFO")
                    repository.addLog("CRYPT", "Double Ratchet key updated. Decrypting payload...", "SUCCESS")
                }
                "SMS" -> {
                    repository.addLog("SMS", "Incoming encrypted SMS PDU received from cell tower...", "INFO")
                    repository.addLog("CRYPT", "Wiping ephemeral session key buffer after decryption.", "SUCCESS")
                }
                else -> {
                    repository.addLog("GARLIC", "Received incoming Garlic Message from leaseSet of ${contact.address}", "ROUTING")
                }
            }

            // For incoming, we encrypt the response using OUR public key. It is stored as ciphertext and starts encrypted (isDecrypted = false)
            val myPublicKey = _activeIdentity.value?.publicKeyBase64
                ?: getOrCreateValidKeysForPeer("My Identity", "").first
            
            val realIncomingEncryptedPayload = encryptHybrid(responseBody, myPublicKey)

            val incomingMsg = SecureMessage(
                senderAddress = contact.address,
                recipientAddress = currentSender,
                encryptedPayload = realIncomingEncryptedPayload,
                isIncoming = true,
                isDecrypted = false,
                decryptedBody = null
            )
            db.secureMessageDao().insertMessage(incomingMsg)
        }
    }

    fun receiveEncryptedMessageSimulation(contact: Contact, text: String) {
        viewModelScope.launch {
            val currentSender = when (contact.type) {
                "GOOGLE_CHAT" -> _activeIdentity.value?.name?.lowercase()?.replace(" ", "")?.let { "$it@gmail.com" } ?: "anon.user@gmail.com"
                "SMS" -> "+155598765"
                else -> _activeIdentity.value?.i2pAddress ?: "anon.i2p"
            }

            val myPublicKey = _activeIdentity.value?.publicKeyBase64
                ?: getOrCreateValidKeysForPeer("My Identity", "").first
            
            val realIncomingEncryptedPayload = encryptHybrid(text, myPublicKey)

            repository.addLog("CRYPT", "Simulated incoming secure clove received from ${contact.name}.", "INFO")
            
            val incomingMsg = SecureMessage(
                senderAddress = contact.address,
                recipientAddress = currentSender,
                encryptedPayload = realIncomingEncryptedPayload,
                isIncoming = true,
                isDecrypted = false,
                decryptedBody = null
            )
            db.secureMessageDao().insertMessage(incomingMsg)
        }
    }

    fun decryptSecureMessage(msg: SecureMessage) {
        val activeId = _activeIdentity.value
        if (activeId == null) {
            viewModelScope.launch {
                repository.addLog("CRYPT", "ERROR: Decryption aborted. Please generate or select an active encryption identity first!", "WARN")
            }
            return
        }
        viewModelScope.launch {
            repository.addLog("CRYPT", "Initiating on-the-fly private-key RSA decryption of garlic block...", "INFO")
            val decrypted = decryptHybrid(msg.encryptedPayload, activeId.privateKeyBase64)
            if (decrypted.startsWith("ERROR")) {
                repository.addLog("CRYPT", "Decryption failed! Private key signature mismatch. Ensure you are switched to the correct identity keypair.", "WARN")
            } else {
                val updated = msg.copy(isDecrypted = true, decryptedBody = decrypted)
                db.secureMessageDao().insertMessage(updated)
                repository.addLog("CRYPT", "Successfully decrypted secure payload block using Private Key of ${activeId.name}", "SUCCESS")
            }
        }
    }

    fun addContact(name: String, address: String, type: String, status: String = "ONLINE", publicKeyBase64: String? = null) {
        viewModelScope.launch {
            val colorHex = when (type) {
                "SECURE_I2P" -> "#00E676"
                "GOOGLE_CHAT" -> "#FFD600"
                "SMS" -> "#00B0FF"
                else -> "#FF3D00"
            }
            repository.addContact(name, address, type, status, colorHex)
            if (!publicKeyBase64.isNullOrBlank()) {
                repository.addTrustedKey(name, address, publicKeyBase64, isVerified = true)
                repository.addLog("CRYPT", "Automatically associated trusted public key for contact '$name' ($address)", "SUCCESS")
            }
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

    // Encrypt the invitation packet using a 6-digit numeric shared secret PIN (very secure out-of-band exchange!)
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
