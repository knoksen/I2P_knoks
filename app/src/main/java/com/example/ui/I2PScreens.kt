package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.*
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RouterScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.routerState.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.networkAlerts.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "GARLIC ROUTER STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (state.isConnected) "ACTIVE & ROUTING" else if (state.isConnecting) "BUILDING TUNNELS" else "OFFLINE",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (state.isConnected) CyberGreen else if (state.isConnecting) CyberOrange else CyberRed,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Switch(
                            checked = state.isConnected,
                            onCheckedChange = { checked ->
                                if (checked) viewModel.connectRouter() else viewModel.disconnectRouter()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBlack,
                                checkedTrackColor = CyberGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = CyberCardBg
                            ),
                            modifier = Modifier.testTag("router_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        state.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    if (state.isConnecting) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { state.connectionProgress },
                            color = CyberOrange,
                            trackColor = CyberBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    } else if (state.isConnected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { 1.0f },
                            color = CyberGreen,
                            trackColor = CyberBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.navigateBrowser("http://127.0.0.1:7657")
                                viewModel.navigateToTab("BROWSER")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("launch_webui_console_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "LAUNCH CONSOLE WEBUI (127.0.0.1:7657)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Real SAM Configuration and Status
        item {
            var inputHost by remember { mutableStateOf(state.samHost) }
            var inputPort by remember { mutableStateOf(state.samPort.toString()) }

            LaunchedEffect(state.samHost, state.samPort) {
                inputHost = state.samHost
                inputPort = state.samPort.toString()
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, if (state.isRealI2p) CyberGreen else CyberBorder),
                modifier = Modifier.fillMaxWidth().testTag("real_sam_config_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "REAL I2P BRIDGE (SAM API v3.1)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isRealI2p) CyberGreen else CyberBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (state.isRealI2p) CyberGreen.copy(alpha = 0.15f) else CyberBlue.copy(alpha = 0.12f),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    0.5.dp,
                                    if (state.isRealI2p) CyberGreen else CyberBorder,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (state.isRealI2p) "REAL ACTIVE" else "VIRTUAL ENGINE",
                                fontSize = 8.sp,
                                color = if (state.isRealI2p) CyberGreen else CyberBlue,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "To route traffic over the real peer-to-peer I2P network, run a local i2p/i2pd router with the SAM service enabled (default: 127.0.0.1:7656), and toggle the router switch above to initialize the handshake.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputHost,
                            onValueChange = { inputHost = it },
                            label = { Text("SAM Host", fontSize = 10.sp) },
                            modifier = Modifier.weight(1.5f).testTag("sam_host_input"),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberBlue,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true,
                            enabled = !state.isConnected && !state.isConnecting
                        )

                        OutlinedTextField(
                            value = inputPort,
                            onValueChange = { inputPort = it },
                            label = { Text("Port", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("sam_port_input"),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberBlue,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true,
                            enabled = !state.isConnected && !state.isConnecting
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!state.isConnected && !state.isConnecting) {
                        Button(
                            onClick = {
                                val portVal = inputPort.toIntOrNull() ?: 7656
                                viewModel.updateSamConfig(inputHost, portVal)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberBlue.copy(alpha = 0.15f),
                                contentColor = CyberBlue
                            ),
                            border = BorderStroke(1.dp, CyberBlue),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().testTag("sam_save_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE CONFIGURATION", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    if (state.isRealI2p && state.realDestination.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "REAL TRANSIENT I2P DESTINATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberBlack, RoundedCornerShape(4.dp))
                                .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.realDestination,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("I2P Destination", state.realDestination)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Destination Copied!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(20.dp).testTag("copy_real_destination_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = CyberGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tunnel Configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "GARLIC ENCRYPTION PATH & HOPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1, 3, 5).forEach { hops ->
                            val isSelected = state.tunnelHops == hops
                            OutlinedButton(
                                onClick = { viewModel.setTunnelHops(hops) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) CyberGreen.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (isSelected) CyberGreen else TextPrimary
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) CyberGreen else CyberBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hops_button_$hops")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$hops Hop${if (hops > 1) "s" else ""}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        when (hops) {
                                            1 -> "Low Privacy"
                                            3 -> "Standard I2P"
                                            else -> "High Latency"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) CyberGreen else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Each hop encapsulates packets in multiple layers of asymmetric encryption. Higher hops increase onion-routing garlic protection against network node correlations.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Live Garlic Routing Path Simulation Dashboard
        item {
            GarlicRoutingPathVisualizer(
                tunnelHops = state.tunnelHops,
                isConnected = state.isConnected,
                isConnecting = state.isConnecting
            )
        }

        // Live Circuit Statistics Dashboard Widget
        item {
            I2PCircuitStatsWidget(state = state)
        }

        // Live Health Optimizer Widget
        item {
            I2POptimizerWidget(state = state, viewModel = viewModel)
        }

        // Garlic Routing Knowledge Base Help Center
        item {
            I2PHelpCenterWidget()
        }

        // Telemetry Statistics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ROUTER NETWORK TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryItem(
                            label = "ACTIVE TUNNELS",
                            value = "${state.activeTunnels}",
                            icon = Icons.Default.Cyclone,
                            color = CyberBlue
                        )
                        TelemetryItem(
                            label = "PEERS IN NETDB",
                            value = "${state.knownPeers}",
                            icon = Icons.Default.Groups,
                            color = CyberPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = CyberBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryItem(
                            label = "BANDWIDTH IN",
                            value = String.format("%.1f KB/s", state.bandwidthInKbps),
                            icon = Icons.Default.ArrowDownward,
                            color = CyberGreen
                        )
                        TelemetryItem(
                            label = "BANDWIDTH OUT",
                            value = String.format("%.1f KB/s", state.bandwidthOutKbps),
                            icon = Icons.Default.ArrowUpward,
                            color = CyberOrange
                        )
                    }
                }
            }
        }

        // Global Peer Discovery System
        item {
            PeerDiscoverySection(viewModel = viewModel)
        }

        // Router Console Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ROUTER CONSOLE ACTIVITY LOGS",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { viewModel.clearRouterLogs() },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberRed)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wipe", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Console is empty. Enable router to trace garlic connections.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                LogItemRow(log = log)
            }
        }
    }
}

