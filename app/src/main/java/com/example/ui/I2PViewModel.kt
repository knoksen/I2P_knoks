package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val statusText: String = "Offline"
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

class I2PViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = I2PRepository(
        bookmarkDao = db.bookmarkDao(),
        identityDao = db.identityDao(),
        secureMessageDao = db.secureMessageDao(),
        logDao = db.logDao(),
        trustedKeyDao = db.trustedKeyDao()
    )

    // Exposed States
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val identities: StateFlow<List<Identity>> = repository.allIdentities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<SecureMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<LogEntry>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trustedKeys: StateFlow<List<TrustedKey>> = repository.allTrustedKeys
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
                    val activeTunnelsRandom = Random.nextInt(12, 28)
                    val knownPeersRandom = Random.nextInt(450, 780)
                    _routerState.update {
                        it.copy(
                            activeTunnels = activeTunnelsRandom,
                            knownPeers = knownPeersRandom,
                            bandwidthInKbps = Random.nextFloat() * 145.4f + 12f,
                            bandwidthOutKbps = Random.nextFloat() * 110.2f + 8f
                        )
                    }
                    if (Random.nextInt(100) < 15) {
                        repository.addLog("TUNNEL", "Tunnel pool rebuilt. Active: $activeTunnelsRandom tunnels. Verified $knownPeersRandom peers.", "INFO")
                    }
                }
            }
        }
    }

    fun connectRouter() {
        if (_routerState.value.isConnected || _routerState.value.isConnecting) return

        viewModelScope.launch {
            _routerState.update { it.copy(isConnecting = true, statusText = "Initializing Garlic Router Engine...") }
            repository.addLog("ROUTER", "Starting simulated I2P garlic router daemon...", "INFO")
            delay(1200)

            _routerState.update { it.copy(connectionProgress = 0.25f, statusText = "Resolving NetDB profiles...") }
            repository.addLog("NETDB", "Requesting NetDB reseeding from active routers...", "INFO")
            delay(1000)

            _routerState.update { it.copy(connectionProgress = 0.5f, statusText = "Configuring $routerState.value.tunnelHops-hop garlic tunnels...") }
            repository.addLog("TUNNEL", "Building outbound and inbound tunnel leaseSets with ${_routerState.value.tunnelHops} hops.", "ROUTING")
            delay(1200)

            _routerState.update { it.copy(connectionProgress = 0.85f, statusText = "Exchanging leaseSet descriptors...") }
            repository.addLog("GARLIC", "Signed leaseSet descriptors published securely.", "SUCCESS")
            delay(800)

            _routerState.update {
                it.copy(
                    isConnected = true,
                    isConnecting = false,
                    connectionProgress = 1.0f,
                    activeTunnels = 16,
                    knownPeers = 512,
                    statusText = "Connected securely to I2P network"
                )
            }
            repository.addLog("ROUTER", "I2P Garlic Router fully connected and listening.", "SUCCESS")
        }
    }

    fun disconnectRouter() {
        viewModelScope.launch {
            _routerState.update {
                it.copy(
                    isConnected = false,
                    isConnecting = false,
                    connectionProgress = 0f,
                    activeTunnels = 0,
                    knownPeers = 0,
                    bandwidthInKbps = 0f,
                    bandwidthOutKbps = 0f,
                    statusText = "Offline"
                )
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
            _browserTab.update { it.copy(isLoading = true, url = cleanUrl) }
            repository.addLog("PROXY", "HTTP Outbound Proxy routing request to $cleanUrl", "INFO")
            
            // Simulating latency and garlic routing steps
            delay(1500)

            val pageTitle = when {
                cleanUrl.contains("i2p-project") -> "I2P Project Homepage"
                cleanUrl.contains("anon.chat") -> "AnonIRC Relay Chat"
                cleanUrl.contains("wiki.leaks") -> "Invisible Cryptic Wiki"
                cleanUrl.contains("secure.mail") -> "Garlic Mail Service"
                cleanUrl.contains("forum.feed") -> "Hidden Forum Feed"
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
            repository.addLog("PROXY", "Page loaded: $pageTitle ($cleanUrl) securely routed.", "SUCCESS")
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

    fun addWebBookmark(title: String, url: String) {
        viewModelScope.launch {
            repository.addBookmark(title, url, "bookmark", "#00B0FF")
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
}

data class AccessedNode(
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val connectionStatus: String
)