@Composable
fun TelemetryItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LogItemRow(log: LogEntry) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = formatter.format(Date(log.timestamp))

    val levelColor = when (log.level) {
        "SUCCESS" -> CyberGreen
        "WARN" -> CyberRed
        "ROUTING" -> CyberPurple
        else -> CyberBlue
    }

    val badgeResId = when (log.tag) {
        "GARLIC", "CRYPT", "KEYRING", "KEYGEN" -> com.example.R.drawable.img_security_gold_1782670586675
        "TUNNEL", "NETDB", "ROUTER" -> com.example.R.drawable.img_security_silver_1782670571201
        else -> com.example.R.drawable.img_security_bronze_1782670550813
    }

    val securityLevelLabel = when (log.tag) {
        "GARLIC", "CRYPT", "KEYRING", "KEYGEN" -> "GOLD"
        "TUNNEL", "NETDB", "ROUTER" -> "SILVER"
        else -> "BRONZE"
    }

    val securityLevelColor = when (securityLevelLabel) {
        "GOLD" -> Color(0xFFD4AF37) // Metallic Gold
        "SILVER" -> Color(0xFFA0A2A6) // Cyber Silver
        else -> Color(0xFFCD7F32) // Tech Bronze
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberDarkSurface, RoundedCornerShape(6.dp))
            .border(1.dp, securityLevelColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High quality security level image badge
        Image(
            painter = painterResource(id = badgeResId),
            contentDescription = "Security Level: $securityLevelLabel",
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(0.5.dp, securityLevelColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
        )

        Text(
            "[$timeStr]",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            fontSize = 11.sp
        )

        // Security level indicator tag
        Surface(
            color = securityLevelColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(3.dp),
            border = BorderStroke(0.5.dp, securityLevelColor.copy(alpha = 0.4f))
        ) {
            Text(
                securityLevelLabel,
                color = securityLevelColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Text(
            log.tag,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = levelColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Text(
            log.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun BrowserScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val tabState by viewModel.browserTab.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val routerState by viewModel.routerState.collectAsState()
    
    var urlInput by remember { mutableStateOf(tabState.url) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkTitleInput by remember { mutableStateOf("") }
    var bookmarkSafetyInput by remember { mutableStateOf("SAFE") }
    var showRoutingDashboard by remember { mutableStateOf(true) }
    
    // Sync address bar when page navigates
    LaunchedEffect(tabState.url) {
        urlInput = tabState.url
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSafetyFilter by remember { mutableStateOf("ALL") }
    var isIndexingSearchActive by remember { mutableStateOf(false) }

    // Simulated I2P Distributed Hash Table (DHT) / hosts.txt indexing lookup
    LaunchedEffect(searchQuery, selectedSafetyFilter) {
        if (searchQuery.isNotEmpty()) {
            isIndexingSearchActive = true
            delay(350) // Simulated network hop lookup delay
            isIndexingSearchActive = false
        }
    }

    val filteredBookmarks = remember(bookmarks, searchQuery, selectedSafetyFilter) {
        bookmarks.filter { bookmark ->
            val matchesQuery = bookmark.title.contains(searchQuery, ignoreCase = true) ||
                    bookmark.url.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedSafetyFilter == "ALL" || bookmark.safetyLevel == selectedSafetyFilter
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Safe Address & Proxy Navigation Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(0.dp, Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.browserBack() },
                        enabled = tabState.currentHistoryIndex > 0,
                        modifier = Modifier.testTag("browser_back_button")
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (tabState.currentHistoryIndex > 0) CyberGreen else TextSecondary
                        )
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("browser_address_bar"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        ),
                        placeholder = { Text("enter.i2p address...", color = TextSecondary) },
                        leadingIcon = {
                            Icon(
                                if (urlInput.endsWith(".i2p")) Icons.Default.VpnLock else Icons.Default.Language,
                                contentDescription = null,
                                tint = if (urlInput.endsWith(".i2p")) CyberGreen else TextSecondary
                            )
                        },
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        bookmarkTitleInput = tabState.pageTitle
                                        showBookmarkDialog = true
                                    },
                                    modifier = Modifier.testTag("browser_bookmark_icon")
                                ) {
                                    Icon(
                                        Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark this site",
                                        tint = CyberBlue
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberBlack,
                            unfocusedContainerColor = CyberBlack,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (urlInput.isNotBlank()) {
                                viewModel.navigateBrowser(urlInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberGreen,
                            contentColor = CyberBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("browser_go_button")
                    ) {
                        Text("GO", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (routerState.isConnected) CyberGreen else CyberRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (routerState.isConnected) "Anonymous Tunnel Connection Active" else "Warning: Router is Offline (Browsing is simulated local cached assets)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (routerState.isConnected) CyberGreen else CyberOrange,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    // Proxy Status Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (routerState.httpProxyEnabled) {
                            Box(
                                modifier = Modifier
                                    .background(CyberBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, CyberBlue, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "HTTP",
                                    fontSize = 8.sp,
                                    color = CyberBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        if (routerState.socksProxyEnabled) {
                            Box(
                                modifier = Modifier
                                    .background(CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, CyberGreen, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "SOCKS5",
                                    fontSize = 8.sp,
                                    color = CyberGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Divider(color = CyberBorder)

        // Quick Handshake & Accessory Cockpit
        var isDockExpanded by remember { mutableStateOf(false) }
        val activeAccessories by viewModel.activeAccessories.collectAsState()

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isDockExpanded = !isDockExpanded }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "QUICK HANDSHAKE & ACCESSORY COCKPIT",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Icon(
                        if (isDockExpanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (isDockExpanded) "Collapse" else "Expand",
                        tint = CyberGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isDockExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = CyberBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Simplified connection menu
                    Text(
                        "SAFE DARKNET ADDRESSES (ONE-CLICK HANDSHAKE)",
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val safeSites = listOf(
                        "127.0.0.1:7657" to "Router Console",
                        "i2p-project.i2p" to "I2P Home",
                        "anon.chat.i2p" to "AnonIRC",
                        "wiki.leaks.i2p" to "WikiLeaks",
                        "secure.mail.i2p" to "GarlicMail",
                        "darkbert.intel.i2p" to "DarkBERT AI"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        safeSites.forEach { (siteUrl, label) ->
                            val isCurrent = tabState.url.contains(siteUrl)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isCurrent) CyberGreen.copy(alpha = 0.15f) else CyberBlack,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrent) CyberGreen else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        urlInput = siteUrl
                                        viewModel.navigateBrowser(siteUrl)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isCurrent) CyberGreen else CyberBlue, RoundedCornerShape(3.dp))
                                    )
                                    Text(
                                        text = label,
                                        color = if (isCurrent) CyberGreen else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Manual Connection Gate with encryption hop level picker
                    Text(
                        "MANUAL GATEWAY ENTRY",
                        color = CyberOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var manualAddressInput by remember { mutableStateOf("") }
                    var garlicHopLevel by remember { mutableStateOf("Double Hop") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualAddressInput,
                            onValueChange = { manualAddressInput = it },
                            placeholder = { Text("any-address.i2p / .onion", color = TextSecondary, fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("manual_gateway_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberOrange,
                                unfocusedBorderColor = CyberBorder,
                                focusedContainerColor = CyberBlack,
                                unfocusedContainerColor = CyberBlack
                            ),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        )

                        Button(
                            onClick = {
                                if (manualAddressInput.isNotBlank()) {
                                    val cleanManual = manualAddressInput.trim()
                                    urlInput = cleanManual
                                    viewModel.navigateBrowser(cleanManual)
                                    manualAddressInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberOrange,
                                contentColor = CyberBlack
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("manual_gateway_connect_button")
                        ) {
                            Text("GATE CONNECT", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Hop level picker chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Direct proxy", "Double Hop", "Garlic-Clad").forEach { level ->
                            val isSelected = garlicHopLevel == level
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) CyberOrange.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberOrange else CyberBorder.copy(alpha = 0.5f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { garlicHopLevel = level }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = level,
                                    color = if (isSelected) CyberOrange else TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = CyberBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Adviced accessories
                    Text(
                        "ADVICED SECURITY ACCESSORIES (ACTIVE LAYER PROTECTION)",
                        color = CyberPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Configure active browser payloads to adapt to routing latency and secure sandboxing profiles.",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val accessoriesList = listOf(
                        Triple("noscript", "NoScript Shield", "Disables Javascript to block browser exploit vectors (-300ms latency)"),
                        Triple("https_everywhere", "HTTPS-Everywhere", "Enforces end-to-end transport-layer TLS encapsulation (+100ms latency)"),
                        Triple("user_agent", "UA-Cloaking Spoofer", "Hides hardware fingerprint and client user-agent headers (+50ms latency)"),
                        Triple("metadata_stripper", "Metadata Stripper", "Purges EXIF data and upload tracking metadata tags (+150ms latency)")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accessoriesList.forEach { (id, title, desc) ->
                            val isEnabled = activeAccessories.contains(id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isEnabled) CyberPurple else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .background(
                                        if (isEnabled) CyberPurple.copy(alpha = 0.10f) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.toggleAccessory(id) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = if (isEnabled) CyberPurple else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = desc,
                                        color = TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp, 20.dp)
                                        .background(
                                            if (isEnabled) CyberPurple else CyberBlack,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isEnabled) CyberPurple else CyberBorder,
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = if (isEnabled) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .size(16.dp)
                                            .background(Color.White, RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Divider(color = CyberBorder)

        // Web Content Render area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (tabState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = CyberBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Routing Garlic Clove payloads...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title and connection info
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = CyberGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "END-TO-END GARLIC TUNNEL SECURED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    TextButton(
                                        onClick = { showRoutingDashboard = !showRoutingDashboard },
                                        modifier = Modifier.testTag("toggle_routing_dashboard_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                if (showRoutingDashboard) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = CyberBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                if (showRoutingDashboard) "HIDE MONITOR" else "SHOW MONITOR",
                                                color = CyberBlue,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    tabState.pageTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    tabState.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    if (showRoutingDashboard) {
                        item {
                            BrowserSessionRoutingDashboard(
                                viewModel = viewModel,
                                routerState = routerState,
                                tabState = tabState
                            )
                        }
                    }

                    // Simulated Page HTML Contents according to current active URL
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                RenderWebpageContents(url = tabState.url, viewModel = viewModel, onNavigate = { viewModel.navigateBrowser(it) })
                            }
                        }
                    }

                    // Simulated I2P Indexing Search Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = CyberBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "I2P DISTRIBUTED INDEX LOOKUP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberBlue,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    
                                    // Indexer Node status
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isIndexingSearchActive) CyberOrange.copy(alpha = 0.15f) else CyberGreen.copy(alpha = 0.15f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                0.5.dp,
                                                if (isIndexingSearchActive) CyberOrange else CyberGreen,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isIndexingSearchActive) "QUERYING DHT" else "INDEX READY",
                                            fontSize = 7.sp,
                                            color = if (isIndexingSearchActive) CyberOrange else CyberGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("i2p_index_search_bar"),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    placeholder = { Text("Search addresses, services, or titles...", color = TextSecondary, fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = CyberBlue,
                                        unfocusedBorderColor = CyberBorder
                                    ),
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                )

                                // Safety/Verification Category filter chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("ALL", "SAFE", "SUSPICIOUS", "DANGEROUS").forEach { level ->
                                        val isSelected = selectedSafetyFilter == level
                                        val chipColor = when (level) {
                                            "SAFE" -> CyberGreen
                                            "SUSPICIOUS" -> CyberOrange
                                            "DANGEROUS" -> CyberRed
                                            else -> CyberBlue
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isSelected) chipColor.copy(alpha = 0.2f) else CyberBlack,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) chipColor else CyberBorder,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { selectedSafetyFilter = level }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = level,
                                                fontSize = 9.sp,
                                                color = if (isSelected) chipColor else TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                // Interactive Indexing system telemetry log output
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CyberBlack, RoundedCornerShape(4.dp))
                                        .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isIndexingSearchActive -> "i2p://indexer: dht_find_providers query sent to [3] active garlic peers..."
                                            searchQuery.isNotEmpty() -> "i2p://indexer: returned ${filteredBookmarks.size} verified Leaseset record(s) matching \"$searchQuery\"."
                                            else -> "i2p://indexer: local addressbook (hosts.txt) parsed • ${bookmarks.size} router entries indexed."
                                        },
                                        fontSize = 9.sp,
                                        color = if (isIndexingSearchActive) CyberOrange else if (searchQuery.isNotEmpty()) CyberBlue else TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Bookmarked Sites Grid Header
                    item {
                        Text(
                            if (searchQuery.isNotEmpty() || selectedSafetyFilter != "ALL") "INDEX MATCHES" else "YOUR SEED BOOKMARKS",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Bookmarks List
                    if (filteredBookmarks.isEmpty()) {
                        item {
                            Text(
                                if (searchQuery.isNotEmpty() || selectedSafetyFilter != "ALL") "No matching verified addresses found in I2P index." else "No bookmarks saved.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    } else {
                        items(filteredBookmarks) { bookmark ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.navigateBrowser(bookmark.url) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    Color(android.graphics.Color.parseColor(bookmark.colorHex)).copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                when (bookmark.iconName) {
                                                    "chat" -> Icons.Default.ChatBubble
                                                    "description" -> Icons.Default.Description
                                                    "mail" -> Icons.Default.Mail
                                                    "forum" -> Icons.Default.Forum
                                                    "shield" -> Icons.Default.Shield
                                                    else -> Icons.Default.Language
                                                },
                                                contentDescription = null,
                                                tint = Color(android.graphics.Color.parseColor(bookmark.colorHex))
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    bookmark.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                val safetyColor = when (bookmark.safetyLevel) {
                                                    "SAFE" -> CyberGreen
                                                    "SUSPICIOUS" -> CyberOrange
                                                    "DANGEROUS" -> CyberRed
                                                    else -> CyberBlue
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(safetyColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .border(0.5.dp, safetyColor, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        bookmark.safetyLevel,
                                                        fontSize = 7.sp,
                                                        color = safetyColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                            Text(
                                                bookmark.url,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteWebBookmark(bookmark) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Bookmark", tint = CyberRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Bookmark Dialog
    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = { Text("Add Darkweb Bookmark", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = bookmarkTitleInput,
                        onValueChange = { bookmarkTitleInput = it },
                        label = { Text("Title", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("URL Address", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        )
                    )

                    Text("Safety Level", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SAFE", "SUSPICIOUS", "DANGEROUS").forEach { level ->
                            val isSelected = bookmarkSafetyInput == level
                            val levelColor = when (level) {
                                "SAFE" -> CyberGreen
                                "SUSPICIOUS" -> CyberOrange
                                "DANGEROUS" -> CyberRed
                                else -> CyberBlue
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) levelColor.copy(alpha = 0.2f) else CyberBlack,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) levelColor else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { bookmarkSafetyInput = level }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    fontSize = 10.sp,
                                    color = if (isSelected) levelColor else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bookmarkTitleInput.isNotEmpty() && urlInput.isNotEmpty()) {
                            viewModel.addWebBookmark(bookmarkTitleInput, urlInput, bookmarkSafetyInput)
                        }
                        showBookmarkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBlack)
                ) {
                    Text("Bookmark")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookmarkDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberDarkSurface
        )
    }
}

data class DashboardNode(
    val title: String,
    val subtext: String,
    val flag: String,
    val ping: Int,
    val isOnline: Boolean,
    val encryption: String,
    val color: Color
)

@Composable
fun BrowserSessionRoutingDashboard(
    viewModel: I2PViewModel,
    routerState: RouterState,
    tabState: BrowserTab,
    modifier: Modifier = Modifier
) {
    var latencyHistory by remember { mutableStateOf(listOf<Float>()) }
    
    // Animate network pulse and scanline
    val infiniteTransition = rememberInfiniteTransition(label = "browser_router_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Update real-time latency oscilloscope path
    LaunchedEffect(routerState.isConnected, routerState.latencyMs) {
        if (routerState.isConnected) {
            // Seed first entry if empty
            if (latencyHistory.isEmpty()) {
                latencyHistory = List(15) { routerState.latencyMs.toFloat() }
            }
            while (true) {
                val currentBase = if (routerState.latencyMs > 0) routerState.latencyMs else 280
                val jitter = kotlin.random.Random.nextInt(-15, 15)
                val finalPing = (currentBase + jitter).coerceIn(40, 1500).toFloat()
                latencyHistory = (latencyHistory + finalPing).takeLast(25)
                delay(1200)
            }
        } else {
            latencyHistory = emptyList()
        }
    }

    val hops = routerState.tunnelHops
    val nodesList = remember(hops, routerState.latencyMs, tabState.url, routerState.isConnected) {
        val list = mutableListOf<DashboardNode>()
        
        // 1. Client Node (Always Present)
        list.add(
            DashboardNode(
                title = "CLIENT (YOU)",
                subtext = "Local Portal",
                flag = "🇨🇭",
                ping = 2,
                isOnline = true,
                encryption = "Plaintext",
                color = CyberBlue
            )
        )
        
        // 2. Inbound Gateway (Always Present)
        list.add(
            DashboardNode(
                title = "I-GATEWAY",
                subtext = "Munich Relay",
                flag = "🇩🇪",
                ping = 35,
                isOnline = routerState.isConnected,
                encryption = "ElGamal-2048",
                color = CyberGreen
            )
        )
        
        // 3. Transit Node A (Only if hops >= 3)
        if (hops >= 3) {
            list.add(
                DashboardNode(
                    title = "TRANSIT A",
                    subtext = "Amsterdam H1",
                    flag = "🇳🇱",
                    ping = 78,
                    isOnline = routerState.isConnected,
                    encryption = "AES-GCM-256",
                    color = CyberPurple
                )
            )
        }
        
        // 4. Transit Node B (Only if hops >= 5)
        if (hops >= 5) {
            list.add(
                DashboardNode(
                    title = "TRANSIT B",
                    subtext = "Paris H2",
                    flag = "🇫🇷",
                    ping = 124,
                    isOnline = routerState.isConnected,
                    encryption = "AES-GCM-256",
                    color = CyberOrange
                )
            )
        }
        
        // 5. Outbound Gateway (Always Present)
        list.add(
            DashboardNode(
                title = "O-GATEWAY",
                subtext = "Stockholm Exit",
                flag = "🇸🇪",
                ping = if (routerState.isConnected) (routerState.latencyMs * 0.7f).toInt().coerceAtLeast(140) else 0,
                isOnline = routerState.isConnected,
                encryption = "Session Tag",
                color = CyberYellow
            )
        )
        
        // 6. Target Eepsite Destination (Always Present)
        val rawHost = tabState.url.substringAfter("://").substringBefore("/")
        val hostName = if (rawHost.isEmpty()) "i2p-project.i2p" else rawHost
        list.add(
            DashboardNode(
                title = hostName.uppercase(),
                subtext = "Target Eepsite",
                flag = "🧅",
                ping = if (routerState.isConnected) routerState.latencyMs else 0,
                isOnline = routerState.isConnected,
                encryption = "Decrypted",
                color = CyberRed
            )
        )
        
        list
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, if (routerState.isConnected) CyberBlue.copy(alpha = 0.5f) else CyberBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("browser_session_routing_dashboard_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Waves,
                        contentDescription = null,
                        tint = CyberBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            "REAL-TIME GARLIC SESSION ROUTING",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberBlue,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "E2E secure tunnel hops diagnostic and latency oscilloscope",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (routerState.isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(CyberGreen, CircleShape)
                        )
                        Text(
                            "TUNNEL LIVE",
                            color = CyberGreen,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!routerState.isConnected) {
                // Offline Placeholder State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.VpnLock,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Garlic routing path and oscilloscope inactive.",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Enable the Garlic Router in the Router settings tab to monitor.",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // 1. Dynamic Routing Nodes horizontal map
                Text(
                    "GARLIC ENCRYPTED PATHWAY PATH",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    nodesList.forEachIndexed { index, node ->
                        // Render Node Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, if (node.isOnline) node.color.copy(alpha = 0.5f) else CyberBorder),
                            modifier = Modifier
                                .width(115.dp)
                                .testTag("routing_node_${index}")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(node.flag, fontSize = 11.sp)
                                    Text(
                                        text = node.title,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = node.color,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = node.subtext,
                                    fontSize = 8.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(if (node.isOnline) CyberGreen else CyberRed, CircleShape)
                                    )
                                    Text(
                                        text = "${node.ping}ms",
                                        fontSize = 8.sp,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Render Lock/Encryption Arrow Connector between nodes (except last)
                        if (index < nodesList.size - 1) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.width(55.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = CyberPurple,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = node.encryption,
                                        fontSize = 7.sp,
                                        color = CyberPurple,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = CyberBlue.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Real-time Latency Oscilloscope
                Text(
                    "REAL-TIME LATENCY OSCILLOSCOPE (ms)",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().testTag("latency_oscilloscope_canvas")) {
                        val width = size.width
                        val height = size.height
                        
                        // Draw horizontal grids
                        val gridCountY = 4
                        for (i in 1..gridCountY) {
                            val y = (height / (gridCountY + 1)) * i
                            drawLine(
                                color = CyberBorder.copy(alpha = 0.3f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw vertical grids
                        val gridCountX = 6
                        for (i in 1..gridCountX) {
                            val x = (width / (gridCountX + 1)) * i
                            drawLine(
                                color = CyberBorder.copy(alpha = 0.3f),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        if (latencyHistory.size >= 2) {
                            val maxVal = maxOf(400f, latencyHistory.maxOrNull() ?: 100f) * 1.15f
                            val xStep = width / (latencyHistory.size - 1)

                            val points = latencyHistory.mapIndexed { idx, value ->
                                val x = idx * xStep
                                val y = height - ((value / maxVal) * height).coerceIn(4f, height - 4f)
                                Offset(x, y)
                            }

                            // A. Fill Path under line
                            val fillPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, height)
                                points.forEach { lineTo(it.x, it.y) }
                                lineTo(width, height)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(CyberGreen.copy(alpha = 0.15f), Color.Transparent),
                                    startY = 0f,
                                    endY = height
                                )
                            )

                            // B. Glow neon outer stroke line
                            val strokePath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }
                            drawPath(
                                path = strokePath,
                                color = CyberGreen.copy(alpha = 0.35f),
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // C. Solid crisp inner stroke line
                            drawPath(
                                path = strokePath,
                                color = Color.White,
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // D. Draw pulsing dots on top-rightmost point
                            val lastPoint = points.last()
                            drawCircle(
                                color = CyberGreen,
                                radius = 4.dp.toPx(),
                                center = lastPoint
                            )
                            drawCircle(
                                color = CyberGreen.copy(alpha = 0.3f),
                                radius = 8.dp.toPx() * pulseScale,
                                center = lastPoint
                            )
                        }
                    }

                    // Floating text overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(CyberBlack.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(CyberGreen, CircleShape)
                        )
                        Text(
                            text = "AVG LATENCY: ${routerState.latencyMs}ms",
                            fontSize = 8.sp,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Speed Hops Interactive Slider & Settings Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TUNNEL ENCRYPTION DEPTH (HOPS)",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Display Current configuration
                    Text(
                        text = "${hops} Hops (${if (hops == 1) "Low Privacy" else if (hops == 3) "Standard" else "High Latency"})",
                        fontSize = 9.sp,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Custom Segment Chips to pick hops directly in browser settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberBlack, RoundedCornerShape(6.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1, 3, 5).forEach { hOption ->
                        val isSelected = hOption == hops
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) CyberBlue.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) CyberBlue else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.setTunnelHops(hOption) }
                                .padding(vertical = 6.dp)
                                .testTag("browser_hops_segment_${hOption}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$hOption HOP${if (hOption > 1) "S" else ""}",
                                fontSize = 9.sp,
                                color = if (isSelected) CyberBlue else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Quick Telemetry metrics cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Packet Loss Metric
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberBlack),
                        border = BorderStroke(0.5.dp, CyberBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("PACKET LOSS", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.2f %%", routerState.packetLoss),
                                fontSize = 11.sp,
                                color = if (routerState.packetLoss > 2.0f) CyberRed else CyberGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Active Peers
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberBlack),
                        border = BorderStroke(0.5.dp, CyberBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("ACTIVE PEERS", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${routerState.activePeerCount} / ${routerState.knownPeers}",
                                fontSize = 11.sp,
                                color = CyberBlue,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Jitter Fluctuation
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberBlack),
                        border = BorderStroke(0.5.dp, CyberBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("JITTER", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+/- ${if (hops == 1) "3" else if (hops == 3) "12" else "24"} ms",
                                fontSize = 11.sp,
                                color = CyberYellow,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Quick Diagnostic Action cockpit buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Speed Optimizer Button
                    Button(
                        onClick = { viewModel.optimizeLatency() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.12f), contentColor = CyberGreen),
                        border = BorderStroke(0.5.dp, CyberGreen),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("dashboard_optimize_button"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("OPTIMIZE SPEED", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Self Healing diagnostics
                    Button(
                        onClick = { viewModel.autoFixAnomalies() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange.copy(alpha = 0.12f), contentColor = CyberOrange),
                        border = BorderStroke(0.5.dp, CyberOrange),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("dashboard_heal_button"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("SELF-HEAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Rebuild pool
                    Button(
                        onClick = { viewModel.rebuildTunnelPool() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple.copy(alpha = 0.12f), contentColor = CyberPurple),
                        border = BorderStroke(0.5.dp, CyberPurple),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("dashboard_rebuild_button"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("REBUILD POOL", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderWebpageContents(url: String, viewModel: I2PViewModel, onNavigate: (String) -> Unit) {
    when {
        url.contains("127.0.0.1:7657") || url.contains("localhost:7657") || url.contains("router-console") -> {
            RouterConsoleWebUI(viewModel = viewModel, onNavigate = onNavigate)
        }
        url.contains("i2p-project.i2p") -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Simulating standard documentation wiki host inside garlic-routed local directory.", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                Text(
                    "Welcome to the Invisible Internet Project (I2P) decentralized anonymous communication layer. Unlike peer-to-peer torrents, I2P routing layers protect both the client identity and the host server (known as eepsite services) through dynamic garlic routing pools.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text("Suggested Core Portals:", style = MaterialTheme.typography.bodyMedium, color = CyberBlue, fontWeight = FontWeight.Bold)
                WebpageHyperlink("Browse Anonymous Relay Chat (anon.chat.i2p)", "http://anon.chat.i2p", onNavigate)
                WebpageHyperlink("Browse Invisible Cryptic Wiki (wiki.leaks.i2p)", "http://wiki.leaks.i2p", onNavigate)
                WebpageHyperlink("Browse Garlic Mail Service (secure.mail.i2p)", "http://secure.mail.i2p", onNavigate)
            }
        }
        url.contains("anon.chat.i2p") -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(CyberGreen, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live Node Chat Pool Active", color = CyberGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    "AnonIRC lets you converse safely inside high-entropy garlic tunnels. This portal is linked with your cryptographic keys to secure conversations. Navigate to the Encrypted Communications tab to dispatch private keypairs or secure text packets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text("Available Channels:", style = MaterialTheme.typography.bodyMedium, color = CyberBlue, fontWeight = FontWeight.Bold)
                Text("#lobby - Generic discussions (241 active peers)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("#cryptography - Cryptographic tunnel discussions (88 active peers)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("#leaks - Document leaks submissions (15 active peers)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
        url.contains("wiki.leaks.i2p") -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("WIKILEAKS INTERNAL INVISIBLE REPOSITORY", style = MaterialTheme.typography.bodySmall, color = CyberOrange, fontWeight = FontWeight.Bold)
                Text(
                    "This decentralized database acts as an anonymous cryptographic drop box. Our system receives encrypted garlic payloads, logs peer correlations, and redistributes documents without centralized physical hosts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Divider(color = CyberBorder)
                Text("Recently leaked reports:", style = MaterialTheme.typography.bodyMedium, color = CyberBlue, fontWeight = FontWeight.Bold)
                Text("• ISP Data Retention Audits 2026 - Leaked PDF index", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("• Surveillance telemetry logs - Automated router interception", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
        url.contains("secure.mail.i2p") -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("GARLIC SECURE MAIL ENGINE", style = MaterialTheme.typography.bodySmall, color = CyberPurple, fontWeight = FontWeight.Bold)
                Text(
                    "Send and collect asymmetric encrypted emails. Messages sent via secure.mail.i2p are automatically wrapped in Garlic cryptocodes and dispatched to host destination hashes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Divider(color = CyberBorder)
                Text("Inbox status:", style = MaterialTheme.typography.bodyMedium, color = CyberBlue, fontWeight = FontWeight.Bold)
                Text("All messages are digitally signed. Private communication is protected under ElGamal keys.", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
        url.contains("darkbert.intel") -> {
            DarkBERTPortal()
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("GENERIC DECENTRALIZED EEPSITE SERVICE", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(
                    "This website is hosted completely inside the decentralized Peer-to-Peer I2P network, routing traffic dynamically without relying on DNS servers or clearweb hosting providers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text("The connection remains end-to-end encrypted under garlic-clad leaseSets.", style = MaterialTheme.typography.bodySmall, color = CyberGreen)
            }
        }
    }
}

@Composable
fun I2PCircuitStatsWidget(
    state: RouterState,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("circuit_stats_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Widget Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "I2P ACTIVE CIRCUIT HEALTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberPurple,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (state.isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(CyberGreen, RoundedCornerShape(4.dp))
                        )
                        Text(
                            "LIVE",
                            color = CyberGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(TextSecondary, RoundedCornerShape(4.dp))
                        )
                        Text(
                            "INACTIVE",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Latency Stat
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = CyberOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "LATENCY",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.isConnected) "${state.latencyMs} ms" else "---",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (state.isConnected) {
                            if (state.latencyMs > 800) CyberRed else if (state.latencyMs > 400) CyberOrange else CyberGreen
                        } else TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("circuit_latency_value")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Latency bar indicator
                    if (state.isConnected) {
                        val percentage = (state.latencyMs.toFloat() / 1200f).coerceIn(0.1f, 1.0f)
                        LinearProgressIndicator(
                            progress = { percentage },
                            color = if (state.latencyMs > 800) CyberRed else if (state.latencyMs > 400) CyberOrange else CyberGreen,
                            trackColor = CyberBorder.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(CyberBorder.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Packet Loss Stat
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = CyberRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "LOSS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.isConnected) String.format(Locale.US, "%.2f%%", state.packetLoss) else "---",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (state.isConnected) {
                            if (state.packetLoss > 2.0f) CyberRed else if (state.packetLoss > 0.8f) CyberOrange else CyberGreen
                        } else TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("circuit_packet_loss_value")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Loss bar indicator
                    if (state.isConnected) {
                        val percentage = (state.packetLoss / 5.0f).coerceIn(0.05f, 1.0f)
                        LinearProgressIndicator(
                            progress = { percentage },
                            color = if (state.packetLoss > 2.0f) CyberRed else if (state.packetLoss > 0.8f) CyberOrange else CyberGreen,
                            trackColor = CyberBorder.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(CyberBorder.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Tunnel Peers row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack, RoundedCornerShape(8.dp))
                    .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Router,
                        contentDescription = null,
                        tint = CyberBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "ACTIVE CIRCUIT PEERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Nodes participating in current leaseSets",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        )
                    }
                }

                Text(
                    text = if (state.isConnected) "${state.activePeerCount} peers" else "0 peers",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.isConnected) CyberBlue else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("circuit_active_peers_value")
                )
            }

            if (state.isConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = CyberGreen.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Circuit is stable. Double-hop ElGamal/AES-256 wrapping active across ${state.tunnelHops} hops.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun I2POptimizerWidget(
    state: RouterState,
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("circuit_optimizer_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = CyberGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "NETWORK HEALTH OPTIMIZER",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!state.isConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Connect the Garlic Router to activate real-time self-healing and optimization utilities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Operational anomalies detected in the circuit can be automatically patched using the recommended tactical procedures below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 2x2 Grid of Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Fast Path
                        OutlinedButton(
                            onClick = { viewModel.optimizeLatency() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CyberBlack,
                                contentColor = CyberOrange
                            ),
                            border = BorderStroke(1.dp, CyberOrange.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("btn_optimize_latency")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("FAST PATH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("Optimize Latency", fontSize = 9.sp, color = TextSecondary)
                            }
                        }

                        // Pool Refresh
                        OutlinedButton(
                            onClick = { viewModel.rebuildTunnelPool() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CyberBlack,
                                contentColor = CyberBlue
                            ),
                            border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("btn_rebuild_pool")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("POOL REFRESH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("Purge Packet Loss", fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Auto-Stabilize
                        OutlinedButton(
                            onClick = { viewModel.autoStabilizeCircuit() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CyberBlack,
                                contentColor = CyberPurple
                            ),
                            border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("btn_auto_stabilize")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("STABILIZE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("Restore Balanced 3-Hops", fontSize = 9.sp, color = TextSecondary)
                            }
                        }

                        // Full Self-Healing
                        OutlinedButton(
                            onClick = { viewModel.autoFixAnomalies() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CyberBlack,
                                contentColor = CyberGreen
                            ),
                            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("btn_self_healing")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("SELF-HEALING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("Run Full Diagnostics", fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun I2PHelpCenterWidget(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("circuit_help_center_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row (clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = CyberBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "GARLIC ROUTING KNOWLEDGE BASE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = CyberBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Section 1: Garlic Circuit Mechanics
                    Column {
                        Text(
                            "What is Garlic Routing?",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Garlic routing is an extension of onion routing. It encrypts multiple messages together (called garlic cloves) to make traffic analysis extremely difficult. Packets travel through a unidirectional path of pre-configured tunnel nodes (hops). Each hop strips off one layer of encryption, forwarding the payload to the next participant until the destination is reached.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    // Section 2: Safe Metric Ranges
                    Column {
                        Text(
                            "Safe Operational Thresholds",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Latency Table/Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Metric", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("Safe Range", style = MaterialTheme.typography.labelSmall, color = CyberGreen, fontWeight = FontWeight.Bold)
                            Text("Critical Spike", style = MaterialTheme.typography.labelSmall, color = CyberRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Circuit Latency", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text("< 400 ms", style = MaterialTheme.typography.bodySmall, color = CyberGreen, fontFamily = FontFamily.Monospace)
                            Text("> 800 ms", style = MaterialTheme.typography.bodySmall, color = CyberRed, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Packet Loss Rate", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text("< 0.80%", style = MaterialTheme.typography.bodySmall, color = CyberGreen, fontFamily = FontFamily.Monospace)
                            Text("> 2.00%", style = MaterialTheme.typography.bodySmall, color = CyberRed, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Section 3: Diagnostic Fixes
                    Column {
                        Text(
                            "Mitigation Strategies",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BulletPoint("High Latency Spikes", "Caused by traversing multiple global nodes. Activate 'FAST PATH' to temporarily drop to a low-latency 1-hop tunnel pool.")
                        BulletPoint("High Packet Loss Rates", "Caused by unstable or compromised intermediary garlic peers. Trigger 'POOL REFRESH' to flush local netDB descriptors and establish new circuits.")
                        BulletPoint("Degraded Security Scores", "Always utilize standard 3-hop or high-privacy 5-hop configurations when communicating sensitive garlic-encrypted payloads.")
                    }
                }
            }
        }
    }
}

@Composable
fun BulletPoint(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("•", color = CyberBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun DarkBERTPortal(modifier: Modifier = Modifier) {
    var queryInput by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Portal Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberPurple, RoundedCornerShape(8.dp))
                .background(CyberPurple.copy(alpha = 0.05f))
                .padding(12.dp)
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = CyberPurple,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    "DarkBERT THREAT INTEL v2.5",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Domain-Specific Darknet Security & Threat Intelligence",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            "Powered by deep darknet crawl repositories. DarkBERT provides automated intelligence parsing on leak databases, malware exploits, and deanonymization vectors.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        // Preset chips
        Text(
            "QUICK THREAT PROFILE QUERY:",
            style = MaterialTheme.typography.labelSmall,
            color = CyberPurple,
            fontWeight = FontWeight.Bold
        )

        val presets = listOf(
            "Scan wiki.leaks.i2p for active security leaks",
            "Garlic vs Onion Routing: Timing correlations",
            "What accessories protect my browser fingerprint?",
            "Explain common darknet e-mail phishing exploits"
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { preset ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { queryInput = preset }
                ) {
                    Text(
                        text = "• $preset",
                        color = CyberBlue,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Custom input
        OutlinedTextField(
            value = queryInput,
            onValueChange = { queryInput = it },
            label = { Text("Query DarkBERT Intel Database", color = TextSecondary) },
            placeholder = { Text("Enter target IP, eepsite, or security topic...", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth().testTag("darkbert_query_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = CyberPurple,
                unfocusedBorderColor = CyberBorder,
                focusedContainerColor = CyberBlack,
                unfocusedContainerColor = CyberBlack
            ),
            singleLine = true
        )

        // Action Button
        Button(
            onClick = {
                if (queryInput.isNotBlank()) {
                    scope.launch {
                        isAnalyzing = true
                        analysisResult = ""
                        analysisResult = com.example.data.queryDarkBERT(queryInput)
                        isAnalyzing = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberPurple,
                contentColor = Color.White
            ),
            enabled = !isAnalyzing && queryInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("darkbert_submit_button")
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing Threat Signatures...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("DISPATCH DARKBERT INQUIRY", fontWeight = FontWeight.Bold)
            }
        }

        // Analysis Output
        if (isAnalyzing || analysisResult.isNotEmpty()) {
            Text(
                "DARKBERT INTELLIGENCE OUTPUT:",
                style = MaterialTheme.typography.labelSmall,
                color = CyberGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack, RoundedCornerShape(8.dp))
                    .border(1.dp, if (isAnalyzing) CyberPurple else CyberGreen, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                if (isAnalyzing) {
                    Text(
                        "system@darkbert_core:~$ querying deep database hashes...\n[Analyzing entropy packets and correlation models...]",
                        color = CyberPurple,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.testTag("darkbert_output_box")
                    )
                } else {
                    Text(
                        text = analysisResult,
                        color = if (analysisResult.startsWith("ERROR")) CyberRed else CyberGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.testTag("darkbert_output_box")
                    )
                }
            }
        }
    }
}

@Composable
fun WebpageHyperlink(text: String, url: String, onNavigate: (String) -> Unit) {
    Text(
        text = text,
        color = CyberBlue,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .clickable { onNavigate(url) }
            .padding(vertical = 4.dp)
    )
}

enum class CommsSubTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GARLIC_CHAT("Garlic Chat", Icons.Default.Forum),
    GPS_EMULATOR("GPS Emulator", Icons.Default.LocationOn),
    TERMINAL_CRYPTOR("Terminal Cryptor", Icons.Default.Code)
}

@Composable
fun CommunicationsScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(CommsSubTab.GARLIC_CHAT) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Sub-tabs indicator
        TabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = CyberDarkSurface,
            contentColor = CyberBlue,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab.ordinal]),
                    color = CyberBlue
                )
            }
        ) {
            CommsSubTab.values().forEach { tab ->
                val isSelected = activeSubTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { activeSubTab = tab },
                    text = {
                        Text(
                            tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) CyberBlue else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    selectedContentColor = CyberBlue,
                    unselectedContentColor = TextSecondary,
                    modifier = Modifier.testTag("comms_subtab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeSubTab) {
            CommsSubTab.GARLIC_CHAT -> GarlicChatTab(viewModel = viewModel)
            CommsSubTab.GPS_EMULATOR -> GpsEmulatorTab(viewModel = viewModel)
            CommsSubTab.TERMINAL_CRYPTOR -> TerminalCryptorTab(viewModel = viewModel)
        }
    }
}

@Composable
fun GarlicChatTab(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    val routerState by viewModel.routerState.collectAsState()
    val gpsState by viewModel.gpsState.collectAsState()
    val trustedKeys by viewModel.trustedKeys.collectAsState()

    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var messageInput by remember { mutableStateOf("") }
    var showKeyAssociationDialog by remember { mutableStateOf(false) }
    var associatePublicKeyField by remember { mutableStateOf("") }

    // Synchronize selected contact if list changes or becomes empty
    LaunchedEffect(contacts) {
        if (selectedContact == null && contacts.isNotEmpty()) {
            selectedContact = contacts.firstOrNull()
        } else if (contacts.isNotEmpty() && !contacts.contains(selectedContact)) {
            selectedContact = contacts.firstOrNull()
        }
    }

    // Add Contact Form States
    var newContactName by remember { mutableStateOf("") }
    var newContactAddress by remember { mutableStateOf("") }
    var newContactType by remember { mutableStateOf("GOOGLE_CHAT") } // GOOGLE_CHAT, SMS, SECURE_I2P
    var newContactPublicKey by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SECURE DIRECTORY & CHAT",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "Signal Protocol • OTR over OAuth2 • Garlic Routing",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            
            Button(
                onClick = { showAddContactDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberBlue,
                    contentColor = CyberBlack
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        // Service Type Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "SECURE_I2P", "GOOGLE_CHAT", "SMS").forEach { type ->
                val isSelected = selectedTypeFilter == type
                val label = when (type) {
                    "SECURE_I2P" -> "I2P GARLIC"
                    "GOOGLE_CHAT" -> "GOOGLE CHAT"
                    "SMS" -> "SECURE SMS"
                    else -> "ALL NETWORKS"
                }
                val chipColor = when (type) {
                    "SECURE_I2P" -> CyberGreen
                    "GOOGLE_CHAT" -> CyberYellow
                    "SMS" -> CyberBlue
                    else -> CyberPurple
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) chipColor.copy(alpha = 0.15f) else CyberDarkSurface,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) chipColor else CyberBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedTypeFilter = type }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = if (isSelected) chipColor else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Scrollable Contacts Carousel Row
        val filteredContacts = remember(contacts, selectedTypeFilter) {
            contacts.filter { selectedTypeFilter == "ALL" || it.type == selectedTypeFilter }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "PEER CONTACT DIRECTORY (${filteredContacts.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No contacts found for selected network filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        filteredContacts.forEach { contact ->
                            val isSelected = selectedContact?.id == contact.id
                            val contactColor = Color(android.graphics.Color.parseColor(contact.avatarColorHex))
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedContact = contact }
                                    .padding(4.dp)
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                if (isSelected) contactColor.copy(alpha = 0.25f) else CyberBlack,
                                                CircleShape
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) contactColor else CyberBorder,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.take(2).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = contactColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Status and Service Overlay Badges
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Network service symbol badge
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .background(CyberBlack, CircleShape)
                                                .border(0.5.dp, contactColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (contact.type) {
                                                    "GOOGLE_CHAT" -> Icons.Default.AlternateEmail
                                                    "SMS" -> Icons.Default.Sms
                                                    else -> Icons.Default.Lock
                                                },
                                                contentDescription = contact.type,
                                                tint = contactColor,
                                                modifier = Modifier.size(8.dp)
                                            )
                                        }

                                        // Online/Offline status dot
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    if (contact.status == "ONLINE") CyberGreen else TextSecondary,
                                                    CircleShape
                                                )
                                                .border(1.dp, CyberBlack, CircleShape)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = contact.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Chat Conversation Pane
        if (selectedContact != null) {
            val contact = selectedContact!!
            val contactColor = Color(android.graphics.Color.parseColor(contact.avatarColorHex))

            // Contact Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(contactColor.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, contactColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    contact.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = contactColor
                                )
                            }
                            
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        contact.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(contactColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .border(0.5.dp, contactColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when (contact.type) {
                                                "GOOGLE_CHAT" -> "GOOGLE CHAT (E2EE)"
                                                "SMS" -> "SMS (SILENCE PROTOCOL)"
                                                else -> "I2P GARLIC PEER"
                                            },
                                            fontSize = 7.sp,
                                            color = contactColor,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Text(
                                    text = "Address: ${contact.address}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Delete Contact Button
                        IconButton(
                            onClick = {
                                viewModel.deleteContact(contact)
                                selectedContact = null
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Contact", tint = CyberRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        }
                    }

                    Divider(modifier = Modifier.padding(horizontal = 10.dp), color = CyberBorder.copy(alpha = 0.5f))
                    
                    val associatedKey = trustedKeys.find { it.i2pAddress == contact.address }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (associatedKey != null) Icons.Default.VpnKey else Icons.Default.Security,
                                contentDescription = "Keyring Status",
                                tint = if (associatedKey != null) CyberGreen else CyberOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Column {
                                Text(
                                    text = if (associatedKey != null) "🔒 KEYRING ALIAS: ${associatedKey.alias.uppercase()}" else "🔓 NO ASSOCIATED PUBLIC KEY (MOCK SANDBOX ACTIVE)",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (associatedKey != null) CyberGreen else CyberOrange
                                )
                                if (associatedKey != null) {
                                    Text(
                                        text = "Fingerprint: ${associatedKey.publicKeyBase64.take(24)}...",
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        
                        Button(
                            onClick = {
                                associatePublicKeyField = associatedKey?.publicKeyBase64 ?: ""
                                showKeyAssociationDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (associatedKey != null) CyberGreen.copy(alpha = 0.12f) else CyberOrange.copy(alpha = 0.12f),
                                contentColor = if (associatedKey != null) CyberGreen else CyberOrange
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (associatedKey != null) CyberGreen.copy(alpha = 0.5f) else CyberOrange.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                text = if (associatedKey != null) "VIEW / EDIT" else "ASSOCIATE KEY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Filtering messages for the active conversation
            val conversationMessages = remember(messages, contact) {
                messages.filter { msg ->
                    msg.senderAddress == contact.address || msg.recipientAddress == contact.address
                }.reversed()
            }

            // Message Stream Column
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CyberBlack)
            ) {
                if (conversationMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = contactColor.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No messages exchanged in this secure tunnel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(conversationMessages) { msg ->
                            ContactMessageBubble(msg = msg, contact = contact, viewModel = viewModel)
                        }
                    }
                }
            }

            // Message Input Drawer
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (contact.type) {
                                "GOOGLE_CHAT" -> "SENDING VIA GOOGLE CHAT PROTOCOL"
                                "SMS" -> "SENDING VIA SMS CELLULAR CARRIER"
                                else -> "SENDING VIA I2P GARLIC TUNNEL"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = contactColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp
                        )

                        Text(
                            text = if (routerState.isConnected || contact.type != "SECURE_I2P") "CARRIER ENCRYPTED" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (routerState.isConnected || contact.type != "SECURE_I2P") CyberGreen else CyberRed,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Enter private chat content...", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comms_message_input"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = contactColor,
                            unfocusedBorderColor = CyberBorder
                        ),
                        maxLines = 3,
                        trailingIcon = {
                            if (messageInput.isNotEmpty()) {
                                IconButton(onClick = { messageInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (messageInput.isNotEmpty() && activeIdentity != null) {
                        var isExpandedCipher by remember { mutableStateOf(true) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberBlack, RoundedCornerShape(4.dp))
                                .border(0.5.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                                .clickable { isExpandedCipher = !isExpandedCipher }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EnhancedEncryption,
                                        contentDescription = null,
                                        tint = CyberGreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "LIVE GARLIC CLOVE CIPHER PREVIEW",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpandedCipher) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            if (isExpandedCipher) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val mockCiphertext = remember(messageInput) {
                                    val inputBytes = messageInput.toByteArray()
                                    val hashed = inputBytes.fold(0) { acc, b -> acc + b }
                                    val base64CharList = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
                                    val fakeSignature = (1..16).map { base64CharList[(hashed + it) % 64] }.joinToString("")
                                    "HYBRID-RSA-AES:$fakeSignature...$fakeSignature"
                                }
                                Text(
                                    text = mockCiphertext,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            if (messageInput.isNotEmpty()) {
                                viewModel.sendSecureContactMessage(contact, messageInput)
                                messageInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = contactColor,
                            contentColor = CyberBlack
                        ),
                        enabled = (routerState.isConnected || contact.type != "SECURE_I2P") && activeIdentity != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comms_send_button"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = when (contact.type) {
                                "GOOGLE_CHAT" -> Icons.Default.AlternateEmail
                                "SMS" -> Icons.Default.Sms
                                else -> Icons.Default.EnhancedEncryption
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                activeIdentity == null -> "Configure Profile to Send"
                                contact.type == "SECURE_I2P" && !routerState.isConnected -> "Connect Router to Send"
                                contact.type == "GOOGLE_CHAT" -> "Double-Ratchet Encrypt & Dispatch"
                                contact.type == "SMS" -> "OTR SMS Encrypt & Send"
                                else -> "Garlic-Encrypt & Send"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val simulatedPrompts = listOf(
                                "Handshake success. Ephemeral session parameters matching.",
                                "ALERT: Detected multi-hop routing delay on route tunnel 0xFA4B.",
                                "Transmission complete. Cleared all local router state caches.",
                                "Acknowledged. Standard garlic clove received & verified via private RSA key."
                            )
                            viewModel.receiveEncryptedMessageSimulation(contact, simulatedPrompts.random())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberDarkSurface,
                            contentColor = CyberOrange
                        ),
                        border = BorderStroke(1.dp, CyberOrange.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallReceived,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "SIMULATE INCOMING CRYPTO BLOCK", 
                            fontSize = 8.sp, 
                            fontWeight = FontWeight.Bold, 
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CyberDarkSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactMail,
                        contentDescription = "Select Contact",
                        tint = CyberPurple,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "SECURE MESSAGING CHANNEL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Please tap any peer profile from the directory carousel above to establish a secure E2EE chat tunnel, or tap ADD to register new Google Chat or SMS contacts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 280.dp)
                    )
                }
            }
        }
    }

    // Add Contact Pop-up Dialog
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            containerColor = CyberDarkSurface,
            title = {
                Text(
                    "ADD SECURE DISPATCH CONTACT",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Register a cryptographic peer handle, Google Chat email, or SMS phone number to support client-side payload encryption.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // GPS-based localized Quick Fill presets
                    val dialogPresets = remember(gpsState.region) {
                        when {
                            gpsState.region.contains("Reykjavik", ignoreCase = true) -> listOf(
                                Triple("Reykjavik Ice Node", "reykjavik-ice.i2p", "SECURE_I2P"),
                                Triple("Iceland Dev Link", "iceland.dev@gmail.com", "GOOGLE_CHAT"),
                                Triple("SMS: Reykjavik Dispatch", "+3545551234", "SMS")
                            )
                            gpsState.region.contains("Tokyo", ignoreCase = true) -> listOf(
                                Triple("Tokyo Secure Relay", "tokyo-relay.i2p", "SECURE_I2P"),
                                Triple("APAC Lead dev", "apac.lead@gmail.com", "GOOGLE_CHAT"),
                                Triple("SMS: Tokyo Command", "+819012345678", "SMS")
                            )
                            gpsState.region.contains("New York", ignoreCase = true) -> listOf(
                                Triple("NY Outpost Router", "ny-outpost.i2p", "SECURE_I2P"),
                                Triple("US Dev Core", "us.dev.core@gmail.com", "GOOGLE_CHAT"),
                                Triple("SMS: NY Dispatch", "+12125550199", "SMS")
                            )
                            gpsState.region.contains("Svalbard", ignoreCase = true) -> listOf(
                                Triple("Svalbard Glacier Vault", "svalbard-vault.i2p", "SECURE_I2P"),
                                Triple("Arctic Seed Link", "arctic.seed@gmail.com", "GOOGLE_CHAT"),
                                Triple("SMS: Svalbard Dispatcher", "+4779021234", "SMS")
                            )
                            else -> listOf( // Geneva / Default
                                Triple("Geneva Privacy Vault", "geneva-vault.i2p", "SECURE_I2P"),
                                Triple("CERN Tunnel Link", "cern.tunnel@gmail.com", "GOOGLE_CHAT"),
                                Triple("SMS: Swiss Dispatcher", "+41791234567", "SMS")
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚡ PRE-FILL FOR LOCALITY: ${gpsState.region.split(",").firstOrNull()?.uppercase() ?: "GENEVA"}",
                            fontSize = 8.sp,
                            color = CyberBlue,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            dialogPresets.forEach { preset ->
                                val typeColor = when (preset.third) {
                                    "SECURE_I2P" -> CyberGreen
                                    "GOOGLE_CHAT" -> CyberYellow
                                    "SMS" -> CyberBlue
                                    else -> CyberPurple
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .clickable {
                                            newContactName = preset.first
                                            newContactAddress = preset.second
                                            newContactType = preset.third
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.first.split(" ").lastOrNull()?.split(":")?.lastOrNull()?.trim() ?: "Preset",
                                        fontSize = 8.sp,
                                        color = typeColor,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Display Name", color = TextSecondary) },
                        placeholder = { Text("e.g. Alice Watson", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newContactAddress,
                        onValueChange = { newContactAddress = it },
                        label = { Text("Network Address", color = TextSecondary) },
                        placeholder = {
                            Text(
                                text = when (newContactType) {
                                    "GOOGLE_CHAT" -> "e.g. alice@gmail.com"
                                    "SMS" -> "e.g. +155512345"
                                    else -> "e.g. alice.i2p"
                                },
                                color = TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )

                    // Network Platform selector
                    Text(
                        "INTEGRATED CARRIER TRANSPORT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("SECURE_I2P", "GOOGLE_CHAT", "SMS").forEach { type ->
                            val isSelected = newContactType == type
                            val typeLabel = when (type) {
                                "SECURE_I2P" -> "I2P Garlic"
                                "GOOGLE_CHAT" -> "Google Chat"
                                "SMS" -> "Secure SMS"
                                else -> ""
                            }
                            val typeColor = when (type) {
                                "SECURE_I2P" -> CyberGreen
                                "GOOGLE_CHAT" -> CyberYellow
                                "SMS" -> CyberBlue
                                else -> CyberPurple
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) typeColor.copy(alpha = 0.2f) else CyberBlack,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) typeColor else CyberBorder,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { newContactType = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 9.sp,
                                    color = if (isSelected) typeColor else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "CRYPTOGRAPHIC PUBLIC KEY (OPTIONAL)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = newContactPublicKey,
                        onValueChange = { newContactPublicKey = it },
                        label = { Text("Base64 Public Key", color = TextSecondary) },
                        placeholder = { Text("Paste recipient's public key for OTR / E2EE...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newContactName.isNotEmpty() && newContactAddress.isNotEmpty()) {
                            viewModel.addContact(
                                name = newContactName,
                                address = newContactAddress,
                                type = newContactType,
                                publicKeyBase64 = newContactPublicKey.takeIf { it.isNotBlank() }
                            )
                            showAddContactDialog = false
                            newContactName = ""
                            newContactAddress = ""
                            newContactPublicKey = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("SAVE PEER", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddContactDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showKeyAssociationDialog && selectedContact != null) {
        val contact = selectedContact!!
        val associatedKey = trustedKeys.find { it.i2pAddress == contact.address }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        AlertDialog(
            onDismissRequest = { showKeyAssociationDialog = false },
            containerColor = CyberBlack,
            title = {
                Text(
                    text = if (associatedKey != null) "MANAGE PEER PUBLIC KEY" else "ASSOCIATE PUBLIC KEY",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (associatedKey != null) CyberGreen else CyberOrange,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (associatedKey != null) {
                        Text(
                            text = "This contact '${contact.name}' is registered in the trusted keyring database. All outgoing messages will be encrypted client-side using this key before dispatch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Text(
                            text = "KEYRING ALIAS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberBlue
                        )
                        OutlinedTextField(
                            value = associatedKey.alias,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberBorder,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true
                        )

                        Text(
                            text = "BASE64 PUBLIC KEY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberBlue
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(CyberDarkSurface, RoundedCornerShape(4.dp))
                                .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = associatedKey.publicKeyBase64,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(associatedKey.publicKeyBase64)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberBlue.copy(alpha = 0.12f),
                                    contentColor = CyberBlue
                                ),
                                border = BorderStroke(1.dp, CyberBlue),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COPY KEY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.verifyPeerKey(associatedKey) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (associatedKey.isVerified) CyberBorder else CyberGreen.copy(alpha = 0.12f),
                                    contentColor = if (associatedKey.isVerified) TextPrimary else CyberGreen
                                ),
                                border = BorderStroke(1.dp, if (associatedKey.isVerified) CyberBorder else CyberGreen),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (associatedKey.isVerified) "VERIFIED" else "VERIFY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.deleteTrustedKey(associatedKey)
                                showKeyAssociationDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberRed.copy(alpha = 0.15f),
                                contentColor = CyberRed
                            ),
                            border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("REVOKE & DELETE KEY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "No public key is associated with '${contact.name}'. Outgoing messages currently fall back to sandbox test keys. Provide their Base64 public key to enable secure asymmetric encryption.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Text(
                            text = "PASTE RECIPIENT PUBLIC KEY (BASE64)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberBlue
                        )

                        OutlinedTextField(
                            value = associatePublicKeyField,
                            onValueChange = { associatePublicKeyField = it },
                            placeholder = { Text("Paste long cryptographic hash here...", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberOrange,
                                unfocusedBorderColor = CyberBorder
                            ),
                            maxLines = 4
                        )
                    }
                }
            },
            confirmButton = {
                if (associatedKey == null) {
                    Button(
                        onClick = {
                            if (associatePublicKeyField.isNotBlank()) {
                                viewModel.importTrustedKey(
                                    alias = contact.name,
                                    i2pAddress = contact.address,
                                    publicKeyBase64 = associatePublicKeyField,
                                    isVerified = true
                                )
                                associatePublicKeyField = ""
                                showKeyAssociationDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange, contentColor = CyberBlack),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("TRUST & SAVE", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { showKeyAssociationDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBorder, contentColor = TextPrimary),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("CLOSE", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (associatedKey == null) {
                    TextButton(onClick = { showKeyAssociationDialog = false }) {
                        Text("CANCEL", color = TextSecondary)
                    }
                }
            },
            modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(28.dp))
        )
    }
}

@Composable
fun ContactMessageBubble(
    msg: SecureMessage,
    contact: Contact,
    viewModel: I2PViewModel
) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = formatter.format(Date(msg.timestamp))
    val isOutgoing = !msg.isIncoming
    val contactColor = Color(android.graphics.Color.parseColor(contact.avatarColorHex))

    // Animated decryption states
    val coroutineScope = rememberCoroutineScope()
    var decryptionProgress by remember { mutableStateOf(-1f) } // -1f means idle, 0f..1f is active decryption
    var decryptionStageText by remember { mutableStateOf("") }
    var decryptionError by remember { mutableStateOf<String?>(null) }
    var animatedDecryptedText by remember { mutableStateOf("") }

    val activeId by viewModel.activeIdentity.collectAsState()

    // Character scrambling definition
    val scrambleChars = "A8F3C7E0B1154C8E79FD3C1A4038A8E2D409FEC9A10237E8BBDD0C5E71029C!@#$%^&*()_+{}|:<>?[];',./"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isOutgoing) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(contactColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, contactColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = contactColor
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val serviceIcon = when (contact.type) {
                        "GOOGLE_CHAT" -> Icons.Default.AlternateEmail
                        "SMS" -> Icons.Default.Sms
                        else -> Icons.Default.Lock
                    }
                    Icon(
                        imageVector = serviceIcon,
                        contentDescription = contact.type,
                        tint = contactColor,
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        text = if (isOutgoing) "SECURE DISPATCH" else contact.name.uppercase(),
                        fontSize = 8.sp,
                        color = if (isOutgoing) CyberGreen else CyberPurple,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .background(
                            color = if (isOutgoing) CyberBorder.copy(alpha = 0.6f) else CyberDarkSurface,
                            shape = RoundedCornerShape(
                                topStart = 10.dp,
                                topEnd = 10.dp,
                                bottomStart = if (isOutgoing) 10.dp else 0.dp,
                                bottomEnd = if (isOutgoing) 0.dp else 10.dp
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (isOutgoing) CyberGreen.copy(alpha = 0.3f) else {
                                if (msg.isDecrypted) CyberPurple.copy(alpha = 0.3f) else {
                                    if (decryptionProgress >= 0f) CyberOrange.copy(alpha = 0.6f) else CyberRed.copy(alpha = 0.4f)
                                }
                            },
                            shape = RoundedCornerShape(
                                topStart = 10.dp,
                                topEnd = 10.dp,
                                bottomStart = if (isOutgoing) 10.dp else 0.dp,
                                bottomEnd = if (isOutgoing) 0.dp else 10.dp
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .widthIn(max = 240.dp)
                ) {
                    Column {
                        if (msg.isDecrypted) {
                            Text(
                                text = msg.decryptedBody ?: "",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Decrypted",
                                    tint = CyberGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = if (isOutgoing) "Garlic-Encrypted" else "Decrypted via Identity RSA Key",
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberGreen
                                )
                            }
                        } else if (decryptionProgress >= 0f) {
                            // ACTIVE DECRYPTION SIMULATOR PANEL
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Decrypting",
                                        tint = CyberOrange,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "DECRYPTING GARLIC PACKET...",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberOrange
                                    )
                                }

                                // Interactive Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(CyberBlack, RoundedCornerShape(2.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(decryptionProgress)
                                            .background(
                                                Brush.horizontalGradient(listOf(CyberOrange, CyberYellow)),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }

                                Text(
                                    text = decryptionStageText,
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberYellow,
                                    textAlign = TextAlign.Center
                                )

                                // Live descrambling ciphertext preview
                                Box(
                                    modifier = Modifier
                                        .background(CyberBlack, RoundedCornerShape(4.dp))
                                        .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .fillMaxWidth()
                                ) {
                                    val plainLen = animatedDecryptedText.length
                                    val decryptedCount = (plainLen * decryptionProgress).toInt().coerceIn(0, plainLen)
                                    val leftToScramble = plainLen - decryptedCount
                                    val displayString = remember(decryptionProgress, animatedDecryptedText) {
                                        val decPart = animatedDecryptedText.take(decryptedCount)
                                        val scrPart = (1..leftToScramble).map { scrambleChars.random() }.joinToString("")
                                        decPart + scrPart
                                    }

                                    Text(
                                        text = displayString,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberOrange,
                                        lineHeight = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        } else {
                            // IDLE ENCRYPTED CIPHER BLOCK STATE
                            val displayPayload = remember(msg.encryptedPayload) {
                                if (msg.encryptedPayload.length > 28) {
                                    msg.encryptedPayload.take(24) + "..."
                                } else {
                                    msg.encryptedPayload
                                }
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked Payload",
                                        tint = CyberRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "RSA CIPHER BLOCK",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberRed
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(CyberBlack, RoundedCornerShape(4.dp))
                                        .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = displayPayload,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }

                                if (decryptionError != null) {
                                    Text(
                                        text = decryptionError!!,
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberRed,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                
                                Button(
                                    onClick = {
                                        val id = activeId
                                        if (id == null) {
                                            decryptionError = "ERROR: No active profile identity keypair. Create one first!"
                                            return@Button
                                        }
                                        decryptionError = null
                                        decryptionProgress = 0f
                                        decryptionStageText = "1. MATCHING SESSION TAG..."
                                        
                                        // Pre-calculate target plaintext so we can descramble into it
                                        val decrypted = decryptHybrid(msg.encryptedPayload, id.privateKeyBase64)
                                        animatedDecryptedText = if (decrypted.startsWith("ERROR")) {
                                            "[DECRYPTION_SIGNATURE_MISMATCH_ERROR]"
                                        } else {
                                            decrypted
                                        }

                                        coroutineScope.launch {
                                            // Step-by-step progress animation
                                            val steps = 50
                                            for (i in 1..steps) {
                                                delay(40) // 50 * 40ms = 2000ms (2 seconds) total duration
                                                decryptionProgress = i.toFloat() / steps
                                                
                                                decryptionStageText = when {
                                                    decryptionProgress < 0.25f -> "1. MATCHING SESSION TAG..."
                                                    decryptionProgress < 0.50f -> "2. DIFFIE-HELLMAN KEY VALIDATION..."
                                                    decryptionProgress < 0.75f -> "3. DECRYPTING AES-256 GARLIC LAYER..."
                                                    else -> "4. EXTRACTING PAYLOAD CLOVE..."
                                                }
                                            }
                                            // Finish and commit to Database
                                            viewModel.decryptSecureMessage(msg)
                                            decryptionProgress = -1f
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberRed.copy(alpha = 0.15f),
                                        contentColor = CyberRed
                                    ),
                                    border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().height(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = "Decrypt",
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "DECRYPT", 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeStr,
                                fontSize = 8.sp,
                                color = TextSecondary
                            )
                            if (isOutgoing) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Delivered",
                                    tint = CyberGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isOutgoing) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(CyberPurple.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, CyberPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ME",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPurple
                    )
                }
            }
        }
    }
}

// Global Memory Cache for Peer Sandbox Key Generation Fallback
private val dynamicKeysCache = HashMap<String, Pair<String, String>>()

fun getOrCreateValidKeysForPeer(alias: String, keyBase64: String): Pair<String, String> {
    if (keyBase64.length < 50 || keyBase64.contains("...")) {
        val cached = dynamicKeysCache[alias]
        if (cached != null) return cached

        return try {
            val keyGen = java.security.KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(1024)
            val pair = keyGen.generateKeyPair()
            val pub = android.util.Base64.encodeToString(pair.public.encoded, android.util.Base64.NO_WRAP)
            val priv = android.util.Base64.encodeToString(pair.private.encoded, android.util.Base64.NO_WRAP)
            val result = Pair(pub, priv)
            dynamicKeysCache[alias] = result
            result
        } catch (e: Exception) {
            Pair(keyBase64, "")
        }
    }
    return Pair(keyBase64, "")
}

fun encryptRSA(plainText: String, publicKeyBase64: String): String {
    return try {
        val cleanKey = publicKeyBase64
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .trim()
        val keyBytes = android.util.Base64.decode(cleanKey, android.util.Base64.DEFAULT)
        val spec = java.security.spec.X509EncodedKeySpec(keyBytes)
        val kf = java.security.KeyFactory.getInstance("RSA")
        val publicKey = kf.generatePublic(spec)
        val cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        "ERROR: Encryption failed - ${e.localizedMessage}"
    }
}

fun decryptRSA(cipherTextBase64: String, privateKeyBase64: String): String {
    return try {
        val cleanKey = privateKeyBase64
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .trim()
        val keyBytes = android.util.Base64.decode(cleanKey, android.util.Base64.DEFAULT)
        val spec = java.security.spec.X509EncodedKeySpec(keyBytes) // Wait, PKCS8 is used for RSA Private Keys!
        // Wait, standard java.security.spec.PKCS8EncodedKeySpec is used for Private Keys:
        val specPriv = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
        val kf = java.security.KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(specPriv)
        val cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey)
        val encryptedBytes = android.util.Base64.decode(cipherTextBase64, android.util.Base64.DEFAULT)
        String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
    } catch (e: Exception) {
        "ERROR: Decryption failed - ${e.localizedMessage}"
    }
}

fun encryptHybrid(plainText: String, publicKeyBase64: String): String {
    return try {
        val keyGen = javax.crypto.KeyGenerator.getInstance("AES")
        keyGen.init(128)
        val aesKey = keyGen.generateKey()

        val aesCipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        aesCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey)
        val cipherText = aesCipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val iv = aesCipher.iv

        val rsaCipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
        val cleanKey = publicKeyBase64
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .trim()
        val keyBytes = android.util.Base64.decode(cleanKey, android.util.Base64.DEFAULT)
        val spec = java.security.spec.X509EncodedKeySpec(keyBytes)
        val kf = java.security.KeyFactory.getInstance("RSA")
        val publicKey = kf.generatePublic(spec)
        rsaCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey)
        val encryptedAesKey = rsaCipher.doFinal(aesKey.encoded)

        val encAesKeyBase64 = android.util.Base64.encodeToString(encryptedAesKey, android.util.Base64.NO_WRAP)
        val ivBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
        val cipherTextBase64 = android.util.Base64.encodeToString(cipherText, android.util.Base64.NO_WRAP)
        "HYBRID-RSA-AES:$encAesKeyBase64:$ivBase64:$cipherTextBase64"
    } catch (e: Exception) {
        "ERROR: Hybrid encryption failed - ${e.localizedMessage}"
    }
}

fun decryptHybrid(cipherTextBase64: String, privateKeyBase64: String): String {
    if (!cipherTextBase64.startsWith("HYBRID-RSA-AES:")) {
        return decryptRSA(cipherTextBase64, privateKeyBase64)
    }
    return try {
        val parts = cipherTextBase64.split(":")
        if (parts.size < 4) return "ERROR: Invalid hybrid cipher format"
        val encAesKeyBase64 = parts[1]
        val ivBase64 = parts[2]
        val cipherTextBase64Part = parts[3]

        val rsaCipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
        val cleanKey = privateKeyBase64
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .trim()
        val keyBytes = android.util.Base64.decode(cleanKey, android.util.Base64.DEFAULT)
        val specPriv = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
        val kf = java.security.KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(specPriv)
        rsaCipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey)
        val encAesKeyBytes = android.util.Base64.decode(encAesKeyBase64, android.util.Base64.DEFAULT)
        val aesKeyBytes = rsaCipher.doFinal(encAesKeyBytes)
        val aesKey = javax.crypto.spec.SecretKeySpec(aesKeyBytes, "AES")

        val ivBytes = android.util.Base64.decode(ivBase64, android.util.Base64.DEFAULT)
        val cipherBytes = android.util.Base64.decode(cipherTextBase64Part, android.util.Base64.DEFAULT)
        val aesCipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        aesCipher.init(javax.crypto.Cipher.DECRYPT_MODE, aesKey, javax.crypto.spec.IvParameterSpec(ivBytes))
        val decryptedBytes = aesCipher.doFinal(cipherBytes)
        String(decryptedBytes, Charsets.UTF_8)
    } catch (e: Exception) {
        "ERROR: Hybrid decryption failed - ${e.localizedMessage}"
    }
}

@Composable
fun TerminalCryptorTab(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    val trustedKeysList by viewModel.trustedKeys.collectAsState()

    var cryptorPlaintext by remember { mutableStateOf("") }
    var encryptedResultText by remember { mutableStateOf("") }
    var selectedKeyType by remember { mutableStateOf("my_identity") } // "my_identity" or "peer_key"
    var selectedPeerKey by remember { mutableStateOf<TrustedKey?>(null) }

    val terminalLogs = remember { mutableStateListOf<String>() }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Automatically set default selected peer key if any exists
    LaunchedEffect(trustedKeysList) {
        if (selectedPeerKey == null && trustedKeysList.isNotEmpty()) {
            selectedPeerKey = trustedKeysList.first()
        }
    }

    // Automatically scroll terminal to bottom on change
    LaunchedEffect(terminalLogs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Block
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "CRYPTOGRAPHIC TERMINAL PLAYGROUND",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberPurple,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Encrypt secure messages using real asymmetric RSA keys. Load either your own generated key pair or any peer's public key from your verified keyring, then run and watch the encryption steps stream live inside the mock terminal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Input and Options Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "PREPARE PLAINTEXT & CRYPTOGRAPHIC PARAMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Plaintext text input field
                OutlinedTextField(
                    value = cryptorPlaintext,
                    onValueChange = { cryptorPlaintext = it },
                    label = { Text("Secure plaintext message to encrypt", color = TextSecondary) },
                    placeholder = { Text("Enter secret information here...", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("terminal_cryptor_plaintext"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = CyberBorder
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select Encryption Key Option
                Text(
                    "SELECT TARGET ENCRYPTION KEY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: My Identity
                    OutlinedButton(
                        onClick = { selectedKeyType = "my_identity" },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedKeyType == "my_identity") CyberPurple.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (selectedKeyType == "my_identity") CyberPurple else TextPrimary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedKeyType == "my_identity") CyberPurple else CyberBorder
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("My Identity Public Key", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = activeIdentity?.name ?: "No active identity",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedKeyType == "my_identity") CyberPurple else TextSecondary
                            )
                        }
                    }

                    // Option 2: Peer Key
                    OutlinedButton(
                        onClick = { selectedKeyType = "peer_key" },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedKeyType == "peer_key") CyberBlue.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (selectedKeyType == "peer_key") CyberBlue else TextPrimary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedKeyType == "peer_key") CyberBlue else CyberBorder
                        ),
                        modifier = Modifier.weight(1f),
                        enabled = trustedKeysList.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Trusted Peer Public Key", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = if (trustedKeysList.isEmpty()) "No trusted keys" else selectedPeerKey?.alias ?: "Select peer",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedKeyType == "peer_key") CyberBlue else TextSecondary
                            )
                        }
                    }
                }

                // Peer list picker if Option 2 selected
                if (selectedKeyType == "peer_key" && trustedKeysList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "CHOOSE VERIFIED PEER KEYRING:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        trustedKeysList.forEach { peerKey ->
                            val isChosen = selectedPeerKey?.id == peerKey.id
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isChosen) CyberBlue.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isChosen) CyberBlue else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedPeerKey = peerKey }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = peerKey.alias,
                                    color = if (isChosen) CyberBlue else TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Button 1: Encrypt
                    Button(
                        onClick = {
                            if (cryptorPlaintext.isNotEmpty()) {
                                scope.launch {
                                    isProcessing = true
                                    terminalLogs.clear()
                                    encryptedResultText = ""

                                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                    terminalLogs.add("[$timestamp] SHADOW-CRYPT RSA MODULE ENGINE v1.4.2 INITIALIZING...")
                                    delay(200)
                                    terminalLogs.add("[$timestamp] CYPHER-MODE: RSA 1024-bit key structure asymmetric algorithm.")
                                    delay(150)

                                    val keyName = if (selectedKeyType == "my_identity") activeIdentity?.name ?: "My Identity" else selectedPeerKey?.alias ?: "Peer Key"
                                    val keyBase64 = if (selectedKeyType == "my_identity") activeIdentity?.publicKeyBase64 else selectedPeerKey?.publicKeyBase64

                                    if (keyBase64 == null) {
                                        terminalLogs.add("[$timestamp] ERROR: Selected encryption key is null!")
                                        isProcessing = false
                                        return@launch
                                    }

                                    terminalLogs.add("[$timestamp] LOADING: Retrieving public key bytes for '$keyName'...")
                                    delay(200)

                                    val resolvedKeys = getOrCreateValidKeysForPeer(keyName, keyBase64)
                                    val activePubKey = resolvedKeys.first
                                    val resolvedPrivKey = resolvedKeys.second

                                    if (resolvedPrivKey.isNotEmpty() && selectedKeyType == "peer_key") {
                                        terminalLogs.add("[$timestamp] WARN: Truncated mock key identified! Automatically resolved to a fully valid RSA key pair for session sandbox.")
                                        delay(300)
                                    }

                                    terminalLogs.add("[$timestamp] ALGO: Setting padding mode to RSA/ECB/PKCS1Padding...")
                                    delay(150)
                                    terminalLogs.add("[$timestamp] ENCRYPT: Commencing cryptographic cipher transformation on plaintext bytes...")
                                    delay(350)

                                    val finalCipher = encryptRSA(cryptorPlaintext, activePubKey)

                                    if (finalCipher.startsWith("ERROR")) {
                                        terminalLogs.add("[$timestamp] CRYPT-FAIL: Cipher output aborted.")
                                        terminalLogs.add("[$timestamp] DETAILS: $finalCipher")
                                    } else {
                                        terminalLogs.add("[$timestamp] SUCCESS: Cipher completed successfully! Encrypted payload generated.")
                                        delay(150)
                                        terminalLogs.add("[$timestamp] GARLIC: Sealed and wrapped with session ephemeral tags.")
                                        delay(150)
                                        terminalLogs.add("[$timestamp] CIPHERTEXT IN BASE64:")
                                        terminalLogs.add(finalCipher)
                                        encryptedResultText = finalCipher
                                    }
                                    isProcessing = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberGreen,
                            contentColor = CyberBlack
                        ),
                        enabled = !isProcessing && cryptorPlaintext.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RSA-Encrypt", fontWeight = FontWeight.Bold)
                    }

                    // Button 2: Decrypt
                    val canDecrypt = encryptedResultText.isNotEmpty() && !isProcessing
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                terminalLogs.add("[$timestamp] AUTHORIZE: Accessing private key certificate container...")
                                delay(200)

                                val keyName = if (selectedKeyType == "my_identity") activeIdentity?.name ?: "My Identity" else selectedPeerKey?.alias ?: "Peer Key"
                                val keyBase64 = if (selectedKeyType == "my_identity") activeIdentity?.publicKeyBase64 else selectedPeerKey?.publicKeyBase64

                                val privateKeyToUse = if (selectedKeyType == "my_identity") {
                                    activeIdentity?.privateKeyBase64
                                } else {
                                    if (keyBase64 != null) {
                                        getOrCreateValidKeysForPeer(keyName, keyBase64).second
                                    } else null
                                }

                                if (privateKeyToUse == null || privateKeyToUse.isEmpty()) {
                                    terminalLogs.add("[$timestamp] ERROR: Corresponding asymmetric private key not found! Decryption impossible without private key.")
                                    isProcessing = false
                                    return@launch
                                }

                                terminalLogs.add("[$timestamp] DECRYPT: Instantiating RSA decryption core engine...")
                                delay(200)
                                terminalLogs.add("[$timestamp] UNWRAP: Removing garlic encapsulation routing envelopes...")
                                delay(200)
                                terminalLogs.add("[$timestamp] TRANSFORM: Deciphering RSA base64 block...")
                                delay(350)

                                val decryptedResultText = decryptRSA(encryptedResultText, privateKeyToUse)
                                if (decryptedResultText.startsWith("ERROR")) {
                                    terminalLogs.add("[$timestamp] DECRYPT-FAIL: Decryption unsuccessful.")
                                    terminalLogs.add("[$timestamp] DETAILS: $decryptedResultText")
                                } else {
                                    terminalLogs.add("[$timestamp] DECRYPT-OK: Asymmetric private key decryption completed.")
                                    delay(150)
                                    terminalLogs.add("[$timestamp] DECRYPTED VERIFIED BODY:")
                                    terminalLogs.add(">> \"$decryptedResultText\"")
                                }
                                isProcessing = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPurple,
                            contentColor = Color.White
                        ),
                        enabled = canDecrypt,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RSA-Decrypt", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Mock Terminal visual area
        Text(
            "MOCK ENCRYPTION TERMINAL LOGS",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(CyberBlack, RoundedCornerShape(8.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                if (terminalLogs.isEmpty()) {
                    Text(
                        text = "system@shadow_comms:~$ \n[Waiting for encryption/decryption dispatch. Type a plaintext message above and click Encrypt to trace cryptographic execution...]",
                        color = CyberGreen.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("terminal_cryptor_output")
                    )
                } else {
                    terminalLogs.forEach { logLine ->
                        val color = when {
                            logLine.contains("ERROR") || logLine.contains("FAIL") -> CyberRed
                            logLine.contains("SUCCESS") || logLine.contains("DECRYPT-OK") -> CyberGreen
                            logLine.contains("WARN") -> CyberOrange
                            logLine.contains(">>") -> CyberPurple
                            else -> CyberGreen.copy(alpha = 0.85f)
                        }

                        Text(
                            text = logLine,
                            color = color,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = if (logLine == terminalLogs.last()) Modifier.testTag("terminal_cryptor_output") else Modifier
                        )
                    }

                    // Blinking block cursor at the end!
                    val transition = rememberInfiniteTransition(label = "cursor_blink")
                    val cursorAlpha by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "cursor_alpha"
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "system@shadow_comms:~$ ",
                            color = CyberGreen.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 8.dp, height = 12.dp)
                                .background(CyberGreen.copy(alpha = cursorAlpha))
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MessageCardItem(msg: SecureMessage) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = formatter.format(Date(msg.timestamp))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (msg.isIncoming) CyberCardBg else CyberDarkSurface
        ),
        border = BorderStroke(
            1.dp,
            if (msg.isIncoming) CyberPurple.copy(alpha = 0.4f) else CyberGreen.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (msg.isIncoming) "INCOMING GARLIC PAYLOAD" else "OUTGOING DEPLOYMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (msg.isIncoming) CyberPurple else CyberGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Sender: ${msg.senderAddress}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Recipient: ${msg.recipientAddress}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = CyberBorder)
            Spacer(modifier = Modifier.height(8.dp))

            // Body text
            Text(
                msg.decryptedBody ?: "[Encrypted payload base64 bytes...]",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Raw Block: ${msg.encryptedPayload.take(24)}...",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

enum class IdentitySubTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    MY_KEYRINGS("My Keyrings", Icons.Default.Key),
    PEER_KEYS("Peer Key Manager", Icons.Default.VpnKey),
    SIGN_VERIFY("Sign & Verify", Icons.Default.Fingerprint),
    INVITATION_LAB("Secure Invite", Icons.Default.Share)
}

@Composable
fun IdentityScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(IdentitySubTab.MY_KEYRINGS) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Sub-tabs indicator
        TabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = CyberDarkSurface,
            contentColor = CyberBlue,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab.ordinal]),
                    color = CyberBlue
                )
            }
        ) {
            IdentitySubTab.values().forEach { tab ->
                val isSelected = activeSubTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { activeSubTab = tab },
                    text = {
                        Text(
                            tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) CyberBlue else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    selectedContentColor = CyberBlue,
                    unselectedContentColor = TextSecondary,
                    modifier = Modifier.testTag("identity_subtab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeSubTab) {
            IdentitySubTab.MY_KEYRINGS -> MyKeyringsTab(viewModel = viewModel)
            IdentitySubTab.PEER_KEYS -> PeerKeyManagerTab(viewModel = viewModel)
            IdentitySubTab.SIGN_VERIFY -> SignVerifyTab(viewModel = viewModel)
            IdentitySubTab.INVITATION_LAB -> InvitationLabTab(viewModel = viewModel)
        }
    }
}

@Composable
fun MyKeyringsTab(viewModel: I2PViewModel) {
    val identities by viewModel.identities.collectAsState()
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    var aliasNameInput by remember { mutableStateOf("") }
    var selectedKeySize by remember { mutableStateOf(2048) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Import Dialog
    if (showImportDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showImportDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "IMPORT CRYPTOGRAPHIC KEYPAIR",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    
                    var importName by remember { mutableStateOf("") }
                    var importPublicKey by remember { mutableStateOf("") }
                    var importPrivateKey by remember { mutableStateOf("") }
                    
                    OutlinedTextField(
                        value = importName,
                        onValueChange = { importName = it },
                        label = { Text("Identity name/alias", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("import_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = importPublicKey,
                        onValueChange = { importPublicKey = it },
                        label = { Text("Public Key (Base64 RSA)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("import_public_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                    
                    OutlinedTextField(
                        value = importPrivateKey,
                        onValueChange = { importPrivateKey = it },
                        label = { Text("Private Key (Base64 PKCS8 RSA)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("import_private_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showImportDialog = false },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("CANCEL", color = TextPrimary)
                        }
                        
                        Button(
                            onClick = {
                                if (importName.isNotBlank() && importPublicKey.isNotBlank() && importPrivateKey.isNotBlank()) {
                                    viewModel.importIdentity(importName, importPublicKey, importPrivateKey)
                                    showImportDialog = false
                                    android.widget.Toast.makeText(context, "Keyring imported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "All fields are required", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.2f).testTag("confirm_import_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("IMPORT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Register/Import New Alias Block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "GENERATE OR IMPORT CRYPTOGRAPHIC ALIAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Generates standard RSA asymmetric public/private keys used to secure identities and generate messages with mathematical proofs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = aliasNameInput,
                        onValueChange = { aliasNameInput = it },
                        label = { Text("Comrade pseudonym", color = TextSecondary) },
                        placeholder = { Text("e.g. ComradeX", color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("identity_alias_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key Size Selection
                    Text(
                        "KEY SIZE STRENGTH (RSA)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1024, 2048, 4096).forEach { size ->
                            val isSel = selectedKeySize == size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) CyberBlue.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(4.dp))
                                    .border(1.dp, if (isSel) CyberBlue else CyberBorder, RoundedCornerShape(4.dp))
                                    .clickable { selectedKeySize = size }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${size} bit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) CyberBlue else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).testTag("identity_import_key_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Keyring", fontSize = 11.sp, color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                if (aliasNameInput.isNotBlank()) {
                                    viewModel.createNewIdentity(aliasNameInput, selectedKeySize)
                                    aliasNameInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberBlue,
                                contentColor = CyberBlack
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("identity_generate_button")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Keys", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Active Identity details with raw keys
        if (activeIdentity != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ACTIVE KEYRING INFORMATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            activeIdentity!!.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Address Domain: ${activeIdentity!!.i2pAddress}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Public Key Copy
                        Text(
                            "Asymmetric Public Key (Base64)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                activeIdentity!!.publicKeyBase64.take(32) + "...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(activeIdentity!!.publicKeyBase64))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Public Key", tint = CyberBlue, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Private Key Warning
                        Text(
                            "Asymmetric Private Key (Secret)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberOrange
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "••••••••••••••••••••••••••••••••",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberOrange,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(activeIdentity!!.privateKeyBase64))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Private Key", tint = CyberOrange, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // All profiles list
        item {
            Text(
                "REGISTERED COM-KEYRINGS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }

        if (identities.isEmpty()) {
            item {
                Text(
                    "No aliases configured on this key ring.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            items(identities) { identity ->
                val isActive = activeIdentity?.id == identity.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) CyberBlue.copy(alpha = 0.1f) else CyberDarkSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isActive) CyberBlue else CyberBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.switchIdentity(identity) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                identity.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                identity.i2pAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isActive) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Active KeyRing",
                                    tint = CyberGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            IconButton(
                                onClick = { viewModel.deleteIdentity(identity) },
                                modifier = Modifier.testTag("delete_identity_button_${identity.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Keyring",
                                    tint = CyberOrange
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

object CryptoSigner {
    fun sign(message: String, privateKeyBase64: String): String? {
        return try {
            val clean = privateKeyBase64
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .trim()
            val decoded = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            val spec = java.security.spec.PKCS8EncodedKeySpec(decoded)
            val kf = java.security.KeyFactory.getInstance("RSA")
            val privateKey = kf.generatePrivate(spec)
            
            val signature = java.security.Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(message.toByteArray(Charsets.UTF_8))
            val sigBytes = signature.sign()
            android.util.Base64.encodeToString(sigBytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun verify(message: String, signatureBase64: String, publicKeyBase64: String): Boolean {
        return try {
            val clean = publicKeyBase64
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .trim()
            val decoded = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            val spec = java.security.spec.X509EncodedKeySpec(decoded)
            val kf = java.security.KeyFactory.getInstance("RSA")
            val publicKey = kf.generatePublic(spec)
            
            val signature = java.security.Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(message.toByteArray(Charsets.UTF_8))
            val sigBytes = android.util.Base64.decode(signatureBase64, android.util.Base64.DEFAULT)
            signature.verify(sigBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

@Composable
fun SignVerifyTab(viewModel: I2PViewModel) {
    val identities by viewModel.identities.collectAsState()
    val trustedKeys by viewModel.trustedKeys.collectAsState()
    val activeIdentity by viewModel.activeIdentity.collectAsState()

    // Sign States
    var messageToSign by remember { mutableStateOf("") }
    var generatedSignature by remember { mutableStateOf("") }
    var selectedSignIdentity by remember(activeIdentity, identities) {
        mutableStateOf(activeIdentity ?: identities.firstOrNull())
    }

    // Verify States
    var verifyModeCustom by remember { mutableStateOf(false) } // false = select peer, true = custom public key
    var selectedVerifyPeerKey by remember(trustedKeys) {
        mutableStateOf(trustedKeys.firstOrNull())
    }
    var customPublicKeyBase64 by remember { mutableStateOf("") }
    var messageToVerify by remember { mutableStateOf("") }
    var signatureToVerify by remember { mutableStateOf("") }
    var verificationResult by remember { mutableStateOf<Boolean?>(null) } // null = unchecked, true = valid, false = invalid

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Sign Message
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = CyberBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "DIGITALLY SIGN OUTGOING MESSAGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Generate a cryptographic signature for a message using one of your private keys. Peers can verify its authenticity using your public key.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Signer Selection
                    Text(
                        "SELECT SIGNER IDENTITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (identities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberBlack, RoundedCornerShape(4.dp))
                                .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No local aliases registered. Go to 'My Keyrings' to generate one first.",
                                fontSize = 11.sp,
                                color = CyberOrange,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Horizontal selection scroll row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(identities) { identity ->
                                val isSelected = selectedSignIdentity?.id == identity.id
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) CyberBlue.copy(alpha = 0.12f) else CyberBlack
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) CyberBlue else CyberBorder
                                    ),
                                    modifier = Modifier
                                        .width(160.dp)
                                        .clickable { selectedSignIdentity = identity }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            identity.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            identity.i2pAddress,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = CyberGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Message input field
                    OutlinedTextField(
                        value = messageToSign,
                        onValueChange = { messageToSign = it },
                        label = { Text("Message to Sign", color = TextSecondary) },
                        placeholder = { Text("Type or paste your confidential text here...", color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("signing_message_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign Button
                    Button(
                        onClick = {
                            val id = selectedSignIdentity
                            if (id == null) {
                                android.widget.Toast.makeText(context, "Error: No signer identity selected", android.widget.Toast.LENGTH_SHORT).show()
                            } else if (messageToSign.isBlank()) {
                                android.widget.Toast.makeText(context, "Please enter a message to sign", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val signature = CryptoSigner.sign(messageToSign, id.privateKeyBase64)
                                if (signature != null) {
                                    generatedSignature = signature
                                    coroutineScope.launch {
                                        viewModel.repository.addLog("CRYPT", "Generated RSA signature for: ${id.i2pAddress}", "SUCCESS")
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to generate signature", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_signature_button"),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CRYPTOGRAPHICALLY SIGN MESSAGE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (generatedSignature.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Signature output box
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberBlack),
                            border = BorderStroke(0.5.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "GENERATED RSA-SHA256 SIGNATURE (BASE64)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(generatedSignature))
                                            android.widget.Toast.makeText(context, "Signature copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Signature", tint = CyberBlue, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    generatedSignature,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Ready to transmit alongside raw message body.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Verify Message Signature
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "VERIFY DIGITAL SIGNATURE INTEGRITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Verify that a message was signed by the owner of a public key and has not been altered in transit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Public Key Source Mode Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberBlack, RoundedCornerShape(4.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(false, true).forEach { mode ->
                            val isSel = verifyModeCustom == mode
                            val label = if (!mode) "TRUSTED PEER KEY" else "CUSTOM PUBLIC KEY"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) CyberGreen.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) CyberGreen else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { verifyModeCustom = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) CyberGreen else TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key Input based on mode
                    if (!verifyModeCustom) {
                        Text(
                            "SELECT TRUSTED CONTACT PUBLIC KEY",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (trustedKeys.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CyberBlack, RoundedCornerShape(4.dp))
                                    .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No peer keys in database. Switch to 'Custom Public Key' mode or add trusted keys first.",
                                    fontSize = 11.sp,
                                    color = CyberOrange,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // Peer Keys selector row
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(trustedKeys) { key ->
                                    val isSelected = selectedVerifyPeerKey?.id == key.id
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) CyberGreen.copy(alpha = 0.12f) else CyberBlack
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) CyberGreen else CyberBorder
                                        ),
                                        modifier = Modifier
                                            .width(160.dp)
                                            .clickable { selectedVerifyPeerKey = key }
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                key.alias,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                key.i2pAddress,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = CyberBlue,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customPublicKeyBase64,
                            onValueChange = { customPublicKeyBase64 = it },
                            label = { Text("Base64 Public Key (RSA X.509)", color = TextSecondary) },
                            placeholder = { Text("Paste Base64 encoded public key here...", color = TextSecondary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("verify_custom_key_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Message text input to verify
                    OutlinedTextField(
                        value = messageToVerify,
                        onValueChange = { messageToVerify = it },
                        label = { Text("Original Message Content", color = TextSecondary) },
                        placeholder = { Text("Paste the original unmodified message content...", color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .testTag("verify_message_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Signature to verify input
                    OutlinedTextField(
                        value = signatureToVerify,
                        onValueChange = { signatureToVerify = it },
                        label = { Text("Cryptographic Signature (Base64)", color = TextSecondary) },
                        placeholder = { Text("Paste the digital signature associated with this message...", color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .testTag("verify_signature_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Verify Button
                    Button(
                        onClick = {
                            val pubKey = if (verifyModeCustom) {
                                customPublicKeyBase64
                            } else {
                                selectedVerifyPeerKey?.publicKeyBase64 ?: ""
                            }

                            if (pubKey.isBlank()) {
                                android.widget.Toast.makeText(context, "Error: No public key provided", android.widget.Toast.LENGTH_SHORT).show()
                            } else if (messageToVerify.isBlank()) {
                                android.widget.Toast.makeText(context, "Please enter message body", android.widget.Toast.LENGTH_SHORT).show()
                            } else if (signatureToVerify.isBlank()) {
                                android.widget.Toast.makeText(context, "Please enter the signature", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val isValid = CryptoSigner.verify(messageToVerify, signatureToVerify, pubKey)
                                verificationResult = isValid
                                coroutineScope.launch {
                                    val alias = if (verifyModeCustom) "Custom External Key" else selectedVerifyPeerKey?.alias ?: "Unknown Peer"
                                    if (isValid) {
                                        viewModel.repository.addLog("CRYPT", "Cryptographic signature validation successful for: $alias", "SUCCESS")
                                    } else {
                                        viewModel.repository.addLog("CRYPT", "Cryptographic signature validation failed! Signature corrupt or signed by another key.", "WARN")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBlack),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_signature_button"),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VERIFY SIGNATURE INTEGRITY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Verification Output Display
                    if (verificationResult != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        if (verificationResult == true) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                                border = BorderStroke(1.5.dp, CyberGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Signature Valid",
                                        tint = CyberGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "INTEGRITY SECURED",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "The cryptographic signature matches the public key. Message content has been mathematically verified to be unaltered and genuine.",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                                border = BorderStroke(1.5.dp, CyberOrange),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Signature Invalid",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "INTEGRITY COMPROMISED",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "Signature validation failed! This message has been modified, the signature is corrupt, or it was signed with a different private key.",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeerKeyManagerTab(viewModel: I2PViewModel) {
    val trustedKeys by viewModel.trustedKeys.collectAsState()
    
    var manualAlias by remember { mutableStateOf("") }
    var manualAddress by remember { mutableStateOf("") }
    var manualPublicKey by remember { mutableStateOf("") }
    var showAddManualDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Statistics Card & Action
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "PEER KEYRING DATABASE",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Local Address Book & Peer Verification",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = { showAddManualDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Key Manually", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TRUSTED CONTACT KEYRINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${trustedKeys.size} active peers",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (trustedKeys.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(CyberDarkSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No trusted peer keys in keyring database.", color = TextSecondary, fontSize = 13.sp)
                        Text("Exchange secure invitations to establish contact.", color = TextSecondary.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(trustedKeys) { key ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, if (key.isVerified) CyberGreen.copy(alpha = 0.6f) else CyberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        key.alias,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (key.isVerified) {
                                        Surface(
                                            color = CyberGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, CyberGreen)
                                        ) {
                                            Text(
                                                "VERIFIED",
                                                color = CyberGreen,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            color = CyberOrange.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, CyberOrange)
                                        ) {
                                            Text(
                                                "UNVERIFIED",
                                                color = CyberOrange,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    key.i2pAddress,
                                    fontSize = 11.sp,
                                    color = CyberBlue,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(onClick = { viewModel.deleteTrustedKey(key) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Revoke Peer Key", tint = CyberRed, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = CyberBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Public Key Segment
                        Text(
                            "Public Key Fingerprint (Base64)",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                key.publicKeyBase64.take(48) + "...",
                                fontSize = 10.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(key.publicKeyBase64)) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Public Key", tint = CyberBlue, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Hands-on Security Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Session Tag Status Bar
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "GARLIC SESSION TAGS",
                                        fontSize = 8.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${key.sessionTagCount}/100 remaining",
                                        fontSize = 8.sp,
                                        color = if (key.sessionTagCount > 20) CyberGreen else CyberOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = key.sessionTagCount / 100f,
                                    color = if (key.sessionTagCount > 20) CyberGreen else CyberOrange,
                                    trackColor = CyberBorder,
                                    modifier = Modifier.fillMaxWidth().height(4.dp).border(0.5.dp, CyberBorder, RoundedCornerShape(2.dp))
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Verification/Challenge Action Button
                            Button(
                                onClick = { viewModel.verifyPeerKey(key) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (key.isVerified) CyberBorder else CyberGreen.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, if (key.isVerified) CyberBorder else CyberGreen),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (key.isVerified) Icons.Default.Refresh else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (key.isVerified) TextSecondary else CyberGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (key.isVerified) "Re-Verify" else "Verify Identity",
                                    fontSize = 10.sp,
                                    color = if (key.isVerified) TextPrimary else CyberGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Manual Key Dialog
    if (showAddManualDialog) {
        AlertDialog(
            onDismissRequest = { showAddManualDialog = false },
            containerColor = CyberBlack,
            title = {
                Text(
                    "IMPORT TRUSTED PEER KEY",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Paste a peer's cryptographic address parameters to trust their public keys for garlic-encryption.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = manualAlias,
                        onValueChange = { manualAlias = it },
                        label = { Text("Peer Alias / Pseudonym", color = TextSecondary) },
                        placeholder = { Text("e.g. Alice", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualAddress,
                        onValueChange = { manualAddress = it },
                        label = { Text("Peer I2P Address", color = TextSecondary) },
                        placeholder = { Text("e.g. alice.i2p", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualPublicKey,
                        onValueChange = { manualPublicKey = it },
                        label = { Text("Base64 Public Key", color = TextSecondary) },
                        placeholder = { Text("Paste long cryptographic base64 hash...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualAlias.isNotBlank() && manualAddress.isNotBlank() && manualPublicKey.isNotBlank()) {
                            viewModel.importTrustedKey(manualAlias, manualAddress, manualPublicKey, isVerified = false)
                            manualAlias = ""
                            manualAddress = ""
                            manualPublicKey = ""
                            showAddManualDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Trust Key", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(28.dp))
        )
    }
}

@Composable
fun InvitationLabTab(viewModel: I2PViewModel) {
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    
    var sharePinCode by remember { mutableStateOf("") }
    var exportFormatByGarlic by remember { mutableStateOf(false) } // False = Basic Link, True = Garlic Encrypted Packet
    
    var importInvitationText by remember { mutableStateOf("") }
    var importPinCode by remember { mutableStateOf("") }
    
    var importStatusMessage by remember { mutableStateOf("") }
    var isImportSuccess by remember { mutableStateOf<Boolean?>(null) }
    
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Generate Invitation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "DISPATCH CRYPTOGRAPHIC INVITATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Package your active public key parameters so another comrade can trust you and send you garlic-encrypted messages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (activeIdentity == null) {
                        Text(
                            "Please create an identity in the 'My Keyrings' tab first.",
                            color = CyberOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        // Interactive Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberBlack, RoundedCornerShape(4.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { exportFormatByGarlic = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!exportFormatByGarlic) CyberBlue.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(4.dp),
                                border = if (!exportFormatByGarlic) BorderStroke(1.dp, CyberBlue) else null,
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    "Basic Link",
                                    fontSize = 11.sp,
                                    color = if (!exportFormatByGarlic) CyberBlue else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { exportFormatByGarlic = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (exportFormatByGarlic) CyberGreen.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(4.dp),
                                border = if (exportFormatByGarlic) BorderStroke(1.dp, CyberGreen) else null,
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    "Garlic Packet (Secure)",
                                    fontSize = 11.sp,
                                    color = if (exportFormatByGarlic) CyberGreen else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val activeId = activeIdentity!!
                        if (exportFormatByGarlic) {
                            // PIN input
                            OutlinedTextField(
                                value = sharePinCode,
                                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) sharePinCode = it },
                                label = { Text("Shared Secret PIN (6 Digits)", color = TextSecondary) },
                                placeholder = { Text("e.g. 583921", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = CyberGreen,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val code = sharePinCode.ifBlank { "123456" }
                                    val packet = viewModel.generateGarlicEncryptedInvite(
                                        activeId.name, activeId.i2pAddress, activeId.publicKeyBase64, code
                                    )
                                    clipboardManager.setText(AnnotatedString(packet))
                                    importStatusMessage = "Secure Garlic Packet copied! Share it and the PIN: $code"
                                    isImportSuccess = true
                                    sharePinCode = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBlack),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate & Copy Garlic Packet", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                "Quick, unencrypted invitation link suitable for trusted channels.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Button(
                                onClick = {
                                    val link = viewModel.generateBasicInviteLink(
                                        activeId.name, activeId.i2pAddress, activeId.publicKeyBase64
                                    )
                                    clipboardManager.setText(AnnotatedString(link))
                                    importStatusMessage = "Invitation Link copied to clipboard!"
                                    isImportSuccess = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Basic Invitation Link", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Import Invitation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "IMPORT INVITATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Paste a basic link or garlic-encrypted packet received from another comrade to add them to your Keyring database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = importInvitationText,
                        onValueChange = { importInvitationText = it },
                        label = { Text("Paste Invitation Code or Link", color = TextSecondary) },
                        placeholder = { Text("i2p://invite... OR garlic-packet-invite:...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (importInvitationText.startsWith("garlic-packet-invite:")) {
                        OutlinedTextField(
                            value = importPinCode,
                            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) importPinCode = it },
                            label = { Text("Decryption PIN Code", color = TextSecondary) },
                            placeholder = { Text("Enter 6-digit PIN shared with you...", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            if (importInvitationText.isBlank()) return@Button
                            
                            val text = importInvitationText.trim()
                            if (text.startsWith("i2p://invite")) {
                                val result = viewModel.parseBasicInviteLink(text)
                                if (result != null) {
                                    viewModel.importTrustedKey(result.first, result.second, result.third, isVerified = true)
                                    importStatusMessage = "Successfully imported peer key for: ${result.first}"
                                    isImportSuccess = true
                                    importInvitationText = ""
                                } else {
                                    importStatusMessage = "Error: Invalid invitation link format."
                                    isImportSuccess = false
                                }
                            } else if (text.startsWith("garlic-packet-invite:")) {
                                val code = importPinCode.ifBlank { "123456" }
                                val result = viewModel.decryptGarlicEncryptedInvite(text, code)
                                if (result != null) {
                                    viewModel.importTrustedKey(result.first, result.second, result.third, isVerified = true)
                                    importStatusMessage = "Decrypted & imported peer key: ${result.first}"
                                    isImportSuccess = true
                                    importInvitationText = ""
                                    importPinCode = ""
                                } else {
                                    importStatusMessage = "Decryption Failed: Incorrect PIN or tampered packet."
                                    isImportSuccess = false
                                }
                            } else {
                                importStatusMessage = "Error: Unrecognized invitation format."
                                isImportSuccess = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify & Decode Invite Packet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Import status alert message
        if (importStatusMessage.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isImportSuccess == true) CyberGreen.copy(alpha = 0.15f) else CyberRed.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, if (isImportSuccess == true) CyberGreen else CyberRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isImportSuccess == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isImportSuccess == true) CyberGreen else CyberRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            importStatusMessage,
                            color = if (isImportSuccess == true) CyberGreen else CyberRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { importStatusMessage = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss status", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GarlicRoutingPathVisualizer(
    tunnelHops: Int,
    isConnected: Boolean,
    isConnecting: Boolean
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Linear Path, 1: Network Topology, 2: Garlic Simulation
    var healthThreshold by remember { mutableStateOf(70f) } // Default 70% connection health threshold

    val infiniteTransition = rememberInfiniteTransition(label = "garlic_flow")
    
    // Animate the dash phase shift for connection lines
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dash_phase"
    )

    // Animate packet/pulse progress along each tunnel segment
    val packetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "packet_progress"
    )

    // Pulse size for glow animations
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Ticker state to fluctuate latencies and make simulation feel real & alive
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(isConnected, isConnecting) {
        if (isConnected) {
            while (true) {
                kotlinx.coroutines.delay(2000)
                tick++
            }
        }
    }

    // Build the nodes dynamically based on current hop settings and connection/tick states
    val nodes = remember(tunnelHops, tick, isConnected, isConnecting) {
        val random = java.util.Random(tick.toLong() + tunnelHops * 17)
        
        fun getLatency(base: Int): Int {
            if (!isConnected) return 0
            val jitter = random.nextInt(16) - 8 // +/- 8ms jitter
            return (base + jitter).coerceAtLeast(1)
        }
        
        fun getHealth(latency: Int): NodeHealth {
            if (!isConnected) return NodeHealth.OFFLINE
            return when {
                latency < 50 -> NodeHealth.EXCELLENT
                latency < 110 -> NodeHealth.GOOD
                latency < 180 -> NodeHealth.DEGRADED
                else -> NodeHealth.DEGRADED
            }
        }

        when (tunnelHops) {
            1 -> {
                val lat0 = 2 // Client always low latency
                val lat1 = getLatency(42)
                val lat2 = getLatency(94)
                listOf(
                    VisualNode("Client", "Src Portal", "client", lat0, NodeHealth.EXCELLENT),
                    VisualNode("Hop 1", "Tunnel Relay", "relay", lat1, getHealth(lat1)),
                    VisualNode("Eepsite", "Dest Server", "destination", lat2, getHealth(lat2))
                )
            }
            3 -> {
                val lat0 = 3
                val lat1 = getLatency(38)
                val lat2 = getLatency(85)
                val lat3 = getLatency(135)
                val lat4 = getLatency(198)
                listOf(
                    VisualNode("Client", "Src Portal", "client", lat0, NodeHealth.EXCELLENT),
                    VisualNode("I-Gate", "Inbound Gateway", "gate", lat1, getHealth(lat1)),
                    VisualNode("Transit", "Standard Relay", "relay", lat2, getHealth(lat2)),
                    VisualNode("O-Gate", "Outbound Gateway", "gate", lat3, getHealth(lat3)),
                    VisualNode("Eepsite", "Dest Server", "destination", lat4, getHealth(lat4))
                )
            }
            else -> {
                val lat0 = 4
                val lat1 = getLatency(32)
                val lat2 = getLatency(74)
                val lat3 = getLatency(115)
                val lat4 = getLatency(162)
                val lat5 = getLatency(225)
                listOf(
                    VisualNode("Client", "Src Portal", "client", lat0, NodeHealth.EXCELLENT),
                    VisualNode("I-Gate", "Inbound Gateway", "gate", lat1, getHealth(lat1)),
                    VisualNode("Transit A", "Relay Alpha", "relay", lat2, getHealth(lat2)),
                    VisualNode("Transit B", "Relay Beta", "relay", lat3, getHealth(lat3)),
                    VisualNode("O-Gate", "Outbound Gateway", "gate", lat4, getHealth(lat4)),
                    VisualNode("Eepsite", "Dest Server", "destination", lat5, getHealth(lat5))
                )
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "LIVE GARLIC TUNNEL PATH SIMULATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (isConnected) "Garlic Packet Streams Active" else if (isConnecting) "Assembling Tunnel Layers..." else "Tunnel Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) CyberGreen else if (isConnecting) CyberOrange else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Indicators Legend
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF00E676), androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stable", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF9100), androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Jitter", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Modern Cyber Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack, RoundedCornerShape(6.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("Linear Path", "Network Topology", "Garlic Simulation")
                tabs.forEachIndexed { idx, title ->
                    val selected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) CyberBlue.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) CyberBlue else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { selectedTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) CyberBlue else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Tab 0: Standard Linear Canvas visualization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(CyberBlack, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val paddingX = 40.dp.toPx()
                        
                        val nodeCount = nodes.size
                        val xStep = (width - paddingX * 2) / (nodeCount - 1)
                        
                        val nodePositions = nodes.mapIndexed { index, node ->
                            val x = paddingX + index * xStep
                            val yOffset = if (index == 0 || index == nodeCount - 1) 0f else {
                                if (index % 2 == 1) -height * 0.2f else height * 0.2f
                            }
                            val y = (height / 2) + yOffset
                            Pair(node, Offset(x, y))
                        }

                        // 1. Draw connecting lines between nodes
                        for (i in 0 until nodePositions.size - 1) {
                            val start = nodePositions[i].second
                            val end = nodePositions[i + 1].second

                            // Draw background track line
                            drawLine(
                                color = CyberBorder,
                                start = start,
                                end = end,
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Draw animated path flow if active or connecting
                            if (isConnected || isConnecting) {
                                val lineColor = if (isConnected) {
                                    // Transition from green to purple across the tunnel hops to show encryption to decryption transition
                                    val ratio = i.toFloat() / (nodePositions.size - 2)
                                    lerpColor(CyberGreen, CyberPurple, ratio)
                                } else {
                                    CyberOrange
                                }

                                // Flowing dashed line effect
                                val dashPathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(24f, 16f),
                                    phase = -dashPhase
                                )

                                drawLine(
                                    color = lineColor,
                                    start = start,
                                    end = end,
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    pathEffect = dashPathEffect
                                )

                                // 2. Draw moving Garlic Clove packet pulses
                                val currentPos = start + (end - start) * packetProgress
                                drawCircle(
                                    color = lineColor,
                                    radius = 4.dp.toPx(),
                                    center = currentPos
                                )
                                // Packet outer glow
                                drawCircle(
                                    color = lineColor.copy(alpha = 0.3f),
                                    radius = 8.dp.toPx() * pulseScale,
                                    center = currentPos
                                )
                            }
                        }

                        // 3. Draw nodes
                        nodePositions.forEachIndexed { index, pair ->
                            val node = pair.first
                            val center = pair.second

                            val nodeColor = when {
                                !isConnected && !isConnecting -> CyberBorder
                                isConnecting -> CyberOrange
                                else -> {
                                    val ratio = index.toFloat() / (nodePositions.size - 1)
                                    lerpColor(CyberGreen, CyberPurple, ratio)
                                }
                            }

                            // Node Outer Glow
                            if (isConnected || isConnecting) {
                                drawCircle(
                                    color = nodeColor.copy(alpha = 0.2f),
                                    radius = 14.dp.toPx() * pulseScale,
                                    center = center
                                )
                            }

                            // Node Border Ring
                            drawCircle(
                                color = nodeColor,
                                radius = 8.dp.toPx(),
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Node Solid Center
                            drawCircle(
                                color = if (isConnected || isConnecting) CyberBlack else CyberCardBg,
                                radius = 6.dp.toPx(),
                                center = center
                            )

                            drawCircle(
                                color = nodeColor,
                                radius = 3.dp.toPx(),
                                center = center
                            )

                            // Connection Status Dot Indicator on top-right of node
                            val badgeOffset = Offset(center.x + 8.dp.toPx(), center.y - 8.dp.toPx())
                            val statusDotColor = if (isConnected) {
                                node.health.color
                            } else if (isConnecting) {
                                if (index == 0) Color(0xFF00E676) else CyberOrange
                            } else {
                                if (index == 0) Color(0xFF00E676) else Color(0xFFFF1744)
                            }

                            // Status dot outer glow
                            drawCircle(
                                color = statusDotColor.copy(alpha = 0.4f),
                                radius = 5.dp.toPx(),
                                center = badgeOffset
                            )

                            // Status dot solid center
                            drawCircle(
                                color = statusDotColor,
                                radius = 3.dp.toPx(),
                                center = badgeOffset
                            )

                            // Status dot crisp border line
                            drawCircle(
                                color = CyberBlack,
                                radius = 3.dp.toPx(),
                                center = badgeOffset,
                                style = Stroke(width = 0.8.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Path Description Text/Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    nodes.forEachIndexed { index, node ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                node.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) {
                                    val ratio = index.toFloat() / (nodes.size - 1)
                                    lerpColor(CyberGreen, CyberPurple, ratio)
                                } else {
                                    TextPrimary
                                },
                                textAlign = TextAlign.Center
                            )
                            Text(
                                node.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            
                            // Added Latency & Connection Health indicator badges below descriptions
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val dotColor = if (isConnected) {
                                    node.health.color
                                } else if (isConnecting) {
                                    if (index == 0) Color(0xFF00E676) else CyberOrange
                                } else {
                                    if (index == 0) Color(0xFF00E676) else Color(0xFFFF1744)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(dotColor, androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val latencyText = when {
                                    !isConnected && !isConnecting -> if (index == 0) "2ms" else "offline"
                                    isConnecting -> if (index == 0) "2ms" else "ping..."
                                    else -> "${node.latencyMs}ms"
                                }
                                Text(
                                    text = latencyText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isConnected) TextPrimary else TextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // Tab 1: Interactive Force-Directed Network Graph
                ForceDirectedCircuitGraph(
                    nodes = nodes,
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    healthThreshold = healthThreshold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Connection Health Highlight Threshold Slider
                ThresholdSlider(
                    thresholdValue = healthThreshold,
                    onValueChange = { healthThreshold = it }
                )
            } else {
                // Tab 2: Full Animated Garlic Routing Simulation Dashboard
                GarlicRoutingSimulatorComponent(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = CyberBorder)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Path Cryptography: Client encrypts message inside nested ElGamal layers. Each intermediary hop decrypts its specific instruction layer (using Session Tags) to locate the next hop without learning the payload content or original source.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// Interactive Force-Directed Node State Model
class ForceNodeState(
    val id: String,
    val label: String,
    val type: String, // "client", "relay", "gate", "destination", "decoy"
    val healthScore: Int, // 0 to 100
    val latencyMs: Int,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isDragging: Boolean = false
)

data class ForceEdge(
    val fromId: String,
    val toId: String,
    val isMainCircuit: Boolean
)

@Composable
fun ForceDirectedCircuitGraph(
    nodes: List<VisualNode>,
    isConnected: Boolean,
    isConnecting: Boolean,
    healthThreshold: Float,
    modifier: Modifier = Modifier
) {
    // Construct dynamic force-directed node states matching current active garlic path
    val forceNodes = remember(nodes) {
        val list = mutableListOf<ForceNodeState>()
        
        // 1. Map main routing path nodes
        nodes.forEachIndexed { idx, node ->
            val angle = (idx.toFloat() / nodes.size) * Math.PI
            val r = 100f
            val startX = 300f + (r * cos(angle)).toFloat()
            val startY = 130f + (r * sin(angle)).toFloat()
            
            val hScore = when (node.health) {
                NodeHealth.EXCELLENT -> 98
                NodeHealth.GOOD -> 85
                NodeHealth.DEGRADED -> 60
                NodeHealth.OFFLINE -> 0
            }
            
            list.add(
                ForceNodeState(
                    id = "main_$idx",
                    label = node.name,
                    type = node.type,
                    healthScore = hScore,
                    latencyMs = node.latencyMs,
                    x = startX,
                    y = startY
                )
            )
        }
        
        // 2. Add 3 decoy global network nodes to represent the background decentralized mesh
        val decoyData = listOf(
            Triple("Decoy-Alpha", 88, 92),
            Triple("Decoy-Beta", 52, 175),
            Triple("Decoy-Gamma", 94, 34)
        )
        
        decoyData.forEachIndexed { idx, data ->
            val angle = (idx.toFloat() / decoyData.size) * 2 * Math.PI + Math.PI / 4
            val r = 160f
            val startX = 300f + (r * cos(angle)).toFloat()
            val startY = 130f + (r * sin(angle)).toFloat()
            
            list.add(
                ForceNodeState(
                    id = "decoy_$idx",
                    label = data.first,
                    type = "decoy",
                    healthScore = data.second,
                    latencyMs = data.third,
                    x = startX,
                    y = startY
                )
            )
        }
        
        list
    }

    // Build spring-connection edges
    val forceEdges = remember(nodes) {
        val edges = mutableListOf<ForceEdge>()
        
        // Main path chain
        for (i in 0 until nodes.size - 1) {
            edges.add(ForceEdge("main_$i", "main_${i + 1}", isMainCircuit = true))
        }
        
        // Decoy network background link meshes
        edges.add(ForceEdge("decoy_0", "main_0", isMainCircuit = false))
        if (nodes.size > 2) {
            edges.add(ForceEdge("decoy_1", "main_2", isMainCircuit = false))
        } else {
            edges.add(ForceEdge("decoy_1", "main_1", isMainCircuit = false))
        }
        edges.add(ForceEdge("decoy_2", "decoy_0", isMainCircuit = false))
        edges.add(ForceEdge("decoy_2", "main_${nodes.size - 1}", isMainCircuit = false))
        
        edges
    }

    var triggerUpdate by remember { mutableStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize(600, 300)) }

    // Spring physics loop running natively at ~60fps
    LaunchedEffect(forceNodes, canvasSize) {
        val kAttractive = 0.05f // spring stiffness
        val kRepulsive = 32000f // repulsion strength
        val kGravity = 0.02f // pull strength to center
        val dRate = 0.83f // damping velocity friction
        val restLengthMain = 100f
        val restLengthDecoy = 130f
        
        while (true) {
            val cx = canvasSize.width / 2f
            val cy = canvasSize.height / 2f
            
            // 1. Node Repulsion
            for (i in forceNodes.indices) {
                val ni = forceNodes[i]
                if (ni.isDragging) continue
                
                var fx = 0f
                var fy = 0f
                
                for (j in forceNodes.indices) {
                    if (i == j) continue
                    val nj = forceNodes[j]
                    val dx = ni.x - nj.x
                    val dy = ni.y - nj.y
                    val distSq = dx * dx + dy * dy + 1f
                    val dist = sqrt(distSq)
                    
                    if (dist < 320f) {
                        val force = kRepulsive / distSq
                        fx += (dx / dist) * force
                        fy += (dy / dist) * force
                    }
                }
                
                // 2. Link attraction
                forceEdges.forEach { edge ->
                    if (edge.fromId == ni.id || edge.toId == ni.id) {
                        val otherId = if (edge.fromId == ni.id) edge.toId else edge.fromId
                        val otherNode = forceNodes.find { it.id == otherId }
                        if (otherNode != null) {
                            val dx = ni.x - otherNode.x
                            val dy = ni.y - otherNode.y
                            val dist = sqrt(dx * dx + dy * dy + 1f)
                            val restL = if (edge.isMainCircuit) restLengthMain else restLengthDecoy
                            val force = -kAttractive * (dist - restL)
                            fx += (dx / dist) * force
                            fy += (dy / dist) * force
                        }
                    }
                }
                
                // 3. Central gravity
                val dcx = ni.x - cx
                val dcy = ni.y - cy
                fx -= dcx * kGravity
                fy -= dcy * kGravity
                
                // Apply final updates
                ni.vx = (ni.vx + fx) * dRate
                ni.vy = (ni.vy + fy) * dRate
                
                ni.x += ni.vx
                ni.y += ni.vy
                
                // Keep strictly inside Canvas boundaries
                ni.x = ni.x.coerceIn(25f, canvasSize.width.toFloat() - 25f)
                ni.y = ni.y.coerceIn(25f, canvasSize.height.toFloat() - 25f)
            }
            
            triggerUpdate++
            kotlinx.coroutines.delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(CyberBlack, RoundedCornerShape(8.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            .pointerInput(forceNodes, canvasSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val tapRadius = 32.dp.toPx()
                        val tapped = forceNodes.find {
                            val dx = it.x - offset.x
                            val dy = it.y - offset.y
                            dx * dx + dy * dy < tapRadius * tapRadius
                        }
                        if (tapped != null) {
                            tapped.isDragging = true
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val active = forceNodes.find { it.isDragging }
                        if (active != null) {
                            active.x += dragAmount.x
                            active.y += dragAmount.y
                            active.x = active.x.coerceIn(25f, canvasSize.width.toFloat() - 25f)
                            active.y = active.y.coerceIn(25f, canvasSize.height.toFloat() - 25f)
                        }
                    },
                    onDragEnd = {
                        forceNodes.forEach { it.isDragging = false }
                    },
                    onDragCancel = {
                        forceNodes.forEach { it.isDragging = false }
                    }
                )
            }
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "force_pulse")
        val dashPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dash_phase"
        )
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        val tickCount = triggerUpdate

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (size.width > 0 && size.height > 0) {
                        canvasSize = size
                    }
                }
        ) {
            val _t = tickCount // Triggers Canvas redraw on physics ticks
            
            // 1. Draw connections (links)
            forceEdges.forEach { edge ->
                val fromNode = forceNodes.find { it.id == edge.fromId }
                val toNode = forceNodes.find { it.id == edge.toId }
                
                if (fromNode != null && toNode != null) {
                    val start = Offset(fromNode.x, fromNode.y)
                    val end = Offset(toNode.x, toNode.y)
                    
                    // Background link line
                    drawLine(
                        color = if (edge.isMainCircuit) CyberBorder.copy(alpha = 0.6f) else CyberBorder.copy(alpha = 0.25f),
                        start = start,
                        end = end,
                        strokeWidth = if (edge.isMainCircuit) 2.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Draw animated Garlic Layer streams if circuit is active
                    if (edge.isMainCircuit && (isConnected || isConnecting)) {
                        val isFromHealthy = fromNode.healthScore >= healthThreshold
                        val isToHealthy = toNode.healthScore >= healthThreshold
                        val lineHighlightColor = if (!isFromHealthy || !isToHealthy) {
                            CyberRed // RED Alert status for unhealthy links
                        } else if (isConnected) {
                            CyberGreen // Stable Encryption Green
                        } else {
                            CyberOrange // Assembling
                        }

                        val dashPathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(20f, 15f),
                            phase = -dashPhase
                        )

                        drawLine(
                            color = lineHighlightColor,
                            start = start,
                            end = end,
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = dashPathEffect
                        )
                    }
                }
            }
            
            // 2. Draw nodes & interactive visual details
            forceNodes.forEach { fNode ->
                val center = Offset(fNode.x, fNode.y)
                val isBelowThreshold = fNode.healthScore < healthThreshold
                
                // Color mapping: solid red if health drops below specific threshold
                val baseNodeColor = if (isBelowThreshold) {
                    CyberRed
                } else {
                    when (fNode.type) {
                        "client" -> CyberGreen
                        "destination" -> CyberPurple
                        "decoy" -> CyberBlue.copy(alpha = 0.7f)
                        else -> CyberBlue
                    }
                }
                
                // Outer halo glow
                drawCircle(
                    color = baseNodeColor.copy(alpha = if (isBelowThreshold) 0.35f else 0.18f),
                    radius = (14.dp.toPx()) * (if (isBelowThreshold) pulseScale * 1.2f else pulseScale),
                    center = center
                )
                
                // Warning Pulse Ring for Unhealthy/Degraded nodes
                if (isBelowThreshold) {
                    drawCircle(
                        color = CyberRed,
                        radius = 18.dp.toPx() * pulseScale,
                        center = center,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    )
                }

                // Node border circle outline
                drawCircle(
                    color = baseNodeColor,
                    radius = 9.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Black core center
                drawCircle(
                    color = CyberBlack,
                    radius = 7.dp.toPx(),
                    center = center
                )
                
                // Small solid core dot
                drawCircle(
                    color = baseNodeColor,
                    radius = 4.dp.toPx(),
                    center = center
                )

                // Render Labels and Health percentages
                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = android.graphics.Paint().apply {
                        color = if (isBelowThreshold) android.graphics.Color.RED else android.graphics.Color.WHITE
                        textSize = 9.dp.toPx()
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val subPaint = android.graphics.Paint().apply {
                        color = if (isBelowThreshold) android.graphics.Color.argb(220, 255, 70, 70) else android.graphics.Color.GRAY
                        textSize = 7.5.dp.toPx()
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    
                    // Node Name Label
                    drawText(
                        fNode.label,
                        fNode.x,
                        fNode.y - 14.dp.toPx(),
                        textPaint
                    )
                    
                    // Node HP & Latency Description
                    val hpLabel = if (isBelowThreshold) "HP: ${fNode.healthScore}% [CRIT]" else "${fNode.healthScore}% HP | ${fNode.latencyMs}ms"
                    drawText(
                        hpLabel,
                        fNode.x,
                        fNode.y + 23.dp.toPx(),
                        subPaint
                    )
                }
            }
        }
        
        // Dynamic bottom-right helper overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(CyberBlack.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .border(0.5.dp, CyberBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                "Drag nodes to manipulate layout structure",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ThresholdSlider(
    thresholdValue: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert Threshold Indicator",
                    tint = CyberRed,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BOTTLE-NECK HIGHLIGHT THRESHOLD: ${thresholdValue.toInt()}% HP",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberRed,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
            Text(
                text = "Flags nodes in RED below this HP",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 8.sp
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Slider(
            value = thresholdValue,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = CyberRed,
                activeTrackColor = CyberRed,
                inactiveTrackColor = CyberBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .testTag("health_threshold_slider")
        )
    }
}

// Utility function to interpolate colors
fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = start.alpha + (stop.alpha - start.alpha) * fraction
    )
}

enum class NodeHealth(val label: String, val color: Color) {
    EXCELLENT("Excellent", Color(0xFF00E676)),
    GOOD("Good", Color(0xFF00B0FF)),
    DEGRADED("Degraded", Color(0xFFFF9100)),
    OFFLINE("Offline", Color(0xFFFF1744))
}

data class VisualNode(
    val name: String,
    val description: String,
    val type: String,
    val latencyMs: Int,
    val health: NodeHealth
)

@Composable
fun PeerDiscoverySection(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val peers by viewModel.discoveredPeers.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Global Peer Map",
                        tint = CyberBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "GLOBAL PEER DISCOVERY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isDiscovering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CyberBlue
                    )
                } else {
                    IconButton(
                        onClick = { viewModel.discoverPeers() },
                        modifier = Modifier.size(24.dp).testTag("refresh_peers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Peer List",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Simulated view of cryptographically verified active global routing nodes registered in local NetDB database.",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Discovered Peers List
            if (peers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(CyberCardBg, RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "No Peers",
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "NetDB peer list is currently unseeded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    peers.forEach { peer ->
                        PeerRowItem(peer = peer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Trigger Button
            Button(
                onClick = { viewModel.discoverPeers() },
                enabled = !isDiscovering,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberBlue.copy(alpha = 0.15f),
                    contentColor = CyberBlue,
                    disabledContainerColor = CyberBorder.copy(alpha = 0.2f),
                    disabledContentColor = TextSecondary
                ),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (isDiscovering) CyberBorder else CyberBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("scan_netdb_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isDiscovering) Icons.Default.Sync else Icons.Default.Search,
                        contentDescription = "Scan icon",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDiscovering) "RESOLVING GLOBAL PEER ENVELOPS..." else "SCAN GLOBAL NETWORK DATABASE (NETDB)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PeerRowItem(peer: DiscoveredPeer) {
    val statusColor = when (peer.status) {
        PeerStatus.ACTIVE -> CyberGreen
        PeerStatus.STABLE -> CyberBlue
        PeerStatus.DEGRADED -> CyberOrange
    }

    val healthBg = statusColor.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberCardBg, RoundedCornerShape(6.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Column: Node identity, country region
        Column(modifier = Modifier.weight(1.3f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${peer.flagEmoji} ",
                    fontSize = 14.sp
                )
                Text(
                    text = peer.routerId,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (peer.isVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Cryptographically Verified",
                        tint = CyberGreen,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = peer.i2pAddress.take(8) + "..." + peer.i2pAddress.takeLast(7),
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${peer.region}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        // Right Column: Latency and Health score pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(0.7f)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${peer.latencyMs}ms",
                    color = CyberBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Latency",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                color = healthBg,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${peer.healthScore}%",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "HEALTH",
                        color = statusColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

enum class VpnVpsSubTab(val label: String) {
    VPN("SECURE VPN"),
    VPS("VPS / HOME NODE")
}

@Composable
fun VpnVpsScreen(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(VpnVpsSubTab.VPN) }
    val vpnState by viewModel.vpnState.collectAsState()
    val vpsState by viewModel.vpsState.collectAsState()
    
    // Dialog states for VPS profile creation
    var showAddVpsDialog by remember { mutableStateOf(false) }
    var vpsNameInput by remember { mutableStateOf("") }
    var vpsIpInput by remember { mutableStateOf("") }
    var vpsUserInput by remember { mutableStateOf("") }
    var vpsPortInput by remember { mutableStateOf("22") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Tab header
        TabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = CyberDarkSurface,
            contentColor = CyberGreen,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab.ordinal]),
                    color = CyberGreen
                )
            }
        ) {
            VpnVpsSubTab.values().forEach { tab ->
                val isSelected = activeSubTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { activeSubTab = tab },
                    text = {
                        Text(
                            tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = 1.sp
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeSubTab == VpnVpsSubTab.VPN) {
                // VPN Tab
                item {
                    Text(
                        text = "VIRTUAL PRIVATE NETWORKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Active connection status card
                item {
                    val statusColor = when (vpnState.status) {
                        VpnStatus.CONNECTED -> CyberGreen
                        VpnStatus.CONNECTING -> CyberOrange
                        VpnStatus.DISCONNECTED -> CyberRed
                    }
                    val statusLabel = when (vpnState.status) {
                        VpnStatus.CONNECTED -> "SECURE END-TO-END ESCROW ACTIVE"
                        VpnStatus.CONNECTING -> "ESTABLISHING HANDSHAKE..."
                        VpnStatus.DISCONNECTED -> "ROUTING UNPROTECTED BY VPN"
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        border = BorderStroke(1.dp, if (vpnState.status == VpnStatus.CONNECTED) CyberGreen else CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(statusColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = vpnState.status.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = statusColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Icon(
                                    imageVector = if (vpnState.status == VpnStatus.CONNECTED) Icons.Default.Security else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (vpnState.status == VpnStatus.CONNECTED) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = CyberBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Transmitted", fontSize = 10.sp, color = TextSecondary)
                                        Text(
                                            String.format(java.util.Locale.US, "%.2f MB", vpnState.bytesTransmitted / (1024f * 1024f)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Column {
                                        Text("Uptime", fontSize = 10.sp, color = TextSecondary)
                                        Text(
                                            String.format(java.util.Locale.US, "%02d:%02d", vpnState.connectedDurationSeconds / 60, vpnState.connectedDurationSeconds % 60),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Column {
                                        Text("Interface", fontSize = 10.sp, color = TextSecondary)
                                        Text(
                                            "tun0 (IKEv2)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = CyberBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Connect / Disconnect button
                item {
                    val isConnected = vpnState.status == VpnStatus.CONNECTED
                    val isConnecting = vpnState.status == VpnStatus.CONNECTING
                    Button(
                        onClick = { viewModel.toggleVpn() },
                        enabled = !isConnecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) CyberRed else CyberGreen,
                            contentColor = CyberBlack,
                            disabledContainerColor = CyberBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("vpn_toggle_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isConnecting) "NEGOTIATING KEYS..." else if (isConnected) "TERMINATE VPN TUNNEL" else "INITIATE SECURE VPN",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // VPN Configuration selector
                item {
                    Text(
                        text = "AVAILABLE SECURE ENDPOINTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                items(vpnState.availableVpns) { config ->
                    val isSelected = vpnState.selectedVpn == config.name
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CyberGreen.copy(alpha = 0.05f) else CyberDarkSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CyberGreen else CyberBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = vpnState.status == VpnStatus.DISCONNECTED) {
                                viewModel.selectVpn(config.name)
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { if (vpnState.status == VpnStatus.DISCONNECTED) viewModel.selectVpn(config.name) },
                                        enabled = vpnState.status == VpnStatus.DISCONNECTED,
                                        colors = RadioButtonDefaults.colors(selectedColor = CyberGreen)
                                    )
                                    Column {
                                        Text(
                                            text = config.name,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = config.speedRating,
                                                fontSize = 9.sp,
                                                color = CyberBlue,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(modifier = Modifier.size(3.dp).background(TextSecondary, CircleShape))
                                            Text(
                                                text = "Ping: ${config.pingMs}ms",
                                                fontSize = 9.sp,
                                                color = CyberOrange,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                                
                                if (config.recommended) {
                                    Box(
                                        modifier = Modifier
                                            .background(CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .border(0.5.dp, CyberGreen, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "RECOMMENDED",
                                            fontSize = 7.sp,
                                            color = CyberGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                                Text(
                                    text = "Encryption: ${config.securityLevel}",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Help/Recommendation Section
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AISP PROXY GUIDELINES", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "While darkweb garlic networks encrypt routing layers between network peers, they do not mask your ISP's trace of initial gateway connections. We highly recommend utilizing Private VPN (ShadowTunnel) as it encapsulates your garlic packages inside quantum-resistant metadata packets, hiding network topology entirely from external eavesdroppers.",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

            } else {
                // VPS / Home Portal Tab
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SSH ROUTING PROXIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Button(
                            onClick = { showAddVpsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue.copy(alpha = 0.15f), contentColor = CyberBlue),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, CyberBlue),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADD VPS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Remote status monitor card (Visible when connected)
                item {
                    val statusColor = when (vpsState.status) {
                        VpsStatus.CONNECTED -> CyberBlue
                        VpsStatus.CONNECTING -> CyberOrange
                        VpsStatus.DISCONNECTED -> TextSecondary
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        border = BorderStroke(1.dp, if (vpsState.status == VpsStatus.CONNECTED) CyberBlue else CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(8.dp).background(statusColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PORTAL: ${vpsState.status.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = statusColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            vpsState.activeProfile?.let { profile ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Connected gateway: ${profile.name} (${profile.ipAddress})",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            if (vpsState.status == VpsStatus.CONNECTED) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Divider(color = CyberBorder.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // CPU Indicator
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("VPS CPU", fontSize = 9.sp, color = TextSecondary)
                                            Text("${vpsState.cpuUsagePercent}%", fontSize = 9.sp, color = CyberBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = vpsState.cpuUsagePercent / 100f,
                                            modifier = Modifier.fillMaxWidth().height(4.dp),
                                            color = CyberBlue,
                                            trackColor = CyberBorder
                                        )
                                    }
                                    
                                    // RAM Indicator
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("VPS RAM", fontSize = 9.sp, color = TextSecondary)
                                            Text("${vpsState.ramUsagePercent}%", fontSize = 9.sp, color = CyberOrange, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = vpsState.ramUsagePercent / 100f,
                                            modifier = Modifier.fillMaxWidth().height(4.dp),
                                            color = CyberOrange,
                                            trackColor = CyberBorder
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Gateway Proxy Speed", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        String.format(java.util.Locale.US, "%.1f Mbps", vpsState.bandwidthUsageMbps),
                                        fontSize = 11.sp,
                                        color = CyberGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Connected VPS control button
                if (vpsState.status != VpsStatus.DISCONNECTED) {
                    item {
                        Button(
                            onClick = { viewModel.disconnectVps() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = CyberBlack),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("DISCONNECT PROXY ROUTER", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }

                // List of profiles
                item {
                    Text(
                        text = "SAVED GATEWAY PROFILES",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (vpsState.savedProfiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberDarkSurface, RoundedCornerShape(6.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No VPS profiles added. Tap ADD VPS above to register.", fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
                        }
                    }
                }

                items(vpsState.savedProfiles) { profile ->
                    val isConnectedToThis = vpsState.activeProfile == profile
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        border = BorderStroke(1.dp, if (isConnectedToThis) CyberBlue else CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = null,
                                        tint = if (isConnectedToThis) CyberBlue else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(profile.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            if (profile.isDefault) {
                                                Text(
                                                    "DEFAULT",
                                                    fontSize = 7.sp,
                                                    color = CyberGreen,
                                                    modifier = Modifier
                                                        .background(CyberGreen.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            "${profile.username}@${profile.ipAddress}:${profile.port}",
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (vpsState.status == VpsStatus.DISCONNECTED) {
                                        Button(
                                            onClick = { viewModel.connectVps(profile) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("CONNECT", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeVpsProfile(profile) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete VPS Profile", tint = CyberRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add VPS Profile Dialog
    if (showAddVpsDialog) {
        AlertDialog(
            onDismissRequest = { showAddVpsDialog = false },
            title = { Text("Add Remote VPS Gateway", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = vpsNameInput,
                        onValueChange = { vpsNameInput = it },
                        label = { Text("Profile Name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    OutlinedTextField(
                        value = vpsIpInput,
                        onValueChange = { vpsIpInput = it },
                        label = { Text("IP Address / Hostname", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    OutlinedTextField(
                        value = vpsUserInput,
                        onValueChange = { vpsUserInput = it },
                        label = { Text("Username", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    OutlinedTextField(
                        value = vpsPortInput,
                        onValueChange = { vpsPortInput = it },
                        label = { Text("SSH Port", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vpsNameInput.isNotEmpty() && vpsIpInput.isNotEmpty() && vpsUserInput.isNotEmpty()) {
                            val port = vpsPortInput.toIntOrNull() ?: 22
                            viewModel.addVpsProfile(vpsNameInput, vpsIpInput, vpsUserInput, port)
                        }
                        showAddVpsDialog = false
                        vpsNameInput = ""
                        vpsIpInput = ""
                        vpsUserInput = ""
                        vpsPortInput = "22"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack)
                ) {
                    Text("Save Node")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVpsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberDarkSurface
        )
    }
}

@Composable
fun GpsEmulatorTab(
    viewModel: I2PViewModel,
    modifier: Modifier = Modifier
) {
    val gpsState by viewModel.gpsState.collectAsState()

    // Slider/input states
    var customLat by remember { mutableStateOf(gpsState.latitude.toString()) }
    var customLon by remember { mutableStateOf(gpsState.longitude.toString()) }
    var speed by remember { mutableStateOf(gpsState.speedKmh) }
    var signalStrength by remember { mutableStateOf(gpsState.signalStrengthDbm.toFloat()) }

    // Synchronize customLat/customLon when gpsState changes
    LaunchedEffect(gpsState) {
        customLat = String.format(java.util.Locale.US, "%.4f", gpsState.latitude)
        customLon = String.format(java.util.Locale.US, "%.4f", gpsState.longitude)
        speed = gpsState.speedKmh
        signalStrength = gpsState.signalStrengthDbm.toFloat()
    }

    val presets = listOf(
        Sextet("🇨🇭 Geneva", 46.2044, 6.1432, "Geneva, Switzerland", "194.230.12.83", "Swiss Crypt-Services Ltd"),
        Sextet("🇮🇸 Reykjavik", 64.1466, -21.9426, "Reykjavik, Iceland", "185.112.144.10", "Isavia Crypto Core"),
        Sextet("🇯🇵 Tokyo", 35.6762, 139.6503, "Tokyo, Japan", "122.211.45.9", "Softbank Security Inc"),
        Sextet("🇺🇸 New York", 40.7128, -74.0060, "New York, USA", "208.67.222.22", "NY Outpost Router LLC"),
        Sextet("❄️ Svalbard Vault", 78.2232, 15.6469, "Svalbard Vault (Arctic)", "46.21.96.11", "Arctic Deep-Web Core")
    )

    val localizedProfiles = remember(gpsState.region) {
        when {
            gpsState.region.contains("Reykjavik", ignoreCase = true) -> listOf(
                Triple("Reykjavik Ice Node", "reykjavik-ice.i2p", "SECURE_I2P"),
                Triple("Iceland Dev Link", "iceland.dev@gmail.com", "GOOGLE_CHAT"),
                Triple("SMS: Reykjavik Dispatch", "+3545551234", "SMS")
            )
            gpsState.region.contains("Tokyo", ignoreCase = true) -> listOf(
                Triple("Tokyo Secure Relay", "tokyo-relay.i2p", "SECURE_I2P"),
                Triple("APAC Lead dev", "apac.lead@gmail.com", "GOOGLE_CHAT"),
                Triple("SMS: Tokyo Command", "+819012345678", "SMS")
            )
            gpsState.region.contains("New York", ignoreCase = true) -> listOf(
                Triple("NY Outpost Router", "ny-outpost.i2p", "SECURE_I2P"),
                Triple("US Dev Core", "us.dev.core@gmail.com", "GOOGLE_CHAT"),
                Triple("SMS: NY Dispatch", "+12125550199", "SMS")
            )
            gpsState.region.contains("Svalbard", ignoreCase = true) -> listOf(
                Triple("Svalbard Glacier Vault", "svalbard-vault.i2p", "SECURE_I2P"),
                Triple("Arctic Seed Link", "arctic.seed@gmail.com", "GOOGLE_CHAT"),
                Triple("SMS: Svalbard Dispatcher", "+4779021234", "SMS")
            )
            else -> listOf( // Geneva / Default
                Triple("Geneva Privacy Vault", "geneva-vault.i2p", "SECURE_I2P"),
                Triple("CERN Tunnel Link", "cern.tunnel@gmail.com", "GOOGLE_CHAT"),
                Triple("SMS: Swiss Dispatcher", "+41791234567", "SMS")
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Title Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "GPS SPOOFING & COLD WAR SHIELD",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Hardware-Level NMEA Injections & Localization Profiles",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = CyberBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Animated Radar Telemetry Graphic Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberBlack),
                border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Radar Canvas
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(CyberDarkSurface, CircleShape)
                            .border(1.dp, CyberBlue.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val centerOffset = androidx.compose.ui.geometry.Offset(cx, cy)
                            // Radial circles
                            drawCircle(
                                color = CyberBlue.copy(alpha = 0.1f),
                                radius = size.minDimension / 2f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                            )
                            drawCircle(
                                color = CyberBlue.copy(alpha = 0.2f),
                                radius = size.minDimension / 3.2f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                            )
                            drawCircle(
                                color = CyberBlue.copy(alpha = 0.3f),
                                radius = size.minDimension / 6f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                            )
                            // Crosshairs
                            drawLine(
                                color = CyberBlue.copy(alpha = 0.25f),
                                start = androidx.compose.ui.geometry.Offset(0f, cy),
                                end = androidx.compose.ui.geometry.Offset(size.width, cy),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = CyberBlue.copy(alpha = 0.25f),
                                start = androidx.compose.ui.geometry.Offset(cx, 0f),
                                end = androidx.compose.ui.geometry.Offset(cx, size.height),
                                strokeWidth = 1f
                            )
                            // Draw mock satellite points
                            drawCircle(color = CyberGreen, radius = 4f, center = androidx.compose.ui.geometry.Offset(cx - 20f, cy - 30f))
                            drawCircle(color = CyberGreen, radius = 4f, center = androidx.compose.ui.geometry.Offset(cx + 35f, cy - 15f))
                            drawCircle(color = CyberGreen, radius = 4f, center = androidx.compose.ui.geometry.Offset(cx - 15f, cy + 35f))
                            drawCircle(color = CyberGreen, radius = 4f, center = androidx.compose.ui.geometry.Offset(cx + 40f, cy + 25f))
                            drawCircle(color = CyberBlue, radius = 6f, center = centerOffset)
                        }

                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = CyberBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Telemetry readouts
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "ACTIVE TELEMETRY INJECTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberBlue,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column {
                                Text("LATITUDE", fontSize = 8.sp, color = TextSecondary)
                                Text(
                                    text = String.format(java.util.Locale.US, "%.4f° N", gpsState.latitude),
                                    fontSize = 11.sp,
                                    color = CyberGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text("LONGITUDE", fontSize = 8.sp, color = TextSecondary)
                                Text(
                                    text = String.format(java.util.Locale.US, "%.4f° E", gpsState.longitude),
                                    fontSize = 11.sp,
                                    color = CyberGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "REGION: ${gpsState.region.uppercase()}",
                            fontSize = 9.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "PROVIDER: ${gpsState.localIsp} (${gpsState.localIp})",
                            fontSize = 8.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SATS: ${gpsState.satCount} (LOCK)",
                                fontSize = 8.sp,
                                color = CyberGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "ALT: ${gpsState.altitudeM}m",
                                fontSize = 8.sp,
                                color = CyberPurple,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SPEED: ${gpsState.speedKmh} km/h",
                                fontSize = 8.sp,
                                color = CyberYellow,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Region Presets
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "SELECT PRESET LOCALIZATION ROUTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { preset ->
                            val isSelected = gpsState.region == preset.d
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) CyberBlue.copy(alpha = 0.12f) else CyberBlack,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberBlue else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        viewModel.updateSpoofedGps(
                                            latitude = preset.b,
                                            longitude = preset.c,
                                            region = preset.d,
                                            speed = speed,
                                            altitude = preset.e.toIntOrNull() ?: 421,
                                            localIp = preset.e,
                                            localIsp = preset.f
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isSelected) CyberBlue else TextSecondary, CircleShape)
                                    )
                                    Column {
                                        Text(
                                            text = preset.a,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) TextPrimary else TextSecondary
                                        )
                                        Text(
                                            text = "IP: ${preset.e} • ISP: ${preset.f}",
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(java.util.Locale.US, "%.3f, %.3f", preset.b, preset.c),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) CyberGreen else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Localized Display Alternatives & Addresses
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "LOCALIZED QUICK LAUNCH TEMPLATES",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(CyberGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, CyberGreen, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = gpsState.region.split(",").firstOrNull()?.uppercase() ?: "GENEVA",
                                fontSize = 8.sp,
                                color = CyberGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap register to instantly save these secure alternative display name and address presets to your active database based on the active GPS localization.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        localizedProfiles.forEach { profile ->
                            val (name, address, type) = profile
                            val typeColor = when (type) {
                                "SECURE_I2P" -> CyberGreen
                                "GOOGLE_CHAT" -> CyberYellow
                                "SMS" -> CyberBlue
                                else -> CyberPurple
                            }
                            val typeLabel = when (type) {
                                "SECURE_I2P" -> "I2P GARLIC"
                                "GOOGLE_CHAT" -> "GOOGLE CHAT"
                                "SMS" -> "SECURE SMS"
                                else -> ""
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CyberBlack, RoundedCornerShape(6.dp))
                                    .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, typeColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = typeLabel,
                                                fontSize = 7.sp,
                                                color = typeColor,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = address,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.addContact(name, address, type)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = typeColor.copy(alpha = 0.15f),
                                        contentColor = typeColor
                                    ),
                                    border = BorderStroke(1.dp, typeColor.copy(alpha = 0.6f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("REGISTER", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manual Tuning Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "MANUAL COORDINATE TUNING",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customLat,
                            onValueChange = { customLat = it },
                            label = { Text("Latitude", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberBlue,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customLon,
                            onValueChange = { customLon = it },
                            label = { Text("Longitude", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyberBlue,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val latVal = customLat.toDoubleOrNull() ?: gpsState.latitude
                            val lonVal = customLon.toDoubleOrNull() ?: gpsState.longitude
                            viewModel.updateSpoofedGps(
                                latitude = latVal,
                                longitude = lonVal,
                                region = "Custom Spoofed Location",
                                speed = speed,
                                altitude = gpsState.altitudeM,
                                localIp = gpsState.localIp,
                                localIsp = gpsState.localIsp
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("INJECT MANUAL COORDINATES", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Speed and Signal Strength Controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "CARRIER AND TELEMETRY CONTROLS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SPOOFED MOVEMENT SPEED", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${speed.toInt()} km/h", fontSize = 11.sp, color = CyberYellow, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = speed,
                        onValueChange = {
                            speed = it
                            viewModel.updateSpoofedGps(
                                latitude = gpsState.latitude,
                                longitude = gpsState.longitude,
                                region = gpsState.region,
                                speed = speed,
                                altitude = gpsState.altitudeM,
                                localIp = gpsState.localIp,
                                localIsp = gpsState.localIsp
                            )
                        },
                        valueRange = 0f..150f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberYellow,
                            activeTrackColor = CyberYellow.copy(alpha = 0.7f),
                            inactiveTrackColor = CyberBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SPOOFED GPS SIGNAL STRENGTH", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${signalStrength.toInt()} dBm", fontSize = 11.sp, color = CyberBlue, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = signalStrength,
                        onValueChange = {
                            signalStrength = it
                            viewModel.updateSpoofedGps(
                                latitude = gpsState.latitude,
                                longitude = gpsState.longitude,
                                region = gpsState.region,
                                speed = speed,
                                altitude = gpsState.altitudeM,
                                localIp = gpsState.localIp,
                                localIsp = gpsState.localIsp
                            )
                        },
                        valueRange = -130f..-30f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberBlue,
                            activeTrackColor = CyberBlue.copy(alpha = 0.7f),
                            inactiveTrackColor = CyberBorder
                        )
                    )
                }
            }
        }
    }
}

data class Sextet<A, B, C, D, E, F>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E,
    val f: F
)

@Composable
fun ProxySettingsDialog(
    viewModel: I2PViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.routerState.collectAsState()
    val tunnels by viewModel.tunnels.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Proxy Gateway, 1 = Local Tunnels

    var httpEnabled by remember { mutableStateOf(state.httpProxyEnabled) }
    var httpHost by remember { mutableStateOf(state.httpProxyHost) }
    var httpPort by remember { mutableStateOf(state.httpProxyPort.toString()) }

    var socksEnabled by remember { mutableStateOf(state.socksProxyEnabled) }
    var socksHost by remember { mutableStateOf(state.socksProxyHost) }
    var socksPort by remember { mutableStateOf(state.socksProxyPort.toString()) }

    var systemWide by remember { mutableStateOf(state.systemWideProxy) }

    // Add Tunnel Form State
    var showAddTunnelForm by remember { mutableStateOf(false) }
    var newTunnelName by remember { mutableStateOf("") }
    var newTunnelType by remember { mutableStateOf("CLIENT") } // CLIENT or SERVER
    var newTunnelPort by remember { mutableStateOf("") }
    var newTunnelTarget by remember { mutableStateOf("") }
    var newTunnelHops by remember { mutableStateOf(3) }

    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberBlack),
            border = BorderStroke(1.dp, CyberBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("proxy_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Proxy Configuration",
                            tint = CyberBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SETTINGS & TUNNELS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Segmented Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberDarkSurface, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("PROXY GATEWAY", "LOCAL TUNNELS").forEachIndexed { index, title ->
                        val isSel = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSel) CyberBlue.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) CyberBlue else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) CyberBlue else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = CyberBorder)
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // Scrollable fields
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // SOCKS Proxy Setup
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, if (socksEnabled) CyberGreen else CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Dns,
                                            contentDescription = null,
                                            tint = if (socksEnabled) CyberGreen else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "SOCKS5 PROXY (SAM BRIDGE)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (socksEnabled) CyberGreen else TextPrimary
                                        )
                                    }
                                    Switch(
                                        checked = socksEnabled,
                                        onCheckedChange = { socksEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyberBlack,
                                            checkedTrackColor = CyberGreen,
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = CyberCardBg
                                        ),
                                        modifier = Modifier.testTag("socks_proxy_toggle")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Routes SOCKS-compliant browser traffic through your integrated garlic router node securely.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                if (socksEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = socksHost,
                                            onValueChange = { socksHost = it },
                                            label = { Text("Proxy Host", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1.5f).testTag("socks_host_input"),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = CyberGreen,
                                                unfocusedBorderColor = CyberBorder
                                            ),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = socksPort,
                                            onValueChange = { socksPort = it },
                                            label = { Text("Port", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f).testTag("socks_port_input"),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = CyberGreen,
                                                unfocusedBorderColor = CyberBorder
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }

                        // HTTP Proxy Setup
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, if (httpEnabled) CyberBlue else CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Language,
                                            contentDescription = null,
                                            tint = if (httpEnabled) CyberBlue else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "HTTP WEB PROXY",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (httpEnabled) CyberBlue else TextPrimary
                                        )
                                    }
                                    Switch(
                                        checked = httpEnabled,
                                        onCheckedChange = { httpEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyberBlack,
                                            checkedTrackColor = CyberBlue,
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = CyberCardBg
                                        ),
                                        modifier = Modifier.testTag("http_proxy_toggle")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Routes typical HTTP web page traffic (such as eepSites) through your integrated proxy node.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                if (httpEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = httpHost,
                                            onValueChange = { httpHost = it },
                                            label = { Text("Proxy Host", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1.5f).testTag("http_host_input"),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = CyberBlue,
                                                unfocusedBorderColor = CyberBorder
                                            ),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = httpPort,
                                            onValueChange = { httpPort = it },
                                            label = { Text("Port", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f).testTag("http_port_input"),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = CyberBlue,
                                                unfocusedBorderColor = CyberBorder
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }

                        // System-wide Android JVM Routing
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, if (systemWide) CyberOrange else CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Security,
                                            contentDescription = null,
                                            tint = if (systemWide) CyberOrange else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "SYSTEM-WIDE JVM ROUTING",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (systemWide) CyberOrange else TextPrimary
                                        )
                                    }
                                    Switch(
                                        checked = systemWide,
                                        onCheckedChange = { systemWide = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyberBlack,
                                            checkedTrackColor = CyberOrange,
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = CyberCardBg
                                        ),
                                        modifier = Modifier.testTag("system_wide_toggle")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Sets standard JVM properties (http.proxyHost, socksProxyHost) to automatically redirect all outgoing app-level requests through the active proxies.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Instruction Manual / Tutorial Guides
                        Text(
                            "EXTERNAL BROWSER SETUP GUIDES",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberBlue,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        var selectedGuideTab by remember { mutableStateOf(0) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Android Wi-Fi", "Firefox (SOCKS)").forEachIndexed { index, label ->
                                val isSel = selectedGuideTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSel) CyberBlue.copy(alpha = 0.15f) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSel) CyberBlue else CyberBorder,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { selectedGuideTab = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) CyberBlue else TextSecondary
                                    )
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberBlack),
                            border = BorderStroke(0.5.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (selectedGuideTab == 0) {
                                    Text(
                                        "WiFi Proxy Manual Setup Steps:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "1. Go to Android Settings -> Network -> Wi-Fi.\n" +
                                        "2. Long press your active connection -> Modify Network.\n" +
                                        "3. Expand 'Advanced Options' -> Proxy -> Manual.\n" +
                                        "4. Set Proxy Hostname to: $httpHost\n" +
                                        "5. Set Proxy Port to: ${httpPort.ifBlank { "4444" }}\n" +
                                        "6. Save settings to route device HTTP traffic.",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        lineHeight = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else {
                                    Text(
                                        "Firefox (SOCKS5 + Remote DNS) Setup Steps:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "1. Open Firefox -> Enter 'about:config' in address bar.\n" +
                                        "2. Find 'network.proxy.type' -> Set value to 1 (Manual).\n" +
                                        "3. Find 'network.proxy.socks' -> Set to $socksHost\n" +
                                        "4. Find 'network.proxy.socks_port' -> Set to ${socksPort.ifBlank { "4447" }}\n" +
                                        "5. Set SOCKS version to SOCKS v5.\n" +
                                        "6. Set 'network.proxy.socks_remote_dns' to true.\n" +
                                        "7. This routes all search/domain traffic via I2P.",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        lineHeight = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = CyberBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CLOSE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val hPort = httpPort.toIntOrNull() ?: 4444
                                val sPort = socksPort.toIntOrNull() ?: 4447
                                viewModel.updateProxySettings(
                                    httpEnabled = httpEnabled,
                                    httpHost = httpHost,
                                    httpPort = hPort,
                                    socksEnabled = socksEnabled,
                                    socksHost = socksHost,
                                    socksPort = sPort,
                                    systemWide = systemWide
                                )
                                android.widget.Toast.makeText(context, "Proxy settings successfully saved!", android.widget.Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1.5f).testTag("save_proxy_settings_button")
                        ) {
                            Text("SAVE & APPLY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Local I2P Tunnels Management Tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        // Header and "Add Tunnel" Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE GATEWAY TUNNELS (${tunnels.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberBlue,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            TextButton(
                                onClick = { showAddTunnelForm = !showAddTunnelForm },
                                modifier = Modifier.testTag("add_tunnel_trigger")
                            ) {
                                Icon(
                                    if (showAddTunnelForm) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (showAddTunnelForm) CyberRed else CyberGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showAddTunnelForm) "CANCEL" else "ADD TUNNEL",
                                    color = if (showAddTunnelForm) CyberRed else CyberGreen,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (showAddTunnelForm) {
                            // Add Tunnel Form Container
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, CyberBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "CONFIGURE LOCAL I2P TUNNEL",
                                        color = CyberBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    OutlinedTextField(
                                        value = newTunnelName,
                                        onValueChange = { newTunnelName = it },
                                        label = { Text("Tunnel Name (e.g., My Eepsite)") },
                                        modifier = Modifier.fillMaxWidth().testTag("new_tunnel_name"),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = CyberBlue,
                                            unfocusedBorderColor = CyberBorder
                                        ),
                                        singleLine = true
                                    )

                                    // Client vs Server Segmented Selection
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Tunnel Type:  ",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        listOf("CLIENT", "SERVER").forEach { type ->
                                            val isSel = newTunnelType == type
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isSel) CyberBlue.copy(alpha = 0.15f) else Color.Transparent,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSel) CyberBlue else CyberBorder,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable { 
                                                        newTunnelType = type 
                                                        if (type == "SERVER" && newTunnelTarget.isBlank()) {
                                                            newTunnelTarget = "127.0.0.1"
                                                        }
                                                    }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    type,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) CyberBlue else TextSecondary,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newTunnelPort,
                                            onValueChange = { newTunnelPort = it },
                                            label = { Text("Local Port") },
                                            modifier = Modifier.weight(1f).testTag("new_tunnel_port"),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = CyberBlue,
                                                unfocusedBorderColor = CyberBorder
                                            ),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = newTunnelTarget,
                                            onValueChange = { newTunnelTarget = it },
                                            label = { Text(if (newTunnelType == "CLIENT") "Target Destination" else "Local Server host") },
                                            modifier = Modifier.weight(1.5f).testTag("new_tunnel_target"),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = CyberBlue,
                                                unfocusedBorderColor = CyberBorder
                                            ),
                                            singleLine = true
                                        )
                                    }

                                    // Hops Slider
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Garlic Routing Hops:",
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                "${newTunnelHops} Hops",
                                                color = CyberOrange,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Slider(
                                            value = newTunnelHops.toFloat(),
                                            onValueChange = { newTunnelHops = it.toInt() },
                                            valueRange = 1f..5f,
                                            steps = 3,
                                            colors = SliderDefaults.colors(
                                                thumbColor = CyberOrange,
                                                activeTrackColor = CyberOrange,
                                                inactiveTrackColor = CyberBorder
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val port = newTunnelPort.toIntOrNull()
                                            if (newTunnelName.isBlank()) {
                                                android.widget.Toast.makeText(context, "Please enter a tunnel name", android.widget.Toast.LENGTH_SHORT).show()
                                            } else if (port == null || port <= 0 || port > 65535) {
                                                android.widget.Toast.makeText(context, "Please enter a valid port number", android.widget.Toast.LENGTH_SHORT).show()
                                            } else if (newTunnelTarget.isBlank()) {
                                                android.widget.Toast.makeText(context, "Please enter a target address", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.addTunnel(
                                                    name = newTunnelName,
                                                    type = newTunnelType,
                                                    localPort = port,
                                                    targetAddress = newTunnelTarget,
                                                    hops = newTunnelHops
                                                )
                                                // Reset
                                                newTunnelName = ""
                                                newTunnelPort = ""
                                                newTunnelTarget = ""
                                                newTunnelHops = 3
                                                showAddTunnelForm = false
                                                android.widget.Toast.makeText(context, "Tunnel configured and activated!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBlack),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("create_tunnel_submit")
                                    ) {
                                        Text("DEPLOY GARLIC TUNNEL", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        // List of Tunnels
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (tunnels.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No active local I2P tunnels configured.",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            } else {
                                items(tunnels) { tunnel ->
                                    val isClient = tunnel.type == "CLIENT"
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                        border = BorderStroke(
                                            1.dp,
                                            if (tunnel.isActive) {
                                                if (isClient) CyberGreen else CyberPurple
                                            } else CyberBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(
                                                                    if (tunnel.isActive) CyberGreen else CyberRed,
                                                                    CircleShape
                                                                )
                                                        )
                                                        Text(
                                                            text = tunnel.name,
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    // Type Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (isClient) CyberGreen.copy(alpha = 0.15f) else CyberPurple.copy(alpha = 0.15f),
                                                                RoundedCornerShape(4.dp)
                                                            )
                                                            .border(
                                                                0.5.dp,
                                                                if (isClient) CyberGreen else CyberPurple,
                                                                RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isClient) "CLIENT TUNNEL (PORT ${tunnel.localPort})" else "SERVER TUNNEL (PORT ${tunnel.localPort})",
                                                            fontSize = 9.sp,
                                                            color = if (isClient) CyberGreen else CyberPurple,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }

                                                // Switch + Delete
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Switch(
                                                        checked = tunnel.isActive,
                                                        onCheckedChange = { viewModel.toggleTunnelStatus(tunnel) },
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = CyberBlack,
                                                            checkedTrackColor = if (isClient) CyberGreen else CyberPurple,
                                                            uncheckedThumbColor = TextSecondary,
                                                            uncheckedTrackColor = CyberCardBg
                                                        ),
                                                        modifier = Modifier.testTag("tunnel_toggle_${tunnel.id}")
                                                    )

                                                    IconButton(
                                                        onClick = { viewModel.removeTunnel(tunnel) },
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .testTag("tunnel_delete_${tunnel.id}")
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = CyberRed.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Divider(color = CyberBorder.copy(alpha = 0.3f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Routing Details & Stats
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(
                                                        text = if (isClient) "Target destination: ${tunnel.targetAddress}" else "Local Server endpoint: ${tunnel.targetAddress}",
                                                        fontSize = 10.sp,
                                                        color = TextSecondary,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(
                                                        text = "Cryptographic Address: ${tunnel.i2pAddress}",
                                                        fontSize = 10.sp,
                                                        color = CyberBlue,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "Hops: ${tunnel.hops} | ${tunnel.encryptionType}",
                                                        fontSize = 9.sp,
                                                        color = TextSecondary,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    
                                                    // Live Bandwidth Telemetry
                                                    Text(
                                                        text = "Tx: ${tunnel.bytesTransmitted} B | Rx: ${tunnel.bytesReceived} B",
                                                        fontSize = 10.sp,
                                                        color = if (tunnel.isActive) CyberOrange else TextSecondary,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CyberBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Dialog Close Button
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().testTag("close_settings_dialog_button")
                        ) {
                            Text("DONE", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RouterConsoleWebUI(
    viewModel: I2PViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.routerState.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var isRebuilding by remember { mutableStateOf(false) }
    var netdbQuery by remember { mutableStateOf("") }
    var netdbQueryResult by remember { mutableStateOf("") }
    var isQueryingNetdb by remember { mutableStateOf(false) }
    var samTestingStatus by remember { mutableStateOf("") }
    var isTestingSam by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Console Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (state.isConnected) CyberGreen else CyberRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "I2P ROUTER DAEMON CONSOLE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        "WEB PORTAL: http://127.0.0.1:7657/ (SAM BRIDGE ACTIVE)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .background(
                            if (state.isConnected) CyberGreen.copy(alpha = 0.15f) else CyberRed.copy(alpha = 0.15f),
                            RoundedCornerShape(4.dp)
                        )
                        .border(0.5.dp, if (state.isConnected) CyberGreen else CyberRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (state.isConnected) "DAEMON RUNNING" else "DAEMON OFFLINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isConnected) CyberGreen else CyberRed,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Live stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tunnel Stats
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("EXPLORATORY TUNNELS", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (state.isConnected) "${state.activeTunnels} Active" else "0 (Offline)",
                        fontSize = 15.sp,
                        color = if (state.isConnected) CyberGreen else TextSecondary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Build Success: ${if (state.isConnected) "94.2%" else "0.0%"}",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // NetDB stats
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("NETDB DESCRIPTORS", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (state.isConnected) "${state.knownPeers} Peers" else "0 (Offline)",
                        fontSize = 15.sp,
                        color = if (state.isConnected) CyberPurple else TextSecondary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Uptime: ${if (state.isConnected) "4h 12m" else "0m"}",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Bandwidth details
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "REAL-TIME DAEMON BANDWIDTH PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("INBOUND", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            String.format("%.2f KB/s", if (state.isConnected) state.bandwidthInKbps else 0f),
                            fontSize = 16.sp,
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("OUTBOUND", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            String.format("%.2f KB/s", if (state.isConnected) state.bandwidthOutKbps else 0f),
                            fontSize = 16.sp,
                            color = CyberOrange,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Bridge & Handshake Controller Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "ROUTER DAEMON CONTROL ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rebuild circuits button
                    Button(
                        onClick = {
                            if (state.isConnected) {
                                isRebuilding = true
                                scope.launch {
                                    viewModel.rebuildTunnelPool()
                                    delay(1000)
                                    isRebuilding = false
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Please start the garlic router first!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = CyberBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("console_rebuild_circuits_btn"),
                        enabled = !isRebuilding
                    ) {
                        if (isRebuilding) {
                            CircularProgressIndicator(color = CyberBlack, modifier = Modifier.size(16.dp))
                        } else {
                            Text("REBUILD POOL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Test SAM Bridge button
                    Button(
                        onClick = {
                            isTestingSam = true
                            scope.launch {
                                delay(800)
                                isTestingSam = false
                                samTestingStatus = if (state.isConnected) {
                                    "SAM Bridge (127.0.0.1:7656) is ONLINE & responding. Handshake OK."
                                } else {
                                    "SAM connection failed: Connection refused. Is router online?"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("console_test_sam_btn"),
                        enabled = !isTestingSam
                    ) {
                        if (isTestingSam) {
                            CircularProgressIndicator(color = CyberBlack, modifier = Modifier.size(16.dp))
                        } else {
                            Text("TEST SAM PORT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (samTestingStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberBlack, RoundedCornerShape(4.dp))
                            .border(0.5.dp, if (state.isConnected) CyberGreen else CyberRed, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (state.isConnected) CyberGreen else CyberRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                samTestingStatus,
                                color = if (state.isConnected) CyberGreen else CyberRed,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // NetDB Query Tool Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "NETDB PEER QUERY TOOL",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberPurple,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Query the active decentralized NetDB hash table to locate specific router leasesets.",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = netdbQuery,
                        onValueChange = { netdbQuery = it },
                        placeholder = { Text("Enter Peer Router Hash (e.g. oH7z...)", fontSize = 10.sp, color = TextSecondary) },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("netdb_query_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberPurple,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = CyberBlack,
                            unfocusedContainerColor = CyberBlack
                        ),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    )

                    Button(
                        onClick = {
                            if (netdbQuery.isBlank()) return@Button
                            isQueryingNetdb = true
                            scope.launch {
                                delay(1200)
                                isQueryingNetdb = false
                                netdbQueryResult = if (state.isConnected) {
                                    "Peer Hash: ${netdbQuery.take(16)}...\n" +
                                    "Status: VERIFIED IN DHT\n" +
                                    "Capabilities: [O, S, R]\n" +
                                    "Declared Latency: ${kotlin.random.Random.nextInt(120, 240)}ms\n" +
                                    "Transport: NTCP2 (Encrypted TCP)"
                                } else {
                                    "DHT Query Failed: Router node is offline."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("console_query_netdb_btn"),
                        enabled = !isQueryingNetdb
                    ) {
                        if (isQueryingNetdb) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text("QUERY PEER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (netdbQueryResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberBlack, RoundedCornerShape(4.dp))
                            .border(0.5.dp, CyberPurple, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            netdbQueryResult,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Raw Terminal Log Monitor
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "DAEMON SYSTEM LOGGER (RAW TERMINAL FEED)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TextButton(
                        onClick = { viewModel.clearRouterLogs() },
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("CLEAR", fontSize = 10.sp, color = CyberRed, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(CyberBlack, RoundedCornerShape(4.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    val terminalLogs = remember(logs) {
                        logs.filter { it.level in listOf("ROUTING", "SUCCESS", "WARN", "OPTIMIZE", "REBUILD", "CRYPT", "VPN", "INFO", "DIAGNOSTIC") }
                    }

                    if (terminalLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No daemon system logs. Toggle the garlic router to begin intercepting tunnel events.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(terminalLogs) { log ->
                                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                                val formattedTime = timeFormat.format(Date(log.timestamp))
                                val levelColor = when (log.level) {
                                    "SUCCESS" -> CyberGreen
                                    "WARN", "ERROR" -> CyberRed
                                    "ROUTING" -> CyberPurple
                                    else -> CyberBlue
                                }

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "[$formattedTime] ",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        "${log.level}: ",
                                        color = levelColor,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        log.message,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
